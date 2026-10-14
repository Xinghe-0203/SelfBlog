package com.selfblog.dto.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GuestCommentRequest {
    @NotNull
    private Long postId;
    @NotBlank
    @Size(min = 8, max = 80)
    private String visitorId;
    @NotBlank
    @Size(max = 80)
    private String nickname;
    @NotBlank
    @Size(max = 2000)
    private String content;
}
