package com.selfblog.controller.site;

import com.selfblog.common.result.Result;
import com.selfblog.dto.site.TrackVisitorRequest;
import com.selfblog.service.site.VisitorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/visitor")
@RequiredArgsConstructor
public class VisitorController {
    private final VisitorService visitorService;

    @PostMapping("/track")
    public Result<Void> track(@Valid @RequestBody TrackVisitorRequest request, HttpServletRequest httpRequest) {
        visitorService.track(request, httpRequest);
        return Result.success(null);
    }
}
