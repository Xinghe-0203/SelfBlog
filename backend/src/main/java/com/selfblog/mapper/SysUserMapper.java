package com.selfblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.selfblog.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户表 Mapper 接口
 * 【说明】继承 MyBatis Plus 的 BaseMapper，自动拥有 CRUD 方法
 * 【数据库兼容】MySQL/SQLite - 自定义 SQL 已移至 SysUserMapper.xml
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 只在密码快照仍有效时更新，防止并发改密覆盖另一笔已提交的密码。 */
    int updatePasswordIfUnchanged(@Param("userId") Long userId,
                                  @Param("oldPassword") String oldPassword,
                                  @Param("newPassword") String newPassword);

    /**
     * 原子性增加登录失败计数并检查是否需要锁定
     */
    int incrementLoginFailCount(@Param("userId") Long userId, @Param("maxFailCount") int maxFailCount, @Param("lockMinutes") int lockMinutes);

    /**
     * 原子性增加粉丝数
     */
    int incrementFollowerCount(@Param("userId") Long userId);

    /**
     * 原子性减少粉丝数
     */
    int decrementFollowerCount(@Param("userId") Long userId);

    /**
     * 原子性增加关注数
     */
    int incrementFollowingCount(@Param("userId") Long userId);

    /**
     * 原子性减少关注数
     */
    int decrementFollowingCount(@Param("userId") Long userId);

    /**
     * 批量统计每日新增用户数（避免 N+1 查询）
     */
    List<Map<String, Object>> countUsersGroupByDate(@Param("since") LocalDateTime since);

    /**
     * 绕过 @TableLogic 按用户名查任意状态（含已软删除）的用户行。
     *
     * <p>sys_user.username 有唯一约束，而删除是逻辑删除：删除后同名行仍占用唯一索引。
     * 启动播种（DataInitializer）必须先能看到那行已删除的记录并恢复它，
     * 否则查不到 → 再次 INSERT → 撞唯一键 → 应用启动失败（生产崩溃循环）。
     * 这里是刻意不过滤 is_deleted 的原始查询。
     */
    SysUser selectRawByUsername(@Param("username") String username);

    /** 恢复被逻辑删除的用户行（is_deleted=1 → 0）。 */
    int restoreDeletedById(@Param("userId") Long userId);
}
