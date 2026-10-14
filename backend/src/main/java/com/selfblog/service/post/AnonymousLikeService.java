package com.selfblog.service.post;

import com.selfblog.vo.post.AnonymousLikeVO;

public interface AnonymousLikeService {
    AnonymousLikeVO getStatus(Long postId, String visitorId);
    AnonymousLikeVO toggle(Long postId, String visitorId);
}
