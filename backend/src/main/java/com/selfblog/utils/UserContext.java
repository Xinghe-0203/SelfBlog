package com.selfblog.utils;

/**
 * 用户上下文信息
 */
public class UserContext {
    private final Long userId;
    private final String role;

    public UserContext(Long userId, String role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
