package com.selfblog.service.auth;

/**
 * 邮件服务接口
 */
public interface EmailService {

    enum VerificationType {
        REGISTER,    // 注册验证码
        PASSWORD_RESET // 密码重置验证码
    }

    /**
     * 发送验证码到指定邮箱
     * @param to 收件人邮箱
     * @param type 验证码类型
     * @return 是否发送成功
     */
    boolean sendVerificationCode(String to, VerificationType type);

    /**
     * 发送注册验证码（验证邮箱真实性）
     * @param to 收件人邮箱
     * @param username 用户名（用于验证）
     * @return 是否发送成功
     */
    boolean sendRegisterVerificationCode(String to, String username);

    /**
     * 验证验证码是否正确
     * @param email 邮箱
     * @param code 验证码
     * @param type 验证码类型
     * @return 是否验证通过
     */
    boolean verifyCode(String email, String code, VerificationType type);

    /**
     * 检查邮箱是否已注册
     * @param email 邮箱
     * @return 是否已注册
     */
    boolean isEmailRegistered(String email);
}