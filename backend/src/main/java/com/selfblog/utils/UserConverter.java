package com.selfblog.utils;

import com.selfblog.entity.SysUser;
import com.selfblog.vo.user.UserVO;

/**
 * 用户实体转换工具类
 */
public class UserConverter {

    /**
     * 将 SysUser 转换为 UserVO
     * 包含常用公开字段，不包含密码
     */
    public static UserVO toUserVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setCoverImage(user.getCoverImage());
        vo.setBio(user.getBio());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setFollowingCount(user.getFollowingCount());
        return vo;
    }
}
