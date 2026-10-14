package com.selfblog.common.exception;

import com.selfblog.common.result.Result;
import com.selfblog.utils.LogUtils;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.IOException;

/**
 * 全局异常处理器
 * 统一处理所有异常，返回标准的JSON响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[EXCEPTION] Business: code={}, message={}", e.getCode(), e.getMessage());
        String clientIp = LogUtils.getClientIp(request);
        LogUtils.logSecurityEvent("BUSINESS_EXCEPTION", e.getMessage(), clientIp);
        int httpStatus;
        switch (e.getCode()) {
            case 400: case 401: case 403: case 404: case 409: case 429: case 500:
                httpStatus = e.getCode();
                break;
            default:
                httpStatus = 400;
        }
        return ResponseEntity.status(httpStatus).body(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理参数校验异常 (JPA/Spring 验证异常)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse("参数校验失败");
        log.warn("[EXCEPTION] ConstraintViolation: {}", message);
        return ResponseEntity.status(400).body(Result.error(400, message));
    }

    /**
     * 处理请求参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = "请求参数 '" + e.getParameterName() + "' 缺失";
        log.warn("[EXCEPTION] MissingParameter: {}", message);
        return ResponseEntity.status(400).body(Result.error(400, message));
    }

    /**
     * 处理请求体解析失败异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "请求体解析失败";
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException) {
            message = "参数类型不匹配：" + ((InvalidFormatException) cause).getValue();
        } else if (cause instanceof MismatchedInputException) {
            message = "请求体格式错误";
        }
        log.warn("[EXCEPTION] MessageNotReadable: {}", message);
        return ResponseEntity.status(400).body(Result.error(400, message));
    }

    /**
     * 处理404资源不存在异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("[EXCEPTION] NotFound: {}", e.getRequestURL());
        return ResponseEntity.status(404).body(Result.error(404, "资源不存在"));
    }

    /**
     * 处理405方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("[EXCEPTION] MethodNotAllowed: {} not supported", e.getMethod());
        return ResponseEntity.status(405).body(Result.error(405, "请求方法不支持"));
    }

    /**
     * 处理406媒体类型不接受异常
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotAcceptableException(HttpMediaTypeNotAcceptableException e) {
        log.warn("[EXCEPTION] MediaTypeNotAcceptable: {}", e.getMessage());
        return ResponseEntity.status(406).body(Result.error(406, "媒体类型不接受"));
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        log.warn("[EXCEPTION] Validation: {}", message);
        return ResponseEntity.status(400).body(Result.error(400, message));
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getFieldError() != null
                ? e.getFieldError().getDefaultMessage()
                : "参数绑定失败";
        log.warn("[EXCEPTION] Bind: {}", message);
        return ResponseEntity.status(400).body(Result.error(400, message));
    }

    /**
     * 处理数据完整性冲突异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("[EXCEPTION] DataIntegrityViolation: {}", e.getMessage());
        return ResponseEntity.status(409).body(Result.error(409, "数据完整性冲突"));
    }

    /**
     * 处理缺少请求部分异常
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("[EXCEPTION] MissingServletRequestPart: {}", e.getMessage());
        return ResponseEntity.status(400).body(Result.error(400, "缺少请求部分"));
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatchException(TypeMismatchException e) {
        log.warn("[EXCEPTION] TypeMismatch: property={}, value={}", e.getPropertyName(), e.getValue());
        return ResponseEntity.status(400).body(Result.error(400, "参数类型不匹配"));
    }

    /**
     * 处理不支持的媒体类型异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.warn("[EXCEPTION] MediaTypeNotSupported: {}", e.getMessage());
        return ResponseEntity.status(415).body(Result.error(415, "不支持的媒体类型"));
    }

    /**
     * 处理数据库重复键异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("[EXCEPTION] DuplicateKey: {}", e.getMessage());
        return ResponseEntity.status(409).body(Result.error(409, "数据已存在，操作冲突"));
    }

    /**
     * 处理访问拒绝异常（权限不足）
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("[EXCEPTION] AccessDenied: {}", e.getMessage());
        LogUtils.logSecurityEvent("ACCESS_DENIED", e.getMessage(), LogUtils.getClientIp(request));
        return ResponseEntity.status(403).body(Result.error(403, "权限不足，拒绝访问"));
    }

    /**
     * 处理认证异常（未登录或认证失败）
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("[EXCEPTION] Authentication: {}", e.getMessage());
        LogUtils.logSecurityEvent("AUTH_FAILED", e.getMessage(), LogUtils.getClientIp(request));
        return ResponseEntity.status(401).body(Result.error(401, "认证失败，请先登录"));
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[EXCEPTION] UploadSizeExceeded: {}", e.getMessage());
        return ResponseEntity.status(400).body(Result.error(400, "上传文件大小超出限制"));
    }

    /**
     * 处理参数类型转换异常（如 query string 中传了非数字给 int 参数）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName();
        Object actualValue = e.getValue();
        log.warn("[EXCEPTION] TypeMismatch: param={}, value={}, requiredType={}", paramName, actualValue, e.getRequiredType());
        return ResponseEntity.status(400).body(Result.error(400, "参数 '" + paramName + "' 格式错误，期望类型: " + (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知")));
    }

    /**
     * 处理IO异常（文件下载中断等）
     * 对ClientAbortException等连接中断异常直接忽略，不返回JSON
     */
    @ExceptionHandler(IOException.class)
    public void handleIOException(IOException e, HttpServletRequest request, HttpServletResponse response) {
        if (e instanceof ClientAbortException) {
            log.warn("[EXCEPTION] ClientAbort: 客户端主动断开连接, URI={}", request.getRequestURI());
            return;
        }
        log.error("[EXCEPTION] IOException: ", e);
        try {
            response.setStatus(500);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"message\":\"服务器内部错误\"}");
        } catch (IOException ignored) {
            // 如果此时还不能写回，直接忽略
        }
    }

    /**
     * 最后的兜底异常处理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        log.error("Unexpected error: ", e);
        return ResponseEntity.status(500).body(Result.error("服务器内部错误"));
    }
}
