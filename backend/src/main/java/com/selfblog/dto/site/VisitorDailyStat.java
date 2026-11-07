package com.selfblog.dto.site;

import lombok.Data;

/**
 * 访客看板的每日聚合行：日期 + 当日独立访客数 + 当日页面浏览量。
 *
 * <p>一条 GROUP BY 查询替代旧的「14 天 × 2 条 COUNT」循环。日期列为数据库原生类型
 * （H2 为 {@code LocalDate}、MySQL 为 {@code java.sql.Date}、SQLite 驱动为字符串），
 * 与 {@link TagLastModified} 同一处理方式：属性声明为 {@code Object}，由写入方
 * （{@code VisitorServiceImpl#dayValue}）统一归一成 {@code yyyy-MM-dd} 字符串，
 * 不依赖方言。计数属性用包装类型，配合 {@code LongTypeHandler} 兼容各库的
 * BIGINT/DECIMAL 返回。</p>
 */
@Data
public class VisitorDailyStat {
    private Object day;
    private Long visitors;
    private Long views;
}
