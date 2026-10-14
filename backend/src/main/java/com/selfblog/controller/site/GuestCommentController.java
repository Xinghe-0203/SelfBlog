package com.selfblog.controller.site;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.result.Result;
import com.selfblog.dto.site.GuestCommentRequest;
import com.selfblog.service.site.GuestCommentService;
import com.selfblog.vo.site.GuestCommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/guest-comments")
@RequiredArgsConstructor
public class GuestCommentController {
    private final GuestCommentService commentService;

    @GetMapping("/post/{postId}")
    public Result<IPage<GuestCommentVO>> list(@PathVariable Long postId,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(commentService.getPublicComments(postId, pageNum, pageSize));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody GuestCommentRequest request) {
        return Result.success(commentService.createComment(request));
    }
}
