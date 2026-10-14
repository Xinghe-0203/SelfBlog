package com.selfblog.utils;

/**
 * 字符串脱敏工具类
 */
public class StringMaskUtils {

    /**
     * 邮箱脱敏处理，将邮箱前缀部分替换为星号
     * 例：zhangsan@gmail.com → z***n@gmail.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        int len = local.length();
        if (len <= 0) {
            return email;
        }
        if (local.isEmpty()) return email;
        if (len <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(len - 1) + "@" + domain;
    }
}
