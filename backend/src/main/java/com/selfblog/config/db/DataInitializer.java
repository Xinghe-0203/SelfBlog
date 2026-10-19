package com.selfblog.config.db;

import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${ADMIN_USERNAME:admin}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${ADMIN_NICKNAME:站长}")
    private String adminNickname;

    @Override
    public void run(String... args) {
        initAdminAccount();
    }

    private void initAdminAccount() {
        // 必须绕过 @TableLogic 查询：username 唯一 + 逻辑删除，被软删除的同名行仍占用唯一索引。
        // 用过滤 is_deleted 的查询会误判为「不存在」→ 再次 INSERT → 撞 uk_username → 启动失败。
        SysUser raw = sysUserMapper.selectRawByUsername(adminUsername);

        if (raw != null && isDeleted(raw)) {
            // 作者账户曾被软删除：恢复该行，但保留作者自己设置的密码（不重置）。
            if (sysUserMapper.restoreDeletedById(raw.getId()) == 1) {
                log.warn("检测到被软删除的作者管理员账户，已恢复: {}", adminUsername);
            }
            return;
        }

        if (raw != null) {
            // Upgrade the legacy sample credential once without replacing a password the owner changed.
            if (adminPassword != null && adminPassword.length() >= 12
                    && passwordEncoder.matches("Admin123", raw.getPassword())) {
                if (sysUserMapper.updatePasswordIfUnchanged(raw.getId(), raw.getPassword(),
                        passwordEncoder.encode(adminPassword)) == 1) {
                    log.warn("已将旧版示例管理员密码替换为 .env 中的 ADMIN_PASSWORD");
                }
            }
            log.info("作者管理员账户已存在，跳过初始化");
            return;
        }

        if (adminPassword == null || adminPassword.length() < 12) {
            throw new IllegalStateException("首次启动必须在 .env 中设置至少 12 位的 ADMIN_PASSWORD");
        }
        SysUser admin = new SysUser();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname(adminNickname);
        admin.setRole("admin");
        admin.setStatus(1);
        admin.setLoginFailCount(0);
        try {
            sysUserMapper.insert(admin);
        } catch (DataAccessException e) {
            // 并发/多上下文竞态兜底：另一个实例可能刚插入同名行（或刚恢复软删除行）。
            // 捕获父类 DataAccessException：H2/MySQL 是 DuplicateKeyException，
            // SQLite 的约束错误是 UncategorizedSQLException，只有父类可移植。
            SysUser concurrent = sysUserMapper.selectRawByUsername(adminUsername);
            if (concurrent == null) {
                throw e;
            }
            log.warn("作者管理员账户已由并发启动创建，跳过初始化: {}", adminUsername);
            return;
        }
        log.info("作者管理员账户初始化成功: {}", adminUsername);
    }

    /** is_deleted 为 null 时按未删除处理（历史数据可能缺省）。 */
    private static boolean isDeleted(SysUser user) {
        return user.getIsDeleted() != null && user.getIsDeleted() == 1;
    }
}
