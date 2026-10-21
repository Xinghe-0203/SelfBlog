package com.selfblog.service.content;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.entity.Topic;

import java.util.List;

/**
 * 话题服务接口
 */
public interface TopicService extends IService<Topic> {

    /**
     * 创建话题（仅管理员）
     * @param name 话题名称
     * @param description 话题描述
     * @return 话题ID
     */
    Long createTopic(String name, String description);

    /**
     * 获取话题列表（分页）
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页话题列表
     */
    Page<Topic> getTopicList(Integer pageNum, Integer pageSize);

    /**
     * 获取热门话题
     * @param limit 数量
     * @return 热门话题列表
     */
    List<Topic> getHotTopics(int limit);

    /**
     * 根据话题名称获取或创建话题
     * @param name 话题名称
     * @return 话题ID
     */
    Long getOrCreateTopic(String name);

    /**
     * 获取话题详情
     * @param topicId 话题ID
     * @return 话题信息
     */
    Topic getTopicById(Long topicId);

    /**
     * 根据ID列表批量获取话题名称
     * @param ids 话题ID列表
     * @return 话题名称列表
     */
    List<String> getTopicNamesByIds(List<Long> ids);

    /**
     * 搜索话题
     * @param keyword 关键词
     * @return 匹配的话题列表
     */
    List<Topic> searchTopics(String keyword);

    /**
     * 更新话题（仅管理员）
     * @param id 话题ID
     * @param name 新名称，null 表示不修改
     * @param description 新描述，null 表示不修改
     * @param status 新状态，null 表示不修改
     * @return 更新后的话题
     */
    Topic updateTopic(Long id, String name, String description, Integer status);

    /**
     * 删除话题（仅管理员）
     * @param id 话题ID
     */
    void deleteTopic(Long id);

    /**
     * 切换话题状态（仅管理员）
     * @param id 话题ID
     * @param status 目标状态，只能是 0 或 1
     */
    void toggleStatus(Long id, Integer status);
}