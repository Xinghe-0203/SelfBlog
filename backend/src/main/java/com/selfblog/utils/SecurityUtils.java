package com.selfblog.utils;

import com.selfblog.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 * 用于从 SecurityContext 获取当前登录用户信息
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户的上下文
     * @return 用户上下文
     * @throws BusinessException 如果用户未登录
     */
    public static UserContext getCurrentUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(401, "请先登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserContext) {
            return (UserContext) principal;
        }
        if (principal instanceof Long) {
            // 兼容旧版本：只存了userId
            return new UserContext((Long) principal, null);
        }
        throw new BusinessException(401, "请先登录");
    }

    /**
     * 获取当前登录用户的ID
     * @return 用户ID
     * @throws BusinessException 如果用户未登录
     */
    public static Long getCurrentUserId() {
        return getCurrentUserContext().getUserId();
    }

    /**
     * 获取当前登录用户的ID，如果未登录返回null
     * @return 用户ID或null
     */
    public static Long getCurrentUserIdOrNull() {
        UserContext context = getCurrentUserContextOrNull();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前登录用户的上下文，如果未登录返回null
     * @return 用户上下文或null
     */
    public static UserContext getCurrentUserContextOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserContext) {
            return (UserContext) principal;
        }
        if (principal instanceof Long) {
            // 兼容旧版本：只存了userId
            return new UserContext((Long) principal, null);
        }
        return null;
    }

    /**
     * 检查当前用户是否是管理员
     * @return true如果是管理员
     */
    public static boolean isCurrentUserAdmin() {
        UserContext context = getCurrentUserContextOrNull();
        return context != null && context.isAdmin();
    }
}
