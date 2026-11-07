package com.selfblog.dto.site;

import lombok.Data;

/**
 * 访客看板的热门页面行：路径 + 30 天内浏览量。
 *
 * <p>SQL 里直接 {@code GROUP BY page_path ... ORDER BY COUNT(*) DESC LIMIT 8}，
 * 替代旧的「LIMIT 20000 拉明细行 + 内存 HashMap 计数」。别名一律双引号，
 * 避免 H2 把不带引号的别名折叠成大写（typed DTO 的属性匹配本身大小写不敏感，
 * 双引号只是与 {@code countPostsGroupByDate} 保持同一书写约定）。</p>
 */
@Data
public class VisitorPathCount {
    private String path;
    private Long views;
}
