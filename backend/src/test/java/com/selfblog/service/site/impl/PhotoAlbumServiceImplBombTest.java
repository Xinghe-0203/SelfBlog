package com.selfblog.service.site.impl;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.PhotoAlbum;
import com.selfblog.mapper.AlbumPhotoMapper;
import com.selfblog.mapper.PhotoAlbumMapper;
import com.selfblog.utils.ImageBombFixtures;
import com.selfblog.utils.ImageDimensionUtils;
import com.selfblog.vo.site.PhotoAlbumVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * S5（相册路径）回归测试：相册照片只存原始字节、从不解码，因此更需要在落盘前
 * 按文件头拦截声明尺寸超大的解压缩炸弹。
 *
 * <p>修复前 {@code detectImageType} 只嗅探 12 个魔数字节，一张声明 40000x40000 的
 * 极小 PNG 会被原样存进 gallery 目录，并在后续每次请求里被原样送出。</p>
 *
 * <p>被测对象手工构造（不启 Spring），mapper 用 Mockito；相册通过
 * {@code albumMapper.selectById} 返回，因此不需要真的建库。</p>
 */
@DisplayName("PhotoAlbumServiceImpl（S5 相册路径）")
class PhotoAlbumServiceImplBombTest {

    @TempDir
    Path tempDir;

    private PhotoAlbumMapper albumMapper;
    private AlbumPhotoMapper photoMapper;
    private PhotoAlbumServiceImpl service;

    @BeforeEach
    void setUp() {
        albumMapper = mock(PhotoAlbumMapper.class);
        photoMapper = mock(AlbumPhotoMapper.class);
        service = new PhotoAlbumServiceImpl(albumMapper, photoMapper, new BCryptPasswordEncoder());
        ReflectionTestUtils.setField(service, "galleryPath", tempDir.toString());

        PhotoAlbum album = new PhotoAlbum();
        album.setId(7L);
        album.setTitle("测试相册");
        album.setIsPrivate(0);
        album.setSortOrder(0);
        album.setIsDeleted(0);
        when(albumMapper.selectById(7L)).thenReturn(album);
        when(photoMapper.selectList(any())).thenReturn(new java.util.ArrayList<>());
        // 真实 MyBatis-Plus insert 会回填自增主键；这里手工模拟，否则 toAlbumView 里
        // 用 photo.getId() 比较封面会 NPE（生产路径不会出现 null id）。
        java.util.concurrent.atomic.AtomicLong nextId = new java.util.concurrent.atomic.AtomicLong(1L);
        when(photoMapper.insert(org.mockito.ArgumentMatchers.<com.selfblog.entity.AlbumPhoto>any()))
                .thenAnswer(invocation -> {
                    com.selfblog.entity.AlbumPhoto photo = invocation.getArgument(0);
                    photo.setId(nextId.getAndIncrement());
                    return 1;
                });
    }

    @Test
    @DisplayName("声明 40000x40000 的极小 PNG 在上传时被拒绝，且不落盘")
    void absurdlyLargeDeclaredPhotoIsRejectedAndNotStored() throws Exception {
        byte[] bomb = ImageBombFixtures.pngWithDeclaredSize(40000, 40000);
        MockMultipartFile file = new MockMultipartFile("files", "bomb.png", "image/png", bomb);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.uploadPhotos(7L, new MockMultipartFile[]{file}));

        assertEquals(400, exception.getCode());
        assertEquals(ImageDimensionUtils.TOO_LARGE_MESSAGE, exception.getMessage());
        // 关键：炸弹没有被写进 gallery 目录。
        try (var stream = Files.walk(tempDir)) {
            assertEquals(0, stream.filter(Files::isRegularFile).count(),
                    "被拒绝的炸弹不得落盘");
        }
    }

    @Test
    @DisplayName("正常尺寸的真实 PNG 仍可上传")
    void normalSizedPhotoIsStillAccepted() throws Exception {
        java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(32, 24, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", out);
        MockMultipartFile file = new MockMultipartFile("files", "ok.png", "image/png", out.toByteArray());

        List<PhotoAlbumVO> result = service.uploadPhotos(7L, new MockMultipartFile[]{file});

        assertEquals(1, result.size());
        try (var stream = Files.walk(tempDir)) {
            assertEquals(1, stream.filter(Files::isRegularFile).count(), "正常图片应当落盘");
        }
    }

    @Test
    @DisplayName("魔数校验仍在：伪装成 PNG 的 HTML 依旧被拒绝")
    void magicNumberCheckIsStillEnforced() {
        MockMultipartFile file = new MockMultipartFile("files", "fake.png", "image/png",
                "<html><body>hi</body></html>".getBytes());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.uploadPhotos(7L, new MockMultipartFile[]{file}));

        assertEquals(400, exception.getCode());
    }

    @Test
    @DisplayName("超过字节上限仍然被拒绝（原有检查未被替换）")
    void byteSizeCapIsStillEnforced() {
        byte[] big = new byte[16 * 1024 * 1024];
        // 先放 PNG 魔数，确保拒绝原因是字节上限而不是魔数。
        System.arraycopy(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, 0, big, 0, 4);
        MockMultipartFile file = new MockMultipartFile("files", "big.png", "image/png", big);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.uploadPhotos(7L, new MockMultipartFile[]{file}));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("15MB"),
                "应命中字节上限文案，实际：" + exception.getMessage());
    }

    /** 确认被测类的构造签名未被这次改动改坏。 */
    @Test
    @DisplayName("构造与相册存在性校验保持原状")
    void albumNotFoundStillReturns404() {
        when(albumMapper.selectById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getPublicAlbum(99L, null));

        assertEquals(404, exception.getCode());
    }
}
