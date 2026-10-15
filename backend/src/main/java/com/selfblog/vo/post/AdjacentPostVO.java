package com.selfblog.vo.post;

import lombok.Data;

/**
 * 相邻文章（上一篇 / 下一篇）的精简信息，文章详情页底部导航使用。
 */
@Data
public class AdjacentPostVO {
    private Long id;
    private String title;
}
