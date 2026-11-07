package com.selfblog.service.site.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.selfblog.entity.AlbumPhoto;
import com.selfblog.entity.PhotoAlbum;
import com.selfblog.mapper.AlbumPhotoMapper;
import com.selfblog.mapper.PhotoAlbumMapper;
import com.selfblog.vo.site.PhotoAlbumVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 相册列表页的 N+1 修复回归：{@code getPublicAlbums}/{@code getAdminAlbums} 原先在
 * {@code toAlbumView} 里对每个相册各查一次照片（只为算 photoCount + 封面）。
 * 修复后用一条 {@code album_id IN (...) + ORDER BY sort_order, create_time} 的批量
 * 查询在内存分组。本测试证明分组结果与旧的逐相册计算逐一相同：
 * 相同的 photoCount、相同的「封面 = 命中 coverPhotoId 否则取第一张」规则、
 * 空相册为 0 张且无封面。
 */
@DisplayName("PhotoAlbumServiceImpl（列表页批量照片加载）")
class PhotoAlbumServiceImplBatchTest {

    private static final LocalDateTime T = LocalDateTime.of(2026, 10, 1, 10, 0);

    private PhotoAlbumMapper albumMapper;
    private AlbumPhotoMapper photoMapper;
    private PhotoAlbumServiceImpl service;

    @BeforeAll
    static void initLambdaCache() {
        // 与 PhotoAlbumServiceImplCaptionTest 相同：LambdaQueryWrapper 的列名解析需要
        // MyBatis-Plus TableInfo lambda 缓存，纯 Mockito 单测里要手工初始化。
        MybatisConfiguration configuration = new MybatisConfiguration();
        GlobalConfigUtils.getGlobalConfig(configuration);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "album-batch-test");
        TableInfoHelper.initTableInfo(assistant, AlbumPhoto.class);
        TableInfoHelper.initTableInfo(assistant, PhotoAlbum.class);
    }

    @BeforeEach
    void setUp() {
        albumMapper = mock(PhotoAlbumMapper.class);
        photoMapper = mock(AlbumPhotoMapper.class);
        service = new PhotoAlbumServiceImpl(albumMapper, photoMapper, new BCryptPasswordEncoder());
    }

    private static PhotoAlbum album(long id, Long coverPhotoId, int sortOrder, int isPrivate) {
        PhotoAlbum album = new PhotoAlbum();
        album.setId(id);
        album.setTitle("相册" + id);
        album.setIsPrivate(isPrivate);
        album.setCoverPhotoId(coverPhotoId);
        album.setSortOrder(sortOrder);
        album.setCreateTime(T);
        album.setIsDeleted(0);
        return album;
    }

    private static AlbumPhoto photo(long id, long albumId, int sortOrder) {
        AlbumPhoto photo = new AlbumPhoto();
        photo.setId(id);
        photo.setAlbumId(albumId);
        photo.setFilePath("x.webp");
        photo.setMimeType("image/webp");
        photo.setSortOrder(sortOrder);
        photo.setCreateTime(T);
        photo.setIsDeleted(0);
        return photo;
    }

    @Test
    @DisplayName("三个相册只发一次照片查询，且 photoCount/封面与逐相册计算完全一致")
    void oneQueryForAllAlbumsWithIdenticalCountsAndCovers() {
        PhotoAlbum covered = album(1L, 3L, 0, 0);      // 封面指向非首张照片
        PhotoAlbum firstWins = album(2L, null, 1, 0);  // 无封面配置 → 取第一张
        PhotoAlbum empty = album(3L, null, 2, 0);      // 空相册
        when(albumMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(covered, firstWins, empty)));

        // 批量查询会按 ORDER BY sort_order, create_time 返回；跨相册交错也无妨。
        // 相册1：p1(sort0) p2(sort1) p3(sort2)；相册2：p5(sort3) p4(sort4)。
        when(photoMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(
                photo(1L, 1L, 0), photo(2L, 1L, 1), photo(3L, 1L, 2),
                photo(5L, 2L, 3), photo(4L, 2L, 4))));

        List<PhotoAlbumVO> albums = service.getPublicAlbums();

        assertEquals(3, albums.size());
        PhotoAlbumVO first = albums.get(0);
        assertEquals(3, first.getPhotoCount(), "photoCount 必须等于该相册照片数");
        assertEquals("/api/gallery/photos/3/image", first.getCoverUrl(),
                "封面必须是命中 coverPhotoId 的那张（p3），而不是第一张");
        assertEquals(List.of(), first.getPhotos(), "公开列表不内联照片明细");

        PhotoAlbumVO second = albums.get(1);
        assertEquals(2, second.getPhotoCount());
        assertEquals("/api/gallery/photos/5/image", second.getCoverUrl(),
                "未配置封面时取排序后的第一张（p5 在 p4 之前）");

        PhotoAlbumVO third = albums.get(2);
        assertEquals(0, third.getPhotoCount(), "空相册照片数为 0");
        assertNull(third.getCoverUrl(), "空相册没有封面");

        verify(photoMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("管理端列表同样只查一次，且照片明细按相册分组返回")
    void adminAlbumsShareTheSameBatchedLoad() {
        PhotoAlbum pub = album(1L, 1L, 0, 0);
        PhotoAlbum secret = album(2L, 2L, 1, 1);
        when(albumMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(pub, secret)));
        when(photoMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(
                photo(1L, 1L, 0), photo(9L, 2L, 0))));

        List<PhotoAlbumVO> albums = service.getAdminAlbums();

        assertEquals(2, albums.size());
        assertEquals(1, albums.get(0).getPhotoCount());
        assertEquals("/api/gallery/photos/1/image", albums.get(0).getCoverUrl());
        assertEquals(1, albums.get(0).getPhotos().size());
        assertEquals(1L, albums.get(0).getPhotos().get(0).getId());

        assertTrue(albums.get(1).isPrivateAlbum());
        assertEquals(1, albums.get(1).getPhotoCount());
        // 管理端 includePhotos=true：照片明细按相册分组，互不串册。
        assertEquals(1, albums.get(1).getPhotos().size());
        assertEquals(9L, albums.get(1).getPhotos().get(0).getId());
        assertTrue(albums.get(1).getCoverUrl().startsWith("/api/gallery/photos/9/image"),
                "管理端封面规则不变：未命中 coverPhotoId 时取排序后第一张；私密相册会带访问令牌参数");
        verify(photoMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("没有任何相册时不再发起照片查询")
    void noAlbumsMeansNoPhotoQuery() {
        when(albumMapper.selectList(any())).thenReturn(new ArrayList<>());

        List<PhotoAlbumVO> albums = service.getPublicAlbums();

        assertTrue(albums.isEmpty());
        verify(photoMapper, never()).selectList(any());
    }
}
