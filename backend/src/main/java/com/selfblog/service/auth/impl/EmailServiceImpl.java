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

        try {
            String subject = type == VerificationType.REGISTER
                    ? "SelfBlog - 注册验证码"
                    : "SelfBlog - 密码找回验证码";
            sendHtmlEmail(to, code, subject);
            incrementDailySendCount(to);
            log.info("验证码已发送至: {}, type={}", StringMaskUtils.maskEmail(to), type);
            return true;
        } catch (MailException e) {
            log.error("发送验证码失败: {}", e.getMessage());
            verificationStore.remove(key);
            sendTimeStore.remove(key);
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

        try {
            sendHtmlEmail(to, code, "SelfBlog - 注册验证码");
            incrementDailySendCount(to);
            log.info("注册验证码已发送至: {}, username={}", StringMaskUtils.maskEmail(to), username);
            return true;
        } catch (BusinessException e) {
            // sendHtmlEmail 内部已经打印验证码到控制台
            // 不删除 verificationStore，允许用户在开发模式使用控制台打印的验证码
            log.info("邮件发送失败但验证码仍有效（可用于开发测试）: {} -> {}", to, code);
            throw e;
        } catch (Exception e) {
            // 捕获其他异常（如 MailException、MessagingException 逃逸或运行时异常）
            log.error("发送邮件时发生未预期异常: {}", e.getMessage());
            // 清理验证码存储，防止内存泄漏
            verificationStore.remove(key);
            sendTimeStore.remove(key);
            log.info("邮件发送失败但验证码仍有效（可用于开发测试）: {} -> {}", to, code);
            throw new BusinessException(500, "邮件发送失败，请检查SMTP配置。开发模式验证码已打印在控制台。");
        }
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
    private void sendHtmlEmail(String to, String code, String subject) {
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
            log.warn("HTML邮件发送失败，尝试发送纯文本邮件: {}", e.getMessage());
            try {
                SimpleMailMessage simpleMessage = new SimpleMailMessage();
                simpleMessage.setFrom(fromEmail);
                simpleMessage.setTo(to);
                simpleMessage.setSubject(subject);
                simpleMessage.setText("您的验证码是: " + code + "\n验证码 " + expireMinutes + " 分钟内有效，请勿泄露给他人。\n\n如非本人操作，请忽略此邮件。");
                mailSender.send(simpleMessage);
            } catch (MailException ex) {
                log.error("纯文本邮件发送也失败: {}", ex.getMessage());
                log.info("============================================");
                log.info("📧 邮件发送失败，验证码已生成：{}", code);
                log.info("   收件人: {}", to);
                log.info("   有效期: {} 分钟", expireMinutes);
                log.info("   请在开发环境中使用此验证码完成注册");
                log.info("============================================");
                throw new BusinessException(500, "邮件发送失败，请检查SMTP配置。开发模式验证码已打印在控制台。");
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