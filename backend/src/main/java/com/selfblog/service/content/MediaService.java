package com.selfblog.service.content;

import com.baomidou.mybatisplus.extension.service.IService;
import com.selfblog.dto.social.MediaQueryRequest;
import com.selfblog.entity.Media;
import com.selfblog.vo.content.MediaVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 媒体服务接口
 */
public interface MediaService extends IService<Media> {

    /**
     * 上传媒体文件
     * @param file 文件
     * @param userId 上传用户ID
     * @param type 上传类型: article=文章(所有文件), circle=校友圈(图片+视频), avatar=头像(仅图片)
     * @return 媒体信息
     */
    MediaVO uploadMedia(MultipartFile file, Long userId, String type);

    /**
     * 批量上传媒体文件
     * @param files 文件数组（最多9个）
     * @param userId 上传用户ID
     * @param type 上传类型
     * @return 媒体信息列表
     */
    List<MediaVO> uploadFiles(MultipartFile[] files, Long userId, String type);

    /**
     * 删除媒体文件
     * @param mediaId 媒体ID
     * @param userId 操作用户ID
     */
    void deleteMedia(Long mediaId, Long userId);

    /**
     * 获取媒体信息
     * @param mediaId 媒体ID
     * @return 媒体信息
     */
    MediaVO getMediaInfo(Long mediaId);

    /**
     * 分页查询用户媒体
     * @param request 查询请求
     * @param userId 用户ID
     * @return 媒体信息列表
     */
    List<MediaVO> getUserMedia(MediaQueryRequest request, Long userId);

    /**
     * 绑定文章媒体关联
     * @param postId 文章ID
     * @param mediaIds 媒体ID列表
     */
    void bindPostMedia(Long postId, List<Long> mediaIds);

    /**
     * 获取文章的媒体列表
     * @param postId 文章ID
     * @return 媒体列表
     */
    List<MediaVO> getPostMedia(Long postId);
}