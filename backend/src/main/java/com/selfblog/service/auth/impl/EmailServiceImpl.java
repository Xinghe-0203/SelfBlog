package com.selfblog.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.auth.EmailService;
import com.selfblog.utils.StringMaskUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final int MAX_DAILY_SEND_COUNT = 10; // 每邮箱每天最大发送次数

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Value("${mail.from:${spring.mail.username:noreply@campusblog.com}}")
    private String fromEmail;

    @Value("${mail.verification.expire-minutes:5}")
    private int expireMinutes;

    @Value("${mail.verification.max-verify-attempts:3}")
    private int maxVerifyAttempts;

    @Value("${mail.verification.send-interval-seconds:60}")
    private int sendIntervalSeconds;

    /**
     * 是否把验证码明文打印到控制台（S2）。默认关闭：
     * 生产环境任何人只要有日志读取权限就能拿它直接重置管理员密码，
     * 所以「开发期看控制台取码」必须是显式开启的行为。
     */
    @Value("${app.mail.dev-console-code:false}")
    private boolean devConsoleCode;

    // 验证码存储: type:email -> VerificationData (分开存储注册和密码重置验证码)
    private final Map<String, VerificationData> verificationStore = new ConcurrentHashMap<>();

    // 发送时间记录: type:email -> lastSendTime
    private final Map<String, Long> sendTimeStore = new ConcurrentHashMap<>();

    // 每日发送次数记录: email -> [sendCount, date]
    private final Map<String, DailySendCount> dailySendCountStore = new ConcurrentHashMap<>();

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    // 易混淆字符排除: 0, O, I, l, 1
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "verification-cleanup");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void init() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredVerifications, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        cleanupExecutor.shutdown();
    }

    private void cleanupExpiredVerifications() {
        long now = System.currentTimeMillis();
        verificationStore.entrySet().removeIf(entry -> {
            if (entry.getValue().expireTime < now) {
                return true;
            }
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length == 2) {
                Long lastSend = sendTimeStore.get(parts[0] + ":" + parts[1]);
                if (lastSend != null) {
                    long elapsed = (now - lastSend) / 1000;
                    return elapsed > sendIntervalSeconds * 10;
                }
            }
            return false;
        });
        sendTimeStore.entrySet().removeIf(entry -> {
            long elapsed = (now - entry.getValue()) / 1000;
            return elapsed > sendIntervalSeconds * 10;
        });
    }

    /**
     * 验证码数据结构
     */
    private static class VerificationData {
        String code;
        long expireTime;
        AtomicInteger attempts;

        VerificationData(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
            this.attempts = new AtomicInteger(0);
        }
    }

    /**
     * 每日发送计数数据结构
     */
    private static class DailySendCount {
        AtomicInteger count;
        int date; // yyyyMMdd格式

        DailySendCount(int date) {
            this.count = new AtomicInteger(0);
            this.date = date;
        }
    }

    @Override
    public boolean sendVerificationCode(String to, VerificationType type) {
        if (to == null || to.trim().isEmpty()) {
            throw new BusinessException(400, "邮箱地址不能为空");
        }

        String key = type + ":" + to;

        // 检查每日发送次数限制
        checkDailySendLimit(to);

        String code;
        synchronized (sendTimeStore) {
            Long currentSendTime = sendTimeStore.get(key);
            if (currentSendTime != null) {
                long elapsed = (System.currentTimeMillis() - currentSendTime) / 1000;
                if (elapsed < sendIntervalSeconds) {
                    throw new BusinessException(429, "发送太频繁，请" + (sendIntervalSeconds - elapsed) + "秒后再试");
                }
            }

            code = generateSecureCode();
            long expireTime = System.currentTimeMillis() + expireMinutes * 60 * 1000L;

            verificationStore.put(key, new VerificationData(code, expireTime));
            sendTimeStore.put(key, System.currentTimeMillis());
        }

        String correlationId = generateCorrelationId();
        try {
            String subject = type == VerificationType.REGISTER
                    ? "SelfBlog - 注册验证码"
                    : "SelfBlog - 密码找回验证码";
            sendHtmlEmail(to, code, subject, correlationId);
            incrementDailySendCount(to);
            log.info("验证码已发送至: {}, type={}, correlationId={}", StringMaskUtils.maskEmail(to), type, correlationId);
            return true;
        } catch (Exception e) {
            // S2：任何发送失败（MailException、BusinessException、MessagingException 逃逸……）
            // 都必须在抛出前把验证码作废，否则一个已进日志/异常链的码还能在 5 分钟内被兑换。
            // 这里捕获 Exception 而不是只捕 MailException，正是原缺陷的根因。
            log.error("发送验证码失败: type={}, correlationId={}, error={}", type, correlationId, e.getMessage());
            verificationStore.remove(key);
            sendTimeStore.remove(key);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(500, "邮件发送失败，请稍后重试");
        }
    }

    @Override
    public boolean sendRegisterVerificationCode(String to, String username) {
        if (to == null || to.trim().isEmpty()) {
            throw new BusinessException(400, "邮箱地址不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }

        if (isEmailRegistered(to)) {
            throw new BusinessException(400, "该邮箱已被注册");
        }

        String key = VerificationType.REGISTER + ":" + to;

        // 检查每日发送次数限制
        checkDailySendLimit(to);

        String code;
        synchronized (sendTimeStore) {
            Long currentSendTime = sendTimeStore.get(key);
            if (currentSendTime != null) {
                long elapsed = (System.currentTimeMillis() - currentSendTime) / 1000;
                if (elapsed < sendIntervalSeconds) {
                    throw new BusinessException(429, "发送太频繁，请" + (sendIntervalSeconds - elapsed) + "秒后再试");
                }
            }

            code = generateSecureCode();
            long expireTime = System.currentTimeMillis() + expireMinutes * 60 * 1000L;

            verificationStore.put(key, new VerificationData(code, expireTime));
            sendTimeStore.put(key, System.currentTimeMillis());
        }

        String correlationId = generateCorrelationId();
        try {
            sendHtmlEmail(to, code, "SelfBlog - 注册验证码", correlationId);
            incrementDailySendCount(to);
            log.info("注册验证码已发送至: {}, username={}, correlationId={}", StringMaskUtils.maskEmail(to), username, correlationId);
            return true;
        } catch (Exception e) {
            // S2：注册路径同样「失败即作废」。原实现故意保留验证码并在日志里打印明文，
            // 等于把一条可用的注册凭据写进日志；注册已由单人作者模式关闭，这条路径
            // 生产不可达，但不能留一个会写明文凭据的日志分支。
            log.error("发送注册验证码失败: username={}, correlationId={}, error={}", username, correlationId, e.getMessage());
            verificationStore.remove(key);
            sendTimeStore.remove(key);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(500, "邮件发送失败，请稍后重试");
        }
    }

    /**
     * 生成一次发送尝试的关联 id，用于在不记录验证码/完整邮箱的前提下串联日志。
     */
    private String generateCorrelationId() {
        byte[] bytes = new byte[8];
        SECURE_RANDOM.nextBytes(bytes);
        return java.util.HexFormat.of().formatHex(bytes);
    }

    @Override
    public boolean verifyCode(String email, String code, VerificationType type) {
        if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            throw new BusinessException(400, "邮箱和验证码不能为空");
        }

        String key = type + ":" + email;
        VerificationData data;

        // 同步块保证原子性：先比对，成功则删除并返回true；失败则递增计数
        synchronized (verificationStore) {
            data = verificationStore.get(key);
            if (data == null) {
                throw new BusinessException(400, "验证码已失效，请重新获取");
            }

            if (System.currentTimeMillis() > data.expireTime) {
                verificationStore.remove(key);
                throw new BusinessException(400, "验证码已过期，请重新获取");
            }

            // 先比对验证码，再递增尝试次数，防止时序攻击
            if (Objects.equals(data.code, code)) {
                verificationStore.remove(key);
                return true;
            }

            int currentAttempts = data.attempts.incrementAndGet();

            if (currentAttempts >= maxVerifyAttempts) {
                verificationStore.remove(key);
                throw new BusinessException(400, "验证失败次数过多，请重新获取验证码");
            }

            throw new BusinessException(400, "验证码错误，剩余" + (maxVerifyAttempts - currentAttempts) + "次尝试机会");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getEmail, email.trim());
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    /**
     * 生成安全的6位混合验证码（排除易混淆字符）
     */
    private String generateSecureCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(SECURE_RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    /**
     * 检查每日发送次数限制
     */
    private void checkDailySendLimit(String email) {
        int today = Integer.parseInt(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")));
        DailySendCount dailyCount = dailySendCountStore.computeIfAbsent(email, k -> new DailySendCount(today));

        // 日期变更检查，重置计数
        if (dailyCount.date != today) {
            dailyCount = new DailySendCount(today);
            dailySendCountStore.put(email, dailyCount);
        }

        if (dailyCount.count.get() >= MAX_DAILY_SEND_COUNT) {
            throw new BusinessException(429, "该邮箱今日发送次数已用完，请明天再试");
        }
    }

    /**
     * 增加每日发送计数
     */
    private void incrementDailySendCount(String email) {
        int today = Integer.parseInt(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")));
        DailySendCount dailyCount = dailySendCountStore.computeIfAbsent(email, k -> new DailySendCount(today));

        if (dailyCount.date != today) {
            dailyCount = new DailySendCount(today);
            dailySendCountStore.put(email, dailyCount);
        }

        dailyCount.count.incrementAndGet();
    }

    /**
     * 发送HTML格式邮件
     */
    private void sendHtmlEmail(String to, String code, String subject, String correlationId) {
        if (devConsoleCode) {
            // S2：仅本地开发使用。SMTP 通常没有配置，这里不发信、只把验证码打到控制台，
            // 并且该分支由 app.mail.dev-console-code 显式开启（默认 false），
            // 因此生产环境不存在「验证码进日志」的路径。
            log.warn("[DEV ONLY] 跳过邮件发送（app.mail.dev-console-code=true）: correlationId={}, email={}, subject={}, code={}",
                    correlationId, StringMaskUtils.maskEmail(to), subject, code);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlContent = buildEmailTemplate(code, subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MailException | MessagingException e) {
            log.warn("HTML邮件发送失败，尝试发送纯文本邮件: correlationId={}, error={}", correlationId, e.getMessage());
            try {
                SimpleMailMessage simpleMessage = new SimpleMailMessage();
                simpleMessage.setFrom(fromEmail);
                simpleMessage.setTo(to);
                simpleMessage.setSubject(subject);
                simpleMessage.setText("您的验证码是: " + code + "\n验证码 " + expireMinutes + " 分钟内有效，请勿泄露给他人。\n\n如非本人操作，请忽略此邮件。");
                mailSender.send(simpleMessage);
            } catch (MailException ex) {
                // S2：这里绝不打印验证码与完整收件人地址——日志读取者拿到验证码即可重置管理员密码。
                // 只留一个关联 id 与脱敏地址，便于按 correlationId 追查同一次发送。
                log.error("纯文本邮件发送也失败: correlationId={}, email={}, error={}",
                        correlationId, StringMaskUtils.maskEmail(to), ex.getMessage());
                throw new BusinessException(500, "邮件发送失败，请稍后重试");
            }
        }
    }

    /**
     * 构建邮件模板
     */
    private String buildEmailTemplate(String code, String subject) {
        boolean isPasswordReset = subject != null && subject.contains("密码找回");
        String actionText = isPasswordReset ? "密码找回" : "注册";
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <style>\n" +
                "        body { font-family: 'Microsoft YaHei', Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }\n" +
                "        .container { max-width: 500px; margin: 0 auto; background: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n" +
                "        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }\n" +
                "        .header h1 { margin: 0; font-size: 24px; font-weight: 500; }\n" +
                "        .content { padding: 40px 30px; text-align: center; }\n" +
                "        .code-box { background: #f8f9fa; border: 2px dashed #667eea; border-radius: 8px; padding: 20px 40px; display: inline-block; margin: 20px 0; }\n" +
                "        .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 8px; }\n" +
                "        .tip { color: #666; font-size: 14px; line-height: 1.6; }\n" +
                "        .footer { background: #f5f5f5; color: #999; font-size: 12px; text-align: center; padding: 15px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>SelfBlog</h1>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <p>您好，</p>\n" +
                "            <p>您正在进行" + actionText + "操作，请使用以下验证码：</p>\n" +
                "            <div class=\"code-box\">\n" +
                "                <span class=\"code\">" + code + "</span>\n" +
                "            </div>\n" +
                "            <p class=\"tip\">验证码在 <strong>" + expireMinutes + " 分钟</strong> 内有效，请勿泄露给他人。</p>\n" +
                "            <p class=\"tip\">如非本人操作，请忽略此邮件。</p>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <p>此邮件由系统自动发出，请勿回复。</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}