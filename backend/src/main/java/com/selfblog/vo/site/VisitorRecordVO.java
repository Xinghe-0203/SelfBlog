package com.selfblog.vo.site;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitorRecordVO {
    private Long id;
    private String visitor;
    private String network;
    private String userAgent;
    private String path;
    private String referrer;
    private LocalDateTime time;
}
