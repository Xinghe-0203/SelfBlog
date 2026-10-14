package com.selfblog.common.enums;

/**
 * 可见性状态枚举
 * 用于 circle_post.visibility 字段
 * 0=公开, 1=仅关注者, 2=仅自己
 */
public enum Visibility {
    PUBLIC(0, "公开"),
    FOLLOWERS(1, "仅关注者"),
    PRIVATE(2, "仅自己");

    private final int value;
    private final String description;

    Visibility(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static Visibility fromValue(int value) {
        for (Visibility status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public boolean isPublic() {
        return this == PUBLIC;
    }

    public boolean isFollowers() {
        return this == FOLLOWERS;
    }

    public boolean isPrivate() {
        return this == PRIVATE;
    }
}