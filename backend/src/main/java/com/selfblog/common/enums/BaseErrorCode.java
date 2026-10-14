package com.selfblog.common.enums;

/**
 * 基础错误码枚举
 * 统一管理所有业务错误码，便于维护和扩展
 */
public enum BaseErrorCode {
    // 通用错误码
    SUCCESS(200, "操作成功"),
    FAILURE(500, "操作失败"),
    INVALID_PARAM(400, "参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // 用户相关错误码 (1000-1999)
    USERNAME_EXISTS(1001, "用户名已存在"),
    EMAIL_EXISTS(1002, "邮箱已存在"),
    USER_NOT_FOUND(1003, "用户不存在"),
    INVALID_CREDENTIALS(1004, "用户名或密码错误"),
    ACCOUNT_LOCKED(1005, "账户被锁定"),
    ACCOUNT_DISABLED(1006, "账户已被禁用"),
    TOKEN_EXPIRED(1007, "令牌已过期"),
    INVALID_TOKEN(1008, "无效令牌"),
    REFRESH_TOKEN_REQUIRED(1009, "需要刷新令牌"),
    INVALID_REFRESH_TOKEN(1010, "无效刷新令牌"),
    REFRESH_TOKEN_USED(1011, "刷新令牌已被使用"),
    VERIFICATION_CODE_ERROR(1012, "验证码错误"),
    VERIFICATION_CODE_EXPIRED(1013, "验证码已过期"),
    VERIFICATION_CODE_SEND_TOO_FREQUENT(1014, "验证码发送过于频繁"),

    // 文章相关错误码 (2000-2999)
    POST_NOT_FOUND(2001, "文章不存在"),
    POST_UNAUTHORIZED(2002, "无权操作此文章"),
    POST_DRAFT_NOT_FOUND(2003, "草稿不存在"),
    POST_DRAFT_UNAUTHORIZED(2004, "无权操作此草稿"),
    POST_TITLE_REQUIRED(2005, "文章标题不能为空"),
    POST_CONTENT_REQUIRED(2006, "文章内容不能为空"),
    POST_STATUS_INVALID(2007, "文章状态无效"),

    // 评论相关错误码 (3000-3999)
    COMMENT_NOT_FOUND(3001, "评论不存在"),
    COMMENT_UNAUTHORIZED(3002, "无权操作此评论"),
    COMMENT_CONTENT_REQUIRED(3003, "评论内容不能为空"),
    COMMENT_POST_NOT_FOUND(3004, "关联的文章不存在"),

    // 私信相关错误码 (4000-4999)
    MESSAGE_SEND_FAILED(4001, "私信发送失败"),
    MESSAGE_NOT_FOUND(4002, "私信不存在"),
    MESSAGE_UNAUTHORIZED(4003, "无权操作此私信"),
    MESSAGE_RECEIVER_BANNED(4004, "接收者已被封禁"),
    MESSAGE_SELF_SEND(4005, "不能给自己发私信"),

    // 关注相关错误码 (5000-5999)
    FOLLOW_SELF(5001, "不能关注自己"),
    FOLLOW_NOT_FOUND(5002, "关注关系不存在"),
    FOLLOW_USER_NOT_FOUND(5003, "目标用户不存在"),

    // 举报相关错误码 (6000-6999)
    REPORT_NOT_FOUND(6001, "举报记录不存在"),
    REPORT_ALREADY_EXISTS(6002, "已存在相同举报"),
    REPORT_UNAUTHORIZED(6003, "无权操作此举报"),
    REPORT_TARGET_NOT_FOUND(6004, "举报目标不存在"),

    // 话题相关错误码 (7000-7999)
    TOPIC_NOT_FOUND(7001, "话题不存在"),
    TOPIC_NAME_EXISTS(7002, "话题名称已存在"),
    TOPIC_UNAUTHORIZED(7003, "无权操作此话题"),

    // 校友圈相关错误码 (8000-8999)
    CIRCLE_NOT_FOUND(8001, "校友圈不存在"),
    CIRCLE_POST_NOT_FOUND(8002, "校友圈帖子不存在"),
    CIRCLE_UNAUTHORIZED(8003, "无权操作此校友圈"),

    // 收藏点赞相关错误码 (9000-9999)
    COLLECT_NOT_FOUND(9001, "收藏记录不存在"),
    COLLECT_ALREADY_EXISTS(9002, "已收藏"),
    LIKE_NOT_FOUND(9003, "点赞记录不存在"),
    LIKE_ALREADY_EXISTS(9004, "已点赞");

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