package com.selfblog.service.auth.impl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.auth.EmailService;
import com.selfblog.service.auth.EmailService.VerificationType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S2 回归：验证码不得进入日志，且发送失败后必须立刻作废。
 *
 * <p>修复前的两个缺陷：</p>
 * <ol>
 *   <li>{@code sendHtmlEmail} 的纯文本回退分支里 {@code log.info("📧 邮件发送失败，验证码已生成：{}", code)}
 *       把**明文验证码**和**完整收件人**写进日志——任何能读日志的人都能拿它重置管理员密码；</li>
 *   <li>{@code sendVerificationCode} 只 {@code catch (MailException)}，而 {@code sendHtmlEmail}
 *       抛的是 {@code BusinessException}，于是它逃逸出去、{@code verificationStore} 里那条
 *       已进日志的验证码仍然可兑换 5 分钟。</li>
 * </ol>
 *
 * <p>本测试不依赖「猜验证码」：在 {@code mailSender.send} 被调用的那一刻（此时验证码仍在
 * store 里）通过反射把真实验证码取出来，然后断言它**没有**出现在任何日志行里，
 * 并且发送失败后整条记录已被移除、无法再兑换。</p>
 */
@DisplayName("S2：验证码日志泄露与失败后作废")
class EmailServiceImplOtpLeakTest {

    private static final String EMAIL = "admin-probe@example.com";

    private EmailServiceImpl emailService;
    private JavaMailSender mailSender;
    private SysUserMapper sysUserMapper;

    private ListAppender<ILoggingEvent> appender;
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl();
        mailSender = Mockito.mock(JavaMailSender.class);
        sysUserMapper = Mockito.mock(SysUserMapper.class);

        Mockito.when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        Mockito.when(sysUserMapper.selectCount(Mockito.any())).thenReturn(0L);

        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "sysUserMapper", sysUserMapper);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.example");
        ReflectionTestUtils.setField(emailService, "expireMinutes", 5);
        ReflectionTestUtils.setField(emailService, "maxVerifyAttempts", 3);
        ReflectionTestUtils.setField(emailService, "sendIntervalSeconds", 60);
        ReflectionTestUtils.setField(emailService, "devConsoleCode", false);

        appender = new ListAppender<>();
        appender.start();
        serviceLogger = (Logger) LoggerFactory.getLogger(EmailServiceImpl.class);
        serviceLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        if (serviceLogger != null && appender != null) {
            serviceLogger.detachAppender(appender);
        }
    }

    @Test
    @DisplayName("HTML 与纯文本都失败时：日志里既没有明文验证码，也没有完整收件人地址")
    void sendFailureLogsNeitherTheCodeNorTheFullAddress() {
        AtomicReference<String> liveCode = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            liveCode.set(readStoredCode(VerificationType.PASSWORD_RESET));
            throw new MailSendException("smtp down");
        }).when(mailSender).send(Mockito.any(MimeMessage.class));
        Mockito.doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(Mockito.any(SimpleMailMessage.class));

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> emailService.sendVerificationCode(EMAIL, VerificationType.PASSWORD_RESET));

        assertEquals(500, thrown.getCode(), "发送失败对外必须是 500 通用错误");
        assertNotNull(liveCode.get(), "测试前提：邮件发送时验证码确实已生成并入库");

        String messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));

        assertFalse(messages.contains(liveCode.get()),
                "明文验证码绝不能出现在日志里，实际日志：\n" + messages);
        assertFalse(messages.contains(EMAIL),
                "完整收件人地址不得出现在日志里，实际日志：\n" + messages);
        assertTrue(messages.contains("纯文本邮件发送也失败"),
                "必须仍然记录这次失败（否则本断言会因日志为空而空转），实际日志：\n" + messages);
    }

    @Test
    @DisplayName("发送失败后验证码立即作废：store 已清空，任何验证码都无法兑换")
    void sendFailureInvalidatesTheVerificationCode() {
        Mockito.doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(Mockito.any(MimeMessage.class));
        Mockito.doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(Mockito.any(SimpleMailMessage.class));

        assertThrows(BusinessException.class,
                () -> emailService.sendVerificationCode(EMAIL, VerificationType.PASSWORD_RESET));

        assertEquals(0, verificationStoreSize(), "失败后 verificationStore 必须已清空");
        BusinessException verifyFailure = assertThrows(BusinessException.class,
                () -> emailService.verifyCode(EMAIL, "ABC123", VerificationType.PASSWORD_RESET));
        assertEquals("验证码已失效，请重新获取", verifyFailure.getMessage(),
                "已发送失败的验证码不得再可兑换");
    }

    @Test
    @DisplayName("非 MailException 的发送失败同样作废验证码（原实现只捕 MailException）")
    void nonMailExceptionFailureAlsoInvalidatesTheCode() {
        // 模拟 HTML 阶段抛出一个运行时异常（原实现下会直接逃逸，store 里的验证码继续有效）。
        Mockito.doThrow(new IllegalStateException("unexpected smtp state"))
                .when(mailSender).send(Mockito.any(MimeMessage.class));

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> emailService.sendVerificationCode(EMAIL, VerificationType.PASSWORD_RESET));

        assertEquals(500, thrown.getCode());
        assertEquals(0, verificationStoreSize(), "任何发送失败都必须清空 store");
    }

    @Test
    @DisplayName("注册路径同样失败即作废，且日志不打印明文验证码")
    void registerPathFailureAlsoInvalidatesTheCodeWithoutLeaking() {
        AtomicReference<String> liveCode = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            liveCode.set(readStoredCode(VerificationType.REGISTER));
            throw new MailSendException("smtp down");
        }).when(mailSender).send(Mockito.any(MimeMessage.class));
        Mockito.doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(Mockito.any(SimpleMailMessage.class));

        assertThrows(BusinessException.class,
                () -> emailService.sendRegisterVerificationCode(EMAIL, "probe-author"));

        String messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
        assertNotNull(liveCode.get(), "测试前提：注册验证码确实已生成");
        assertFalse(messages.contains(liveCode.get()),
                "注册路径也不得打印明文验证码，实际日志：\n" + messages);
        assertFalse(messages.contains(EMAIL), "注册路径也不得打印完整收件人，实际日志：\n" + messages);
        assertEquals(0, verificationStoreSize(), "注册路径失败后必须清空 store");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> verificationStore() {
        Object store = ReflectionTestUtils.getField(emailService, "verificationStore");
        assertNotNull(store, "测试前提：verificationStore 字段存在");
        return (Map<String, Object>) store;
    }

    private int verificationStoreSize() {
        return verificationStore().size();
    }

    private String readStoredCode(VerificationType type) {
        Object data = verificationStore().get(type + ":" + EMAIL);
        if (data == null) {
            return null;
        }
        try {
            Field codeField = data.getClass().getDeclaredField("code");
            codeField.setAccessible(true);
            return (String) codeField.get(data);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法读取验证码字段", e);
        }
    }
}
