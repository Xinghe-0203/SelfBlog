package com.selfblog.common.enums;

/**
 * 文章状态枚举
 * 0=待审核, 1=已发布, 2=已驳回
 */
public enum PostStatus {
    PENDING_REVIEW(0, "待审核"),
    PUBLISHED(1, "已发布"),
    REJECTED(2, "已驳回");

    private final int value;
    private final String description;

    PostStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static PostStatus fromValue(int value) {
        for (PostStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public boolean isPublished() {
        return this == PUBLISHED;
    }

    public boolean isPendingReview() {
        return this == PENDING_REVIEW;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }
}