package com.selfblog.service.seo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SeoGenerationParser} 的防御性解析测试（纯单元，无 Spring、无网络）。
 */
@DisplayName("SeoGenerationParser")
class SeoGenerationParserTest {

    private final SeoGenerationParser parser = new SeoGenerationParser(new ObjectMapper());

    private static String description(int length) {
        return "描".repeat(length);
    }

    private static String validJson() {
        return "{\"title\":\"Spring Boot 事务传播行为详解\","
                + "\"description\":\"" + description(140) + "\","
                + "\"keywords\":[\"Spring Boot\",\"事务\",\"传播行为\"]}";
    }

    @Test
    @DisplayName("parse_HappyPath_ReturnsDraftWithCommaJoinedKeywords")
    void parse_HappyPath_ReturnsDraftWithCommaJoinedKeywords() {
        SeoGenerationParser.SeoDraft draft = parser.parse(validJson());

        assertEquals("Spring Boot 事务传播行为详解", draft.title());
        assertEquals(140, draft.description().length());
        assertEquals("Spring Boot,事务,传播行为", draft.keywords());
    }

    @Test
    @DisplayName("parse_StripsMarkdownFenceAndSurroundingProse")
    void parse_StripsMarkdownFenceAndSurroundingProse() {
        String raw = "好的，下面是结果：\n```json\n" + validJson() + "\n```\n希望有帮助。";

        SeoGenerationParser.SeoDraft draft = parser.parse(raw);

        assertEquals("Spring Boot 事务传播行为详解", draft.title());
    }

    @Test
    @DisplayName("parse_ToleratesKeywordsAsCommaSeparatedString")
    void parse_ToleratesKeywordsAsCommaSeparatedString() {
        String raw = "{\"title\":\"标题\",\"description\":\"" + description(120)
                + "\",\"keywords\":\"事务，Spring Boot、传播行为\"}";

        SeoGenerationParser.SeoDraft draft = parser.parse(raw);

        assertEquals("事务,Spring Boot,传播行为", draft.keywords());
    }

    @Test
    @DisplayName("parse_DedupesKeywords")
    void parse_DedupesKeywords() {
        // 去重后必须仍有 3 个（KEYWORD_MIN），所以给 4 项、其中 1 项重复
        String raw = "{\"title\":\"标题\",\"description\":\"" + description(120)
                + "\",\"keywords\":[\"事务\",\"事务\",\"传播行为\",\"事务管理\"]}";

        SeoGenerationParser.SeoDraft draft = parser.parse(raw);

        assertEquals("事务,传播行为,事务管理", draft.keywords());
    }

    @Test
    @DisplayName("parse_RejectsOverlongTitle")
    void parse_RejectsOverlongTitle() {
        String raw = "{\"title\":\"" + "标".repeat(61) + "\",\"description\":\"" + description(120)
                + "\",\"keywords\":[\"a\",\"b\",\"c\"]}";

        SeoGenerationParser.InvalidSeoOutputException error = assertThrows(
                SeoGenerationParser.InvalidSeoOutputException.class, () -> parser.parse(raw));

        assertTrue(error.getMessage().contains("title"), "问题描述要能指导模型修复，必须点名 title");
        assertTrue(error.getMessage().contains("61"));
    }

    @Test
    @DisplayName("parse_RejectsTooFewKeywords")
    void parse_RejectsTooFewKeywords() {
        String raw = "{\"title\":\"标题\",\"description\":\"" + description(120)
                + "\",\"keywords\":[\"只有两个\",\"关键词\"]}";

        SeoGenerationParser.InvalidSeoOutputException error = assertThrows(
                SeoGenerationParser.InvalidSeoOutputException.class, () -> parser.parse(raw));

        assertTrue(error.getMessage().contains("keywords"));
    }

    @Test
    @DisplayName("parse_RejectsOverlongKeyword")
    void parse_RejectsOverlongKeyword() {
        String raw = "{\"title\":\"标题\",\"description\":\"" + description(120)
                + "\",\"keywords\":[\"a\",\"b\",\"" + "超".repeat(21) + "\"]}";

        SeoGenerationParser.InvalidSeoOutputException error = assertThrows(
                SeoGenerationParser.InvalidSeoOutputException.class, () -> parser.parse(raw));

        assertTrue(error.getMessage().contains("关键词"));
    }

    @Test
    @DisplayName("parse_AcceptsDescriptionOutsideIdealRange")
    void parse_AcceptsDescriptionOutsideIdealRange() {
        // 60-300 是硬区间，120-160 只是理想区间：落在 200 仍然算成功
        String raw = "{\"title\":\"标题\",\"description\":\"" + description(200)
                + "\",\"keywords\":[\"a\",\"b\",\"c\"]}";

        assertEquals(200, parser.parse(raw).description().length());
    }

    @Test
    @DisplayName("parse_RejectsDescriptionTooShort")
    void parse_RejectsDescriptionTooShort() {
        String raw = "{\"title\":\"标题\",\"description\":\"太短了\",\"keywords\":[\"a\",\"b\",\"c\"]}";

        SeoGenerationParser.InvalidSeoOutputException error = assertThrows(
                SeoGenerationParser.InvalidSeoOutputException.class, () -> parser.parse(raw));

        assertTrue(error.getMessage().contains("description"));
    }

    @Test
    @DisplayName("parse_RejectsEmptyOutput")
    void parse_RejectsEmptyOutput() {
        assertThrows(SeoGenerationParser.InvalidSeoOutputException.class, () -> parser.parse("   "));
        assertThrows(SeoGenerationParser.InvalidSeoOutputException.class, () -> parser.parse(null));
    }

    @Test
    @DisplayName("parse_RejectsUnbalancedJson")
    void parse_RejectsUnbalancedJson() {
        assertThrows(SeoGenerationParser.InvalidSeoOutputException.class,
                () -> parser.parse("{\"title\":\"标题\",\"description\":\"x\""));
    }

    @Test
    @DisplayName("parse_RejectsNonJsonText")
    void parse_RejectsNonJsonText() {
        assertThrows(SeoGenerationParser.InvalidSeoOutputException.class,
                () -> parser.parse("抱歉，我无法完成这个请求。"));
    }

    @Test
    @DisplayName("parse_HandlesBracesInsideStrings")
    void parse_HandlesBracesInsideStrings() {
        // 描述里出现 { } 不能干扰平衡扫描
        String raw = "{\"title\":\"标题\",\"description\":\"" + description(60) + "{配置}文件\""
                + ",\"keywords\":[\"a\",\"b\",\"c\"]}";

        assertEquals(66, parser.parse(raw).description().length());
    }

    @Test
    @DisplayName("parse_HandlesEscapedQuotesInsideStrings")
    void parse_HandlesEscapedQuotesInsideStrings() {
        String raw = "{\"title\":\"标题\",\"description\":\"" + description(60) + "\\\"引用\\\"内容\""
                + ",\"keywords\":[\"a\",\"b\",\"c\"]}";

        assertEquals(66, parser.parse(raw).description().length());
    }
}
