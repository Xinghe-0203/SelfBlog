package com.selfblog.service.site.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.site.AlbumForm;
import com.selfblog.entity.AlbumPhoto;
import com.selfblog.entity.PhotoAlbum;
import com.selfblog.mapper.AlbumPhotoMapper;
import com.selfblog.mapper.PhotoAlbumMapper;
import com.selfblog.service.site.PhotoAlbumService;
import com.selfblog.utils.ImageDimensionUtils;
import com.selfblog.vo.site.AlbumAccessVO;
import com.selfblog.vo.site.AlbumImage;
import com.selfblog.vo.site.AlbumPhotoVO;
import com.selfblog.vo.site.PhotoAlbumVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class PhotoAlbumServiceImpl implements PhotoAlbumService {
    private static final long MAX_IMAGE_BYTES = 15L * 1024 * 1024;
    private final PhotoAlbumMapper albumMapper;
    private final AlbumPhotoMapper photoMapper;
    private final PasswordEncoder passwordEncoder;
    private final Cache<String, Long> accessGrants = Caffeine.newBuilder()
            .maximumSize(20000).expireAfterWrite(2, TimeUnit.HOURS).build();

    @Value("${upload.gallery-path:gallery-media}")
    private String galleryPath;

    public PhotoAlbumServiceImpl(PhotoAlbumMapper albumMapper, AlbumPhotoMapper photoMapper, PasswordEncoder passwordEncoder) {
        this.albumMapper = albumMapper;
        this.photoMapper = photoMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoAlbumVO> getPublicAlbums() {
        LambdaQueryWrapper<PhotoAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PhotoAlbum::getIsDeleted, 0).orderByAsc(PhotoAlbum::getSortOrder).orderByDesc(PhotoAlbum::getCreateTime);
        return albumMapper.selectList(wrapper).stream().map(album -> toAlbumView(album, null, false)).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoAlbumVO> getAdminAlbums() {
        LambdaQueryWrapper<PhotoAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PhotoAlbum::getIsDeleted, 0).orderByAsc(PhotoAlbum::getSortOrder).orderByDesc(PhotoAlbum::getCreateTime);
        return albumMapper.selectList(wrapper).stream().map(album -> {
            String token = null;
            if (isPrivate(album)) {
                token = UUID.randomUUID().toString().replace("-", "");
                accessGrants.put(token, album.getId());
            }
            return toAlbumView(album, token, true);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PhotoAlbumVO getPublicAlbum(Long albumId, String accessToken) {
        PhotoAlbum album = requireAlbum(albumId);
        boolean unlocked = !isPrivate(album) || isGrantValid(accessToken, albumId);
        return toAlbumView(album, unlocked ? accessToken : null, unlocked);
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumAccessVO unlock(Long albumId, String password) {
        PhotoAlbum album = requireAlbum(albumId);
        if (!isPrivate(album)) throw new BusinessException(400, "这是公开相册，无需密码");
        if (password == null || album.getPasswordHash() == null || !passwordEncoder.matches(password, album.getPasswordHash())) {
            throw new BusinessException(403, "相册密码不正确");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        accessGrants.put(token, albumId);
        return new AlbumAccessVO(token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAlbum(AlbumForm form) {
        PhotoAlbum album = new PhotoAlbum();
        applyForm(album, form, true);
        album.setIsDeleted(0);
        albumMapper.insert(album);
        return album.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbum(Long albumId, AlbumForm form) {
        PhotoAlbum album = requireAlbum(albumId);
        applyForm(album, form, false);
        albumMapper.updateById(album);
        accessGrants.asMap().entrySet().removeIf(entry -> entry.getValue().equals(albumId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Long albumId) {
        PhotoAlbum album = requireAlbum(albumId);
        List<AlbumPhoto> photos = getPhotos(albumId);
        for (AlbumPhoto photo : photos) deleteStoredFile(photo.getFilePath());
        photoMapper.delete(new LambdaQueryWrapper<AlbumPhoto>().eq(AlbumPhoto::getAlbumId, albumId));
        albumMapper.deleteById(album.getId());
        accessGrants.asMap().entrySet().removeIf(entry -> entry.getValue().equals(albumId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PhotoAlbumVO> uploadPhotos(Long albumId, MultipartFile[] files) {
        PhotoAlbum album = requireAlbum(albumId);
        if (files == null || files.length == 0 || files.length > 30) throw new BusinessException(400, "请选择1-30张图片");
        List<AlbumPhoto> current = getPhotos(albumId);
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            if (file.getSize() > MAX_IMAGE_BYTES) throw new BusinessException(400, "单张图片不能超过15MB");
            String mimeType = detectImageType(file);
            Path target = storeImage(file, mimeType);
            AlbumPhoto photo = new AlbumPhoto();
            photo.setAlbumId(albumId);
            photo.setFilePath(target.toString());
            photo.setMimeType(mimeType);
            photo.setSortOrder(current.size());
            photo.setIsDeleted(0);
            try {
                photoMapper.insert(photo);
                current.add(photo);
            } catch (Exception e) {
                deleteStoredFile(target.toString());
                throw e;
            }
            if (album.getCoverPhotoId() == null) album.setCoverPhotoId(photo.getId());
        }
        albumMapper.updateById(album);
        return List.of(toAlbumView(album, null, true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePhoto(Long albumId, Long photoId) {
        PhotoAlbum album = requireAlbum(albumId);
        AlbumPhoto photo = photoMapper.selectById(photoId);
        if (photo == null || !albumId.equals(photo.getAlbumId())) throw new BusinessException(404, "照片不存在");
        photoMapper.deleteById(photoId);
        deleteStoredFile(photo.getFilePath());
        if (photoId.equals(album.getCoverPhotoId())) {
            List<AlbumPhoto> remaining = getPhotos(albumId);
            album.setCoverPhotoId(remaining.isEmpty() ? null : remaining.get(0).getId());
            albumMapper.updateById(album);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePhotoCaption(Long albumId, Long photoId, String caption) {
        requireAlbum(albumId);
        AlbumPhoto photo = photoMapper.selectById(photoId);
        if (photo == null || !albumId.equals(photo.getAlbumId())) throw new BusinessException(404, "照片不存在");
        String normalized = caption == null ? null : caption.trim();
        if (normalized != null && normalized.isEmpty()) normalized = null;
        // 必须用 LambdaUpdateWrapper 而不是 photo.setCaption(x); updateById(photo)：
        // 全局更新策略是 NOT_NULL，setCaption(null) 会让 caption 整列从 SET 子句剔除，
        // 于是「清除图注」静默失效（与本仓库 post_seo 的同一类陷阱）。
        photoMapper.update(null, new LambdaUpdateWrapper<AlbumPhoto>()
                .eq(AlbumPhoto::getId, photoId)
                .set(AlbumPhoto::getCaption, normalized));
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumImage getImage(Long photoId, String accessToken) {
        AlbumPhoto photo = photoMapper.selectById(photoId);
        if (photo == null) throw new BusinessException(404, "照片不存在");
        PhotoAlbum album = requireAlbum(photo.getAlbumId());
        if (isPrivate(album) && !isGrantValid(accessToken, album.getId())) throw new BusinessException(403, "请输入相册密码后查看");
        Path root = resolveGalleryRoot();
        Path image = Paths.get(photo.getFilePath()).toAbsolutePath().normalize();
        if (!image.startsWith(root) || !Files.isRegularFile(image)) throw new BusinessException(404, "照片文件不存在");
        return new AlbumImage(image, photo.getMimeType());
    }

    private PhotoAlbumVO toAlbumView(PhotoAlbum album, String accessToken, boolean includePhotos) {
        List<AlbumPhoto> photoList = getPhotos(album.getId());
        boolean privateAlbum = isPrivate(album);
        PhotoAlbumVO vo = new PhotoAlbumVO();
        vo.setId(album.getId());
        vo.setTitle(album.getTitle());
        vo.setDescription(album.getDescription());
        vo.setPrivateAlbum(privateAlbum);
        vo.setLocked(privateAlbum && !includePhotos);
        vo.setPhotoCount(photoList.size());
        vo.setSortOrder(album.getSortOrder());
        vo.setCreateTime(album.getCreateTime());
        if (!privateAlbum || includePhotos) {
            AlbumPhoto cover = photoList.stream().filter(photo -> photo.getId().equals(album.getCoverPhotoId())).findFirst()
                    .orElse(photoList.isEmpty() ? null : photoList.get(0));
            if (cover != null) vo.setCoverUrl(imageUrl(cover.getId(), privateAlbum ? accessToken : null));
        }
        if (includePhotos) {
            vo.setPhotos(photoList.stream().map(photo -> toPhotoView(photo, privateAlbum ? accessToken : null)).collect(Collectors.toList()));
        } else {
            vo.setPhotos(List.of());
        }
        return vo;
    }

    private AlbumPhotoVO toPhotoView(AlbumPhoto photo, String accessToken) {
        AlbumPhotoVO vo = new AlbumPhotoVO();
        vo.setId(photo.getId());
        vo.setImageUrl(imageUrl(photo.getId(), accessToken));
        vo.setCaption(photo.getCaption());
        vo.setSortOrder(photo.getSortOrder());
        return vo;
    }

    private String imageUrl(Long photoId, String token) {
        String url = "/api/gallery/photos/" + photoId + "/image";
        return token == null || token.isBlank() ? url : url + "?token=" + token;
    }

    private void applyForm(PhotoAlbum album, AlbumForm form, boolean creating) {
        if (form.getTitle() == null || form.getTitle().trim().isBlank()) throw new BusinessException(400, "相册名称不能为空");
        boolean privateAlbum = Boolean.TRUE.equals(form.getPrivateAlbum());
        String password = form.getPassword() == null ? "" : form.getPassword().trim();
        if (privateAlbum && (password.length() < 6 && (creating || album.getPasswordHash() == null))) {
            throw new BusinessException(400, "密码相册至少需要6位密码");
        }
        if (!privateAlbum) album.setPasswordHash(null);
        else if (!password.isBlank()) album.setPasswordHash(passwordEncoder.encode(password));
        else if (creating) throw new BusinessException(400, "请设置密码相册的访问密码");
        album.setTitle(form.getTitle().trim());
        album.setDescription(form.getDescription() == null ? null : form.getDescription().trim());
        album.setIsPrivate(privateAlbum ? 1 : 0);
        album.setSortOrder(form.getSortOrder() == null ? 0 : form.getSortOrder());
    }

    private PhotoAlbum requireAlbum(Long id) {
        PhotoAlbum album = albumMapper.selectById(id);
        if (album == null || (album.getIsDeleted() != null && album.getIsDeleted() == 1)) throw new BusinessException(404, "相册不存在");
        return album;
    }

    private List<AlbumPhoto> getPhotos(Long albumId) {
        LambdaQueryWrapper<AlbumPhoto> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumPhoto::getAlbumId, albumId).eq(AlbumPhoto::getIsDeleted, 0)
                .orderByAsc(AlbumPhoto::getSortOrder).orderByAsc(AlbumPhoto::getCreateTime);
        return photoMapper.selectList(wrapper);
    }

    private boolean isPrivate(PhotoAlbum album) { return album.getIsPrivate() != null && album.getIsPrivate() == 1; }

    private boolean isGrantValid(String token, Long albumId) {
        return token != null && albumId.equals(accessGrants.getIfPresent(token));
    }

    private String detectImageType(MultipartFile file) {
        String mimeType = sniffMagicBytes(file);
        // S5：相册照片只存原始字节、从不解码，因此必须在落盘前按文件头拦截声明尺寸
        // 超大的解压缩炸弹（否则它会被原样存下并在后续请求里被原样送出）。
        try (var input = file.getInputStream()) {
            ImageDimensionUtils.validatePixelCount(input, file.getOriginalFilename());
        } catch (IOException e) {
            throw new BusinessException(400, "图片读取失败");
        }
        return mimeType;
    }

    private String sniffMagicBytes(MultipartFile file) {
        try {
            byte[] header;
            try (var input = file.getInputStream()) {
                header = input.readNBytes(12);
            }
            if (header.length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff) return "image/jpeg";
            if (header.length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47) return "image/png";
            if (header.length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F') return "image/gif";
            if (header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return "image/webp";
        } catch (IOException e) {
            throw new BusinessException(400, "图片读取失败");
        }
        throw new BusinessException(400, "仅支持真实的 jpg、png、gif 或 webp 图片");
    }

    private Path storeImage(MultipartFile file, String mimeType) {
        try {
            Path root = resolveGalleryRoot();
            Files.createDirectories(root);
            String extension = switch (mimeType) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                default -> ".webp";
            };
            Path target = root.resolve(UUID.randomUUID().toString().replace("-", "") + extension).normalize();
            if (!target.startsWith(root)) throw new BusinessException(400, "文件路径无效");
            try (var input = file.getInputStream()) {
                Files.copy(input, target);
            }
            return target;
        } catch (IOException e) {
            throw new BusinessException(500, "照片保存失败");
        }
    }

    private Path resolveGalleryRoot() {
        Path root = Paths.get(galleryPath);
        if (!root.isAbsolute()) root = Paths.get(System.getProperty("user.dir")).resolve(root);
        return root.toAbsolutePath().normalize();
    }

    private void deleteStoredFile(String path) {
        if (path == null) return;
        try {
            Path file = Paths.get(path).toAbsolutePath().normalize();
            if (file.startsWith(resolveGalleryRoot())) Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Keep database operations predictable; orphan cleanup can be done by the owner.
        }
    }
}
