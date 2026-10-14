package com.selfblog.controller.site;

import com.selfblog.common.result.Result;
import com.selfblog.service.post.AnonymousLikeService;
import com.selfblog.vo.post.AnonymousLikeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/anonymous-likes/posts")
@RequiredArgsConstructor
public class AnonymousLikeController {
    private final AnonymousLikeService anonymousLikeService;

    @GetMapping("/{postId}")
    public Result<AnonymousLikeVO> status(@PathVariable Long postId,
                                         @RequestHeader("X-Visitor-Id") String visitorId) {
        return Result.success(anonymousLikeService.getStatus(postId, visitorId));
    }

    @PostMapping("/{postId}")
    public Result<AnonymousLikeVO> toggle(@PathVariable Long postId,
                                          @RequestHeader("X-Visitor-Id") String visitorId) {
        return Result.success(anonymousLikeService.toggle(postId, visitorId));
    }
}
