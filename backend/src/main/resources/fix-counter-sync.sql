-- ============================================================================
-- blog_post 计数器修复脚本 (SQLite)
-- ============================================================================
-- 用途: 修复 blog_post 表中 like_count / collect_count 与实际记录不一致的问题
-- 原因: 软删除、并发写入、手动改库等可能导致计数器与真实记录数不符
-- 安全: 所有查询均过滤 is_deleted=0，仅更新有效记录的计数
-- ============================================================================

-- ============================================================================
-- 第一步：备份当前计数（用于修复前后对比）
-- ============================================================================
-- 执行后可手动比对 backup_post_counts 和 actual_counts 的差异

CREATE TEMP TABLE IF NOT EXISTS backup_post_counts AS
SELECT id,
       title,
       like_count    AS old_like_count,
       collect_count AS old_collect_count
FROM blog_post
WHERE is_deleted = 0;

-- ============================================================================
-- 第二步：计算实际的 like / collect 记录数
-- ============================================================================

-- 2a: 实际点赞数（只统计 blog_like.is_deleted = 0 的记录）
CREATE TEMP TABLE IF NOT EXISTS actual_like_counts AS
SELECT l.post_id,
       COUNT(*) AS actual_count
FROM blog_like l
INNER JOIN blog_post p ON p.id = l.post_id AND p.is_deleted = 0
WHERE l.is_deleted = 0
GROUP BY l.post_id;

-- 2b: 实际收藏数（只统计 blog_collect.is_deleted = 0 的记录）
CREATE TEMP TABLE IF NOT EXISTS actual_collect_counts AS
SELECT c.post_id,
       COUNT(*) AS actual_count
FROM blog_collect c
INNER JOIN blog_post p ON p.id = c.post_id AND p.is_deleted = 0
WHERE c.is_deleted = 0
GROUP BY c.post_id;

-- ============================================================================
-- 第三步：预览差异（可选，仅查看不修改）
-- ============================================================================
-- 取消下面的注释可先查看哪些帖子的计数有偏差

-- SELECT bp.id,
--        bp.title,
--        bp.like_count                                   AS current_like,
--        COALESCE(a.actual_count, 0)                     AS actual_like,
--        bp.like_count - COALESCE(a.actual_count, 0)     AS like_diff,
--        bp.collect_count                                AS current_collect,
--        COALESCE(c.actual_count, 0)                     AS actual_collect,
--        bp.collect_count - COALESCE(c.actual_count, 0)  AS collect_diff
-- FROM blog_post bp
-- LEFT JOIN actual_like_counts a    ON a.post_id = bp.id
-- LEFT JOIN actual_collect_counts c ON c.post_id = bp.id
-- WHERE bp.is_deleted = 0
--   AND (bp.like_count != COALESCE(a.actual_count, 0)
--        OR bp.collect_count != COALESCE(c.actual_count, 0))
-- ORDER BY bp.id;

-- ============================================================================
-- 第四步：执行修复（UPDATE ... SET ... = 子查询）
-- ============================================================================
-- SQLite 支持在 SET 中使用标量子查询，无需 JOIN 语法
-- 仅更新有差异的行，减少写入量

UPDATE blog_post
SET like_count = (
        SELECT COALESCE(actual_count, 0)
        FROM actual_like_counts
        WHERE post_id = blog_post.id
    ),
    collect_count = (
        SELECT COALESCE(actual_count, 0)
        FROM actual_collect_counts
        WHERE post_id = blog_post.id
    ),
    update_time = datetime('now', 'localtime')
WHERE is_deleted = 0
  AND (
      like_count != COALESCE(
          (SELECT actual_count FROM actual_like_counts WHERE post_id = blog_post.id), 0
      )
      OR
      collect_count != COALESCE(
          (SELECT actual_count FROM actual_collect_counts WHERE post_id = blog_post.id), 0
      )
  );

-- ============================================================================
-- 第五步：验证修复结果
-- ============================================================================

-- 5a: 查看修复了多少行（受影响的行数在命令行工具中会自动显示）
--     若使用 SQLite CLI，可通过 .changes ON 查看

-- 5b: 确认修复后无残留差异
SELECT 'DIFF_CHECK' AS check_type,
       bp.id,
       bp.title,
       bp.like_count                                AS fixed_like,
       COALESCE(a.actual_count, 0)                  AS actual_like,
       bp.collect_count                             AS fixed_collect,
       COALESCE(c.actual_count, 0)                  AS actual_collect
FROM blog_post bp
LEFT JOIN actual_like_counts a    ON a.post_id = bp.id
LEFT JOIN actual_collect_counts c ON c.post_id = bp.id
WHERE bp.is_deleted = 0
  AND (bp.like_count != COALESCE(a.actual_count, 0)
       OR bp.collect_count != COALESCE(c.actual_count, 0));

-- 若上面查询返回 0 行，说明修复完全成功

-- 5c: 查看修复前后的对比
SELECT 'FIX_SUMMARY' AS check_type,
       b.id,
       b.title,
       b.old_like_count,
       p.like_count                                   AS new_like_count,
       b.old_collect_count,
       p.collect_count                                AS new_collect_count
FROM backup_post_counts b
INNER JOIN blog_post p ON p.id = b.id
WHERE b.old_like_count != p.like_count
   OR b.old_collect_count != p.collect_count
ORDER BY b.id;

-- ============================================================================
-- 清理临时表（会话结束自动清理，此处显式调用更清晰）
-- ============================================================================
DROP TABLE IF EXISTS backup_post_counts;
DROP TABLE IF EXISTS actual_like_counts;
DROP TABLE IF EXISTS actual_collect_counts;
