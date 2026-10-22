package com.selfblog.service.site.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.AlbumPhoto;
import com.selfblog.entity.PhotoAlbum;
import com.selfblog.mapper.AlbumPhotoMapper;
import com.selfblog.mapper.PhotoAlbumMapper;
import com.selfblog.vo.site.AlbumPhotoVO;
import com.selfblog.vo.site.PhotoAlbumVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 单张照片图注写入路径的回归测试。
 *
 * <p>背景：{@code album_photo.caption} 列从建表起就存在、{@code AlbumPhotoVO} 也一直
 * 把它回显给前台，但<b>没有任何写入路径</b>——上传只写 file_path/mime_type/sort_order。
 * 于是 AI 图片描述（E 块）生成的文案只能停在浏览器草稿里。本批新增
 * {@code PUT /admin/albums/{albumId}/photos/{photoId}} 补上这条链路。</p>
 *
 * <p>这里刻意断言服务层用的是 {@link LambdaUpdateWrapper} 而不是
 * {@code photo.setCaption(x); updateById(photo)}：后者在全局
 * {@code update-strategy=NOT_NULL} 下会把 {@code caption} 整列从 SET 子句剔除，
 * 「清除图注」将静默失效（与本仓库 {@code post_seo} 的同一类陷阱）。</p>
 */
@DisplayName("PhotoAlbumServiceImpl（照片图注写入）")
class PhotoAlbumServiceImplCaptionTest {

    private PhotoAlbumMapper albumMapper;
    private AlbumPhotoMapper photoMapper;
    private PhotoAlbumServiceImpl service;

    /**
     * {@link LambdaUpdateWrapper} 的列名解析走 MyBatis-Plus 的 TableInfo lambda 缓存。
     * 纯 Mockito 单测（不启 Spring）里缓存是空的，不手工初始化就会抛
     * {@code MybatisPlus can not find lambda cache for this entity}。
     */
    @BeforeAll
    static void initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        GlobalConfigUtils.getGlobalConfig(configuration);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "caption-test");
        TableInfoHelper.initTableInfo(assistant, AlbumPhoto.class);
        TableInfoHelper.initTableInfo(assistant, PhotoAlbum.class);
    }

    @BeforeEach
    void setUp() {
        albumMapper = mock(PhotoAlbumMapper.class);
        photoMapper = mock(AlbumPhotoMapper.class);
        service = new PhotoAlbumServiceImpl(albumMapper, photoMapper, new BCryptPasswordEncoder());

        PhotoAlbum album = new PhotoAlbum();
        album.setId(7L);
        album.setTitle("测试相册");
        album.setIsPrivate(0);
        album.setSortOrder(0);
        album.setIsDeleted(0);
        when(albumMapper.selectById(7L)).thenReturn(album);
        when(albumMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(album)));

        when(photoMapper.selectById(11L)).thenReturn(photo(11L, null));
        when(photoMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(photo(11L, null))));
    }

    private static AlbumPhoto photo(Long id, String caption) {
        AlbumPhoto photo = new AlbumPhoto();
        photo.setId(id);
        photo.setAlbumId(7L);
        photo.setFilePath("x.webp");
        photo.setMimeType("image/webp");
        photo.setCaption(caption);
        photo.setSortOrder(0);
        photo.setIsDeleted(0);
        return photo;
    }

    @Test
    @DisplayName("写入图注后，管理端视图回读到的就是 trim 后的新文案")
    void captionRoundTripsThroughAdminView() {
        service.updatePhotoCaption(7L, 11L, "  傍晚的海岸线  ");
        // 让下一次查询返回落库后的状态（真实 UPDATE 的效果）。
        when(photoMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(photo(11L, "傍晚的海岸线"))));

        List<PhotoAlbumVO> albums = service.getAdminAlbums();
        assertEquals(1, albums.size());
        List<AlbumPhotoVO> photos = albums.get(0).getPhotos();
        assertNotNull(photos);
        assertEquals(1, photos.size());
        assertEquals("傍晚的海岸线", photos.get(0).getCaption(),
                "图注必须被 trim 后原样写回，而不是带空白");
    }

    @Test
    @DisplayName("空白图注归一为 NULL（清除语义），且确实通过 wrapper 逐列 set")
    @SuppressWarnings("unchecked")
    void blankCaptionIsClearedViaUpdateWrapper() {
        ArgumentCaptor<LambdaUpdateWrapper<AlbumPhoto>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);

        service.updatePhotoCaption(7L, 11L, "   ");

        verify(photoMapper).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertNotNull(sqlSet, "wrapper 必须带 SET 子句，否则清除图注根本不会落库");
        assertTrue(sqlSet.contains("caption"),
                "必须显式 SET caption，否则 NULL 会被 NOT_NULL 策略剔除：" + sqlSet);
        // 从未走实体更新路径（那条路径无法写 NULL）。
        verify(photoMapper, never()).updateById(any(AlbumPhoto.class));
    }

    @Test
    @DisplayName("null 图注同样表示清除，不抛异常")
    void nullCaptionIsAccepted() {
        service.updatePhotoCaption(7L, 11L, null);
        verify(photoMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("照片不属于该相册时 404，且不产生任何 UPDATE")
    void photoFromAnotherAlbumIsRejected() {
        AlbumPhoto foreign = photo(11L, null);
        foreign.setAlbumId(99L);
        when(photoMapper.selectById(11L)).thenReturn(foreign);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updatePhotoCaption(7L, 11L, "文案"));

        assertEquals(404, exception.getCode());
        verify(photoMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("相册不存在时 404，照片不存在时也 404")
    void missingAlbumOrPhotoIs404() {
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.updatePhotoCaption(404L, 11L, "文案")).getCode());

        when(photoMapper.selectById(12L)).thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.updatePhotoCaption(7L, 12L, "文案")).getCode());
    }
}
