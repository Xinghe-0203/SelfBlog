package com.selfblog.common.enums;

/**
 * 用户状态枚举
 * 0=禁用, 1=正常
 */
public enum UserStatus {
    DISABLED(0, "禁用"),
    NORMAL(1, "正常");

    private final int value;
    private final String description;

    UserStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static UserStatus fromValue(int value) {
        for (UserStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public boolean isNormal() {
        return this == NORMAL;
    }

    public boolean isDisabled() {
        return this == DISABLED;
    }
}