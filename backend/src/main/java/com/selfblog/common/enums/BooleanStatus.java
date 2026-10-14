package com.selfblog.common.enums;

/**
 * 布尔状态枚举
 * 用于 allow_repost, allow_comment 等字段
 * 0=否(false), 1=是(true)
 */
public enum BooleanStatus {
    DISABLE(0, "禁用"),
    ENABLE(1, "启用");

    private final int value;
    private final String description;

    BooleanStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static BooleanStatus fromValue(int value) {
        for (BooleanStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public boolean isEnabled() {
        return this == ENABLE;
    }

    public boolean isDisabled() {
        return this == DISABLE;
    }
}