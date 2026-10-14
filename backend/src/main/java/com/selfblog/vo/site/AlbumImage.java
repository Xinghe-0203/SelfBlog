package com.selfblog.vo.site;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.nio.file.Path;

@Data
@AllArgsConstructor
public class AlbumImage {
    private Path path;
    private String mimeType;
}
