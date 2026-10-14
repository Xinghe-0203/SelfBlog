package com.selfblog.common.enums;

/**
 * 举报状态枚举
 * 0=待处理, 1=已驳回, 2=已核实
 */
public enum ReportStatus {
    PENDING(0, "待处理"),
    REJECTED(1, "已驳回"),
    RESOLVED(2, "已核实");

    private final int value;
    private final String description;

    ReportStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static ReportStatus fromValue(int value) {
        for (ReportStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isResolved() {
        return this == RESOLVED;
    }

    public boolean isRejected() {
        return this == REJECTED;
    }
}