-- ============================================================================
-- blog_post / blog_draft 正文转义回滚脚本 (SQLite / MySQL 通用)
-- ============================================================================
-- 用途：把历史上被整篇 HTML 转义过的正文还原成原始 Markdown
--
-- 原因：v2.0.30 第五批之前，文章与草稿的正文在**保存时**被
--       HtmlUtils.htmlEscape 整篇转义（`&` → `&amp;`、`<` → `&lt;`、
--       `>` → `&gt;`、" → `&quot;`、' → `&#39;`）。这会破坏 Markdown 语法：
--         · 引用块标记 `>` 变成 `&gt;`，不再是引用块；
--         · 代码块里的 `>` 与 `'` 变成实体，读者在代码块里看到的是
--           `&gt;` / `&#39;` 而不是代码；
--         · 段落里的 `&` 变成 `&amp;`。
--       代码块按规范不解码实体，于是这些实体会以**字面量**出现在读者眼前。
--
-- 现在：正文改为**原样保存**，安全边界移到渲染时
--       （服务端 SafeMarkdown 的 Jsoup 白名单 / 前端 DOMPurify）。
--       本脚本负责把历史数据还原。
--
-- 安全：
--   · 只处理**带转义特征**的行：包含 `&#39;` / `&quot;` / `&gt;` / `&lt;`
--     这类 htmlEscape 产物的特征串。手工写的 `&amp;` 不会被误伤
--     （它不在特征串里）。
--   · 反解顺序固定：先 `&quot;` / `&#39;`，再 `&lt;` / `&gt;`，最后 `&amp;`
--     —— 与 htmlEscape 的转义顺序相反，才能把 `&amp;gt;` 正确还原成 `&gt;`
--     再还原成 `>`。
--   · 可重复执行：还原后的内容不再含特征串，第二次执行不会改动任何行。
--   · 建议先备份数据库。
--
-- 执行方式：
--   SQLite：sqlite3 campus_blog.db < fix-markdown-escaping.sql
--   MySQL ：mysql -u <user> -p <db> < fix-markdown-escaping.sql
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 第一步：先看看有多少行会被改（只读预览，随时可中断）
-- ---------------------------------------------------------------------------
-- SELECT 'PREVIEW' AS check_type, id, title,
--        substr(content, 1, 60) AS content_head
-- FROM blog_post
-- WHERE is_deleted = 0
--   AND (content LIKE '%&#39;%' OR content LIKE '%&quot;%'
--        OR content LIKE '%&gt;%' OR content LIKE '%&lt;%')
-- ORDER BY id;

-- ---------------------------------------------------------------------------
-- 第二步：还原 blog_post 正文
-- ---------------------------------------------------------------------------
UPDATE blog_post
SET content = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        content,
        '&quot;', '"'),
        '&#39;', ''''),
        '&lt;', '<'),
        '&gt;', '>'),
        '&amp;', '&')
WHERE is_deleted = 0
  AND (content LIKE '%&#39;%' OR content LIKE '%&quot;%'
       OR content LIKE '%&gt;%' OR content LIKE '%&lt;%');

-- ---------------------------------------------------------------------------
-- 第三步：还原 blog_draft 正文（草稿同样被整篇转义过）
-- ---------------------------------------------------------------------------
UPDATE blog_draft
SET content = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
        content,
        '&quot;', '"'),
        '&#39;', ''''),
        '&lt;', '<'),
        '&gt;', '>'),
        '&amp;', '&')
WHERE content LIKE '%&#39;%' OR content LIKE '%&quot;%'
   OR content LIKE '%&gt;%' OR content LIKE '%&lt;%';
-- 说明：blog_draft 的主键列是 id（不是 draft_id）。

-- ---------------------------------------------------------------------------
-- 第四步：验证（下面这条应返回 0 行）
-- ---------------------------------------------------------------------------
-- SELECT 'REMAINING' AS check_type, id, title
-- FROM blog_post
-- WHERE is_deleted = 0
--   AND (content LIKE '%&#39;%' OR content LIKE '%&quot;%'
--        OR content LIKE '%&gt;%' OR content LIKE '%&lt;%')
-- ORDER BY id;
