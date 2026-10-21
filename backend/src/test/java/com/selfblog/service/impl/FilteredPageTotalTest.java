package com.selfblog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.entity.BlogCollect;
import com.selfblog.entity.BlogComment;
import com.selfblog.entity.BlogFollow;
import com.selfblog.entity.BlogLike;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.SysUser;
import com.selfblog.service.social.BlogCollectService;
import com.selfblog.service.social.BlogCommentService;
import com.selfblog.service.social.BlogLikeService;
import com.selfblog.service.social.FollowService;
import com.selfblog.vo.post.CollectItemVO;
import com.selfblog.vo.post.CommentVO;
import com.selfblog.vo.post.LikeItemVO;
import com.selfblog.vo.user.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 过滤后分页的 total 契约（C21）。
 *
 * <p>缺陷回顾：这些列表接口原本「先按全部行分页、再在 Java 里丢掉不可见行」，
 * 于是 {@code total} 统计的是被过滤掉的行，{@code records} 却是过滤后的行。
 * 表现是「本页条数 &lt; total」以及深页返回空数组却仍宣称有数据。</p>
 *
 * <p>修复方式是把过滤谓词下推到 SQL，让 records 与 total 同源。本测试用真实的
 * H2 库构造「可见 / 未发布 / 已删除」三类数据，断言 total 只数可见行，
 * 且分页不会出现幽灵页。所有用例都在事务里跑，结束时回滚。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FilteredPageTotalTest {

    @Autowired
    private BlogLikeService blogLikeService;

    @Autowired
    private BlogCollectService blogCollectService;

    @Autowired
    private BlogCommentService blogCommentService;

    @Autowired
    private FollowService followService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long authorId;
    private Long readerId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM blog_like");
        jdbc.update("DELETE FROM blog_collect");
        jdbc.update("DELETE FROM blog_follow");
        jdbc.update("DELETE FROM blog_comment");
        jdbc.update("DELETE FROM blog_post");
        jdbc.update("DELETE FROM sys_user");

        authorId = insertUser("total-author", 1);
        readerId = insertUser("total-reader", 1);
    }

    // ==================== 点赞列表 ====================

    @Test
    @DisplayName("点赞列表 total 只统计已发布且未删除的文章")
    void likePageTotalMatchesTheVisibleRows() {
        Long published = insertPost(authorId, "已发布文章", 1, 0);
        Long draft = insertPost(authorId, "草稿文章", 0, 0);
        Long deleted = insertPost(authorId, "已删除文章", 1, 1);

        insertLike(readerId, published);
        insertLike(readerId, draft);
        insertLike(readerId, deleted);

        IPage<LikeItemVO> page = blogLikeService.getMyLikes(readerId, 1, 10);

        assertEquals(1L, page.getTotal(), "total 不能把草稿/已删除文章的点赞算进来");
        assertEquals(1, page.getRecords().size(), "records 与 total 必须同源");
        assertEquals(published, page.getRecords().get(0).getPostId());
        assertEquals(1L, page.getPages());
    }

    @Test
    @DisplayName("点赞列表深页不返回幽灵页")
    void likePageHasNoGhostPageBeyondTheVisibleRows() {
        insertLike(readerId, insertPost(authorId, "唯一可见文章", 1, 0));
        insertLike(readerId, insertPost(authorId, "草稿文章", 0, 0));

        IPage<LikeItemVO> secondPage = blogLikeService.getMyLikes(readerId, 2, 1);

        assertEquals(1L, secondPage.getTotal());
        assertTrue(secondPage.getRecords().isEmpty(), "第 2 页不该有数据，因为可见行只有 1 条");
        assertEquals(1L, secondPage.getPages());
    }

    @Test
    @DisplayName("点赞列表对空 page/pageSize 做钳制而不是抛 NPE")
    void likePageClampsNullAndNonPositiveArguments() {
        insertLike(readerId, insertPost(authorId, "可见文章", 1, 0));

        IPage<LikeItemVO> page = blogLikeService.getMyLikes(readerId, null, null);

        assertEquals(1L, page.getCurrent());
        assertEquals(10L, page.getSize());
        assertEquals(1L, page.getTotal());
    }

    // ==================== 收藏列表 ====================

    @Test
    @DisplayName("收藏列表 total 只统计已发布且未删除的文章")
    void collectPageTotalMatchesTheVisibleRows() {
        Long published = insertPost(authorId, "已发布文章", 1, 0);
        Long draft = insertPost(authorId, "草稿文章", 0, 0);

        insertCollect(readerId, published);
        insertCollect(readerId, draft);

        IPage<CollectItemVO> page = blogCollectService.getMyCollections(readerId, 1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getRecords().size());
        assertEquals(published, page.getRecords().get(0).getPostId());
    }

    // ==================== 关注 / 粉丝列表 ====================

    @Test
    @DisplayName("关注列表 total 排除被封禁用户与逻辑删除的关系")
    void followingPageTotalExcludesBannedAndDeletedUsers() {
        Long active = insertUser("follow-active", 1);
        Long banned = insertUser("follow-banned", 0);
        Long deleted = insertUser("follow-deleted", 1);

        insertFollow(readerId, active);
        insertFollow(readerId, banned);
        insertFollow(readerId, deleted);
        jdbc.update("UPDATE blog_follow SET is_deleted = 1 WHERE follower_id = ? AND following_id = ?", readerId, deleted);

        IPage<UserVO> page = followService.getFollowing(readerId, 1, 10);

        assertEquals(1L, page.getTotal(), "被封禁 / 逻辑删除的关系不能计入 total");
        assertEquals(1, page.getRecords().size());
        assertEquals(active, page.getRecords().get(0).getId());
    }

    @Test
    @DisplayName("粉丝列表 total 排除被封禁的粉丝")
    void followerPageTotalExcludesBannedFollowers() {
        Long active = insertUser("fans-active", 1);
        Long banned = insertUser("fans-banned", 0);

        insertFollow(active, readerId);
        insertFollow(banned, readerId);

        IPage<UserVO> page = followService.getFollowers(readerId, 1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getRecords().size());
        assertEquals(active, page.getRecords().get(0).getId());
    }

    @Test
    @DisplayName("粉丝列表深页不返回幽灵页")
    void followerPageHasNoGhostPageBeyondTheVisibleRows() {
        Long active = insertUser("fans-only", 1);
        Long banned = insertUser("fans-banned", 0);

        insertFollow(active, readerId);
        insertFollow(banned, readerId);

        IPage<UserVO> secondPage = followService.getFollowers(readerId, 2, 1);

        assertEquals(1L, secondPage.getTotal());
        assertTrue(secondPage.getRecords().isEmpty());
    }

    // ==================== 评论列表 ====================

    @Test
    @DisplayName("评论列表 total 只统计根评论，回复不算一页")
    void commentPageTotalCountsRootCommentsOnly() {
        Long postId = insertPost(authorId, "有评论的文章", 1, 0);
        Long root1 = insertComment(postId, readerId, null, "根评论一");
        insertComment(postId, readerId, root1, "回复一");
        insertComment(postId, readerId, root1, "回复二");
        Long root2 = insertComment(postId, readerId, null, "根评论二");
        insertComment(postId, readerId, root2, "回复三");

        IPage<CommentVO> page = blogCommentService.getCommentsByPostId(postId, 1, 10);

        assertEquals(2L, page.getTotal(), "total 必须是根评论数，不能把回复算进页数");
        assertEquals(2, page.getRecords().size());
        // 两条根评论的 create_time 可能落在同一时刻，排序不稳定，因此按内容定位而不按下标
        Map<String, CommentVO> byContent = page.getRecords().stream()
                .collect(Collectors.toMap(CommentVO::getContent, vo -> vo));
        CommentVO first = byContent.get("根评论一");
        CommentVO second = byContent.get("根评论二");
        assertNotNull(first);
        assertNotNull(second);
        assertEquals(2, first.getReplies().size(), "根评论一的直接回复必须挂在它下面");
        assertEquals(1, second.getReplies().size());
    }

    @Test
    @DisplayName("评论分页时只返回本页根评论下的回复，不泄露其它页的回复")
    void commentPageOnlyAttachesRepliesOfRootsOnThatPage() {
        Long postId = insertPost(authorId, "多根评论的文章", 1, 0);
        Long root1 = insertComment(postId, readerId, null, "根评论一");
        insertComment(postId, readerId, root1, "属于根评论一的回复");
        Long root2 = insertComment(postId, readerId, null, "根评论二");
        insertComment(postId, readerId, root2, "属于根评论二的回复");

        IPage<CommentVO> firstPage = blogCommentService.getCommentsByPostId(postId, 1, 1);

        assertEquals(2L, firstPage.getTotal());
        assertEquals(1, firstPage.getRecords().size());
        CommentVO only = firstPage.getRecords().get(0);
        assertEquals(1, only.getReplies().size(), "本页只有一条根评论，回复也只能是它自己的");
        assertEquals(only.getId(), only.getReplies().get(0).getParentId());

        IPage<CommentVO> secondPage = blogCommentService.getCommentsByPostId(postId, 2, 1);
        assertEquals(1, secondPage.getRecords().size());
        assertEquals(1, secondPage.getRecords().get(0).getReplies().size());
    }

    // ==================== 测试数据 ====================

    private Long insertUser(String username, int status) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword("password");
        user.setNickname(username);
        user.setRole("user");
        user.setStatus(status);
        user.setIsDeleted(0);
        jdbc.update("INSERT INTO sys_user (username, password, nickname, role, status, is_deleted) VALUES (?, ?, ?, ?, ?, ?)",
                user.getUsername(), user.getPassword(), user.getNickname(), user.getRole(), user.getStatus(), user.getIsDeleted());
        return jdbc.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
    }

    private Long insertPost(Long userId, String title, int status, int isDeleted) {
        jdbc.update("INSERT INTO blog_post (user_id, title, summary, content, status, is_deleted) VALUES (?, ?, ?, ?, ?, ?)",
                userId, title, title + "摘要", "正文", status, isDeleted);
        return jdbc.queryForObject("SELECT id FROM blog_post WHERE title = ?", Long.class, title);
    }

    private void insertLike(Long userId, Long postId) {
        BlogLike like = new BlogLike();
        like.setUserId(userId);
        like.setPostId(postId);
        like.setIsDeleted(0);
        jdbc.update("INSERT INTO blog_like (user_id, post_id, is_deleted) VALUES (?, ?, ?)",
                like.getUserId(), like.getPostId(), like.getIsDeleted());
    }

    private void insertCollect(Long userId, Long postId) {
        BlogCollect collect = new BlogCollect();
        collect.setUserId(userId);
        collect.setPostId(postId);
        collect.setIsDeleted(0);
        jdbc.update("INSERT INTO blog_collect (user_id, post_id, is_deleted) VALUES (?, ?, ?)",
                collect.getUserId(), collect.getPostId(), collect.getIsDeleted());
    }

    private void insertFollow(Long followerId, Long followingId) {
        BlogFollow follow = new BlogFollow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(followingId);
        follow.setIsDeleted(0);
        jdbc.update("INSERT INTO blog_follow (follower_id, following_id, is_deleted) VALUES (?, ?, ?)",
                follow.getFollowerId(), follow.getFollowingId(), follow.getIsDeleted());
    }

    private Long insertComment(Long postId, Long userId, Long parentId, String content) {
        BlogComment comment = new BlogComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setContent(content);
        jdbc.update("INSERT INTO blog_comment (post_id, user_id, parent_id, content, is_deleted) VALUES (?, ?, ?, ?, ?)",
                comment.getPostId(), comment.getUserId(), comment.getParentId(), comment.getContent(), 0);
        return jdbc.queryForObject("SELECT id FROM blog_comment WHERE content = ?", Long.class, content);
    }
}
