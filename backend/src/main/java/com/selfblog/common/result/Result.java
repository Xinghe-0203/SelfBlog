package com.selfblog.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一API响应结果封装类
 * 【设计说明】
 *   前后端分离项目中，所有接口返回统一的 JSON 格式。
 *   前端通过 code 判断请求是否成功，message 获取提示信息，data 获取业务数据。
 *
 * @param <T> 返回数据的类型
 */
@Data
@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     * 200 = 成功
     * 500 = 失败
     * 401 = 未登录
     * 403 = 无权限
     */
    @Schema(description = "响应状态码：200=成功，500=失败，401=未登录，403=无权限")
    private Integer code;

    /**
     * 响应信息
     * 成功时返回"操作成功"，失败时返回具体错误信息
     */
    @Schema(description = "响应信息")
    private String message;

    /**
     * 响应数据
     * 可以是任意对象、List、Map 等
     */
    @Schema(description = "响应数据")
    private T data;

    /**
     * 时间戳
     */
    @Schema(description = "响应时间戳")
    private long timestamp;

    /**
     * 私有构造方法，防止直接实例化
     */
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 成功响应方法 ====================

    /**
     * 成功返回（不带数据）
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        return result;
    }

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    /**
     * 成功返回（自定义消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    // ==================== 失败响应方法 ====================

    /**
     * 失败返回（默认错误消息）
     */
    public static <T> Result<T> error() {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage("操作失败");
        return result;
    }

    /**
     * 失败返回（自定义错误消息）
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败返回（自定义错误码和消息）
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
