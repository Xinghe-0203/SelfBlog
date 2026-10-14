-- ============================================================================
-- SelfBlog - 数据库初始化脚本 (SelfBlog Forum System)
-- ============================================================================
-- 作者: Xinghe / 刘畅
-- 作者网站 starsx.top
-- 作者博客 blog.starsx.top
-- 创建日期: 2025-07-01
-- 更新日期: 2026-04-27
-- 数据库版本: MySQL 8.0 及以上
-- 字符集: utf8mb4 (支持 emoji 表情和特殊字符)
-- 说明:
--   本脚本用于创建SelfBlog的核心数据表
--   采用"逻辑删除"策略，确保数据可追溯，防止误删
--   所有表均使用 InnoDB 引擎，保证事务支持和行级锁
-- ============================================================================


-- -------------------------------------------------------
-- 第一步：创建数据库（如果不存在）
-- -------------------------------------------------------
-- 如果已存在同名数据库，请删除后重新运行，或将此处注释掉
CREATE DATABASE IF NOT EXISTS `campus_blog`
    DEFAULT CHARACTER SET utf8mb4          -- 设置默认字符集为 utf8mb4，支持存储中文和 emoji
    DEFAULT COLLATE utf8mb4_unicode_ci;   -- 排序规则，使用 unicode 排序，兼容性好

-- 切换到当前数据库
USE `campus_blog`;


-- ============================================================================
-- 表一：用户表 (sys_user)
-- ============================================================================
-- 【业务说明】
--   存储论坛的所有用户信息，包括普通学生用户和管理员用户
--   用户可以进行注册、登录、发布文章、发表评论、点赞等操作
--
-- 【设计要点】
--   1. 用户名 (username) 设置唯一索引，防止重复注册
--   2. 密码 (password) 存储加密后的值（实际开发中应使用 BCrypt 加密）
--   3. 头像 (avatar) 存储头像图片的 URL 地址，默认为 NULL 表示未上传
--   4. 使用逻辑删除 (is_deleted) 而非物理删除，保留用户历史数据
--   5. create_time 和 update_time 自动维护，记录用户创建和最后修改时间
--   6. follower_count 和 following_count 为关注系统冗余计数字段
--
DROP TABLE IF EXISTS `sys_user`;  -- 如果表已存在，则先删除（重新建表时使用）

CREATE TABLE `sys_user` (
    -- -------------------- 基础信息字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID，自增长，用于唯一标识每个用户',

    `username`          VARCHAR(50)     NOT NULL                     COMMENT '用户名，用于登录系统的唯一账号',

    `password`          VARCHAR(100)    NOT NULL                     COMMENT '密码，经过加密处理后的存储值（实际项目应使用BCrypt加密存储）',

    `nickname`          VARCHAR(50)     DEFAULT NULL                  COMMENT '用户昵称，用于在论坛中显示的名称，不强制要求，可为空',

    `avatar`            VARCHAR(255)    DEFAULT NULL                  COMMENT '用户头像图片的URL地址链接，默认为NULL表示使用系统默认头像',

    `cover_image`       VARCHAR(500)    DEFAULT NULL                  COMMENT '用户资料页背景图 URL',

    `bio`               VARCHAR(500)    DEFAULT NULL                  COMMENT '用户简介',

    -- -------------------- 社交统计字段（关注系统） --------------------
    `follower_count`    INT              DEFAULT 0                    COMMENT '粉丝数，关注该用户的用户数量',

    `following_count`   INT              DEFAULT 0                    COMMENT '关注数，该用户关注的用户数量',

    `email`             VARCHAR(100)    DEFAULT NULL                  COMMENT '用户邮箱地址，用于找回密码或接收系统通知，可为空',

    -- -------------------- 权限与状态字段 --------------------
    `role`              VARCHAR(20)     DEFAULT 'user'                COMMENT '用户角色标识：user=普通用户，admin=管理员。可扩展其他角色',

    `status`            TINYINT(1)      DEFAULT 1                    COMMENT '账号状态：1=正常启用，0=禁用封禁。管理员可修改此字段来封禁违规用户',

    `login_fail_count`  INT              DEFAULT 0                    COMMENT '登录失败次数，连续失败5次后账号将被锁定',

    `lock_until`        DATETIME        DEFAULT NULL                  COMMENT '账号锁定截止时间，锁定期间无法登录，为NULL表示未锁定',

    -- -------------------- 时间戳字段 --------------------
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP     COMMENT '用户注册时间，自动取当前时间戳，记录用户创建时间',

    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP  COMMENT '用户信息最后更新时间，当记录被修改时自动更新',

    -- -------------------- 逻辑删除字段 --------------------
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除标记：0=正常数据（未删除），1=已删除数据。避免物理删除，保留数据可追溯',

    -- -------------------- 索引约束 --------------------
    PRIMARY KEY (`id`),                                                 -- 主键索引，加速按 ID 查询
    UNIQUE KEY `uk_username` (`username`)                              -- 唯一索引，保证用户名不重复，用于快速登录校验

) ENGINE=InnoDB                                                      -- 使用 InnoDB 引擎，支持事务和行级锁
  DEFAULT CHARSET=utf8mb4                                             -- 字符集，支持存储中文、emoji
  COMMENT='用户信息表：存储论坛所有用户的账号、密码和个人资料信息';


-- ============================================================================
-- 表二：帖子/文章表 (blog_post)
-- ============================================================================
-- 【业务说明】
--   存储用户发布的博客文章，是论坛的核心内容表
--   支持 Markdown 格式撰写文章正文，包含阅读量、点赞数等互动统计
--
-- 【设计要点】
--   1. 文章内容 (content) 使用 LONGTEXT 类型，支持存储大型 Markdown 文本
--   2. 摘要 (summary) 字段用于在列表页展示文章概要，避免加载全文影响性能
--   3. 阅读量 (view_count) 和点赞数 (like_count) 在列表查询时频繁使用
--      与文章基本信息一起存储，避免频繁 JOIN 查询（以空间换时间）
--   4. 分类 (category) 字段用于简单的分类筛选，如"技术分享"、"校园生活"等
--   5. user_id 外键关联用户表，记录文章作者信息
--   6. collect_count 收藏数、cover_url 封面图为增强功能字段
--
DROP TABLE IF EXISTS `blog_post`;

