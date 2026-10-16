CREATE ALIAS IF NOT EXISTS DATE_ADD AS '
java.sql.Timestamp dateAdd(java.sql.Timestamp d, int n, String u)
    throws Exception
{
    java.util.Calendar c = java.util.Calendar.getInstance();
    c.setTime(d);
    if ("MINUTE".equalsIgnoreCase(u)) {
        c.add(java.util.Calendar.MINUTE, n);
    } else if ("SECOND".equalsIgnoreCase(u)) {
        c.add(java.util.Calendar.SECOND, n);
    } else if ("HOUR".equalsIgnoreCase(u)) {
        c.add(java.util.Calendar.HOUR, n);
    }
    return new java.sql.Timestamp(c.getTimeInMillis());
}
';

-- ============================================================================
-- 表一：用户表 (sys_user)
-- ============================================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(500),
    cover_image VARCHAR(500),
    bio VARCHAR(500),
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'user',
    status INT DEFAULT 1,
    follower_count INT DEFAULT 0,
    following_count INT DEFAULT 0,
    login_fail_count INT DEFAULT 0,
    lock_until TIMESTAMP NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表二：帖子/文章表 (blog_post)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500),
    content CLOB,
    topic_ids VARCHAR(500),
    category VARCHAR(50),
    cover_url VARCHAR(500),
    view_count BIGINT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    collect_count INT DEFAULT 0,
    share_count INT DEFAULT 0,
    status INT DEFAULT 1,
    reviewer_id BIGINT,
    review_time TIMESTAMP NULL,
    reject_reason VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表三：评论表 (blog_comment)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表四：标签表 (blog_tag)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    post_count INT DEFAULT 0,
    is_deleted INT DEFAULT 0,
    -- 与 MySQL/SQLite 一致：标签名唯一，删除后重建依赖恢复而不是新插一行
    UNIQUE(name)
);

-- ============================================================================
-- 表五：帖子-标签关联表 (blog_post_tag)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_post_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    -- 与 MySQL/SQLite 一致：同一篇同一标签只有一行，重插依赖 upsert 恢复 is_deleted
    UNIQUE(post_id, tag_id)
);

-- ============================================================================
-- 表六：点赞记录表 (blog_like)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    -- 与 MySQL/SQLite 一致：同一用户对同一文章只有一行，取消后重新点赞依赖恢复标记
    UNIQUE(user_id, post_id)
);

-- ============================================================================
-- 表七：收藏记录表 (blog_collect)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_collect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    -- 与 MySQL/SQLite 一致
    UNIQUE(user_id, post_id)
);

-- ============================================================================
-- 表七点五：文章分享记录表 (blog_share)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    post_id BIGINT NOT NULL,
    platform VARCHAR(50) DEFAULT 'web',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表八：关注关系表 (blog_follow)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_follow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    -- 与 MySQL/SQLite 一致
    UNIQUE(follower_id, following_id)
);

-- ============================================================================
-- 表九：通知表 (blog_notification)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(500),
    from_user_id BIGINT,
    target_type VARCHAR(50),
    target_id BIGINT,
    is_read INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表十：文章热度表 (blog_trending)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_trending (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    score DOUBLE DEFAULT 0,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    date DATE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    -- 与 MySQL/SQLite 一致：每篇文章每天只有一行，upsert 依赖该唯一键
    UNIQUE(post_id, date)
);

-- ============================================================================
-- 表十一：文章草稿表 (blog_draft)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_draft (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    content CLOB,
    cover_image VARCHAR(500),
    summary VARCHAR(500),
    category VARCHAR(50),
    tag_ids VARCHAR(200),
    topic_id BIGINT,
    post_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表十一点五：草稿-标签关联表 (blog_draft_tag)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_draft_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    draft_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    -- 与 MySQL/SQLite 一致
    UNIQUE(draft_id, tag_id)
);

-- ============================================================================
-- 表十二：内容举报表 (blog_report)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    reported_user_id BIGINT,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status INT DEFAULT 0,
    handler_id BIGINT,
    handler_result VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    handle_time TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表十三：校友圈动态表 (blog_circle_post)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_circle_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    content_type INT DEFAULT 1,
    image_urls TEXT,
    video_urls TEXT,
    repost_id BIGINT,
    repost_user_id BIGINT,
    repost_content VARCHAR(500),
    tags TEXT,
    mentions TEXT,
    topic_ids TEXT,
    location VARCHAR(100),
    like_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    repost_count INT DEFAULT 0,
    view_count BIGINT DEFAULT 0,
    is_top INT DEFAULT 0,
    visibility INT DEFAULT 0,
    allow_comment INT DEFAULT 1,
    allow_repost INT DEFAULT 1,
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表十四：校友圈点赞表 (blog_circle_like)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_circle_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表十五：校友圈评论表 (blog_circle_comment)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_circle_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(500) NOT NULL,
    parent_id BIGINT,
    reply_to_user_id BIGINT,
    like_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表十六：校友圈转发表 (blog_circle_repost)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_circle_repost (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_post_id BIGINT NOT NULL,
    new_post_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表十七：媒体资源表 (blog_media)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    width INT,
    height INT,
    thumb_url VARCHAR(500),
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表十八：文章媒体关联表 (blog_post_media)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_post_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    media_id BIGINT NOT NULL,
    display_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表十九：话题表 (blog_topic)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_topic (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    post_count INT DEFAULT 0,
    trending_score INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

-- ============================================================================
-- 表二十：私信表 (blog_message)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_read INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS site_setting (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value CLOB NOT NULL,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS visitor_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visitor_hash VARCHAR(64) NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    user_agent VARCHAR(500),
    page_path VARCHAR(255) NOT NULL,
    referrer VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_visitor_event_time ON visitor_event (create_time);
CREATE INDEX IF NOT EXISTS idx_visitor_event_visitor_time ON visitor_event (visitor_hash, create_time);
CREATE INDEX IF NOT EXISTS idx_visitor_event_path_time ON visitor_event (page_path, create_time);

CREATE TABLE IF NOT EXISTS anonymous_post_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    visitor_hash VARCHAR(64) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0,
    CONSTRAINT uk_anon_like_post_visitor UNIQUE (post_id, visitor_hash)
);
CREATE INDEX IF NOT EXISTS idx_anonymous_like_post ON anonymous_post_like (post_id, is_deleted);

CREATE TABLE IF NOT EXISTS guest_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    visitor_hash VARCHAR(64) NOT NULL,
    nickname VARCHAR(80) NOT NULL,
    content CLOB NOT NULL,
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_guest_comment_post ON guest_comment (post_id, status, create_time);

CREATE TABLE IF NOT EXISTS photo_album (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    is_private INT DEFAULT 0,
    password_hash VARCHAR(100),
    cover_photo_id BIGINT,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_photo_album_order ON photo_album (is_deleted, sort_order, create_time);

CREATE TABLE IF NOT EXISTS album_photo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    album_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(80) NOT NULL,
    caption VARCHAR(500),
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted INT DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_album_photo_order ON album_photo (album_id, is_deleted, sort_order, create_time);
