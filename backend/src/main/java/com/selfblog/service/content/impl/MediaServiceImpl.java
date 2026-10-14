package com.selfblog.service.content.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.social.MediaQueryRequest;
import com.selfblog.entity.BlogPostMedia;
import com.selfblog.entity.Media;
import com.selfblog.mapper.BlogPostMediaMapper;
import com.selfblog.mapper.MediaMapper;
import com.selfblog.service.post.BlogPostService;
import com.selfblog.service.content.MediaService;
import com.selfblog.utils.SecurityUtils;
import com.selfblog.vo.content.MediaVO;
import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MediaServiceImpl extends ServiceImpl<MediaMapper, Media> implements MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaServiceImpl.class);

    @Autowired
    private BlogPostMediaMapper blogPostMediaMapper;

    @Autowired
    private BlogPostService blogPostService;

    @Value("${upload.base-path:./uploads}")
    private String uploadPath;

    @Value("${upload.url-prefix:/uploads}")
    private String urlPrefix;

    @PostConstruct
    public void init() {
        File dir = new File(uploadPath);
        if (!dir.isAbsolute()) {
            uploadPath = new File(System.getProperty("user.dir"), uploadPath).getAbsolutePath();
        }
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        log.info("[Media] 上传目录: {}", uploadPath);
    }

    @Value("${upload.image.max-width:1920}")
    private int maxImageWidth;

    @Value("${upload.image.quality:0.85}")
    private double imageQuality;

    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    ));

    private static final Set<String> ALLOWED_VIDEO_TYPES = new HashSet<>(Arrays.asList(
            "video/mp4", "video/webm"
    ));

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".mp4", ".webm"
    ));

    private static final long IMAGE_MAX_SIZE = 10 * 1024 * 1024;
    private static final long VIDEO_MAX_SIZE = 500 * 1024 * 1024;

    private static final byte[] MAGIC_JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] MAGIC_GIF = new byte[]{0x47, 0x49, 0x46, 0x38};
    private static final byte[] MAGIC_WEBP_RIFF = new byte[]{0x52, 0x49, 0x46, 0x46};
    private static final byte[] MAGIC_MP4_FTYP = new byte[]{0x66, 0x74, 0x79, 0x70};
    private static final byte[] MAGIC_WEBM = new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};

    private void validateMagicNumber(MultipartFile file, boolean isImage) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[12];
            int headerLen = inputStream.read(header);
            if (headerLen < 12) {
                throw new BusinessException(400, "文件太小，无法读取文件头");
            }

            if (isImage) {
                if (matchesMagic(header, MAGIC_JPEG) || matchesMagic(header, MAGIC_PNG)
                        || matchesMagic(header, MAGIC_GIF) || matchesWebp(header)) {
                    return;
                }
                throw new BusinessException(400, "图片文件格式无效，请上传真实的 jpg、png、gif 或 webp 图片");
            } else {
                if (matchesMp4(header) || matchesWebm(header)) {
                    return;
                }
                throw new BusinessException(400, "视频文件格式无效，请上传真实的 mp4 或 webm 视频");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(400, "文件读取失败，无法校验文件类型: " + e.getMessage());
        }
    }

    private boolean matchesMagic(byte[] header, byte[] magic) {
        if (header.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesWebp(byte[] header) {
        if (!matchesMagic(header, MAGIC_WEBP_RIFF)) {
            return false;
        }
        return header.length >= 12
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
    }

    private boolean matchesWebm(byte[] header) {
        return matchesMagic(header, MAGIC_WEBM);
    }

    private boolean matchesMp4(byte[] header) {
        if (header.length < 12) {
            return false;
        }
        return header[4] == (byte) 0x66 && header[5] == (byte) 0x74 &&
               header[6] == (byte) 0x79 && header[7] == (byte) 0x70;
    }

    private File processImage(File inputFile, File outputFile) throws IOException {
        log.info("[Media] 处理图片: {} -> {}", inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
        log.info("[Media] 输入文件存在: {}, 大小: {}", inputFile.exists(), inputFile.length());

        BufferedImage image = ImageIO.read(inputFile);
        if (image == null) {
            log.error("[Media] ImageIO.read 返回 null，文件可能不是有效图片: {}", inputFile.getName());
            throw new BusinessException(400, "无法读取图片文件，请上传 jpg、png、gif 或 webp 格式的图片");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        log.info("[Media] 图片尺寸: {}x{}", width, height);

        if (width > maxImageWidth) {
            double ratio = (double) maxImageWidth / width;
            int newHeight = (int) (height * ratio);
            Thumbnails.of(inputFile)
                    .width(maxImageWidth)
                    .height(newHeight)
                    .outputFormat("jpg")
                    .outputQuality(imageQuality)
                    .toFile(outputFile);
        } else {
            Thumbnails.of(inputFile)
                    .scale(1.0)
                    .outputFormat("jpg")
                    .outputQuality(imageQuality)
                    .toFile(outputFile);
        }

        return outputFile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MediaVO uploadMedia(MultipartFile file, Long userId, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        if (type == null) {
            throw new BusinessException(400, "媒体类型不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFilename.substring(dotIndex).toLowerCase();
            }
            originalFilename = originalFilename.replaceAll("[/\\\\]", "_");
        }
        String contentType = file.getContentType();
        boolean isImage = contentType != null && ALLOWED_IMAGE_TYPES.contains(contentType);
        boolean isVideo = contentType != null && ALLOWED_VIDEO_TYPES.contains(contentType);

        switch (type) {
            case "avatar":
                if (!isImage) {
                    throw new BusinessException(400, "头像仅支持 jpg、png、gif、webp 格式的图片");
                }
                validateMagicNumber(file, true);
                if (file.getSize() > IMAGE_MAX_SIZE) {
                    throw new BusinessException(400, "头像图片大小不能超过10MB");
                }
                break;
            case "circle":
                if (!isImage && !isVideo) {
                    throw new BusinessException(400, "校友圈支持 jpg、png、gif、webp 图片和 mp4、webm 视频");
                }
                validateMagicNumber(file, isImage);
                if (isVideo && file.getSize() > VIDEO_MAX_SIZE) {
                    throw new BusinessException(400, "视频大小不能超过500MB");
                }
                if (isImage && file.getSize() > IMAGE_MAX_SIZE) {
                    throw new BusinessException(400, "图片大小不能超过10MB");
                }
                break;
            case "cover":
                if (!isImage) {
                    throw new BusinessException(400, "封面图仅支持 jpg、png、gif、webp 格式的图片");
                }
                validateMagicNumber(file, true);
                if (file.getSize() > IMAGE_MAX_SIZE) {
                    throw new BusinessException(400, "封面图大小不能超过10MB");
                }
                break;
            case "article":
            default:
                if (!isImage && !isVideo) {
                    throw new BusinessException(400, "文章支持 jpg、png、gif、webp 图片和 mp4、webm 视频");
                }
                validateMagicNumber(file, isImage);
                if (file.getSize() > VIDEO_MAX_SIZE) {
                    throw new BusinessException(400, "文件大小不能超过500MB");
                }
                break;
        }

        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File uploadDir = new File(uploadPath, datePath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // extension was already extracted before filename sanitization (line ~200)
        // fallback: derive from contentType
        if (extension.isEmpty()) {
            if (isImage) extension = ".jpg";
            else if (contentType != null && contentType.contains("mp4")) extension = ".mp4";
            else if (contentType != null && contentType.contains("webm")) extension = ".webm";
            else extension = ".jpg";
        }

        // 视频文件扩展名校验
        if (isVideo && !ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "视频文件扩展名无效，仅支持 .mp4、.webm 格式");
        }

        File destFile;
        long fileSize;
        String newFilename;

        if (isImage) {
            newFilename = UUID.randomUUID().toString().replace("-", "") + ".jpg";
            File tempFile = new File(uploadDir, UUID.randomUUID().toString().replace("-", "") + "_temp" + extension);
            destFile = new File(uploadDir, newFilename);

            try {
                file.transferTo(tempFile);
                processImage(tempFile, destFile);
                fileSize = destFile.length();
            } catch (IOException e) {
                log.error("[Media] 图片处理失败: {}", e.getMessage(), e);
                throw new BusinessException(500, "图片处理失败: " + e.getMessage());
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        } else {
            newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
            destFile = new File(uploadDir, newFilename);
            try {
                file.transferTo(destFile);
            } catch (IOException e) {
                log.error("[Media] 文件保存失败: {}", e.getMessage(), e);
                throw new BusinessException(500, "文件保存失败: " + e.getMessage());
            }
            fileSize = file.getSize();
        }

        Integer width = null;
        Integer height = null;

        if (isImage) {
            try {
                BufferedImage image = ImageIO.read(destFile);
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (IOException e) {
                log.warn("图片尺寸获取失败: filePath={}, error={}", destFile.getAbsolutePath(), e.getMessage(), e);
            }
        }

        String fileUrl = urlPrefix + "/" + datePath + "/" + newFilename;

        Media media = new Media();
        media.setUserId(userId);
        media.setOriginalName(originalFilename);
        media.setFilePath(destFile.getAbsolutePath());
        media.setFileUrl(fileUrl);
        media.setThumbUrl(fileUrl);
        media.setFileSize(fileSize);
        media.setFileType(isImage ? "image" : (isVideo ? "video" : "file"));
        media.setMimeType(contentType != null ? contentType : "application/octet-stream");
        media.setWidth(width);
        media.setHeight(height);
        media.setStatus(1);

        this.save(media);

        MediaVO vo = new MediaVO();
        vo.setId(media.getId());
        vo.setFileUrl(fileUrl);
        vo.setThumbUrl(media.getThumbUrl());
        vo.setFileSize(fileSize);
        vo.setWidth(width);
        vo.setHeight(height);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMedia(Long mediaId, Long userId) {
        Media media = this.getById(mediaId);
        if (media == null) {
            throw new BusinessException(404, "媒体文件不存在");
        }

        if (!Objects.equals(media.getUserId(), userId) && !SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "无权删除此媒体文件");
        }

        File file = new File(media.getFilePath());
        if (file.exists()) {
            file.delete();
        }

        this.removeById(mediaId);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaVO getMediaInfo(Long mediaId) {
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();

        Media media = this.getById(mediaId);
        if (media == null) {
            throw new BusinessException(404, "媒体文件不存在");
        }

        if (currentUserId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!Objects.equals(media.getUserId(), currentUserId) && !SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "无权查看此媒体文件");
        }

        MediaVO vo = new MediaVO();
        vo.setId(media.getId());
        vo.setFileUrl(media.getFileUrl());
        vo.setThumbUrl(media.getThumbUrl());
        vo.setFileSize(media.getFileSize());
        vo.setWidth(media.getWidth());
        vo.setHeight(media.getHeight());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindPostMedia(Long postId, List<Long> mediaIds) {
        if (postId == null) {
            throw new BusinessException(400, "文章ID不能为空");
        }

        com.selfblog.entity.BlogPost post = blogPostService.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!Objects.equals(post.getUserId(), currentUserId) && !SecurityUtils.isCurrentUserAdmin()) {
            throw new BusinessException(403, "无权操作此文章的媒体");
        }

        if (mediaIds == null || mediaIds.isEmpty()) {
            blogPostMediaMapper.deleteByPostId(postId);
            return;
        }

        List<Media> mediaList = this.listByIds(mediaIds);
        for (Media media : mediaList) {
            if (!Objects.equals(media.getUserId(), currentUserId) && !SecurityUtils.isCurrentUserAdmin()) {
                throw new BusinessException(403, "无权绑定他人的媒体文件");
            }
        }

        blogPostMediaMapper.deleteByPostId(postId);
        blogPostMediaMapper.batchInsert(postId, mediaIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaVO> getPostMedia(Long postId) {
        if (postId == null) {
            throw new BusinessException(400, "文章ID不能为空");
        }

        com.selfblog.entity.BlogPost post = blogPostService.getById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }
        if (post.getStatus() == null || post.getStatus() != 1) {
            throw new BusinessException(403, "文章未发布或已下架");
        }

        List<BlogPostMedia> postMediaList = blogPostMediaMapper.selectByPostId(postId);
        List<Long> mediaIds = postMediaList.stream()
                .map(BlogPostMedia::getMediaId)
                .collect(Collectors.toList());
        List<Media> mediaList = mediaIds.isEmpty() ? Collections.emptyList() : this.listByIds(mediaIds);

        return mediaList.stream().map(media -> {
            MediaVO vo = new MediaVO();
            vo.setId(media.getId());
            vo.setFileUrl(media.getFileUrl());
            vo.setThumbUrl(media.getThumbUrl());
            vo.setFileSize(media.getFileSize());
            vo.setWidth(media.getWidth());
            vo.setHeight(media.getHeight());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MediaVO> uploadFiles(MultipartFile[] files, Long userId, String type) {
        if (files == null || files.length == 0) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        if (files.length > 9) {
            throw new BusinessException(400, "最多只能上传9个文件");
        }

        List<MediaVO> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                MediaVO mediaVO = uploadMedia(file, userId, type);
                result.add(mediaVO);
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaVO> getUserMedia(MediaQueryRequest request, Long userId) {
        Integer pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        Integer pageSize = request.getPageSize() != null ? request.getPageSize() : 10;

        Page<Media> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Media> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Media::getUserId, userId);

        if (StrUtil.isNotBlank(request.getFileType())) {
            wrapper.likeRight(Media::getMimeType, request.getFileType() + "/");
        }

        Page<Media> resultPage = this.page(page, wrapper);

        return resultPage.getRecords().stream().map(media -> {
            MediaVO vo = new MediaVO();
            vo.setId(media.getId());
            vo.setFileUrl(media.getFileUrl());
            vo.setThumbUrl(media.getThumbUrl());
            vo.setFileSize(media.getFileSize());
            vo.setWidth(media.getWidth());
            vo.setHeight(media.getHeight());
            return vo;
        }).collect(Collectors.toList());
    }
}