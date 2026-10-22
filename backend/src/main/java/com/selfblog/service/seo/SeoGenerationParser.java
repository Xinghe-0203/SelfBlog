package com.selfblog.service.seo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 模型输出的防御性解析与校验。
 *
 * <p>真实模型（尤其小参数模型）经常：包 ```json 代码块、在 JSON 前后写解释、
 * 少逗号、把 keywords 写成字符串而不是数组、标题超长。这里先做「宽容解析」
 * （找第一个平衡的 {@code {...}}），再做「严格校验」，任何一项不达标都抛
 * {@link InvalidSeoOutputException} 让上层决定是否重试一次。</p>
 */
@Slf4j
@Component
public class SeoGenerationParser {

    /** 标题硬上限：超出直接判失败（搜索结果的标题截断点）。 */
    public static final int TITLE_MAX = 60;
    /** 描述硬下限/上限：超出 60-300 判失败。 */
    public static final int DESCRIPTION_MIN = 60;
    public static final int DESCRIPTION_MAX = 300;
    /** 描述「理想区间」，只用于日志警告，不判失败。 */
    public static final int DESCRIPTION_IDEAL_MIN = 120;
    public static final int DESCRIPTION_IDEAL_MAX = 160;
    /** 关键词数量与单项长度。 */
    public static final int KEYWORD_MIN = 3;
    public static final int KEYWORD_MAX = 8;
    public static final int KEYWORD_LENGTH_MAX = 20;

    private final ObjectMapper json;

    public SeoGenerationParser(ObjectMapper json) {
        this.json = json;
    }

    /**
     * 解析并校验模型输出。
     *
     * @throws InvalidSeoOutputException 解析或校验失败，消息是给模型的修复提示（不含敏感信息）
     */
    public SeoDraft parse(String rawOutput) {
        String body = extractJsonObject(rawOutput);
        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (Exception e) {
            throw new InvalidSeoOutputException("输出不是合法 JSON（" + shortReason(e) + "）");
        }
        if (root == null || !root.isObject()) {
            throw new InvalidSeoOutputException("输出不是 JSON 对象");
        }
        String title = text(root, "title");
        String description = text(root, "description");
        List<String> keywords = keywords(root.get("keywords"));

        String problem = validate(title, description, keywords);
        if (problem != null) {
            throw new InvalidSeoOutputException(problem);
        }
        if (description.length() < DESCRIPTION_IDEAL_MIN || description.length() > DESCRIPTION_IDEAL_MAX) {
            // 只告警不失败：不同模型的长度控制能力差异很大，120-160 是目标而非硬门槛。
            log.warn("[SEO] 描述长度 {} 不在理想区间 {}-{}：{}", description.length(),
                    DESCRIPTION_IDEAL_MIN, DESCRIPTION_IDEAL_MAX, abbreviate(description));
        }
        return new SeoDraft(title, description, String.join(",", keywords));
    }

    /** 校验通过返回 {@code null}，否则返回面向模型的简短问题描述。 */
    String validate(String title, String description, List<String> keywords) {
        if (title == null || title.isBlank()) return "title 为空";
        if (title.length() > TITLE_MAX) return "title 超过 " + TITLE_MAX + " 个字符（当前 " + title.length() + "）";
        if (description == null || description.isBlank()) return "description 为空";
        if (description.length() < DESCRIPTION_MIN || description.length() > DESCRIPTION_MAX) {
            return "description 长度需在 " + DESCRIPTION_MIN + "-" + DESCRIPTION_MAX + " 个字符之间（当前 " + description.length() + "）";
        }
        if (keywords.size() < KEYWORD_MIN || keywords.size() > KEYWORD_MAX) {
            return "keywords 需为 " + KEYWORD_MIN + "-" + KEYWORD_MAX + " 个（当前 " + keywords.size() + "）";
        }
        for (String keyword : keywords) {
            if (keyword.length() > KEYWORD_LENGTH_MAX) {
                return "关键词「" + abbreviate(keyword) + "」超过 " + KEYWORD_LENGTH_MAX + " 个字符";
            }
        }
        return null;
    }

    /**
     * 从任意文本里取出第一个平衡的 JSON 对象。
     *
     * <p>用花括号计数而不是正则：模型输出里可能有字符串内的 {@code \{} 或嵌套对象，
     * 正则会截错位置。跳过字符串内部的括号（含转义），同时把 ``` 围栏一起去掉。</p>
     */
    static String extractJsonObject(String raw) {
        if (raw == null) {
            throw new InvalidSeoOutputException("模型返回为空");
        }
        String text = raw.trim();
        int start = text.indexOf('{');
        if (start < 0) {
            throw new InvalidSeoOutputException("输出中没有 JSON 对象");
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        throw new InvalidSeoOutputException("输出中的 JSON 对象不完整");
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node == null || node.isNull() ? null : node.asText().trim();
    }

    /**
     * keywords 宽容读取：数组取每项；字符串按逗号/顿号/分号拆；
     * 对象取 {@code value}/{@code keyword} 字段。最后去重、去空、去首尾引号。
     */
    static List<String> keywords(JsonNode node) {
        Set<String> values = new LinkedHashSet<>();
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                addKeyword(values, item.isObject() ? firstTextField(item) : item.asText());
            }
        } else if (node.isTextual()) {
            for (String part : node.asText().split("[,，;；、]")) {
                addKeyword(values, part);
            }
        } else if (node.isObject()) {
            addKeyword(values, firstTextField(node));
        }
        return new ArrayList<>(values);
    }

    private static String firstTextField(JsonNode object) {
        for (String field : List.of("value", "keyword", "name", "text")) {
            JsonNode value = object.get(field);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }
        return null;
    }

    private static void addKeyword(Set<String> values, String raw) {
        if (raw == null) return;
        String value = raw.trim().replaceAll("^[\"'“”]+|[\"'“”]+$", "").trim();
        if (!value.isEmpty()) {
            values.add(value);
        }
    }

    private static String shortReason(Exception e) {
        String message = e.getMessage();
        if (message == null) return "无法解析";
        int newline = message.indexOf('\n');
        String value = newline > 0 ? message.substring(0, newline) : message;
        return abbreviate(value);
    }

    static String abbreviate(String value) {
        return value == null || value.length() <= 80 ? value : value.substring(0, 80) + "…";
    }

    /** 校验通过的草稿。{@code keywords} 已用英文逗号连接，可直接入库。 */
    public record SeoDraft(String title, String description, String keywords) {
    }

    /** 输出不可用（可修复）。消息本身会作为修复提示回给模型，故不含敏感信息。 */
    public static class InvalidSeoOutputException extends RuntimeException {
        public InvalidSeoOutputException(String message) {
            super(message);
        }
    }
}
