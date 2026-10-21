package com.selfblog.service.social.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.enums.ReportStatus;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.social.HandleReportRequest;
import com.selfblog.dto.social.ReportRequest;
import com.selfblog.entity.*;
import com.selfblog.mapper.*;
import com.selfblog.service.social.BlogCommentService;
import com.selfblog.service.social.ReportService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.utils.UserConverter;
import com.selfblog.vo.social.ReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 举报服务实现类
 */
@Slf4j
@Service
public class ReportServiceImpl extends ServiceImpl<BlogReportMapper, BlogReport> implements ReportService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BlogPostMapper blogPostMapper;

    @Autowired
    private BlogCommentMapper blogCommentMapper;

    @Autowired
    private BlogCommentService blogCommentService;

    @Autowired
    private BlogCollectMapper blogCollectMapper;

    @Autowired
    private BlogLikeMapper blogLikeMapper;

    @Autowired
    private com.selfblog.service.social.NotificationService notificationService;

    @Autowired
    private com.selfblog.mapper.TopicMapper topicMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createReport(ReportRequest request, Long reporterId) {
        // 参数校验
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException(400, "举报原因不能为空");
        }
        if (request.getReason().length() > 1000) {
            throw new BusinessException(400, "举报原因不能超过1000字符");
        }

        // 校验 targetType
        String targetType = request.getTargetType();
        if (!isValidTargetType(targetType)) {
            throw new BusinessException(400, "无效的举报目标类型");
        }

        // 校验 targetId
        if (request.getTargetId() == null || request.getTargetId() <= 0) {
            throw new BusinessException(400, "无效的举报目标ID");
        }

        // 获取被举报用户ID
        Long reportedUserId = getReportedUserId(targetType, request.getTargetId());
        if (reportedUserId == null) {
            throw new BusinessException(404, "举报目标不存在");
        }

        // 不能举报自己
        if (Objects.equals(reporterId, reportedUserId)) {
            throw new BusinessException(400, "不能举报自己");
        }

        // 检查是否存在非REJECTED状态的举报（PENDING或RESOLVED）
        LambdaQueryWrapper<BlogReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogReport::getReporterId, reporterId)
               .eq(BlogReport::getTargetType, targetType)
               .eq(BlogReport::getTargetId, request.getTargetId())
               .ne(BlogReport::getStatus, ReportStatus.REJECTED.getValue());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(400, "您已提交过该举报，请勿重复提交");
        }

        // 创建举报记录
        BlogReport report = new BlogReport();
        report.setReporterId(reporterId);
        report.setReportedUserId(reportedUserId);
        report.setTargetType(targetType);
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason().trim());
        report.setStatus(0); // 待处理

        this.save(report);

        log.info("[AUDIT] 举报创建: reportId={}, reporterId={}, targetType={}, targetId={}, reason={}",
                report.getId(), reporterId, targetType, request.getTargetId(), request.getReason());
        return report.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<ReportVO> getMyReports(Integer page, Integer pageSize, Long reporterId) {
        Page<BlogReport> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<BlogReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogReport::getReporterId, reporterId)
                .orderByDesc(BlogReport::getCreateTime);

        IPage<BlogReport> reportPage = this.page(pageParam, wrapper);
        return convertToVOPage(reportPage);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<ReportVO> getPendingReports(Integer page, Integer pageSize) {
        // 管理员权限校验
        if (!SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "需要管理员权限");
        }
        Page<BlogReport> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<BlogReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogReport::getStatus, 0) // 待处理状态
                .orderByAsc(BlogReport::getCreateTime); // 按时间正序，先举报先处理

        IPage<BlogReport> reportPage = this.page(pageParam, wrapper);
        return convertToVOPage(reportPage);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<ReportVO> getReportsByStatus(Integer page, Integer pageSize, Integer status) {
        if (!SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "需要管理员权限");
        }
        Page<BlogReport> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<BlogReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(BlogReport::getStatus, status);
        }
        wrapper.orderByAsc(BlogReport::getCreateTime);

        IPage<BlogReport> reportPage = this.page(pageParam, wrapper);
        return convertToVOPage(reportPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportVO getReportDetail(Long reportId) {
        // 管理员权限校验
        if (!SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "需要管理员权限");
        }
        BlogReport report = this.getById(reportId);
        if (report == null) {
            throw new BusinessException(404, "举报记录不存在");
        }
        // 使用单条记录转换（复用batch逻辑以保证一致性）
        return convertSingleToVO(report, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleReport(Long reportId, HandleReportRequest request, Long handlerId) {
        // 管理员权限校验
        if (!SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "需要管理员权限");
        }
        // 参数校验
        if (request.getStatus() == null) {
            throw new BusinessException(400, "处理状态不能为空");
        }
        if (request.getStatus() != 1 && request.getStatus() != 2) {
            throw new BusinessException(400, "处理状态必须是1（已驳回）或2（已核实）");
        }

        BlogReport report = this.getById(reportId);
        if (report == null) {
            throw new BusinessException(404, "举报记录不存在");
        }
        if (report.getStatus() != 0) {
            throw new BusinessException(400, "该举报已被处理");
        }

        // 更新举报状态
        report.setStatus(request.getStatus());
        report.setHandlerId(handlerId);
        report.setHandlerResult(request.getHandlerResult());
        report.setHandleTime(LocalDateTime.now());

        this.updateById(report);

        log.info("[AUDIT] 举报处理: reportId={}, handlerId={}, status={}, result={}",
                reportId, handlerId, request.getStatus(), request.getHandlerResult());

        // 如果是已核实状态，对被举报内容进行相应处理
        if (request.getStatus() == ReportStatus.RESOLVED.getValue()) {
            String targetType = report.getTargetType();
            Long targetId = report.getTargetId();

            switch (targetType) {
                case "post":
                    BlogPost post = blogPostMapper.selectById(targetId);
                    if (post != null) {
                        // 记录文章的话题信息，用于后续调整话题计数
                        List<Long> topicIds = parseTopicIds(post.getTopicIds());

                        // 必须用显式 UpdateWrapper 写 is_deleted：MyBatis-Plus 的 updateById 会把
                        // logic-delete-field 从 SET 子句剔除并在 WHERE 追加 is_deleted = 0，
                        // 于是 post.setIsDeleted(1) + updateById 是静默空操作——被举报文章会继续
                        // 在线可读，而下面的点赞/收藏清理与话题计数却已经执行。
                        blogPostMapper.update(null, new LambdaUpdateWrapper<BlogPost>()
                                .eq(BlogPost::getId, targetId)
                                .set(BlogPost::getIsDeleted, 1));
                        // 级联清理点赞记录
                        blogLikeMapper.update(null, new LambdaUpdateWrapper<BlogLike>()
                                .eq(BlogLike::getPostId, targetId)
                                .set(BlogLike::getIsDeleted, 1));
                        // 级联清理收藏记录
                        blogCollectMapper.update(null, new LambdaUpdateWrapper<BlogCollect>()
                                .eq(BlogCollect::getPostId, targetId)
                                .set(BlogCollect::getIsDeleted, 1));

                        // 调整话题的文章计数
                        for (Long topicId : topicIds) {
                            topicMapper.decrementPostCount(topicId);
                        }
                    }
                    break;
                case "comment":
                    blogCommentService.removeById(targetId);
                    break;
                case "user":
                    // 封禁用户
                    SysUser user = sysUserMapper.selectById(targetId);
                    if (user != null) {
                        user.setStatus(0); // 设为禁用状态
                        sysUserMapper.updateById(user);
                    }
                    break;
            }
        }

        // 通知举报人处理结果
        String notifyTitle = request.getStatus() == ReportStatus.RESOLVED.getValue() ? "举报已核实" : "举报已驳回";
        String notifyContent = "您提交的举报（ID: " + reportId + "）已处理，处理结果：" + (request.getHandlerResult() != null ? request.getHandlerResult() : (request.getStatus() == ReportStatus.RESOLVED.getValue() ? "举报内容已被处理" : "举报不成立"));
        notificationService.sendNotification("REPORT", notifyTitle, notifyContent, handlerId, report.getReporterId(), report.getTargetType(), report.getTargetId());

        // 通知被举报内容作者处理结果
        if (request.getStatus() == ReportStatus.RESOLVED.getValue()) {
            String targetNotifyTitle = "内容处理通知";
            String targetNotifyContent = "您的" + getTargetTypeLabel(report.getTargetType()) + "因违反社区规定已被处理，详情：" + (request.getHandlerResult() != null ? request.getHandlerResult() : "内容已被删除或限制");
            Long targetAuthorId = getReportedUserId(report.getTargetType(), report.getTargetId());
            if (targetAuthorId != null && !targetAuthorId.equals(report.getReporterId())) {
                notificationService.sendNotification("REPORT", targetNotifyTitle, targetNotifyContent, handlerId, targetAuthorId, report.getTargetType(), report.getTargetId());
            }
        }
    }

    /**
     * 获取目标类型的中文标签
     */
    private String getTargetTypeLabel(String targetType) {
        return switch (targetType) {
            case "post" -> "文章";
            case "comment" -> "评论";
            case "user" -> "账号";
            default -> "内容";
        };
    }

    /**
     * 校验目标类型是否合法
     */
    private boolean isValidTargetType(String targetType) {
        return "post".equals(targetType) || "comment".equals(targetType) || "user".equals(targetType);
    }

    /**
     * 根据目标类型和ID获取被举报用户ID
     */
    private Long getReportedUserId(String targetType, Long targetId) {
        switch (targetType) {
            case "post":
                BlogPost post = blogPostMapper.selectById(targetId);
                return post != null ? post.getUserId() : null;
            case "comment":
                BlogComment comment = blogCommentMapper.selectById(targetId);
                return comment != null ? comment.getUserId() : null;
            case "user":
                SysUser user = sysUserMapper.selectById(targetId);
                return user != null ? user.getId() : null;
            default:
                return null;
        }
    }

    /**
     * 转换单条举报记录为VO（使用批量查询避免N+1问题）
     * @param report 举报记录
     * @param userMap 预加载的用户Map（可选，用于批量转换场景）
     */
    private ReportVO convertSingleToVO(BlogReport report, Map<Long, SysUser> userMap) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setTargetType(report.getTargetType());
        vo.setTargetId(report.getTargetId());
        vo.setReason(report.getReason());
        vo.setStatus(report.getStatus());
        vo.setHandlerResult(report.getHandlerResult());
        vo.setHandleTime(report.getHandleTime());
        vo.setCreateTime(report.getCreateTime());

        // 如果没有传入userMap，则使用批量查询（避免N+1）
        if (userMap == null) {
            userMap = new HashMap<>();
            Set<Long> userIds = new HashSet<>();
            userIds.add(report.getReporterId());
            userIds.add(report.getReportedUserId());
            if (report.getHandlerId() != null) {
                userIds.add(report.getHandlerId());
            }
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
            for (SysUser user : users) {
                userMap.put(user.getId(), user);
            }
        }

        // 设置举报人信息
        SysUser reporter = userMap.get(report.getReporterId());
        if (reporter != null) {
            vo.setReporter(UserConverter.toUserVO(reporter));
        }

        // 设置被举报用户信息
        SysUser reportedUser = userMap.get(report.getReportedUserId());
        if (reportedUser != null) {
            vo.setReportedUser(UserConverter.toUserVO(reportedUser));
        }

        // 设置处理人信息
        if (report.getHandlerId() != null) {
            vo.setHandlerId(report.getHandlerId());
            SysUser handler = userMap.get(report.getHandlerId());
            if (handler != null) {
                vo.setHandler(UserConverter.toUserVO(handler));
            }
        }

        return vo;
    }

    /**
     * 转换分页结果（优化：批量查询用户信息避免N+1）
     */
    private IPage<ReportVO> convertToVOPage(IPage<BlogReport> reportPage) {
        List<BlogReport> reports = reportPage.getRecords();
        if (reports.isEmpty()) {
            return new Page<>(reportPage.getCurrent(), reportPage.getSize(), reportPage.getTotal());
        }

        // 收集所有需要的用户ID（包括handler）
        Map<Long, SysUser> userMap = new HashMap<>();
        for (BlogReport report : reports) {
            userMap.put(report.getReporterId(), null);
            userMap.put(report.getReportedUserId(), null);
            if (report.getHandlerId() != null) {
                userMap.put(report.getHandlerId(), null);
            }
        }

        // 批量查询用户信息（一次查询替代N次）
        if (!userMap.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userMap.keySet());
            for (SysUser user : users) {
                userMap.put(user.getId(), user);
            }
        }

        // 转换为VO
        Page<ReportVO> result = new Page<>(reportPage.getCurrent(), reportPage.getSize(), reportPage.getTotal());
        result.setRecords(reports.stream()
                .map(report -> convertSingleToVO(report, userMap))
                .collect(java.util.stream.Collectors.toList()));

        return result;
    }

    /**
     * 解析话题ID数组字符串
     */
    private List<Long> parseTopicIds(String topicIdsJson) {
        if (topicIdsJson == null || topicIdsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return cn.hutool.json.JSONUtil.toList(topicIdsJson, Long.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}