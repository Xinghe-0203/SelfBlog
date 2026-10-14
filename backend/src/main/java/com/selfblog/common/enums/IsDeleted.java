package com.selfblog.common.enums;

/**
 * 逻辑删除状态枚举
 * 0=正常, 1=删除
 */
public enum IsDeleted {
    NORMAL(0, "正常"),
    DELETED(1, "已删除");

    private final int value;
    private final String description;

    IsDeleted(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static IsDeleted fromValue(int value) {
        for (IsDeleted status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public boolean isDeleted() {
        return this == DELETED;
    }

    public boolean isNormal() {
        return this == NORMAL;
    }
}