package com.selfblog.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具类
 */
public class TimeUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 计算相对时间描述（如"刚刚"、"5分钟前"、"3小时前"、"2天前"等）
     */
    public static String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);
        if (duration.isNegative()) {
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "刚刚";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分钟前";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "小时前";
        }
        long days = hours / 24;
        if (days < 30) {
            return days + "天前";
        }
        long months = days / 30;
        if (months < 12) {
            return months + "个月前";
        }
        long years = days / 365;
        if (years < 3) {
            return years + "年前";
        }
        return dateTime.format(DATE_FORMATTER);
    }
}
