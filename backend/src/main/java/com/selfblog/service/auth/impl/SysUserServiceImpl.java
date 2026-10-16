package com.selfblog.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.UserStatus;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.auth.RegisterVerifyRequest;
import com.selfblog.dto.auth.UserLoginRequest;
import com.selfblog.dto.auth.UserRegisterRequest;
import com.selfblog.dto.user.AdminUserQueryRequest;
import com.selfblog.dto.user.UserSearchRequest;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import com.selfblog.service.auth.SysUserService;
import com.selfblog.config.security.SecurityProperties;
import com.selfblog.utils.*;
import com.selfblog.vo.user.AdminUserVO;
import com.selfblog.vo.user.UserLoginResponse;
import com.selfblog.vo.user.UserRegisterResponse;
import com.selfblog.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    /**
     * 校验密码复杂度
     * 规则：至少8位，包含大小写字母、数字或特殊字符中的至少3种
     * @param password 密码
     * @throws BusinessException 如果密码不符合要求
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException(400, "密码长度至少为8位");
        }
        int categories = 0;
        if (password.matches(".*[A-Z].*")) categories++;
        if (password.matches(".*[a-z].*")) categories++;
        if (password.matches(".*\\d.*")) categories++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};'\':\"\\\\|,.<>\\/?].*")) categories++;
        if (categories < 3) {
            throw new BusinessException(400, "密码必须包含大小写字母、数字或特殊字符中的至少3种");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRegisterResponse register(UserRegisterRequest request) {
        // 密码复杂度校验
        validatePasswordStrength(request.getPassword());

        // 提前检查用户名和邮箱是否已存在（使用模糊错误信息防止用户枚举攻击）
        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, request.getUsername());
        if (this.count(usernameWrapper) > 0) {
            throw new BusinessException(400, "注册失败，请稍后重试");
        }

        // 检查邮箱是否已存在（如果提供了邮箱）
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(SysUser::getEmail, request.getEmail());
            if (this.count(emailWrapper) > 0) {
                throw new BusinessException(400, "该邮箱已被注册，请使用其他邮箱");
            }
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        user.setRole("user");
        user.setStatus(1);
        user.setLoginFailCount(0);

        try {
            this.save(user);
        } catch (DataAccessException e) {
            // 并发注册：约束错误在不同数据库翻译成不同异常（SQLite 为 UncategorizedSQLException），
            // 统一按父类转换为可读失败。（注册入口本身已关闭，这里只保证不再暴露 500。）
            throw new BusinessException(400, "注册失败，请稍后重试");
        }

        log.info("用户注册成功: username={}, userId={}", user.getUsername(), user.getId());
        return new UserRegisterResponse(user.getId(), user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerWithVerifiedEmail(RegisterVerifyRequest request) {
        validatePasswordStrength(request.getPassword());

        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, request.getUsername());
        if (this.count(usernameWrapper) > 0) {
            throw new BusinessException(400, "注册失败，请稍后重试");
        }

        LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(SysUser::getEmail, request.getEmail());
        if (this.count(emailWrapper) > 0) {
            throw new BusinessException(400, "该邮箱已被注册，请使用其他邮箱或直接登录");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        user.setRole("user");
        user.setStatus(1);
        user.setLoginFailCount(0);

        try {
            this.save(user);
        } catch (DataAccessException e) {
            throw new BusinessException(400, "注册失败，请稍后重试");
        }

        log.info("邮箱验证注册成功: username={}, userId={}, email={}", user.getUsername(), user.getId(), StringMaskUtils.maskEmail(request.getEmail()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginResponse login(UserLoginRequest request) {
        // 参数校验：支持用户名或邮箱登录
        String loginAccount = request.getUsername();
        boolean isEmailLogin = false;

        if (loginAccount == null || loginAccount.trim().isEmpty()) {
            loginAccount = request.getEmail();
            isEmailLogin = true;
        }

        if (loginAccount == null || loginAccount.trim().isEmpty()) {
            throw new BusinessException(400, "用户名或邮箱不能为空");
        }

        loginAccount = loginAccount.trim();

        // 查询用户：优先按用户名查询，如果失败且输入是邮箱格式则按邮箱查询
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        SysUser user = null;

        if (isEmailLogin || loginAccount.contains("@")) {
            // 邮箱登录
            wrapper.eq(SysUser::getEmail, loginAccount);
            user = this.getOne(wrapper);
            if (user == null) {
                log.warn("用户登录失败: email={}, 原因=邮箱或密码错误", StringMaskUtils.maskEmail(loginAccount));
                throw new BusinessException(401, "用户名/邮箱或密码错误");
            }
        } else {
            // 用户名登录
            wrapper.eq(SysUser::getUsername, loginAccount);
            user = this.getOne(wrapper);
            if (user == null) {
                log.warn("用户登录失败: username={}, 原因=用户名或密码错误", loginAccount);
                throw new BusinessException(401, "用户名/邮箱或密码错误");
            }
        }

        // 检查账户是否被锁定
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = java.time.Duration.between(LocalDateTime.now(), user.getLockUntil()).toMinutes() + 1;
            throw new BusinessException(403, "登录失败次数过多，请" + minutesLeft + "分钟后再试");
        }

        // 检查账号状态（先于密码验证）
        if (user.getStatus() == UserStatus.DISABLED.getValue()) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            String accountLabel = isEmailLogin ? "邮箱" : "用户名";
            log.warn("用户登录失败: account={}, 原因={}或密码错误", loginAccount, accountLabel);
            // 密码错误，使用原子操作增加失败计数
            int affected = handleLoginFailAtomic(user.getId());
            if (affected == 0) {
                // 更新行数为0，说明账号已被锁定（SQL中的WHERE条件不满足）
                throw new BusinessException(403, "登录失败次数过多，请稍后再试");
            }
            // 重新查询获取最新锁定状态
            SysUser updatedUser = this.getById(user.getId());
            if (updatedUser.getLockUntil() != null && updatedUser.getLockUntil().isAfter(LocalDateTime.now())) {
                long minutesLeft = java.time.Duration.between(LocalDateTime.now(), updatedUser.getLockUntil()).toMinutes() + 1;
                log.warn("用户登录失败: account={}, 原因=账号已被锁定至{}", loginAccount, updatedUser.getLockUntil());
                throw new BusinessException(403, "登录失败次数过多，请" + minutesLeft + "分钟后再试");
            }
            throw new BusinessException(401, "用户名/邮箱或密码错误");
        }

        if (!"admin".equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(403, "此博客仅开放作者后台登录");
        }

        // 登录成功，重置失败计数和锁定
        if (user.getLoginFailCount() == null || user.getLoginFailCount() > 0) {
            user.setLoginFailCount(0);
            user.setLockUntil(null);
            this.updateById(user);
        }

        // 生成 Token（包含角色）
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        // 生成刷新Token（7天有效期）
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        // 注册设备Token
        jwtUtils.registerDeviceToken(user.getId(), token);

        log.info("用户登录成功: username={}, userId={}, email={}", user.getUsername(), user.getId(),
                user.getEmail() != null ? StringMaskUtils.maskEmail(user.getEmail()) : "未设置");
        return new UserLoginResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getEmail(),
                token,
                refreshToken,
                user.getRole()
        );
    }

    /**
     * 原子性处理登录失败（解决并发问题）
     */
    private int handleLoginFailAtomic(Long userId) {
        return baseMapper.incrementLoginFailCount(userId,
                securityProperties.getLogin().getMaxFailCount(),
                securityProperties.getLogin().getLockMinutes());
    }

    @Override
    @Transactional(readOnly = true)
    public UserVO getUserById(Long id) {
        SysUser user = this.getById(id);
        if (user == null) return null;
        return UserConverter.toUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        // 参数校验
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            throw new BusinessException(400, "旧密码不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BusinessException(400, "新密码不能为空");
        }
        if (newPassword.length() < 8) {
            throw new BusinessException(400, "新密码长度至少为8位");
        }

        // 获取用户
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("用户修改密码失败: userId={}, 原因=旧密码错误", userId);
            throw new BusinessException(400, "旧密码不正确");
        }

        // 新密码复杂度校验
        validatePasswordStrength(newPassword);

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);

        // 密码修改后使当前用户所有 Token 失效
        jwtUtils.revokeAllUserTokens(userId);
        log.info("用户修改密码成功: userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<UserVO> searchUsers(UserSearchRequest request) {
        Page<SysUser> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, 1);
        // 关键词搜索：支持 username 和 nickname 模糊匹配
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickname, keyword));
        }

        // 按创建时间倒序
        wrapper.orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> userPage = this.page(page, wrapper);

        // 转换为 UserVO（搜索用户只返回公开信息，隐藏email和role）
        IPage<UserVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(userPage.getRecords().stream()
                .map(user -> {
                    UserVO vo = UserConverter.toUserVO(user);
                    vo.setEmail(null);
                    vo.setRole(null);
                    return vo;
                })
                .collect(java.util.stream.Collectors.toList()));

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<AdminUserVO> getAdminUserList(AdminUserQueryRequest request) {
        Page<SysUser> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SysUser::getIsDeleted, 1);
        // 关键词搜索：支持 username、nickname 和 email 模糊匹配
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            String keyword = request.getKeyword().trim();
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getNickname, keyword)
                    .or()
                    .like(SysUser::getEmail, keyword));
        }
        // 状态筛选
        if (request.getStatus() != null) {
            wrapper.eq(SysUser::getStatus, request.getStatus());
        }

        // 按创建时间倒序
        wrapper.orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> userPage = this.page(page, wrapper);

        // 转换为 AdminUserVO
        IPage<AdminUserVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        result.setRecords(userPage.getRecords().stream()
                .map(this::convertToAdminUserVO)
                .collect(java.util.stream.Collectors.toList()));

        return result;
    }

    private AdminUserVO convertToAdminUserVO(SysUser user) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setFollowingCount(user.getFollowingCount());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserProfile(Long userId, String nickname, String bio, String email) {
        // 参数校验
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new BusinessException(400, "昵称不能为空");
        }
        if (nickname.length() > 50) {
            throw new BusinessException(400, "昵称长度不能超过50字符");
        }
        if (bio != null && bio.length() > 200) {
            throw new BusinessException(400, "个人简介不能超过200字符");
        }

        // 获取用户
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 检查邮箱唯一性（如果提供了新邮箱）
        if (email != null && !email.trim().isEmpty()) {
            LambdaQueryWrapper<SysUser> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(SysUser::getEmail, email)
                    .ne(SysUser::getId, userId); // 排除当前用户
            if (this.count(emailWrapper) > 0) {
                throw new BusinessException(400, "邮箱已被使用");
            }
            user.setEmail(email);
        }

        user.setNickname(htmlSanitizer.sanitizePlainText(nickname.trim()));
        if (bio != null) {
            user.setBio(htmlSanitizer.sanitizePlainText(bio));
        }
        this.updateById(user);
        log.info("用户资料更新成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(Long userId, String avatar) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (avatar == null || avatar.trim().isEmpty()) {
            throw new BusinessException(400, "头像URL不能为空");
        }
        if (avatar.length() > 2048) {
            throw new BusinessException(400, "头像URL长度不能超过2048字符");
        }
        if (!avatar.startsWith("/") && !avatar.startsWith("http://") && !avatar.startsWith("https://")) {
            throw new BusinessException(400, "头像URL格式无效");
        }

        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        user.setAvatar(avatar.trim());
        this.updateById(user);
        log.info("用户头像更新成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCoverImage(Long userId, String coverImage) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }
        if (coverImage != null && coverImage.length() > 2048) {
            throw new BusinessException(400, "封面图URL长度不能超过2048字符");
        }

        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (coverImage != null) {
            String trimmed = coverImage.trim();
            if (!trimmed.startsWith("/") && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                throw new BusinessException(400, "封面图URL格式无效");
            }
            user.setCoverImage(trimmed);
        } else {
            user.setCoverImage(null);
        }
        this.updateById(user);
        log.info("用户封面图更新成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long userId, boolean ban) {
        if (userId == null) {
            throw new BusinessException(400, "用户ID不能为空");
        }

        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 不能操作自己
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new BusinessException(400, "不能操作自己的账号");
        }

        // 不能操作其他管理员
        if ("admin".equals(user.getRole())) {
            throw new BusinessException(400, "不能操作管理员账号");
        }

        user.setStatus(ban ? 0 : 1);
        this.updateById(user);
        log.info("用户{}: userId={}", ban ? "已封禁" : "已解封", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status, Long adminId) {
        if (Objects.equals(userId, adminId)) {
            throw new BusinessException(400, "不能修改自己的账号状态");
        }
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new BusinessException(400, "不能修改其他管理员的账号状态");
        }

        int previousStatus = user.getStatus();
        user.setStatus(status);
        this.updateById(user);

        log.info("管理员修改用户状态: adminId={}, targetUserId={}, previousStatus={}, newStatus={}",
                adminId, userId, previousStatus, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String resetPassword(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        String newPassword = generateRandomPassword(12);
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
        jwtUtils.revokeAllUserTokens(userId);

        log.info("管理员重置用户密码: adminId={}, targetUserId={}, targetUsername={}",
                SecurityUtils.getCurrentUserId(), userId, user.getUsername());

        return newPassword;
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public SysUser getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(400, "邮箱不能为空");
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getEmail, email.trim());
        return this.getOne(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public UserVO getUserVOByEmail(String email) {
        SysUser user = getUserByEmail(email);
        if (user == null) return null;
        return UserConverter.toUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String email, String newPassword) {
        // 参数校验
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(400, "邮箱不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BusinessException(400, "新密码不能为空");
        }

        // 密码复杂度校验
        validatePasswordStrength(newPassword);

        // 查找用户
        SysUser user = getUserByEmail(email);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
        jwtUtils.revokeAllUserTokens(user.getId());
        log.info("用户密码重置成功: userId={}, email={}", user.getId(), StringMaskUtils.maskEmail(email));
    }
}
