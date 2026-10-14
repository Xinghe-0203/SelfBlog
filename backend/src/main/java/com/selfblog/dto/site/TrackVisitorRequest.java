package com.selfblog.dto.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TrackVisitorRequest {
    @NotBlank
    @Size(min = 8, max = 80)
    private String visitorId;

    @NotBlank
    @Size(max = 255)
    private String path;

    @Size(max = 500)
    private String referrer;
}
