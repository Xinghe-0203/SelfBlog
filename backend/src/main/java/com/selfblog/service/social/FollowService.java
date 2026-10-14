package com.selfblog.service.social;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.entity.BlogFollow;
import com.selfblog.vo.social.FollowStatusVO;
import com.selfblog.vo.user.UserVO;

import java.util.List;

/**
 * 关注服务接口
 */
public interface FollowService extends IService<BlogFollow> {

    /**
     * 关注用户
     * @param targetUserId 目标用户ID
     * @param currentUserId 当前用户ID
     * @return 关注状态
     */
    FollowStatusVO follow(Long targetUserId, Long currentUserId);

    /**
     * 取消关注
     * @param targetUserId 目标用户ID
     * @param currentUserId 当前用户ID
     * @return 关注状态
     */
    FollowStatusVO unfollow(Long targetUserId, Long currentUserId);

    /**
     * 检查是否关注
     * @param targetUserId 目标用户ID
     * @param currentUserId 当前用户ID
     * @return 是否关注
     */
    boolean isFollowing(Long targetUserId, Long currentUserId);

    /**
     * 获取粉丝列表（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 粉丝分页列表
     */
    IPage<UserVO> getFollowers(Long userId, Integer page, Integer pageSize);

    /**
     * 获取关注列表（分页）
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 关注分页列表
     */
    IPage<UserVO> getFollowing(Long userId, Integer page, Integer pageSize);

    /**
     * 获取粉丝列表
     * @param userId 用户ID
     * @return 粉丝列表
     */
    List<UserVO> getFollowers(Long userId);

    /**
     * 获取关注列表
     * @param userId 用户ID
     * @return 关注列表
     */
    List<UserVO> getFollowing(Long userId);

    /**
     * 获取用户粉丝数和关注数
     * @param userId 用户ID
     * @return 粉丝数和关注数
     */
    FollowCountsVO getCounts(Long userId);

    /**
     * 粉丝数和关注数 VO
     */
    class FollowCountsVO {
        private Long userId;
        private Integer followerCount;
        private Integer followingCount;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Integer getFollowerCount() { return followerCount; }
        public void setFollowerCount(Integer followerCount) { this.followerCount = followerCount; }
        public Integer getFollowingCount() { return followingCount; }
        public void setFollowingCount(Integer followingCount) { this.followingCount = followingCount; }
    }
}