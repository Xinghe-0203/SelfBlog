package com.selfblog.dto.site;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlbumForm {
    @NotBlank
    @Size(max = 160)
    private String title;
    @Size(max = 1000)
    private String description;
    private Boolean privateAlbum;
    @Size(max = 100)
    private String password;
    private Integer sortOrder;
}
