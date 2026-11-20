package com.selfblog.common.enums;

/**
 * 基础错误码枚举
 * 统一管理所有业务错误码，便于维护和扩展
 *
 * <p>2026-11-21 审计清理：原枚举里的通用码（400/401/403/404/409/429）与
 * 1001-9004 领域码在仓库里零引用（grep 验证，唯一的调用方是
 * {@code BusinessException} 的 {@code BaseErrorCode.FAILURE} 默认值），
 * 业务错误码实际以裸 int + {@code BusinessException(code, message)} 的形式使用。
 * 死常量已删除，仅保留 {@link #SUCCESS}/{@link #FAILURE} 与 {@link #fromCode(int)}。
 * 新增领域错误码时优先直接使用 HTTP 语义码，勿再回填本枚举。</p>
 */
public enum BaseErrorCode {
    SUCCESS(200, "操作成功"),
    FAILURE(500, "操作失败");

    private final int code;
    private final String message;

    BaseErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 根据错误码获取枚举值
     *
     * @param code 错误码
     * @return 对应的错误码枚举，如果不存在则返回FAILURE
     */
    public static BaseErrorCode fromCode(int code) {
        for (BaseErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return FAILURE;
    }
}
