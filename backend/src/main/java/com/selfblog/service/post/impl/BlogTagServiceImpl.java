package com.selfblog.service.post.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.BlogTag;
import com.selfblog.mapper.BlogTagMapper;
import com.selfblog.service.post.BlogTagService;
import com.selfblog.utils.HtmlSanitizer;
import com.selfblog.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 标签服务实现类
 */
@Service
public class BlogTagServiceImpl extends ServiceImpl<BlogTagMapper, BlogTag> implements BlogTagService {

    @Autowired
    private HtmlSanitizer htmlSanitizer;

    @Override
    @Transactional(readOnly = true)
    public List<BlogTag> listAllTags() {
        LambdaQueryWrapper<BlogTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(BlogTag::getName);
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogTag createTag(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(400, "标签名称不能为空");
        }
        String trimmedName = htmlSanitizer.sanitizePlainText(name.trim());
        if (trimmedName.length() > 20) {
            throw new BusinessException(400, "标签名称不能超过20个字符");
        }
        // 检查标签是否已存在
        LambdaQueryWrapper<BlogTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogTag::getName, trimmedName);
        BlogTag existingTag = this.getOne(wrapper);
        if (existingTag != null) {
            throw new BusinessException(409, "标签已存在");
        }
        BlogTag tag = new BlogTag();
        tag.setName(trimmedName);
        try {
            this.save(tag);
        } catch (DataAccessException e) {
            // 名称唯一 + 逻辑删除：冲突行可能是被删除的同名标签，需要恢复而不是返回 null。
            // 捕获父类 DataAccessException：H2/MySQL 是 DuplicateKeyException，
            // SQLite 的约束错误是 UncategorizedSQLException，只有父类可移植。
            BlogTag existing = restoreOrFind(trimmedName);
            if (existing == null) {
                throw e;
            }
            return existing;
        }
        return tag;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreateTag(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String sanitizedName = htmlSanitizer.sanitizePlainText(name.trim());
        if (sanitizedName.length() > 20) {
            sanitizedName = sanitizedName.substring(0, 20);
        }
        LambdaQueryWrapper<BlogTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogTag::getName, sanitizedName);
        BlogTag existing = this.getOne(wrapper);
        if (existing != null) {
            return existing.getId();
        }
        BlogTag tag = new BlogTag();
        tag.setName(sanitizedName);
        try {
            this.save(tag);
        } catch (DataAccessException e) {
            // 与 createTag 相同：同名行可能只是被逻辑删除，恢复后返回其 id。
            BlogTag restored = restoreOrFind(sanitizedName);
            if (restored == null) {
                throw e;
            }
            return restored.getId();
        }
        return tag.getId();
    }

    /**
     * 名称冲突时找回同名标签：先恢复被逻辑删除的行，再退回到正常查询。
     * 返回值可能为 null（极端并发下记录已被物理删除），由调用方决定如何报错。
     */
    private BlogTag restoreOrFind(String name) {
        BlogTag raw = baseMapper.selectRawByName(name);
        if (raw == null) {
            return null;
        }
        if (raw.getIsDeleted() != null && raw.getIsDeleted() == 1) {
            baseMapper.restoreById(raw.getId());
            raw.setIsDeleted(0);
        }
        return raw;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long tagId) {
        if (!SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "仅管理员可删除标签");
        }
        BlogTag tag = this.getById(tagId);
        if (tag == null) {
            throw new BusinessException(404, "标签不存在");
        }
        this.removeById(tagId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogTag> searchTags(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<BlogTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(BlogTag::getName, keyword.trim())
               .orderByAsc(BlogTag::getName);
        return this.list(wrapper);
    }
}