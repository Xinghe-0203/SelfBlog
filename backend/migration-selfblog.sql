-- Additive selfblog schema migration for an existing MySQL database.
-- Select the existing application database before running this file.
-- Safe to re-run; it does not drop, rebuild, or modify existing article tables/data.

CREATE TABLE IF NOT EXISTS `site_setting` (
    `setting_key` VARCHAR(100) NOT NULL,
    `setting_value` TEXT NOT NULL,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点设置';

CREATE TABLE IF NOT EXISTS `visitor_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `visitor_hash` CHAR(64) NOT NULL,
    `ip_hash` CHAR(64) NOT NULL,
    `user_agent` VARCHAR(500) DEFAULT NULL,
    `page_path` VARCHAR(255) NOT NULL,
    `referrer` VARCHAR(500) DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_visitor_event_time` (`create_time`),
    INDEX `idx_visitor_event_visitor_time` (`visitor_hash`, `create_time`),
    INDEX `idx_visitor_event_path_time` (`page_path`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='匿名访客访问记录';

CREATE TABLE IF NOT EXISTS `anonymous_post_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `visitor_hash` CHAR(64) NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_anon_like_post_visitor` (`post_id`, `visitor_hash`),
    INDEX `idx_anonymous_like_post` (`post_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客匿名文章点赞';

CREATE TABLE IF NOT EXISTS `guest_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `visitor_hash` CHAR(64) NOT NULL,
    `nickname` VARCHAR(80) NOT NULL,
    `content` TEXT NOT NULL,
    `status` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_guest_comment_post` (`post_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客文章评论';

CREATE TABLE IF NOT EXISTS `photo_album` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(160) NOT NULL,
    `description` VARCHAR(1000) DEFAULT NULL,
    `is_private` TINYINT(1) DEFAULT 0,
    `password_hash` VARCHAR(100) DEFAULT NULL,
    `cover_photo_id` BIGINT DEFAULT NULL,
    `sort_order` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_photo_album_order` (`is_deleted`, `sort_order`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照片相册';

CREATE TABLE IF NOT EXISTS `album_photo` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `album_id` BIGINT NOT NULL,
    `file_path` VARCHAR(500) NOT NULL,
    `mime_type` VARCHAR(80) NOT NULL,
    `caption` VARCHAR(500) DEFAULT NULL,
    `sort_order` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_deleted` TINYINT(1) DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_album_photo_order` (`album_id`, `is_deleted`, `sort_order`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相册照片';

CREATE TABLE IF NOT EXISTS `post_seo` (
    `post_id` BIGINT NOT NULL,
    `seo_title` VARCHAR(200) DEFAULT NULL,
    `seo_description` VARCHAR(400) DEFAULT NULL,
    `seo_keywords` VARCHAR(300) DEFAULT NULL,
    `og_image_url` VARCHAR(500) DEFAULT NULL,
    `source` VARCHAR(20) NOT NULL DEFAULT 'manual',
    `generated_at` DATETIME DEFAULT NULL,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`post_id`),
    INDEX `idx_post_seo_generated_at` (`generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章 SEO 元数据';
