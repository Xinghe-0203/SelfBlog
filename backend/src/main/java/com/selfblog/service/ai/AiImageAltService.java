package com.selfblog.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.entity.AlbumPhoto;
import com.selfblog.entity.BlogPost;
import com.selfblog.mapper.AlbumPhotoMapper;
import com.selfblog.mapper.BlogPostMapper;
import com.selfblog.service.seo.LlmUpstreamException;
import com.selfblog.service.seo.OpenAiCompatibleLlmClient;
import com.selfblog.service.seo.OpenAiCompatibleLlmClient.ModelInfo;
import com.selfblog.service.seo.SeoLlmConfig;
import com.selfblog.vo.ai.AiImageAltVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * E 图片描述：给封面图或相册照片生成 alt 文本与图注。
 *
 * <p>只返回建议值，<b>不写库</b>——管理员在编辑页确认后自己保存。</p>
 *
 * <p>模型是否支持图像输入的处理遵循契约 §6 与 Lead 的裁定：</p>
 * <ul>
 *   <li>{@code listModels} 成功且当前模型明确 {@code supportsImage == false} → 直接 502，
 *       不白花一次上游调用（fail-closed）；</li>
 *   <li>{@code listModels} 失败（端点没实现 {@code /models}、网络抖动）→ 视为「未知支持」，
 *       继续调 {@code completeVision} 让上游自己决定（fail-open）；此时上游以 400/422
 *       拒绝，翻译成同一句「不支持图像输入」的文案。</li>
 * </ul>
 *
 * <p>刻意不加 {@code @Transactional}：读图 + 模型调用都是网络/文件 IO，不能占数据库连接。</p>
 */
@Slf4j
@Service
public class AiImageAltService {

    static final String SOURCE_COVER = "cover";
    static final String SOURCE_ALBUM = "album";

    static final int ALT_MAX = 120;
    static final int CAPTION_MAX = 80;

    static final String NOT_SUPPORTED_MESSAGE =
            "当前模型不支持图像输入，请改用 deepseek-flash 等支持视觉的模型";
    static final String NO_COVER_MESSAGE = "这篇文章还没有封面图，请先设置封面";
    static final String POST_MISSING_MESSAGE = "文章不存在";
    static final String PHOTO_MISSING_MESSAGE = "照片不存在";
    static final String SOURCE_INVALID_MESSAGE = "图片来源只能是 cover 或 album";

    private final AiAvailability availability;
    private final AiModelCaller caller;
    private final AiJson json;
    private final AiPromptBuilder prompts;
    private final AiImageLoader imageLoader;
    private final OpenAiCompatibleLlmClient client;
    private final BlogPostMapper postMapper;
    private final AlbumPhotoMapper albumPhotoMapper;

    public AiImageAltService(AiAvailability availability, AiModelCaller caller, AiJson json,
                             AiPromptBuilder prompts, AiImageLoader imageLoader,
                             OpenAiCompatibleLlmClient client, BlogPostMapper postMapper,
                             AlbumPhotoMapper albumPhotoMapper) {
        this.availability = availability;
        this.caller = caller;
        this.json = json;
        this.prompts = prompts;
        this.imageLoader = imageLoader;
        this.client = client;
        this.postMapper = postMapper;
        this.albumPhotoMapper = albumPhotoMapper;
    }

    /** 按来源取图并生成描述。 */
    public AiImageAltVO describe(String source, Long postId, Long photoId) {
        SeoLlmConfig config = availability.requireUsable();
        String normalized = normalizeSource(source);
        String imagePath;
        String hint = "";
        if (SOURCE_ALBUM.equals(normalized)) {
            AlbumPhoto photo = requirePhoto(photoId);
            imagePath = photo.getFilePath();
            hint = photo.getCaption() == null ? "" : photo.getCaption();
        } else {
            BlogPost post = requirePost(postId);
            imagePath = post.getCoverUrl();
            hint = post.getTitle() == null ? "" : post.getTitle();
            if (imagePath == null || imagePath.isBlank()) {
                throw new BusinessException(400, NO_COVER_MESSAGE);
            }
        }
        // 先做「模型是否支持图像」的判定，再读图：不支持时不必浪费一次文件读取
        requireVisionSupport(config);
        String dataUrl = imageLoader.toDataUrl(imagePath);
        long started = System.nanoTime();
        AiImageAltVO vo = caller.generateVision(config, AiPromptBuilder.IMAGE_ALT_SYSTEM_PROMPT,
                prompts.imageAltUserPrompt(hint), dataUrl, this::parseAlt,
                AiImageAltService::translateVisionFailure);
        vo.setModel(config.model());
        vo.setLatencyMs(AiWritingService.elapsedMs(started));
        return vo;
    }

