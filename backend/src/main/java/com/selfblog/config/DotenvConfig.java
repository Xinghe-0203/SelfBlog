package com.selfblog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * .env 文件加载器
 *
 * 在应用启动最早阶段加载 .env 文件到系统属性
 * 确保在 Spring Boot 读取任何配置之前，环境变量已经可用
 */
public class DotenvConfig implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvConfig.class);
    private static final String ENV_FILE = ".env";
    private static final String[] SENSITIVE_KEYS = {
        "DB_PASSWORD", "JWT_SECRET", "MAIL_PASSWORD", "DB_USERNAME"
    };

    private DotenvConfig() {
        // 私有构造函数，防止实例化
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        load();
    }

    /**
     * 手动加载 .env 文件
     * 在 SpringApplication.run() 之前调用
     */
    public static void load() {
        loadEnvFile();
    }

    /**
     * 在 JVM 启动时加载 .env 文件
     */
    private static void loadEnvFile() {
        Path envPath = findEnvFile();
        if (envPath == null) {
            log.warn("[Dotenv] 未找到 .env 文件，将使用系统环境变量或默认值");
            return;
        }

        log.info("[Dotenv] 加载配置文件: {}", envPath.toAbsolutePath());

        try {
            Properties properties = new Properties();
            // 必须按 UTF-8 读取：Properties.load(InputStream) 会按 ISO-8859-1 解码，
            // 导致 .env 中的中文值（如 ADMIN_NICKNAME=站长）变成乱码。
            try (var reader = Files.newBufferedReader(envPath, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }

            int loadedCount = 0;
            int sensitiveCount = 0;
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                if (System.getProperty(key) == null) {
                    System.setProperty(key, value);
                    loadedCount++;
                    if (isSensitiveKey(key)) {
                        sensitiveCount++;
                    }
                }
            }

            log.info("[Dotenv] 成功加载 {} 个配置项（其中 {} 个敏感配置）", loadedCount, sensitiveCount);
        } catch (IOException e) {
            log.error("[Dotenv] 加载 .env 文件失败: {}", e.getMessage());
        }
    }

    /**
     * 查找 .env 文件
     * 搜索顺序：项目根目录 -> 当前工作目录 -> 用户主目录
     */
    private static Path findEnvFile() {
        String[] searchPaths = {
            System.getProperty("user.dir"),
            System.getProperty("user.home")
        };

        for (String dir : searchPaths) {
            if (dir != null) {
                Path envFile = Paths.get(dir, ENV_FILE);
                if (Files.exists(envFile)) {
                    return envFile;
                }
            }
        }

        return null;
    }

    private static boolean isSensitiveKey(String key) {
        for (String sensitive : SENSITIVE_KEYS) {
            if (sensitive.equals(key) || key.endsWith("_PASSWORD") || key.endsWith("_SECRET")) {
                return true;
            }
        }
        return false;
    }
}
