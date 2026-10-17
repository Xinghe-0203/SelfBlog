package com.selfblog.dto.site;

import lombok.Data;

/**
 * 站点地图用的标签更新时间行：标签 id + 该标签下最新一篇已发布文章的时间。
 *
 * <p>时间为数据库原生类型（H2/MySQL 为 {@code LocalDateTime}，SQLite 驱动为字符串），
 * 由写入方统一归一为 {@code yyyy-MM-dd}；{@link Object} 属性走 MyBatis 的
 * {@code ObjectTypeHandler}（即 {@code ResultSet.getObject}），不依赖方言。
 */
@Data
public class TagLastModified {
    private Long tagId;
    private Object lastModified;
}
