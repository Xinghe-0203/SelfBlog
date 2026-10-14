package com.selfblog.service.auth;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.dto.auth.RegisterVerifyRequest;
import com.selfblog.dto.auth.UserLoginRequest;
import com.selfblog.dto.auth.UserRegisterRequest;
import com.selfblog.dto.user.AdminUserQueryRequest;
import com.selfblog.dto.user.UserSearchRequest;
import com.selfblog.entity.SysUser;
import com.selfblog.vo.user.AdminUserVO;
import com.selfblog.vo.user.UserLoginResponse;
import com.selfblog.vo.user.UserRegisterResponse;
import com.selfblog.vo.user.UserVO;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户注册
     * @param request 注册请求
     * @return 注册响应
     */
    UserRegisterResponse register(UserRegisterRequest request);

    /**
     * 邮箱验证后注册（已验证邮箱真实性）
     * @param request 验证注册请求
     */
    void registerWithVerifiedEmail(RegisterVerifyRequest request);

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录响应
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * 根据ID获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    UserVO getUserById(Long id);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 搜索用户（支持用户名和昵称模糊匹配）
     * @param request 搜索请求
     * @return 分页用户列表
     */
    IPage<UserVO> searchUsers(UserSearchRequest request);

    /**
     * 获取管理员用户列表（支持状态筛选和关键词搜索）
     * @param request 查询请求
     * @return 分页用户列表
     */
    IPage<AdminUserVO> getAdminUserList(AdminUserQueryRequest request);

    /**
     * 修改用户资料
     * @param userId 用户ID
     * @param nickname 昵称
     * @param bio 个人简介
     * @param email 邮箱
     */
    void updateUserProfile(Long userId, String nickname, String bio, String email);

    /**
     * 修改用户头像
     * @param userId 用户ID
     * @param avatar 头像URL
     */
    void updateAvatar(Long userId, String avatar);

    /**
     * 修改用户封面图
     * @param userId 用户ID
     * @param coverImage 封面图URL
     */
    void updateCoverImage(Long userId, String coverImage);

    /**
     * 封禁/解封用户（切换状态）
     * @param userId 用户ID
     * @param ban true=封禁，false=解封
     */
    void banUser(Long userId, boolean ban);

    /**
     * 管理员更新用户状态
     * @param userId 目标用户ID
     * @param status 新状态
     * @param adminId 管理员ID
     */
    void updateUserStatus(Long userId, Integer status, Long adminId);

    /**
     * 管理员重置用户密码
     * @param userId 目标用户ID
     * @return 新生成的明文密码
     */
    String resetPassword(Long userId);

    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return 用户信息
     */
    SysUser getUserByEmail(String email);

    /**
     * 根据邮箱查找用户并返回VO（不含密码等敏感信息）
     * @param email 邮箱
     * @return 用户VO
     */
    UserVO getUserVOByEmail(String email);

    /**
     * 重置密码（通过邮箱验证码）
     * @param email 邮箱
     * @param newPassword 新密码
     */
    void resetPassword(String email, String newPassword);
}
