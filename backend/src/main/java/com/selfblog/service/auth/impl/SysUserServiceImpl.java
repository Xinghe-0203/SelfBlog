package com.selfblog.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * S4：账号不可用（已禁用或仍处于锁定）时统一使用的中性文案。
     * 不区分两种原因，避免向调用方泄露账号状态。
     */
    private static final String ACCOUNT_UNAVAILABLE_MESSAGE = "该账号当前不可用，请联系管理员";

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    /**
     * 事务管理器：login 需要把 BCrypt 校验放到事务之外，因此显式拆成多个短事务。
     * required=false 是为了兼容手工 new 出来的实例（单元测试），此时 inTransaction 退化为直接执行。
     */
    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    private volatile TransactionTemplate transactionTemplate;

    /**
     * 在独立短事务中执行一段数据库操作，返回前连接已归还连接池。
     *
     * <p><b>注意：</b>异常必须在事务块之外抛出。若在块内抛出 RuntimeException，
     * 该短事务会回滚，写入（例如登录失败计数）将不会被持久化。</p>
     *
     * @param action 需要在事务中执行的数据库操作
     * @return 操作结果
     */
    private <T> T inTransaction(Supplier<T> action) {
        if (transactionManager == null) {
            return action.get();
        }
        TransactionTemplate template = this.transactionTemplate;
        if (template == null) {
            template = new TransactionTemplate(transactionManager);
            this.transactionTemplate = template;
        }
        return template.execute(status -> action.get());
    }

    /**
     * S4：清空登录失败计数与锁定时间。必须在调用方的写事务内执行（本方法不加事务注解）。
     *
     * @param userId 目标用户
     */
    private void clearLoginLockout(Long userId) {
        this.update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getLoginFailCount, 0)
                .set(SysUser::getLockUntil, null));
    }

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

    /**
     * 账号脱敏：邮箱走 {@link StringMaskUtils#maskEmail}，用户名只保留首尾字符。
     * 审计日志不能出现明文账号（尤其是邮箱/手机号形式的登录名）。
     */
    private static String maskAccount(String account) {
        if (account == null || account.isEmpty()) {
            return account;
        }
        if (account.contains("@")) {
            return StringMaskUtils.maskEmail(account);
        }
        if (account.length() <= 2) {
            return account.charAt(0) + "***";
        }
        return account.charAt(0) + "***" + account.charAt(account.length() - 1);
    }

    @Override
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
        final String account = loginAccount;
        final boolean emailLogin = isEmailLogin;

        // 查询用户：优先按用户名查询，如果失败且输入是邮箱格式则按邮箱查询。
        // 短事务 1：只读查询，出块即归还数据库连接，不把连接占在密码校验期间。
        SysUser user = inTransaction(() -> {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            if (emailLogin || account.contains("@")) {
                // 邮箱登录
                wrapper.eq(SysUser::getEmail, account);
            } else {
                // 用户名登录
                wrapper.eq(SysUser::getUsername, account);
            }
            return this.getOne(wrapper);
        });

        if (user == null) {
            if (emailLogin || account.contains("@")) {
                log.warn("用户登录失败: email={}, 原因=邮箱或密码错误", StringMaskUtils.maskEmail(account));
            } else {
                log.warn("用户登录失败: username={}, 原因=用户名或密码错误", account);
            }
            LogUtils.logLogin(maskAccount(account), false,
                    LogUtils.getClientIpFromCurrentRequest(), "account_not_found");
            throw new BusinessException(401, "用户名/邮箱或密码错误");
        }
        final SysUser verifiedUser = user;

        // 检查账户是否被锁定
        if (verifiedUser.getLockUntil() != null && verifiedUser.getLockUntil().isAfter(LocalDateTime.now())) {
            long minutesLeft = java.time.Duration.between(LocalDateTime.now(), verifiedUser.getLockUntil()).toMinutes() + 1;
            LogUtils.logLogin(maskAccount(account), false,
                    LogUtils.getClientIpFromCurrentRequest(), "account_locked");
            throw new BusinessException(403, "登录失败次数过多，请" + minutesLeft + "分钟后再试");
        }

        // 检查账号状态（先于密码验证）
        if (verifiedUser.getStatus() == UserStatus.DISABLED.getValue()) {
            LogUtils.logLogin(maskAccount(account), false,
                    LogUtils.getClientIpFromCurrentRequest(), "account_disabled");
            throw new BusinessException(403, "账号已被禁用");
        }

        // 验证密码。BCrypt 是 CPU 密集型慢哈希（cost=12 时单次约 0.4-0.5s），
        // 必须在事务之外执行：否则整个校验期间会独占数据库连接（SQLite 池大小 1 时阻塞全站）。
        if (!passwordEncoder.matches(request.getPassword(), verifiedUser.getPassword())) {
            String accountLabel = emailLogin ? "邮箱" : "用户名";
            log.warn("用户登录失败: account={}, 原因={}或密码错误", account, accountLabel);
            LogUtils.logLogin(maskAccount(account), false,
                    LogUtils.getClientIpFromCurrentRequest(), "bad_credentials");
            // 密码错误，使用原子操作增加失败计数。
            // 短事务 2：计数必须提交；BusinessException 一律在事务块之外抛出，否则会被回滚。
            int affected = inTransaction(() -> handleLoginFailAtomic(verifiedUser.getId()));
            if (affected == 0) {
                // 更新行数为0，说明账号已被锁定（SQL中的WHERE条件不满足）
                throw new BusinessException(403, "登录失败次数过多，请稍后再试");
            }
            // 重新查询获取最新锁定状态（独立短事务，读到的是已提交的计数与锁定时间）
            SysUser updatedUser = inTransaction(() -> this.getById(verifiedUser.getId()));
            if (updatedUser.getLockUntil() != null && updatedUser.getLockUntil().isAfter(LocalDateTime.now())) {
                long minutesLeft = java.time.Duration.between(LocalDateTime.now(), updatedUser.getLockUntil()).toMinutes() + 1;
                log.warn("用户登录失败: account={}, 原因=账号已被锁定至{}", account, updatedUser.getLockUntil());
                throw new BusinessException(403, "登录失败次数过多，请" + minutesLeft + "分钟后再试");
            }
            throw new BusinessException(401, "用户名/邮箱或密码错误");
        }

        if (!"admin".equalsIgnoreCase(verifiedUser.getRole())) {
            LogUtils.logLogin(maskAccount(account), false,
                    LogUtils.getClientIpFromCurrentRequest(), "not_admin");
            throw new BusinessException(403, "此博客仅开放作者后台登录");
        }

        // 登录成功，重置失败计数和锁定（短事务 3）
        if (verifiedUser.getLoginFailCount() == null || verifiedUser.getLoginFailCount() > 0) {
            inTransaction(() -> {
                this.update(new LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, verifiedUser.getId())
                        .set(SysUser::getLoginFailCount, 0)
                        .set(SysUser::getLockUntil, null));
                return null;
            });
        }

        // 令牌对绑定实际通过密码验证的快照，并发改密时不能升级旧凭据。
        String credentialVersion = jwtUtils.credentialVersionFor(verifiedUser);
        String token = jwtUtils.generateToken(verifiedUser.getId(), verifiedUser.getUsername(), verifiedUser.getRole(), credentialVersion);
        String refreshToken = jwtUtils.generateRefreshToken(verifiedUser.getId(), verifiedUser.getUsername(), verifiedUser.getRole(), credentialVersion);

        // 注册设备Token
        jwtUtils.registerDeviceToken(verifiedUser.getId(), token);

        log.info("用户登录成功: username={}, userId={}, email={}", verifiedUser.getUsername(), verifiedUser.getId(),
                verifiedUser.getEmail() != null ? StringMaskUtils.maskEmail(verifiedUser.getEmail()) : "未设置");
        LogUtils.logLogin(maskAccount(verifiedUser.getUsername()), true,
                LogUtils.getClientIpFromCurrentRequest(), null);
        return new UserLoginResponse(
                verifiedUser.getId(),
                verifiedUser.getUsername(),
                verifiedUser.getNickname(),
                verifiedUser.getAvatar(),
                verifiedUser.getEmail(),
                token,
                refreshToken,
                verifiedUser.getRole()
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

        // 短只读事务：取出用户与 CAS 用的旧哈希快照，出块即归还数据库连接。
        SysUser user = inTransaction(() -> this.getById(userId));
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        final String currentHash = user.getPassword();

        // 验证旧密码。BCrypt 是 CPU 密集型慢哈希（cost=12 单次约 0.4-0.5s），必须在事务之外执行，
        // 否则整个校验期间会独占数据库连接（生产池较小时阻塞全站其它请求）。
        if (!passwordEncoder.matches(oldPassword, currentHash)) {
            log.warn("用户修改密码失败: userId={}, 原因=旧密码错误", userId);
            throw new BusinessException(400, "旧密码不正确");
        }

        // 新密码复杂度校验（顺序与异常不变）
        validatePasswordStrength(newPassword);

        // 新哈希同样在事务之外计算（encode 与 matches 同为慢哈希）。
        String newHash = passwordEncoder.encode(newPassword);

        // 短写事务：CAS 条件更新 + 撤销令牌。
        // 注意：旧哈希的读取与条件更新现在分处两个事务，并发防覆盖仍由 SQL 的
        // `WHERE id = ? AND password = <读到的旧哈希>` 保证 —— 该条件更新是唯一的防覆盖手段，
        // 拆分事务并未削弱它（原实现也只在默认隔离级别下读后写，没有 SELECT FOR UPDATE）。
        // BusinessException 在块内抛出会回滚本短事务并向上传播，语义与原先一致。
        inTransaction(() -> {
            if (baseMapper.updatePasswordIfUnchanged(userId, currentHash, newHash) != 1) {
                throw new BusinessException(409, "密码已发生变化，请重新登录后重试");
            }
            // 密码修改后使当前用户所有 Token 失效（必须在密码更新成功之后）
            jwtUtils.revokeAllUserTokens(userId);
            return null;
        });

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
            String pattern = SqlLikeUtils.containsPattern(request.getKeyword().trim());
            wrapper.and(w -> w.apply("username LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern)
                    .or()
                    .apply("nickname LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern));
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
            String pattern = SqlLikeUtils.containsPattern(request.getKeyword().trim());
            wrapper.and(w -> w.apply("username LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern)
                    .or()
                    .apply("nickname LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern)
                    .or()
                    .apply("email LIKE {0}" + SqlLikeUtils.ESCAPE_CLAUSE, pattern));
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
    public String resetPassword(Long userId) {
        // 短只读事务：取用户与 CAS 用的旧哈希快照，出块即归还连接。
        SysUser user = inTransaction(() -> this.getById(userId));
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        final String currentHash = user.getPassword();

        // 随机密码与 BCrypt 编码都在事务之外完成（encode 是慢哈希）。
        String newPassword = generateRandomPassword(12);
        String newHash = passwordEncoder.encode(newPassword);

        // 短写事务：CAS 条件更新 + 撤销令牌；防覆盖仍由条件更新保证。
        inTransaction(() -> {
            if (baseMapper.updatePasswordIfUnchanged(userId, currentHash, newHash) != 1) {
                throw new BusinessException(409, "密码已发生变化，请重新登录后重试");
            }
            // S4：管理员重置成功同样清掉登录失败计数与锁定，否则刚被重置的账号仍被锁在门外。
            clearLoginLockout(userId);
            jwtUtils.revokeAllUserTokens(userId);
            return null;
        });

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
    public void resetPassword(String email, String newPassword) {
        // 参数校验
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(400, "邮箱不能为空");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BusinessException(400, "新密码不能为空");
        }

        // 密码复杂度校验（顺序与异常不变，仍在查用户之前）
        validatePasswordStrength(newPassword);

        // 短只读事务：按邮箱取用户与 CAS 用的旧哈希快照。
        SysUser user = inTransaction(() -> {
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getEmail, email.trim());
            return this.getOne(wrapper);
        });
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // S4：自助重置必须拒绝已禁用或仍处于锁定状态的账号。两种原因共用同一句中性文案，
        // 不向调用方透露账号究竟处于哪种状态（与 S3 的"统一信封"同一思路）。
        if (user.getStatus() == null || user.getStatus() != UserStatus.NORMAL.getValue()) {
            log.warn("邮箱重置被拒: userId={}, email={}, 原因=账号非正常状态", user.getId(),
                    StringMaskUtils.maskEmail(email));
            throw new BusinessException(403, ACCOUNT_UNAVAILABLE_MESSAGE);
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            log.warn("邮箱重置被拒: userId={}, email={}, 原因=账号仍处于锁定中", user.getId(),
                    StringMaskUtils.maskEmail(email));
            throw new BusinessException(403, ACCOUNT_UNAVAILABLE_MESSAGE);
        }
        final String currentHash = user.getPassword();
        final Long userId = user.getId();

        // BCrypt 编码在事务之外完成。
        String newHash = passwordEncoder.encode(newPassword);

        // 短写事务：CAS 条件更新 + 撤销令牌；防覆盖仍由条件更新保证。
        inTransaction(() -> {
            if (baseMapper.updatePasswordIfUnchanged(userId, currentHash, newHash) != 1) {
                throw new BusinessException(409, "密码已发生变化，请重新登录后重试");
            }
            // S4：重置成功即解锁，否则用户刚拿到新密码却仍被旧锁定时间拒绝登录。
            clearLoginLockout(userId);
            jwtUtils.revokeAllUserTokens(userId);
            return null;
        });

        log.info("用户密码重置成功: userId={}, email={}", userId, StringMaskUtils.maskEmail(email));
    }
}
