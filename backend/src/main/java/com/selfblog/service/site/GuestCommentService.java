package com.selfblog.service.site;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.dto.site.GuestCommentRequest;
import com.selfblog.vo.site.GuestCommentVO;

public interface GuestCommentService {
    IPage<GuestCommentVO> getPublicComments(Long postId, Integer page, Integer pageSize);
    Long createComment(GuestCommentRequest request);
    IPage<GuestCommentVO> getAdminComments(Integer page, Integer pageSize, Integer status);
    void updateStatus(Long id, Integer status);
    void delete(Long id);
}
