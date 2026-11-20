package com.selfblog.service.post;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.entity.BlogTag;
import com.selfblog.vo.post.TagWithCountVO;

/**
 * 标签服务接口
 */
public interface BlogTagService extends IService<BlogTag> {

    /**
     * 获取所有标签列表
     * @return 标签列表
     */
    java.util.List<BlogTag> listAllTags();

    /**
     * 公开标签索引：标签 + 已发布文章数（按文章数倒序，同数按名称升序）。
     * @return 带计数的标签列表
     */
    java.util.List<TagWithCountVO> listTagsWithCounts();

    /**
     * 创建标签
     * @param name 标签名称
     * @return 创建的标签
     */
    BlogTag createTag(String name);

    /**
     * 删除标签（仅管理员可操作）
     * @param tagId 标签ID
     */
    void deleteTag(Long tagId);

    /**
     * 根据标签名称获取或创建标签
     * @param name 标签名称
     * @return 标签ID，如果名称为空则返回 null
     */
    Long getOrCreateTag(String name);

    /**
     * 搜索标签
     * @param keyword 关键词
     * @return 匹配的标签列表
     */
    java.util.List<BlogTag> searchTags(String keyword);
}