CREATE TABLE `blog_post` (
    -- -------------------- 基础信息字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID，自增长，唯一标识每篇帖子',

    `user_id`           BIGINT          NOT NULL                     COMMENT '作者用户ID，关联sys_user 表的外键，记录文章发布者',

    `title`             VARCHAR(200)    NOT NULL                     COMMENT '文章标题，最大200字符，在列表页和详情页展示',

    `summary`           VARCHAR(500)    DEFAULT NULL                  COMMENT '文章摘要/简介，最大500字符，用于列表页快速展示文章大概',

    `cover_url`         VARCHAR(500)    DEFAULT NULL                  COMMENT '文章封面图 URL，用于列表页展示封面',

    `content`           LONGTEXT        NOT NULL                     COMMENT '文章正文内容，存储 Markdown 格式的原始文本，支持大型富文本',

    `topic_ids`         JSON            DEFAULT NULL                  COMMENT '关联话题ID数组（JSON格式）',

    `category`          VARCHAR(50)     DEFAULT '其他'                COMMENT '文章所属分类，如"技术分享"、"校园生活"、"资源下载"等，默认为"其他"',

    -- -------------------- 互动统计字段 --------------------
    `view_count`        BIGINT          DEFAULT 0                    COMMENT '文章阅读量/浏览次数，每次进入详情页时自增，用于展示热门文章',

    `like_count`        INT              DEFAULT 0                    COMMENT '文章点赞数量，用户点赞时自增，用于展示受欢迎程度',

    `comment_count`     INT              DEFAULT 0                    COMMENT '文章评论数量，实时统计该文章的评论总数，避免频繁COUNT查询',

    `collect_count`     INT              DEFAULT 0                    COMMENT '文章收藏数量，用户收藏时自增',

    `share_count`       INT              DEFAULT 0                    COMMENT '文章分享数量，用户分享时自增',

    -- -------------------- 状态与时间字段 --------------------
    `status`            TINYINT(1)      DEFAULT 1                    COMMENT '文章状态：0=待审核，1=已发布，2=已驳回',

    `reviewer_id`       BIGINT          DEFAULT NULL                  COMMENT '审核人ID',

    `review_time`       DATETIME        DEFAULT NULL                  COMMENT '审核时间',

    `reject_reason`     VARCHAR(500)    DEFAULT NULL                  COMMENT '驳回原因',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP     COMMENT '文章发布时间，自动记录首次发布的时间',

    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP  COMMENT '文章最后修改时间，当文章被编辑时自动更新',

    -- -------------------- 逻辑删除字段 --------------------
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除标记：0=正常文章，1=已删除（被作者或管理员删除）',

    -- -------------------- 索引约束 --------------------
    PRIMARY KEY (`id`),                                                 -- 主键索引
    INDEX `idx_user_id` (`user_id`),                                    -- 按用户查询其所有文章
    INDEX `idx_category` (`category`),                                  -- 按分类筛选文章
    INDEX `idx_create_time` (`create_time`),                           -- 按时间排序（最新发布）
    INDEX `idx_status_deleted` (`status`, `is_deleted`)                -- 复合索引，加速公开文章的查询

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='帖子/文章表：存储用户发布的博客文章内容，支持 Markdown 格式';


-- ============================================================================
-- 表三：评论表 (blog_comment)
-- ============================================================================
-- 【业务说明】
--   存储用户对文章的评论内容，支持多级回复（树形结构）
--   通过 parent_id 字段实现评论的嵌套回复功能
--
-- 【设计要点】
--   1. parent_id 字段实现树形评论结构
--      - parent_id = NULL，表示这是一级评论（直接评论文章）
--      - parent_id = 某评论ID，表示这是对该评论的回复（二级评论）
--   2. content 字段存储评论的纯文本内容
--   3. 支持逻辑删除，删除评论时会级联删除所有子评论
--   4. 删除评论时使用逻辑删除 (is_deleted)，保留互动记录可追溯
--
DROP TABLE IF EXISTS `blog_comment`;

CREATE TABLE `blog_comment` (
    -- -------------------- 基础信息字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID，自增长，唯一标识每条评论',

    `post_id`           BIGINT          NOT NULL                     COMMENT '所属文章ID，关联 blog_post 表，表示该评论属于哪篇文章',

    `user_id`           BIGINT          NOT NULL                     COMMENT '评论者用户ID，关联 sys_user 表，记录发表评论的用户',

    `parent_id`         BIGINT          DEFAULT NULL                  COMMENT '父评论ID，用于实现回复功能。NULL=一级评论（非回复），有值=回复某条评论',

    `content`           TEXT            NOT NULL                     COMMENT '评论内容，存储评论文本，支持较长内容',

    -- -------------------- 时间戳字段 --------------------
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '评论发布时间，自动记录',

    -- -------------------- 逻辑删除字段 --------------------
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除标记：0=正常评论，1=已删除',

    -- -------------------- 索引约束 --------------------
    PRIMARY KEY (`id`),                                                 -- 主键索引
    INDEX `idx_post_id` (`post_id`),                                    -- 按文章查询所有评论
    INDEX `idx_user_id` (`user_id`),                                    -- 按用户查询其所有评论
    INDEX `idx_parent_id` (`parent_id`)                                -- 按父评论ID查询回复列表

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='评论表：存储用户对文章的评论，支持多级回复（树形结构）';


-- ============================================================================
-- 表四：标签表 (blog_tag)
-- ============================================================================
-- 【业务说明】
--   存储文章的标签信息，如"Java"、"Spring Boot"、"校园"等
--   标签用于对文章进行多维度归类，方便用户按兴趣筛选
--
-- 【设计要点】
--   1. 标签名 (name) 设置唯一索引，防止重复添加同名标签
--   2. 标签表本身不存储文章关联关系，而是通过中间表 blog_post_tag 实现多对多关联
--   3. 支持逻辑删除，删除标签时保留历史关联记录
--   4. post_count 为该标签下的文章数量冗余计数字段
--
DROP TABLE IF EXISTS `blog_tag`;

CREATE TABLE `blog_tag` (
    -- -------------------- 基础信息字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID，自增长，唯一标识每个标签',

    `name`              VARCHAR(50)     NOT NULL                     COMMENT '标签名称，如"Java"、"Python"、"校园生活"等，唯一不重复',

    -- -------------------- 统计字段 --------------------
    `post_count`        INT              DEFAULT 0                    COMMENT '该标签下的文章数量，用于展示热门标签',

    -- -------------------- 逻辑删除字段 --------------------
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除标记：0=正常标签，1=已删除',

    -- -------------------- 索引约束 --------------------
    PRIMARY KEY (`id`),                                                 -- 主键索引
    UNIQUE KEY `uk_name` (`name`)                                      -- 唯一索引，保证标签名不重复，便于精确查询

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='标签表：存储文章的分类标签，支持文章多标签管理';


-- ============================================================================
-- 表五：帖子-标签关联表 (blog_post_tag)
-- ============================================================================
-- 【业务说明】
--   实现帖子表和标签表之间的多对多关联关系
--   一篇文章可以拥有多个标签（如"Java"+"Spring Boot"），
--   一个标签也可以关联多篇文章
--
-- 【设计要点】
--   1. 使用自增主键 id，便于管理中间表记录
--   2. 联合唯一索引 (post_id, tag_id)，保证同一篇文章不会重复关联同一标签
--   3. 查询某篇文章的所有标签：SELECT tag.* FROM blog_tag tag
--      JOIN blog_post_tag pt ON tag.id = pt.tag_id WHERE pt.post_id = ?
--   4. 查询某个标签下的所有文章：SELECT post.* FROM blog_post post
--      JOIN blog_post_tag pt ON post.id = pt.post_id WHERE pt.tag_id = ?
--
DROP TABLE IF EXISTS `blog_post_tag`;

CREATE TABLE `blog_post_tag` (
    -- -------------------- 主键字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID，自增长',

    -- -------------------- 关联字段 --------------------
    `post_id`           BIGINT          NOT NULL                     COMMENT '帖子ID，关联 blog_post 表的外键',

    `tag_id`            BIGINT          NOT NULL                     COMMENT '标签ID，关联 blog_tag 表的外键',

    -- -------------------- 时间戳和软删除字段 --------------------
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    -- -------------------- 主键和唯一约束 --------------------
    PRIMARY KEY (`id`),                                               -- 主键索引
    UNIQUE KEY `uk_post_tag` (`post_id`, `tag_id`),                   -- 联合唯一索引，保证 (文章+标签) 组合唯一
    INDEX `idx_tag_id` (`tag_id`)                                     -- 按标签ID查询文章

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='帖子-标签关联表（中间表）：实现文章与标签的多对多关联关系';


-- ============================================================================
-- 表六：点赞记录表 (blog_like)
-- ============================================================================
-- 【业务说明】
--   记录用户对文章的点赞行为，用于精确统计和防止重复点赞
--   与 blog_post 表中的 like_count 字段配合使用
--
-- 【设计要点】
--   1. 使用自增主键 id，便于管理记录
--   2. 联合唯一索引 (user_id, post_id) 保证同一用户对同一篇文章只能点赞一次
--   3. 与 blog_post.like_count 的区别：
--      - like_count 是冗余字段，用于快速排序和展示
--      - blog_like 是明细表，用于精确控制和记录
--   4. 删除点赞时，需要同时更新 blog_post.like_count 字段
--
DROP TABLE IF EXISTS `blog_like`;

CREATE TABLE `blog_like` (
    -- -------------------- 主键字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID，自增长',

    -- -------------------- 关联字段 --------------------
    `user_id`           BIGINT          NOT NULL                     COMMENT '点赞用户ID，关联 sys_user 表',

    `post_id`           BIGINT          NOT NULL                     COMMENT '被点赞的文章ID，关联 blog_post 表',

    -- -------------------- 时间戳字段 --------------------
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '点赞时间，记录用户点赞的时间戳',

    -- -------------------- 逻辑删除字段 --------------------
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除标记：0=正常，1=删除',

    -- -------------------- 主键和唯一约束 --------------------
    PRIMARY KEY (`id`),                                               -- 主键索引
    UNIQUE KEY `uk_like_user_post` (`user_id`, `post_id`),           -- 联合唯一索引，防止重复点赞
    INDEX `idx_post_id` (`post_id`)                                   -- 按文章查询点赞用户列表

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='点赞记录表：记录用户对文章的点赞行为，防止重复点赞';


-- ============================================================================
-- 表七：收藏记录表 (blog_collect)
-- ============================================================================
-- 【业务说明】
--   记录用户收藏的文章，便于用户稍后阅读
--
-- 【设计要点】
--   1. 使用自增主键 id，便于管理记录
--   2. 联合唯一索引 (user_id, post_id) 保证同一用户对同一篇文章只能收藏一次
--   3. 支持逻辑删除
--
DROP TABLE IF EXISTS `blog_collect`;

CREATE TABLE `blog_collect` (
    -- -------------------- 主键字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID，自增长',

    -- -------------------- 关联字段 --------------------
    `user_id`           BIGINT          NOT NULL                     COMMENT '收藏用户ID',

    `post_id`           BIGINT          NOT NULL                     COMMENT '被收藏的文章ID',

    -- -------------------- 时间戳字段 --------------------
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '收藏时间',

    -- -------------------- 逻辑删除字段 --------------------
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除标记：0=正常，1=删除',

    -- -------------------- 主键和唯一约束 --------------------
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collect_user_post` (`user_id`, `post_id`),
    INDEX `idx_post_id` (`post_id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='收藏记录表：记录用户收藏的文章，便于稍后阅读';


-- ============================================================================
-- 表七点五：文章分享记录表 (blog_share)
-- ============================================================================
-- 【业务说明】
--   记录用户分享文章的行为，用于统计分享次数
--
DROP TABLE IF EXISTS `blog_share`;

CREATE TABLE `blog_share` (
    -- -------------------- 主键字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    -- -------------------- 关联字段 --------------------
    `user_id`           BIGINT          DEFAULT NULL                  COMMENT '分享者用户ID',

    `post_id`           BIGINT          NOT NULL                     COMMENT '被分享的文章ID',

    -- -------------------- 平台字段 --------------------
    `platform`          VARCHAR(50)    DEFAULT 'web'                  COMMENT '分享平台：web=网页分享, weixin=微信, qq=QQ, weibo=微博',

    -- -------------------- 时间戳字段 --------------------
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '分享时间',

    -- -------------------- 逻辑删除字段 --------------------
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    -- -------------------- 主键和索引 --------------------
    PRIMARY KEY (`id`),
    INDEX `idx_post_id` (`post_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='文章分享记录表：记录用户分享文章的行为';


-- ============================================================================
-- 以下为功能增强模块新增的表（v1.17+版本）
-- ============================================================================

-- ============================================================================
-- 表八：关注关系表 (blog_follow)
-- ============================================================================
-- 【业务说明】
--   记录用户之间的关注关系，实现社交功能
--   用户可以关注其他用户，关注后可查看关注用户的动态
--
-- 【设计要点】
--   1. follower_id 为关注者，following_id 为被关注者
--   2. 联合唯一索引 (follower_id, following_id) 防止重复关注
--   3. 支持逻辑删除，取消关注时标记为已删除
--   4. sys_user 表的 follower_count 和 following_count 为冗余计数字段
--
DROP TABLE IF EXISTS `blog_follow`;

CREATE TABLE `blog_follow` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `follower_id`       BIGINT          NOT NULL                     COMMENT '关注者用户ID',

    `following_id`      BIGINT          NOT NULL                     COMMENT '被关注者用户ID',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '关注时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
    INDEX `idx_following_id` (`following_id`),
    INDEX `idx_create_time` (`create_time`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='关注关系表：记录用户之间的关注关系';


-- ============================================================================
-- 表九：通知表 (blog_notification)
-- ============================================================================
-- 【业务说明】
--   存储用户的通知消息，包括关注、点赞、评论、提及等类型
--   用户可以在通知中心查看所有通知
--
-- 【设计要点】
--   1. type 字段区分通知类型：LIKE/COMMENT/REPLY/FOLLOW/MENTION/MESSAGE/SYSTEM
--   2. from_user_id 为触发通知的用户，user_id 为接收通知的用户
--   3. target_type 和 target_id 关联到具体的内容（文章/评论）
--   4. 支持逻辑删除
--
DROP TABLE IF EXISTS `blog_notification`;

CREATE TABLE `blog_notification` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `user_id`           BIGINT          NOT NULL                     COMMENT '通知所属用户ID（接收者）',

    `type`              VARCHAR(50)     NOT NULL                     COMMENT '通知类型：LIKE=点赞，COMMENT=评论，REPLY=回复，FOLLOW=关注，MENTION=提及，MESSAGE=私信，SYSTEM=系统通知',

    `title`             VARCHAR(200)    NOT NULL                     COMMENT '通知标题',

    `content`           VARCHAR(500)    DEFAULT NULL                  COMMENT '通知内容',

    `from_user_id`      BIGINT          DEFAULT NULL                  COMMENT '触发通知的用户ID（发送者）',

    `target_type`       VARCHAR(50)     DEFAULT NULL                  COMMENT '关联目标类型：POST=文章，COMMENT=评论，MESSAGE=私信，USER=用户',

    `target_id`         BIGINT          DEFAULT NULL                  COMMENT '关联目标ID',

    `is_read`           TINYINT(1)      DEFAULT 0                    COMMENT '是否已读：0=未读，1=已读',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_unread` (`user_id`, `is_read`),
    INDEX `idx_create_time` (`create_time`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='通知表：存储用户的站内通知消息';


-- ============================================================================
-- 表十：文章热度表 (blog_trending)
-- ============================================================================
-- 【业务说明】
--   记录文章每日热度分数，用于热门推荐和趋势展示
--
-- 【设计要点】
--   1. 每天凌晨定时计算并更新热度分数
--   2. 热度公式：score = view*1 + like*5 + comment*10
--   3. 使用逻辑删除 (is_deleted)，与全局 @TableLogic 策略保持一致
--
DROP TABLE IF EXISTS `blog_trending`;

CREATE TABLE `blog_trending` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `post_id`           BIGINT          NOT NULL                     COMMENT '文章ID',

    `score`             DOUBLE          NOT NULL    DEFAULT 0        COMMENT '热度分数',

    `view_count`        INT              NOT NULL    DEFAULT 0        COMMENT '当日阅读量',

    `like_count`        INT              NOT NULL    DEFAULT 0        COMMENT '当日点赞量',

    `comment_count`     INT              NOT NULL    DEFAULT 0        COMMENT '当日评论量',

    `date`              DATE            NOT NULL                     COMMENT '统计日期',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_date` (`post_id`, `date`),
    INDEX `idx_score` (`score` DESC)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='文章热度表：用于热门推荐和趋势展示';

-- ============================================================================
-- 表十一：文章草稿表 (blog_draft)
-- ============================================================================
-- 【业务说明】
--   存储用户的文章草稿，支持自动保存功能
--
DROP TABLE IF EXISTS `blog_draft`;

CREATE TABLE `blog_draft` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `user_id`           BIGINT          NOT NULL                     COMMENT '用户ID',

    `title`             VARCHAR(200)    DEFAULT NULL                  COMMENT '草稿标题',

    `content`           LONGTEXT        DEFAULT NULL                  COMMENT '草稿内容',

    `cover_image`       VARCHAR(500)    DEFAULT NULL                  COMMENT '封面图片URL',

    `summary`           VARCHAR(500)    DEFAULT NULL                  COMMENT '草稿摘要',

    `category`          VARCHAR(50)     DEFAULT NULL                  COMMENT '草稿分类',

    `tag_ids`           VARCHAR(200)    DEFAULT NULL                  COMMENT '草稿标签ID（逗号分隔）',

    `topic_id`          BIGINT          DEFAULT NULL                  COMMENT '关联话题ID',

    `post_id`           BIGINT          DEFAULT NULL                  COMMENT '关联的文章ID（编辑已有文章时使用）',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_post_id` (`post_id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='文章草稿表：存储用户的文章草稿，支持自动保存';

-- ============================================================================
-- 表十一点五：草稿-标签关联表 (blog_draft_tag)
-- ============================================================================
-- 【业务说明】
--   实现草稿和标签之间的多对多关联，符合第一范式(1NF)
--   替代 blog_draft 表中的 tag_ids 逗号分隔字段
--
DROP TABLE IF EXISTS `blog_draft_tag`;

CREATE TABLE `blog_draft_tag` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键',
    `draft_id`          BIGINT          NOT NULL                     COMMENT '草稿ID',
    `tag_id`            BIGINT          NOT NULL                     COMMENT '标签ID',
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_draft_tag` (`draft_id`, `tag_id`),
    INDEX `idx_tag_id` (`tag_id`),
    INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='草稿-标签关联表';


-- ============================================================================
-- 表十二：内容举报表 (blog_report)
-- ============================================================================
-- 【业务说明】
--   记录用户举报的内容（文章/评论/用户），供管理员审核处理
--
-- 【设计要点】
--   1. status 字段：0=待处理，1=已处理，2=已驳回
--   2. 支持逻辑删除
--
DROP TABLE IF EXISTS `blog_report`;

CREATE TABLE `blog_report` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `reporter_id`       BIGINT          NOT NULL                     COMMENT '举报人ID',

    `reported_user_id`   BIGINT          DEFAULT NULL                  COMMENT '被举报用户ID',

    `target_type`       VARCHAR(50)     NOT NULL                     COMMENT '举报目标类型：post/comment/user',

    `target_id`         BIGINT          NOT NULL                     COMMENT '举报目标ID',

    `reason`            VARCHAR(500)    NOT NULL                     COMMENT '举报原因',

    `status`            TINYINT(1)      DEFAULT 0                    COMMENT '处理状态：0=待处理，1=已驳回，2=已核实',

    `handler_id`        BIGINT          DEFAULT NULL                  COMMENT '处理人ID（管理员）',

    `handler_result`    VARCHAR(500)    DEFAULT NULL                  COMMENT '处理结果',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    `handle_time`       DATETIME        DEFAULT NULL                  COMMENT '处理时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    INDEX `idx_reporter_id` (`reporter_id`),
    INDEX `idx_status` (`status`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='内容举报表：记录用户举报的内容，供管理员审核';


-- ============================================================================
-- 表十三：校友圈动态表 (blog_circle_post)
-- ============================================================================
-- 【业务说明】
--   校友圈是一个类似微博/Twitter 的独立社交动态流功能
--   支持发布短文本动态、图片动态和转发
--
-- 【设计要点】
--   1. content_type：1=纯文本，2=图文，3=转发，4=视频
--   2. image_urls 存储 JSON 格式的图片URL 数组（最多9张）
--   3. tags 存储 JSON 格式的话题标签数组
--   4. mentions 存储 JSON 格式的@提及用户 ID 数组
--   5. 使用 is_deleted + @TableLogic 实现逻辑删除，与全局策略保持一致
--
DROP TABLE IF EXISTS `blog_circle_post`;

CREATE TABLE `blog_circle_post` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `user_id`           BIGINT          NOT NULL                     COMMENT '发布者用户ID',

    `content`           VARCHAR(2000)   NOT NULL                     COMMENT '动态内容（最大2000字）',

    `content_type`      TINYINT(1)     DEFAULT 1                    COMMENT '内容类型：1=纯文本，2=图文，3=转发，4=视频',

    `image_urls`        JSON            DEFAULT NULL                  COMMENT '图片URL数组（最多9张）',

    `video_urls`        JSON            DEFAULT NULL                  COMMENT '视频URL数组',

    `repost_id`         BIGINT          DEFAULT NULL                  COMMENT '转发的动态ID',

    `repost_user_id`    BIGINT          DEFAULT NULL                  COMMENT '被转发者用户ID',

    `repost_content`    VARCHAR(500)   DEFAULT NULL                  COMMENT '转发时添加的内容',

    `tags`              JSON            DEFAULT NULL                  COMMENT '话题标签数组',

    `mentions`          JSON            DEFAULT NULL                  COMMENT '@提及的用户ID数组',

    `topic_ids`         JSON            DEFAULT NULL                  COMMENT '关联话题ID数组（JSON格式）',

    `location`          VARCHAR(100)   DEFAULT NULL                  COMMENT '位置信息',

    `like_count`        INT              DEFAULT 0                    COMMENT '点赞数',

    `comment_count`     INT              DEFAULT 0                    COMMENT '评论数',

    `repost_count`      INT              DEFAULT 0                    COMMENT '转发数',

    `view_count`        BIGINT          DEFAULT 0                    COMMENT '查看数',

    `is_top`            TINYINT(1)      DEFAULT 0                    COMMENT '是否置顶，0=否，1=是',

    `visibility`        TINYINT(1)      DEFAULT 0                    COMMENT '可见性：0=公开，1=仅关注者，2=仅自己',

    `allow_comment`     TINYINT(1)      DEFAULT 1                    COMMENT '是否允许评论，1=允许，0=不允许',

    `allow_repost`      TINYINT(1)      DEFAULT 1                    COMMENT '是否允许转发，1=允许，0=不允许',

    `status`            TINYINT(1)      DEFAULT 1                    COMMENT '状态：1=正常，2=隐藏',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',

    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_like_count` (`like_count` DESC),
    INDEX `idx_repost_id` (`repost_id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='校友圈动态表：类似微博的短动态发布功能';


-- ============================================================================
-- 表十四：校友圈点赞表 (blog_circle_like)
-- ============================================================================
-- 【业务说明】
--   记录用户对校友圈动态的点赞行为
--
DROP TABLE IF EXISTS `blog_circle_like`;

CREATE TABLE `blog_circle_like` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `user_id`           BIGINT          NOT NULL                     COMMENT '点赞用户ID',

    `post_id`           BIGINT          NOT NULL                     COMMENT '动态ID',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '点赞时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_circle_like_user_post` (`user_id`, `post_id`),
    INDEX `idx_post_id` (`post_id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='校友圈点赞表：记录用户对校友圈动态的点赞';


-- ============================================================================
-- 表十五：校友圈评论表 (blog_circle_comment)
-- ============================================================================
-- 【业务说明】
--   存储校友圈动态的评论，支持二级回复
--
DROP TABLE IF EXISTS `blog_circle_comment`;

CREATE TABLE `blog_circle_comment` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `post_id`           BIGINT          NOT NULL                     COMMENT '动态ID',

    `user_id`           BIGINT          NOT NULL                     COMMENT '评论者用户ID',

    `content`           VARCHAR(500)    NOT NULL                     COMMENT '评论内容',

    `parent_id`         BIGINT          DEFAULT NULL                  COMMENT '父评论ID（二级回复）',

    `reply_to_user_id`  BIGINT          DEFAULT NULL                  COMMENT '回复给的用户ID',

    `like_count`        INT              DEFAULT 0                    COMMENT '点赞数',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    INDEX `idx_post_id` (`post_id`),
    INDEX `idx_parent_id` (`parent_id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='校友圈评论表：支持二级回复的评论功能';


-- ============================================================================
-- 表十六：校友圈转发表 (blog_circle_repost)
-- ============================================================================
-- 【业务说明】
--   记录校友圈动态的转发关系
--
DROP TABLE IF EXISTS `blog_circle_repost`;

CREATE TABLE `blog_circle_repost` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `user_id`           BIGINT          NOT NULL                     COMMENT '转发者用户ID',

    `original_post_id`  BIGINT          NOT NULL                     COMMENT '原始动态ID',

    `new_post_id`       BIGINT          NOT NULL                     COMMENT '新动态ID（转发生成的新动态）',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '转发时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    INDEX `idx_original_post_id` (`original_post_id`),
    INDEX `idx_user_id` (`user_id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='校友圈转发表：记录动态转发关系';


-- ============================================================================
-- 表十七：媒体资源表 (blog_media)
-- ============================================================================
-- 【业务说明】
--   存储用户上传的图片、视频等媒体文件
--
DROP TABLE IF EXISTS `blog_media`;

CREATE TABLE `blog_media` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `user_id`           BIGINT          NOT NULL                     COMMENT '上传用户ID',

    `file_name`         VARCHAR(255)    NOT NULL                     COMMENT '原始文件名',

    `file_path`         VARCHAR(500)    NOT NULL                     COMMENT '存储路径',

    `file_url`          VARCHAR(500)    NOT NULL                     COMMENT '访问URL',

    `file_type`         VARCHAR(50)     NOT NULL                     COMMENT '文件类型：image/video',

    `mime_type`         VARCHAR(100)   NOT NULL                     COMMENT 'MIME类型',

    `file_size`         BIGINT          NOT NULL                     COMMENT '文件大小（字节）',

    `width`             INT              DEFAULT NULL                  COMMENT '图片宽度（仅图片有效）',

    `height`            INT              DEFAULT NULL                  COMMENT '图片高度（仅图片有效）',

    `thumb_url`         VARCHAR(500)    DEFAULT NULL                  COMMENT '视频缩略图URL',

    `status`            TINYINT(1)      DEFAULT 1                    COMMENT '状态：1=正常，0=禁用',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_create_time` (`create_time`),
    INDEX `idx_file_type` (`file_type`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='媒体资源表：存储用户上传的图片、视频等文件';


-- ============================================================================
-- 表十八：文章媒体关联表 (blog_post_media)
-- ============================================================================
-- 【业务说明】
--   实现文章和媒体文件的多对多关联
--
DROP TABLE IF EXISTS `blog_post_media`;

CREATE TABLE `blog_post_media` (
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID',

    `post_id`           BIGINT          NOT NULL                     COMMENT '文章ID',

    `media_id`          BIGINT          NOT NULL                     COMMENT '媒体ID',

    `display_order`     INT              DEFAULT 0                    COMMENT '显示顺序',

    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP    COMMENT '创建时间',

    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    PRIMARY KEY (`id`),
    INDEX `idx_post_id` (`post_id`),
    INDEX `idx_media_id` (`media_id`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='文章媒体关联表：实现文章与媒体文件的多对多关联';


-- ============================================================================
-- 表十九：话题表 (blog_topic)
-- ============================================================================
-- 【业务说明】
--   存储校友圈的话题信息，如"Java"、"校园生活"等
--   用户发布动态时可通过 #话题名 自动关联话题
--
-- 【设计要点】
--   1. name 设置唯一索引，防止重复添加同名话题
--   2. post_count 为该话题下的动态数量冗余计数字段
--   3. trending_score 用于热门话题排序
--   4. 支持逻辑删除
--
DROP TABLE IF EXISTS `blog_topic`;

CREATE TABLE `blog_topic` (
    -- -------------------- 主键字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID，自增长',

    -- -------------------- 基础信息字段 --------------------
    `name`              VARCHAR(50)     NOT NULL                     COMMENT '话题名称（如 Java、Python）',

    `description`       VARCHAR(500)    DEFAULT NULL                  COMMENT '话题描述',

    -- -------------------- 统计字段 --------------------
    `post_count`        INT              DEFAULT 0                    COMMENT '关联动态数',

    `trending_score`    INT              DEFAULT 0                    COMMENT '热度分数',

    -- -------------------- 状态字段 --------------------
    `status`            TINYINT(1)      DEFAULT 1                    COMMENT '状态：1=正常，0=禁用',

    -- -------------------- 时间戳字段 --------------------
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',

    `update_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',

    -- -------------------- 软删除字段 --------------------
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    -- -------------------- 主键和唯一约束 --------------------
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_name` (`name`)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='话题表：存储校友圈话题信息';


-- ============================================================================
-- 表二十：私信表 (blog_message)
-- ============================================================================
-- 【业务说明】
--   存储用户之间的私信消息，支持一对一私聊功能
--
-- 【设计要点】
--   1. sender_id 和 receiver_id 实现发送者和接收者的关联
--   2. is_read 字段标记私信是否已读
--   3. 支持逻辑删除，删除私信时保留记录可追溯
--   4. 按时间倒序查询，方便展示最新消息
--
DROP TABLE IF EXISTS `blog_message`;

CREATE TABLE `blog_message` (
    -- -------------------- 主键字段 --------------------
    `id`                BIGINT          NOT NULL    AUTO_INCREMENT   COMMENT '主键ID，自增长',

    -- -------------------- 关联字段 --------------------
    `sender_id`         BIGINT          NOT NULL                     COMMENT '发送者用户ID',

    `receiver_id`       BIGINT          NOT NULL                     COMMENT '接收者用户ID',

    -- -------------------- 内容字段 --------------------
    `content`           TEXT            NOT NULL                     COMMENT '私信内容（最大2000字符）',

    -- -------------------- 状态字段 --------------------
    `is_read`           TINYINT(1)      DEFAULT 0                    COMMENT '是否已读：0=未读，1=已读',

    -- -------------------- 时间戳字段 --------------------
    `create_time`       DATETIME        DEFAULT CURRENT_TIMESTAMP     COMMENT '发送时间',

    -- -------------------- 逻辑删除字段 --------------------
    `is_deleted`        TINYINT(1)      DEFAULT 0                    COMMENT '逻辑删除，0=正常，1=删除',

    -- -------------------- 索引约束 --------------------
    PRIMARY KEY (`id`),                                               -- 主键索引
    INDEX `idx_receiver_id` (`receiver_id`),                          -- 按接收者查询收到的私信
    INDEX `idx_sender_id` (`sender_id`),                             -- 按发送者查询发送的私信
    INDEX `idx_create_time` (`create_time`)                           -- 按时间排序

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='私信表：存储用户之间的私信消息';


-- ============================================================================
-- 性能优化索引 - 合并版本 v2.0
-- 解决 "VERY SLOW" 接口问题（15秒+ 响应时间）
-- 说明: 所有索引创建语句都是幂等的，重复执行不会报错
-- 合并了 v1.42 + v2.0 所有唯一索引
-- ============================================================================

-- 辅助存储过程：创建索引（如果不存在）
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS create_index_if_not_exists(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_index_sql VARCHAR(512)
)
BEGIN
    SET @exist:= (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = p_table_name
                  AND index_name = p_index_name);
    IF @exist = 0 THEN
        SET @sqlstmt:= p_index_sql;
        PREPARE stmt FROM @sqlstmt;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- ============================================================================
-- blog_post 表索引
-- ============================================================================
-- 用户+创建时间复合索引（用户文章列表）
CALL create_index_if_not_exists('blog_post', 'idx_blog_post_user_create_time', 'CREATE INDEX idx_blog_post_user_create_time ON blog_post (user_id, create_time)');

-- 点赞数索引（热门文章排序）
CALL create_index_if_not_exists('blog_post', 'idx_blog_post_like_count', 'CREATE INDEX idx_blog_post_like_count ON blog_post (like_count)');

-- 浏览数索引
CALL create_index_if_not_exists('blog_post', 'idx_blog_post_view_count', 'CREATE INDEX idx_blog_post_view_count ON blog_post (view_count)');

-- 文章列表查询优化（状态+删除+创建时间DESC）
CALL create_index_if_not_exists('blog_post', 'idx_post_status_deleted_create', 'CREATE INDEX idx_post_status_deleted_create ON blog_post (status, is_deleted, create_time DESC)');

-- 文章热门排序优化（状态+删除+浏览量DESC）
CALL create_index_if_not_exists('blog_post', 'idx_post_status_deleted_view', 'CREATE INDEX idx_post_status_deleted_view ON blog_post (status, is_deleted, view_count DESC)');

-- 文章精华排序优化（状态+删除+点赞量DESC）
CALL create_index_if_not_exists('blog_post', 'idx_post_status_deleted_like', 'CREATE INDEX idx_post_status_deleted_like ON blog_post (status, is_deleted, like_count DESC)');

-- 注意：全文搜索索引已移除，项目使用 LIKE 查询实现搜索功能
-- 避免了 MySQL FULLTEXT 索引配置复杂、对中文支持不佳的问题

-- ============================================================================
-- blog_comment 表索引
-- ============================================================================
-- 评论文章ID索引（统计评论数）
CALL create_index_if_not_exists('blog_comment', 'idx_comment_post_id', 'CREATE INDEX idx_comment_post_id ON blog_comment (post_id, create_time DESC)');

-- 评论+创建时间复合索引
CALL create_index_if_not_exists('blog_comment', 'idx_blog_comment_post_create_time', 'CREATE INDEX idx_blog_comment_post_create_time ON blog_comment (post_id, create_time)');

-- 父评论+创建时间复合索引（二级回复）
CALL create_index_if_not_exists('blog_comment', 'idx_blog_comment_parent_create_time', 'CREATE INDEX idx_blog_comment_parent_create_time ON blog_comment (parent_id, create_time)');

-- ============================================================================
-- blog_like 表索引
-- ============================================================================
-- 点赞文章ID索引（统计点赞数）
CALL create_index_if_not_exists('blog_like', 'idx_like_post_id', 'CREATE INDEX idx_like_post_id ON blog_like (post_id, create_time DESC)');

-- 用户+创建时间复合索引（用户点赞列表）
CALL create_index_if_not_exists('blog_like', 'idx_blog_like_user_create_time', 'CREATE INDEX idx_blog_like_user_create_time ON blog_like (user_id, create_time)');

-- ============================================================================
-- blog_collect 表索引
-- ============================================================================
-- 收藏文章ID索引（统计收藏数）
CALL create_index_if_not_exists('blog_collect', 'idx_collect_post_id', 'CREATE INDEX idx_collect_post_id ON blog_collect (post_id, create_time DESC)');

-- 用户+创建时间复合索引（用户收藏列表）
CALL create_index_if_not_exists('blog_collect', 'idx_blog_collect_user_create_time', 'CREATE INDEX idx_blog_collect_user_create_time ON blog_collect (user_id, create_time)');

-- ============================================================================
-- blog_follow 表索引
-- ============================================================================
-- 关注者+删除状态复合索引
CALL create_index_if_not_exists('blog_follow', 'idx_blog_follow_follower_is_deleted', 'CREATE INDEX idx_blog_follow_follower_is_deleted ON blog_follow (follower_id, is_deleted)');

-- 关注关系创建时间索引
CALL create_index_if_not_exists('blog_follow', 'idx_follow_create_time', 'CREATE INDEX idx_follow_create_time ON blog_follow (create_time DESC)');

-- ============================================================================
-- blog_report 表索引
-- ============================================================================
-- 举报处理时间查询优化
CALL create_index_if_not_exists('blog_report', 'idx_report_handle_time', 'CREATE INDEX idx_report_handle_time ON blog_report (handle_time, status)');

-- 状态+创建时间复合索引（举报列表）
CALL create_index_if_not_exists('blog_report', 'idx_blog_report_status_create_time', 'CREATE INDEX idx_blog_report_status_create_time ON blog_report (status, create_time)');

-- 举报目标索引（按target查询）
CALL create_index_if_not_exists('blog_report', 'idx_blog_report_target', 'CREATE INDEX idx_blog_report_target ON blog_report (target_type, target_id)');

-- ============================================================================
-- blog_notification 表索引
-- ============================================================================
-- 通知列表查询优化
CALL create_index_if_not_exists('blog_notification', 'idx_notification_user_read_create', 'CREATE INDEX idx_notification_user_read_create ON blog_notification (user_id, is_read, create_time DESC)');

-- 用户+已读状态复合索引
CALL create_index_if_not_exists('blog_notification', 'idx_blog_notification_user_is_read', 'CREATE INDEX idx_blog_notification_user_is_read ON blog_notification (user_id, is_read)');

-- 通知类型索引
CALL create_index_if_not_exists('blog_notification', 'idx_blog_notification_type', 'CREATE INDEX idx_blog_notification_type ON blog_notification (type)');

-- 创建时间索引
CALL create_index_if_not_exists('blog_notification', 'idx_blog_notification_create_time', 'CREATE INDEX idx_blog_notification_create_time ON blog_notification (create_time)');

-- ============================================================================
-- blog_message 表索引
-- ============================================================================
-- 消息未读计数优化（receiver_id + is_read）
CALL create_index_if_not_exists('blog_message', 'idx_message_receiver_read', 'CREATE INDEX idx_message_receiver_read ON blog_message (receiver_id, is_read)');

-- 消息会话查询优化（sender + create_time）
CALL create_index_if_not_exists('blog_message', 'idx_message_sender_time', 'CREATE INDEX idx_message_sender_time ON blog_message (sender_id, create_time DESC)');

-- 消息会话查询优化（receiver + create_time）
CALL create_index_if_not_exists('blog_message', 'idx_message_receiver_time', 'CREATE INDEX idx_message_receiver_time ON blog_message (receiver_id, create_time DESC)');

-- 发送者+接收者+删除状态复合索引
CALL create_index_if_not_exists('blog_message', 'idx_blog_message_sender_receiver_deleted', 'CREATE INDEX idx_blog_message_sender_receiver_deleted ON blog_message (sender_id, receiver_id, is_deleted)');

-- ============================================================================
-- blog_draft 表索引
-- ============================================================================
-- 草稿用户+删除状态复合索引
CALL create_index_if_not_exists('blog_draft', 'idx_blog_draft_user_deleted', 'CREATE INDEX idx_blog_draft_user_deleted ON blog_draft (user_id, is_deleted)');

-- ============================================================================
-- blog_circle_post 表索引
-- ============================================================================
-- 校友圈热门内容优化（状态+可见性+点赞量）
CALL create_index_if_not_exists('blog_circle_post', 'idx_circle_status_visibility_like', 'CREATE INDEX idx_circle_status_visibility_like ON blog_circle_post (status, visibility, like_count DESC)');

-- 可见性+删除状态复合索引
CALL create_index_if_not_exists('blog_circle_post', 'idx_circle_post_visibility_deleted', 'CREATE INDEX idx_circle_post_visibility_deleted ON blog_circle_post (visibility, is_deleted)');

-- 置顶索引
CALL create_index_if_not_exists('blog_circle_post', 'idx_circle_post_top', 'CREATE INDEX idx_circle_post_top ON blog_circle_post (is_top DESC)');

-- ============================================================================
-- blog_topic 表索引
-- ============================================================================
-- 话题热度排序
CALL create_index_if_not_exists('blog_topic', 'idx_topic_trending', 'CREATE INDEX idx_topic_trending ON blog_topic (trending_score DESC)');

-- ============================================================================
-- blog_media 表索引
-- ============================================================================
-- 媒体用户+类型复合索引
CALL create_index_if_not_exists('blog_media', 'idx_media_user_type', 'CREATE INDEX idx_media_user_type ON blog_media (user_id, file_type)');

-- ============================================================================
-- blog_circle_comment 表索引
-- ============================================================================
-- 校友圈评论优化
CALL create_index_if_not_exists('blog_circle_comment', 'idx_circle_comment_post', 'CREATE INDEX idx_circle_comment_post ON blog_circle_comment (post_id, create_time DESC)');

-- 校友圈评论+时间复合索引
CALL create_index_if_not_exists('blog_circle_comment', 'idx_circle_comment_post_time', 'CREATE INDEX idx_circle_comment_post_time ON blog_circle_comment (post_id, create_time)');

-- ============================================================================
-- blog_trending 表索引
-- ============================================================================
-- 热门内容查询优化（日期+热度分数）
CALL create_index_if_not_exists('blog_trending', 'idx_trending_date_score', 'CREATE INDEX idx_trending_date_score ON blog_trending (date, score DESC)');

-- ============================================================================
-- blog_post_tag 表索引
-- ============================================================================
-- 标签文章计数优化
CALL create_index_if_not_exists('blog_post_tag', 'idx_post_tag_tag_id', 'CREATE INDEX idx_post_tag_tag_id ON blog_post_tag (tag_id, post_id)');

-- ============================================================================
-- sys_user 表索引
-- ============================================================================
-- 用户创建时间索引（统计增长趋势）
CALL create_index_if_not_exists('sys_user', 'idx_user_create_time', 'CREATE INDEX idx_user_create_time ON sys_user (create_time DESC)');

-- 邮箱唯一索引（一个邮箱只能注册一个账户）
-- 【注意】如果数据库中已存在重复的 email 值，此索引创建会失败
-- 请先执行以下 SQL 清理重复数据后再创建索引：
--   UPDATE sys_user SET email = NULL WHERE email = '';
--   DELETE FROM sys_user WHERE id NOT IN (SELECT * FROM (SELECT MIN(id) FROM sys_user WHERE email IS NOT NULL GROUP BY email) t);
CALL create_index_if_not_exists('sys_user', 'idx_user_email_unique', 'CREATE UNIQUE INDEX idx_user_email_unique ON sys_user (email)');

-- 删除辅助存储过程
DROP PROCEDURE IF EXISTS create_index_if_not_exists;

-- ============================================================================
-- 数据迁移：topic_ids 格式修复（v2.0.16）
-- JacksonTypeHandler 将 String 字段双重编码为 JSON 字符串而非 JSON 数组，
-- 导致 JSON_CONTAINS 查询失效。此迁移将 JSON 字符串转为 JSON 数组。
-- ============================================================================
-- UPDATE blog_circle_post SET topic_ids = JSON_UNQUOTE(topic_ids) WHERE JSON_TYPE(topic_ids) = 'STRING';
-- UPDATE blog_post SET topic_ids = JSON_UNQUOTE(topic_ids) WHERE JSON_TYPE(topic_ids) = 'STRING';

-- SelfBlog features. These tables are additive and retain existing post data.
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
