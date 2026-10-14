package com.selfblog.vo.site;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GuestCommentVO {
    private Long id;
    private Long postId;
    private String postTitle;
    private String nickname;
    private String content;
    private Integer status;
    private LocalDateTime createTime;
}
