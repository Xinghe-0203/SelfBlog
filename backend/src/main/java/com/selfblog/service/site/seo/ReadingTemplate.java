package com.selfblog.service.site.seo;

import com.selfblog.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 读取构建产物里的 {@code static/index.html}，保留 Vite 生成的哈希资源引用。
 *
 * <p>只缓存原始文本；每个请求由渲染器解析出独立 Document，避免并发修改共享 DOM。
 * 资源缺失（例如只跑后端、没构建前端）时按 503 处理，而不是输出空壳。
 */
@Component
public class ReadingTemplate {
    private static final String DEFAULT_PATH = "static/index.html";

    private final String resourcePath;
    private volatile String cached;

    /** 允许通过 app.reading-template 覆盖（测试夹具/自定义构建产物）。 */
    public ReadingTemplate(@Value("${app.reading-template:" + DEFAULT_PATH + "}") String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String load() {
        String value = cached;
        if (value == null) {
            try (var input = new ClassPathResource(resourcePath).getInputStream()) {
                value = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                cached = value;
            } catch (IOException e) {
                throw new BusinessException(503, "页面模板暂不可用，请稍后重试");
            }
        }
        return value;
    }
}
