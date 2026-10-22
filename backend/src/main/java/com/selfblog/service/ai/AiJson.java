package com.selfblog.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型输出的宽容解析工具。
 *
 * <p>模型常把 JSON 包在 Markdown 代码块里、在前面加一句「好的，结果如下」，或在 JSON
 * 之后补一段解释。这里先按花括号计数截出第一个平衡的 JSON 对象（跳过字符串字面量内的
 * 括号与转义），再交给 Jackson；任何结构问题都转成 {@link AiOutputException}，
 * 由调用方决定是否让模型修复一次。</p>
 */
@Component
public class AiJson {

    /** 数组/单字符串里用作分隔符的字符（中英文逗号、分号、顿号、换行）。 */
    private static final String SPLIT_REGEX = "[,，;；、\\n]";

    private final ObjectMapper json;

    public AiJson(ObjectMapper json) {
        this.json = json;
    }

    /** 解析成 JSON 对象；任何结构问题都转成 {@link AiOutputException}。 */
    public JsonNode read(String raw) {
        String candidate = extractJsonObject(raw);
        JsonNode root;
        try {
            root = json.readTree(candidate);
        } catch (Exception e) {
            throw new AiOutputException("输出不是合法的 JSON 对象");
        }
        if (root == null || !root.isObject()) {
            throw new AiOutputException("输出不是合法的 JSON 对象");
        }
        return root;
    }

    /**
     * 截取第一个平衡的 {@code {...}}。
     *
     * <p>按花括号计数，字符串字面量内的括号与转义字符一律跳过，否则
     * {@code {"text":"}"}} 这类内容会被提前截断。</p>
     */
    static String extractJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AiOutputException("模型没有输出任何内容");
        }
        int start = raw.indexOf('{');
        if (start < 0) {
            throw new AiOutputException("输出里没有 JSON 对象");
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < raw.length(); i++) {
            char c = raw.charAt(i);
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
                    return raw.substring(start, i + 1);
                }
            }
        }
        throw new AiOutputException("输出里的 JSON 对象不完整");
    }

    /** 读一个字符串字段；缺失或非文本返回 {@code null}。 */
    static String text(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        return node == null || !node.isTextual() ? null : node.asText();
    }

    /** 读一个整数字段；缺失、非数字或越界返回 {@code null}。 */
    static Integer integer(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isInt() || node.isLong()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 宽容读取字符串数组：支持 JSON 数组（元素可为字符串或对象）、单个字符串
     * （按中英文逗号/顿号/分号/换行拆）、以及对象里的 {@code value|keyword|name|text|title|tag}。
     */
    static List<String> textList(JsonNode node) {
        List<String> values = new ArrayList<>();
        collectTexts(node, values);
        return values;
    }

    private static void collectTexts(JsonNode node, List<String> values) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectTexts(item, values);
            }
            return;
        }
        if (node.isTextual()) {
            for (String part : node.asText().split(SPLIT_REGEX)) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
            return;
        }
        if (node.isObject()) {
            for (String field : new String[]{"value", "keyword", "name", "text", "title", "tag"}) {
                JsonNode inner = node.get(field);
                if (inner != null && inner.isTextual()) {
                    collectTexts(inner, values);
                    return;
                }
            }
        }
    }

    /** 去掉常见 Markdown 标记并压缩空白，得到可直接展示的纯文本。 */
    static String plainText(String value) {
        if (value == null) {
            return "";
        }
        String text = value
                .replaceAll("```[a-zA-Z0-9]*", "")
                .replace("`", "")
                .replace("**", "")
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")
                .replaceAll("(?m)^\\s{0,3}[-*+]\\s+", "")
                .replaceAll("\\s+", " ")
                .trim();
        return text;
    }

    /** 按字符截断（不追加省略号，保证结果不超出契约上限）。 */
    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
