package com.selfblog.vo.site;

import lombok.Data;

import java.util.List;

@Data
public class VisitorSummaryVO {
    private long totalVisitors;
    private long todayVisitors;
    private long totalPageViews;
    private long todayPageViews;
    private long articleViews;
    private List<Daily> daily;
    private List<TopPage> topPages;

    @Data
    public static class Daily {
        private String date;
        private long visitors;
        private long pageViews;
    }

    @Data
    public static class TopPage {
        private String path;
        private long views;
    }
}
