package com.selfblog.service.content.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.Topic;
import com.selfblog.mapper.TopicMapper;
import com.selfblog.service.content.TopicService;
import com.selfblog.utils.HtmlSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 话题服务实现类
 */
@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTopic(String name, String description) {
        // 校验话题名称
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(400, "话题名称不能为空");
        }
        String sanitizedName = htmlSanitizer.sanitizePlainText(name.trim());
        if (sanitizedName.length() > 50) {
            throw new BusinessException(400, "话题名称不能超过50字符");
        }

        // 检查话题是否已存在
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Topic::getName, sanitizedName);
        Topic existingTopic = this.getOne(wrapper);
        if (existingTopic != null) {
            throw new BusinessException(400, "话题已存在");
        }

        // 创建话题
        Topic topic = new Topic();
        topic.setName(sanitizedName);
        topic.setDescription(description != null ? htmlSanitizer.sanitizePlainText(description) : null);
        topic.setTrendingScore(0);
        topic.setStatus(1);

        this.save(topic);
        return topic.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Topic> getTopicList(Integer pageNum, Integer pageSize) {
        Page<Topic> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Topic::getStatus, 1) // 只查询正常状态的话题
                .orderByDesc(Topic::getTrendingScore) // 按热度排序
                .orderByDesc(Topic::getCreateTime);

        return this.page(page, wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> getHotTopics(int limit) {
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Topic::getStatus, 1) // 只查询正常状态的话题
                .orderByDesc(Topic::getTrendingScore) // 按热度排序
                .last("LIMIT " + limit);

        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreateTopic(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String trimmedName = name.trim();
        // 去除#号
        if (trimmedName.startsWith("#")) {
            trimmedName = trimmedName.substring(1);
        }

        String sanitizedName = htmlSanitizer.sanitizePlainText(trimmedName);

        if (sanitizedName.isEmpty()) {
            return null;
        }

        // 限制话题名称长度
        if (sanitizedName.length() > 50) {
            sanitizedName = sanitizedName.substring(0, 50);
        }

        // 查询已存在的话题
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Topic::getName, sanitizedName);
        Topic existingTopic = this.getOne(wrapper);

        if (existingTopic != null) {
            return existingTopic.getId();
        }

        // 创建新话题
        Topic topic = new Topic();
        topic.setName(sanitizedName);
        topic.setDescription(null);
        topic.setTrendingScore(0);
        topic.setStatus(1);

        try {
            this.save(topic);
            return topic.getId();
        } catch (DataAccessException e) {
            // 并发创建时处理唯一约束冲突：不同数据库把约束错误翻译成不同异常类型
            // （H2/MySQL → DuplicateKeyException，SQLite → UncategorizedSQLException），
            // 统一捕获父类；找不到同名记录时保留原始异常而不是掩盖。
            LambdaQueryWrapper<Topic> retryWrapper = new LambdaQueryWrapper<>();
            retryWrapper.eq(Topic::getName, sanitizedName);
            Topic existing = this.getOne(retryWrapper);
            if (existing != null) {
                return existing.getId();
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTopicNamesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Topic::getId, ids)
                .eq(Topic::getStatus, 1);
        return this.list(wrapper).stream()
                .map(Topic::getName)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Topic getTopicById(Long topicId) {
        Topic topic = this.getById(topicId);
        if (topic == null || topic.getStatus() != 1) {
            throw new BusinessException(404, "话题不存在");
        }
        return topic;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Topic> searchTopics(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Topic::getName, keyword.trim())
               .eq(Topic::getStatus, 1)
               .orderByDesc(Topic::getTrendingScore)
               .last("LIMIT 20");
        return this.list(wrapper);
    }
}