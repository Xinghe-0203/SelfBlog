package com.selfblog.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.selfblog.common.result.Result;
import com.selfblog.service.site.GuestCommentService;
import com.selfblog.vo.site.GuestCommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/guest-comments")
@PreAuthorize("hasRole('admin')")
@RequiredArgsConstructor
public class AdminGuestCommentController {
    private final GuestCommentService commentService;

    @GetMapping
    public Result<IPage<GuestCommentVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize,
                                              @RequestParam(required = false) Integer status) {
        return Result.success(commentService.getAdminComments(pageNum, pageSize, status));
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        commentService.updateStatus(id, body.get("status"));
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.success(null);
    }
}
