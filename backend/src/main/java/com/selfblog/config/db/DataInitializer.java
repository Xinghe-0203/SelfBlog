package com.selfblog.config.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfblog.entity.SysUser;
import com.selfblog.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
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
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, adminUsername);
        SysUser existingAdmin = sysUserMapper.selectOne(wrapper);

        if (existingAdmin == null) {
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
            sysUserMapper.insert(admin);
            log.info("作者管理员账户初始化成功: {}", adminUsername);
        } else {
            // Upgrade the legacy sample credential once without replacing a password the owner changed.
            if (adminPassword != null && adminPassword.length() >= 12
                    && passwordEncoder.matches("Admin123", existingAdmin.getPassword())) {
                if (sysUserMapper.updatePasswordIfUnchanged(existingAdmin.getId(), existingAdmin.getPassword(),
                        passwordEncoder.encode(adminPassword)) == 1) {
                    log.warn("已将旧版示例管理员密码替换为 .env 中的 ADMIN_PASSWORD");
                }
            }
            log.info("作者管理员账户已存在，跳过初始化");
        }
    }
}
