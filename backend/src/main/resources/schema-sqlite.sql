-- ============================================================================
-- SelfBlog - SQLite 数据库初始化脚本
-- ============================================================================
-- 说明: 此脚本为 SQLite 兼容版本，由 MySQL 主脚本转换而来
--       SQLite 不支持: ENGINE, CHARSET, COMMENT, AUTO_INCREMENT, ON UPDATE CURRENT_TIMESTAMP
--       已转换为 SQLite 等价语法
-- ============================================================================

-- ============================================================================
-- 表一：用户表 (sys_user)
-- ============================================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    username          TEXT            NOT NULL UNIQUE,
    password          TEXT            NOT NULL,
    nickname          TEXT            DEFAULT NULL,
    avatar            TEXT            DEFAULT NULL,
    cover_image       TEXT            DEFAULT NULL,
    bio               TEXT            DEFAULT NULL,
    follower_count    INTEGER         DEFAULT 0,
    following_count   INTEGER         DEFAULT 0,
    email             TEXT            DEFAULT NULL UNIQUE,
    role              TEXT            DEFAULT 'user',
    status            INTEGER         DEFAULT 1,
    login_fail_count  INTEGER         DEFAULT 0,
    lock_until        TEXT            DEFAULT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    update_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

-- ============================================================================
-- 表二：帖子/文章表 (blog_post)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_post (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         NOT NULL,
    title             TEXT            NOT NULL,
    summary           TEXT            DEFAULT NULL,
    cover_url         TEXT            DEFAULT NULL,
    content           TEXT            NOT NULL,
    topic_ids         TEXT            DEFAULT NULL,
    category          TEXT            DEFAULT '其他',
    view_count        INTEGER         DEFAULT 0,
    like_count        INTEGER         DEFAULT 0,
    comment_count     INTEGER         DEFAULT 0,
    collect_count     INTEGER         DEFAULT 0,
    share_count       INTEGER         DEFAULT 0,
    status            INTEGER         DEFAULT 1,
    reviewer_id       INTEGER         DEFAULT NULL,
    review_time       TEXT            DEFAULT NULL,
    reject_reason     TEXT            DEFAULT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    update_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_post_user_id ON blog_post (user_id);
CREATE INDEX IF NOT EXISTS idx_blog_post_category ON blog_post (category);
CREATE INDEX IF NOT EXISTS idx_blog_post_create_time ON blog_post (create_time);
CREATE INDEX IF NOT EXISTS idx_blog_post_status_deleted ON blog_post (status, is_deleted);

-- ============================================================================
-- 表三：评论表 (blog_comment)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_comment (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    post_id           INTEGER         NOT NULL,
    user_id           INTEGER         NOT NULL,
    parent_id         INTEGER         DEFAULT NULL,
    content           TEXT            NOT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_comment_post_id ON blog_comment (post_id);
CREATE INDEX IF NOT EXISTS idx_blog_comment_user_id ON blog_comment (user_id);
CREATE INDEX IF NOT EXISTS idx_blog_comment_parent_id ON blog_comment (parent_id);

-- ============================================================================
-- 表四：标签表 (blog_tag)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_tag (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    name              TEXT            NOT NULL UNIQUE,
    post_count        INTEGER         DEFAULT 0,
    is_deleted        INTEGER         DEFAULT 0
);

-- ============================================================================
-- 表五：帖子-标签关联表 (blog_post_tag)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_post_tag (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    post_id           INTEGER         NOT NULL,
    tag_id            INTEGER         NOT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0,
    UNIQUE(post_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_post_tag_tag_id ON blog_post_tag (tag_id);

-- ============================================================================
-- 表六：点赞记录表 (blog_like)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_like (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         NOT NULL,
    post_id           INTEGER         NOT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0,
    UNIQUE(user_id, post_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_like_post_id ON blog_like (post_id);

-- ============================================================================
-- 表七：收藏记录表 (blog_collect)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_collect (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         NOT NULL,
    post_id           INTEGER         NOT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0,
    UNIQUE(user_id, post_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_collect_post_id ON blog_collect (post_id);

-- ============================================================================
-- 表七点五：文章分享记录表 (blog_share)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_share (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         DEFAULT NULL,
    post_id           INTEGER         NOT NULL,
    platform          TEXT            DEFAULT 'web',
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_share_post_id ON blog_share (post_id);
CREATE INDEX IF NOT EXISTS idx_blog_share_user_id ON blog_share (user_id);
CREATE INDEX IF NOT EXISTS idx_blog_share_create_time ON blog_share (create_time);

-- ============================================================================
-- 表八：关注关系表 (blog_follow)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_follow (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    follower_id       INTEGER         NOT NULL,
    following_id      INTEGER         NOT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0,
    UNIQUE(follower_id, following_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_follow_following_id ON blog_follow (following_id);
CREATE INDEX IF NOT EXISTS idx_blog_follow_create_time ON blog_follow (create_time);

-- ============================================================================
-- 表九：通知表 (blog_notification)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_notification (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         NOT NULL,
    type              TEXT            NOT NULL,
    title             TEXT            NOT NULL,
    content           TEXT            DEFAULT NULL,
    from_user_id      INTEGER         DEFAULT NULL,
    target_type       TEXT            DEFAULT NULL,
    target_id         INTEGER         DEFAULT NULL,
    is_read           INTEGER         DEFAULT 0,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_notification_user_id ON blog_notification (user_id);
CREATE INDEX IF NOT EXISTS idx_blog_notification_user_unread ON blog_notification (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_blog_notification_create_time ON blog_notification (create_time);

-- ============================================================================
-- 表十：文章热度表 (blog_trending)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_trending (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    post_id           INTEGER         NOT NULL,
    score             REAL            NOT NULL DEFAULT 0,
    view_count        INTEGER         NOT NULL DEFAULT 0,
    like_count        INTEGER         NOT NULL DEFAULT 0,
    comment_count     INTEGER         NOT NULL DEFAULT 0,
    date              TEXT            NOT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    update_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0,
    UNIQUE(post_id, date)
);

CREATE INDEX IF NOT EXISTS idx_blog_trending_score ON blog_trending (score DESC);

-- ============================================================================
-- 表十一：文章草稿表 (blog_draft)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_draft (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         NOT NULL,
    title             TEXT            DEFAULT NULL,
    content           TEXT            DEFAULT NULL,
    cover_image       TEXT            DEFAULT NULL,
    summary           TEXT            DEFAULT NULL,
    category          TEXT            DEFAULT NULL,
    tag_ids           TEXT            DEFAULT NULL,
    topic_id          INTEGER         DEFAULT NULL,
    post_id           INTEGER         DEFAULT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    update_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_draft_user_id ON blog_draft (user_id);
CREATE INDEX IF NOT EXISTS idx_blog_draft_post_id ON blog_draft (post_id);

-- ============================================================================
-- 表十一点五：草稿-标签关联表 (blog_draft_tag)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_draft_tag (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    draft_id          INTEGER         NOT NULL,
    tag_id            INTEGER         NOT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0,
    UNIQUE(draft_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_draft_tag_tag_id ON blog_draft_tag (tag_id);

-- ============================================================================
-- 表十二：内容举报表 (blog_report)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_report (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    reporter_id       INTEGER         NOT NULL,
    reported_user_id  INTEGER         DEFAULT NULL,
    target_type       TEXT            NOT NULL,
    target_id         INTEGER         NOT NULL,
    reason            TEXT            NOT NULL,
    status            INTEGER         DEFAULT 0,
    handler_id        INTEGER         DEFAULT NULL,
    handler_result    TEXT            DEFAULT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    handle_time       TEXT            DEFAULT NULL,
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_report_reporter_id ON blog_report (reporter_id);
CREATE INDEX IF NOT EXISTS idx_blog_report_status ON blog_report (status);

-- ============================================================================
-- 表十三：校友圈动态表 (blog_circle_post)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_circle_post (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         NOT NULL,
    content           TEXT            NOT NULL,
    content_type      INTEGER         DEFAULT 1,
    image_urls        TEXT            DEFAULT NULL,
    video_urls        TEXT            DEFAULT NULL,
    repost_id         INTEGER         DEFAULT NULL,
    repost_user_id    INTEGER         DEFAULT NULL,
    repost_content    TEXT            DEFAULT NULL,
    tags              TEXT            DEFAULT NULL,
    mentions          TEXT            DEFAULT NULL,
    topic_ids         TEXT            DEFAULT NULL,
    location          TEXT            DEFAULT NULL,
    like_count        INTEGER         DEFAULT 0,
    comment_count     INTEGER         DEFAULT 0,
    repost_count      INTEGER         DEFAULT 0,
    view_count        INTEGER         DEFAULT 0,
    is_top            INTEGER         DEFAULT 0,
    visibility        INTEGER         DEFAULT 0,
    allow_comment     INTEGER         DEFAULT 1,
    allow_repost      INTEGER         DEFAULT 1,
    status            INTEGER         DEFAULT 1,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    update_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_circle_post_user_id ON blog_circle_post (user_id);
CREATE INDEX IF NOT EXISTS idx_blog_circle_post_create_time ON blog_circle_post (create_time);
CREATE INDEX IF NOT EXISTS idx_blog_circle_post_like_count ON blog_circle_post (like_count DESC);
CREATE INDEX IF NOT EXISTS idx_blog_circle_post_repost_id ON blog_circle_post (repost_id);

-- ============================================================================
-- 表十四：校友圈点赞表 (blog_circle_like)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_circle_like (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         NOT NULL,
    post_id           INTEGER         NOT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0,
    UNIQUE(user_id, post_id)
);

CREATE INDEX IF NOT EXISTS idx_blog_circle_like_post_id ON blog_circle_like (post_id);

-- ============================================================================
-- 表十五：校友圈评论表 (blog_circle_comment)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_circle_comment (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    post_id           INTEGER         NOT NULL,
    user_id           INTEGER         NOT NULL,
    content           TEXT            NOT NULL,
    parent_id         INTEGER         DEFAULT NULL,
    reply_to_user_id  INTEGER         DEFAULT NULL,
    like_count        INTEGER         DEFAULT 0,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    update_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_circle_comment_post_id ON blog_circle_comment (post_id);
CREATE INDEX IF NOT EXISTS idx_blog_circle_comment_parent_id ON blog_circle_comment (parent_id);

-- ============================================================================
-- 表十六：校友圈转发表 (blog_circle_repost)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_circle_repost (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         NOT NULL,
    original_post_id  INTEGER         NOT NULL,
    new_post_id       INTEGER         NOT NULL,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_circle_repost_original_post_id ON blog_circle_repost (original_post_id);
CREATE INDEX IF NOT EXISTS idx_blog_circle_repost_user_id ON blog_circle_repost (user_id);

-- ============================================================================
-- 表十七：媒体资源表 (blog_media)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_media (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER         NOT NULL,
    file_name         TEXT            NOT NULL,
    file_path         TEXT            NOT NULL,
    file_url          TEXT            NOT NULL,
    file_type         TEXT            NOT NULL,
    mime_type         TEXT            NOT NULL,
    file_size         INTEGER         NOT NULL,
    width             INTEGER         DEFAULT NULL,
    height            INTEGER         DEFAULT NULL,
    thumb_url         TEXT            DEFAULT NULL,
    status            INTEGER         DEFAULT 1,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    update_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_media_user_id ON blog_media (user_id);
CREATE INDEX IF NOT EXISTS idx_blog_media_create_time ON blog_media (create_time);
CREATE INDEX IF NOT EXISTS idx_blog_media_file_type ON blog_media (file_type);

-- ============================================================================
-- 表十八：文章媒体关联表 (blog_post_media)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_post_media (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    post_id           INTEGER         NOT NULL,
    media_id          INTEGER         NOT NULL,
    display_order     INTEGER         DEFAULT 0,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_post_media_post_id ON blog_post_media (post_id);
CREATE INDEX IF NOT EXISTS idx_blog_post_media_media_id ON blog_post_media (media_id);

-- ============================================================================
-- 表十九：话题表 (blog_topic)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_topic (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    name              TEXT            NOT NULL UNIQUE,
    description       TEXT            DEFAULT NULL,
    post_count        INTEGER         DEFAULT 0,
    trending_score    INTEGER         DEFAULT 0,
    status            INTEGER         DEFAULT 1,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    update_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

-- ============================================================================
-- 表二十：私信表 (blog_message)
-- ============================================================================
CREATE TABLE IF NOT EXISTS blog_message (
    id                INTEGER         PRIMARY KEY AUTOINCREMENT,
    sender_id         INTEGER         NOT NULL,
    receiver_id       INTEGER         NOT NULL,
    content           TEXT            NOT NULL,
    is_read           INTEGER         DEFAULT 0,
    create_time       TEXT            DEFAULT (datetime('now','localtime')),
    is_deleted        INTEGER         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_blog_message_receiver_id ON blog_message (receiver_id);
CREATE INDEX IF NOT EXISTS idx_blog_message_sender_id ON blog_message (sender_id);
CREATE INDEX IF NOT EXISTS idx_blog_message_create_time ON blog_message (create_time);

-- ============================================================================
-- 性能优化索引（SQLite 版本，使用 CREATE INDEX IF NOT EXISTS）
-- ============================================================================

-- blog_post 性能索引
CREATE INDEX IF NOT EXISTS idx_blog_post_user_create_time ON blog_post (user_id, create_time);
CREATE INDEX IF NOT EXISTS idx_blog_post_like_count ON blog_post (like_count);
CREATE INDEX IF NOT EXISTS idx_blog_post_view_count ON blog_post (view_count);
CREATE INDEX IF NOT EXISTS idx_post_status_deleted_create ON blog_post (status, is_deleted, create_time);
CREATE INDEX IF NOT EXISTS idx_post_status_deleted_view ON blog_post (status, is_deleted, view_count);
CREATE INDEX IF NOT EXISTS idx_post_status_deleted_like ON blog_post (status, is_deleted, like_count);

-- blog_comment 性能索引
CREATE INDEX IF NOT EXISTS idx_comment_post_id ON blog_comment (post_id, create_time);
CREATE INDEX IF NOT EXISTS idx_blog_comment_post_create_time ON blog_comment (post_id, create_time);
CREATE INDEX IF NOT EXISTS idx_blog_comment_parent_create_time ON blog_comment (parent_id, create_time);

-- blog_like 性能索引
CREATE INDEX IF NOT EXISTS idx_like_post_id ON blog_like (post_id, create_time);
CREATE INDEX IF NOT EXISTS idx_blog_like_user_create_time ON blog_like (user_id, create_time);

-- blog_collect 性能索引
CREATE INDEX IF NOT EXISTS idx_collect_post_id ON blog_collect (post_id, create_time);
CREATE INDEX IF NOT EXISTS idx_blog_collect_user_create_time ON blog_collect (user_id, create_time);

-- blog_follow 性能索引
CREATE INDEX IF NOT EXISTS idx_blog_follow_follower_is_deleted ON blog_follow (follower_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_follow_create_time ON blog_follow (create_time);

-- blog_report 性能索引
CREATE INDEX IF NOT EXISTS idx_report_handle_time ON blog_report (handle_time, status);
CREATE INDEX IF NOT EXISTS idx_blog_report_status_create_time ON blog_report (status, create_time);
CREATE INDEX IF NOT EXISTS idx_blog_report_target ON blog_report (target_type, target_id);

-- blog_notification 性能索引
CREATE INDEX IF NOT EXISTS idx_notification_user_read_create ON blog_notification (user_id, is_read, create_time);
CREATE INDEX IF NOT EXISTS idx_blog_notification_user_is_read ON blog_notification (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_blog_notification_type ON blog_notification (type);
CREATE INDEX IF NOT EXISTS idx_blog_notification_create_time ON blog_notification (create_time);

-- blog_message 性能索引
CREATE INDEX IF NOT EXISTS idx_message_receiver_read ON blog_message (receiver_id, is_read);
CREATE INDEX IF NOT EXISTS idx_message_sender_time ON blog_message (sender_id, create_time);
CREATE INDEX IF NOT EXISTS idx_message_receiver_time ON blog_message (receiver_id, create_time);
CREATE INDEX IF NOT EXISTS idx_blog_message_sender_receiver_deleted ON blog_message (sender_id, receiver_id, is_deleted);

-- blog_draft 性能索引
CREATE INDEX IF NOT EXISTS idx_blog_draft_user_deleted ON blog_draft (user_id, is_deleted);

-- blog_circle_post 性能索引
CREATE INDEX IF NOT EXISTS idx_circle_status_visibility_like ON blog_circle_post (status, visibility, like_count DESC);
CREATE INDEX IF NOT EXISTS idx_circle_post_visibility_deleted ON blog_circle_post (visibility, is_deleted);
CREATE INDEX IF NOT EXISTS idx_circle_post_top ON blog_circle_post (is_top DESC);

-- blog_topic 性能索引
CREATE INDEX IF NOT EXISTS idx_topic_trending ON blog_topic (trending_score DESC);

-- blog_media 性能索引
CREATE INDEX IF NOT EXISTS idx_media_user_type ON blog_media (user_id, file_type);

-- blog_circle_comment 性能索引
CREATE INDEX IF NOT EXISTS idx_circle_comment_post ON blog_circle_comment (post_id, create_time);
CREATE INDEX IF NOT EXISTS idx_circle_comment_post_time ON blog_circle_comment (post_id, create_time);

-- blog_trending 性能索引
CREATE INDEX IF NOT EXISTS idx_trending_date_score ON blog_trending (date, score DESC);

-- blog_post_tag 性能索引
CREATE INDEX IF NOT EXISTS idx_post_tag_tag_id ON blog_post_tag (tag_id, post_id);

-- sys_user 性能索引
CREATE INDEX IF NOT EXISTS idx_user_create_time ON sys_user (create_time);

-- One-time bootstrap credentials are read from .env by DataInitializer.

-- SelfBlog features; CREATE IF NOT EXISTS also upgrades existing SQLite files.
CREATE TABLE IF NOT EXISTS site_setting (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL,
    update_time TEXT DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS visitor_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    visitor_hash TEXT NOT NULL,
    ip_hash TEXT NOT NULL,
    user_agent TEXT DEFAULT NULL,
    page_path TEXT NOT NULL,
    referrer TEXT DEFAULT NULL,
    create_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_visitor_event_time ON visitor_event (create_time);
CREATE INDEX IF NOT EXISTS idx_visitor_event_visitor_time ON visitor_event (visitor_hash, create_time);
CREATE INDEX IF NOT EXISTS idx_visitor_event_path_time ON visitor_event (page_path, create_time);

CREATE TABLE IF NOT EXISTS anonymous_post_like (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL,
    visitor_hash TEXT NOT NULL,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    is_deleted INTEGER DEFAULT 0,
    UNIQUE(post_id, visitor_hash)
);
CREATE INDEX IF NOT EXISTS idx_anonymous_like_post ON anonymous_post_like (post_id, is_deleted);

CREATE TABLE IF NOT EXISTS guest_comment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL,
    visitor_hash TEXT NOT NULL,
    nickname TEXT NOT NULL,
    content TEXT NOT NULL,
    status INTEGER DEFAULT 1,
    create_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_guest_comment_post ON guest_comment (post_id, status, create_time);

CREATE TABLE IF NOT EXISTS photo_album (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT DEFAULT NULL,
    is_private INTEGER DEFAULT 0,
    password_hash TEXT DEFAULT NULL,
    cover_photo_id INTEGER DEFAULT NULL,
    sort_order INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime')),
    is_deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_photo_album_order ON photo_album (is_deleted, sort_order, create_time);

CREATE TABLE IF NOT EXISTS album_photo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    album_id INTEGER NOT NULL,
    file_path TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    caption TEXT DEFAULT NULL,
    sort_order INTEGER DEFAULT 0,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    is_deleted INTEGER DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_album_photo_order ON album_photo (album_id, is_deleted, sort_order, create_time);

CREATE TABLE IF NOT EXISTS post_seo (
    post_id          INTEGER     PRIMARY KEY,
    seo_title        TEXT        DEFAULT NULL,
    seo_description  TEXT        DEFAULT NULL,
    seo_keywords     TEXT        DEFAULT NULL,
    og_image_url     TEXT        DEFAULT NULL,
    source           TEXT        NOT NULL DEFAULT 'manual',
    generated_at     TEXT        DEFAULT NULL,
    update_time      TEXT        NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_post_seo_generated_at ON post_seo (generated_at);