    /**
     * 模型明确不支持图像输入时直接拒绝；查不到模型清单时放行（fail-open）。
     */
    private void requireVisionSupport(SeoLlmConfig config) {
        List<ModelInfo> models;
        try {
            models = client.listModels(config);
        } catch (LlmUpstreamException e) {
            log.info("[AI] 无法获取模型清单，按「未知支持」继续尝试视觉调用：{}", e.getMessage());
            return;
        }
        String target = config.model() == null ? "" : config.model().trim();
        for (ModelInfo model : models) {
            if (model == null || model.id() == null || !model.id().equalsIgnoreCase(target)) {
                continue;
            }
            if (!model.supportsImage()) {
                throw new BusinessException(502, NOT_SUPPORTED_MESSAGE);
            }
            return;
        }
        // 清单里没有当前模型：同样视为未知支持，交给上游判定
        log.info("[AI] 模型清单里没有 {}，按「未知支持」继续尝试视觉调用", target);
    }

    private AiImageAltVO parseAlt(String raw) {
        JsonNode root = json.read(raw);
        String alt = AiJson.plainText(AiJson.text(root, "alt"));
        if (alt.isEmpty()) {
            throw new AiOutputException("缺少 alt 字段");
        }
        AiImageAltVO vo = new AiImageAltVO();
        vo.setAlt(AiJson.truncate(alt, ALT_MAX));
        String caption = AiJson.plainText(AiJson.text(root, "caption"));
        vo.setCaption(caption.isEmpty() ? null : AiJson.truncate(caption, CAPTION_MAX));
        return vo;
    }

    /**
     * 把上游「参数被拒」翻译成契约 §6 的固定文案。
     *
     * <p>{@code completeVision} 在 400/422 时会去掉 {@code response_format} 重试一次，
     * 因此「模型不支持图像」最终表现为 {@code 模型拒绝了这些参数…}。这里只拦参数类错误，
     * 鉴权/额度/超时等仍然原样透出，免得误导管理员去换模型。</p>
     */
    private static String translateVisionFailure(LlmUpstreamException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("拒绝了这些参数") || message.contains("拒绝了请求参数")
                || message.contains("接口不存在")) {
            return NOT_SUPPORTED_MESSAGE;
        }
        return message.isBlank() ? NOT_SUPPORTED_MESSAGE : message;
    }

    private BlogPost requirePost(Long postId) {
        if (postId == null) {
            throw new BusinessException(400, "请指定文章");
        }
        BlogPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, POST_MISSING_MESSAGE);
        }
        return post;
    }

    private AlbumPhoto requirePhoto(Long photoId) {
        if (photoId == null) {
            throw new BusinessException(400, "请指定照片");
        }
        AlbumPhoto photo = albumPhotoMapper.selectById(photoId);
        if (photo == null || (photo.getIsDeleted() != null && photo.getIsDeleted() != 0)) {
            throw new BusinessException(404, PHOTO_MISSING_MESSAGE);
        }
        if (photo.getFilePath() == null || photo.getFilePath().isBlank()) {
            throw new BusinessException(400, "这张照片没有可读取的文件");
        }
        return photo;
    }

    static String normalizeSource(String source) {
        String value = source == null ? "" : source.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return SOURCE_COVER;
        }
        if (!SOURCE_COVER.equals(value) && !SOURCE_ALBUM.equals(value)) {
            throw new BusinessException(400, SOURCE_INVALID_MESSAGE);
        }
        return value;
    }
}
