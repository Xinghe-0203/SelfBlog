package com.selfblog.utils;

/**
 * LIKE 通配符转义工具。
 *
 * <p>用户输入的关键词直接拼进 LIKE 模式时，`%` / `_` 会被当成通配符：
 * 例如 `keyword=%` 会匹配整表、`keyword=_` 会匹配任意单字符，既是信息泄露
 * （可枚举全部内容）也是绕过搜索语义的注入面。所有关键词 LIKE 都必须先用
 * {@link #containsPattern(String)} 构造模式，并显式带上 {@link #ESCAPE_CLAUSE}。</p>
 *
 * <p><b>为什么转义字符是 `!` 而不是反斜杠：</b>反斜杠没有一种字面量写法能同时满足
 * 三种数据库。实测结论（H2 2.3.232 与 SQLite 3.50.4 均在本机验证）：</p>
 * <ul>
 *   <li>MySQL 的字符串字面量用 C 转义，要表示“一个反斜杠”必须写 `'\\'`；写成 `'\'`
 *       是未闭合字符串，直接语法错误。</li>
 *   <li>H2 / SQLite 不在字面量里处理反斜杠转义，`'\\'` 因此是<b>两个</b>字符：
 *       SQLite 报 “ESCAPE expression must be a single character”，H2 报
 *       “Error in LIKE ESCAPE”。</li>
 *   <li>而 `'\'`（一个反斜杠）在 H2 / SQLite 上合法、语义正确，到了 MySQL 又语法错误。</li>
 * </ul>
 * <p>也就是说 `'\'` 与 `'\\'` 各自只能覆盖一半方言，无法“一处编写、三方运行”。
 * 改用 `ESCAPE '!'` 后，SQLite 3.50.4 与 H2 2.3.232（MODE=MySQL 与常规模式）实测语义完全一致
 * （`%!%%` 只命中字面 `%`、`%!_%` 只命中字面 `_`、`%!!%` 只命中字面 `!`），
 * 且 MySQL 官方文档明确允许任意单字符 ESCAPE（示例即 `ESCAPE '|'`），因此 `!` 是唯一
 * 三种方言通用的选择。</p>
 *
 * <p><b>注意：不要转义反斜杠。</b>转义字符是 `!` 时，反斜杠不是元字符，把它也转义
 * 会额外插入一个 `!`（`a\b` → `a!\b`），在 SQL 标准下属于“转义符后跟非 `%`/`_`/自身字符”
 * 的未定义行为，语义可能随方言漂移。</p>
 */
public final class SqlLikeUtils {

    /**
     * 转义字符本身。用 `!` 而不是 `\`：见类注释，反斜杠在 MySQL 与 H2/SQLite 之间
     * 没有通用的字面量写法。
     */
    public static final char ESCAPE_CHAR = '!';

    /**
     * 追加到 LIKE 片段后的 ESCAPE 子句。所有关键词 LIKE 都必须带上它：
     * 省略 ESCAPE 时 `!%` 会被当成字面两字符序列而不是“字面百分号”，转义静默失效。
     */
    public static final String ESCAPE_CLAUSE = " ESCAPE '!'";

    private SqlLikeUtils() {
    }

    /**
     * 转义关键词中所有对 LIKE 有特殊含义的字符：转义符自身、`%`、`_`。
     *
     * @param keyword 原始关键词
     * @return 可安全拼进 LIKE 模式的字符串；入参为 null 时返回 null
     */
    public static String escape(String keyword) {
        if (keyword == null) {
            return null;
        }
        StringBuilder escaped = new StringBuilder(keyword.length() + 8);
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            if (c == ESCAPE_CHAR || c == '%' || c == '_') {
                escaped.append(ESCAPE_CHAR);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    /**
     * 构造“包含关键词”的 LIKE 模式（两侧 `%` 通配）。
     *
     * @param keyword 原始关键词
     * @return 形如 `%keyword%` 的已转义模式
     */
    public static String containsPattern(String keyword) {
        return "%" + escape(keyword) + "%";
    }

    /**
     * 构造“以关键词开头”的 LIKE 模式（右侧 `%` 通配）。
     */
    public static String prefixPattern(String keyword) {
        return escape(keyword) + "%";
    }

    /**
     * 构造“以关键词结尾”的 LIKE 模式（左侧 `%` 通配）。
     */
    public static String suffixPattern(String keyword) {
        return "%" + escape(keyword);
    }
}
