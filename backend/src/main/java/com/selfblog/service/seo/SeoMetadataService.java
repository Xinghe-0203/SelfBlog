package com.selfblog.service.seo;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.selfblog.common.exception.BusinessException;
import com.selfblog.dto.seo.SeoUpdateRequest;
import com.selfblog.entity.BlogPost;
import com.selfblog.entity.PostSeo;
import com.selfblog.mapper.PostSeoMapper;
import com.selfblog.vo.seo.SeoDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 手工维护 SEO 元数据（后台表单保存 / 清除）。
 *
 * <p>与 {@link SeoGenerationService} 分开：那条路径要跨越网络 IO，这条只有一次单行写，
 * 因此这里可以安全地用事务。两者共用 {@link SeoFallback} 与 {@link SeoAuditService}
 * 的判定，保证「手填」和「生成」落库语义一致。</p>
 */
@Slf4j
@Service
public class SeoMetadataService {

    /** 字段长度上限（与前端输入框 maxlength 对齐）。 */
    public static final int TITLE_MAX = 200;
    public static final int DESCRIPTION_MAX = 400;
    public static final int KEYWORDS_MAX = 300;
    public static final int OG_IMAGE_MAX = 500;

    private static final String SOURCE_MANUAL = "manual";
    /** 只允许站内相对路径（单个前导斜杠）或 http(s) 绝对地址。 */
    private static final Pattern HTTP_URL = Pattern.compile("^https?://[^\\s]+$", Pattern.CASE_INSENSITIVE);

    private final PostSeoMapper postSeoMapper;
    private final SeoAuditService audit;

    public SeoMetadataService(PostSeoMapper postSeoMapper, SeoAuditService audit) {
        this.postSeoMapper = postSeoMapper;
        this.audit = audit;
    }

    /**
     * 保存手填的 SEO 元数据（整组覆盖，空值即删除对应字段）。
     *
     * <p>{@code generated_at} 不清空：它记录「最近一次由模型生成」的时间，
     * 手改描述不应该让这个时间线消失；{@code source} 则改成 {@code manual}，
     * 表示当前内容由人维护。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public SeoDetailVO save(Long postId, SeoUpdateRequest request) {
        BlogPost post = audit.requirePost(postId);
        SeoUpdateRequest body = request == null ? new SeoUpdateRequest() : request;
        String title = normalize(body.getSeoTitle(), TITLE_MAX, "SEO 标题");
        String description = normalize(body.getSeoDescription(), DESCRIPTION_MAX, "SEO 描述");
        String keywords = normalize(body.getSeoKeywords(), KEYWORDS_MAX, "关键词");
        String ogImage = normalize(body.getOgImageUrl(), OG_IMAGE_MAX, "分享图地址");
        validateOgImage(ogImage);

        PostSeo row = postSeoMapper.selectById(post.getId());
        if (row == null) {
            row = new PostSeo();
            row.setPostId(post.getId());
            row.setSeoTitle(title);
            row.setSeoDescription(description);
            row.setSeoKeywords(keywords);
            row.setOgImageUrl(ogImage);
            row.setSource(SOURCE_MANUAL);
            row.setGeneratedAt(null);
            postSeoMapper.insert(row);
        } else {
            // 这里必须用 LambdaUpdateWrapper 显式 .set(...)，不能用 updateById(entity)：
            // MyBatis-Plus 的默认更新策略是 NOT_NULL，会把 null 字段整个从 SET 子句里剔除，
            // 于是「把描述清空」这种操作会静默失效（回读仍是旧值，用户看到的是保存没生效）。
            // wrapper 的 set 是逐列显式赋值，null 会真的写成 NULL。
            // updateTime 有 FieldFill.INSERT_UPDATE，但 update(null, wrapper) 没有实体可供
            // MetaObjectHandler 填充，所以这里要手动写。
            postSeoMapper.update(null, new LambdaUpdateWrapper<PostSeo>()
                    .eq(PostSeo::getPostId, post.getId())
                    .set(PostSeo::getSeoTitle, title)
                    .set(PostSeo::getSeoDescription, description)
                    .set(PostSeo::getSeoKeywords, keywords)
                    .set(PostSeo::getOgImageUrl, ogImage)
                    .set(PostSeo::getSource, SOURCE_MANUAL)
                    .set(PostSeo::getUpdateTime, LocalDateTime.now()));
        }
        log.info("[SEO] 手工保存 SEO 元数据：postId={}", postId);
        return audit.detailOrDerived(post.getId());
    }

    /** 清除 SEO 元数据（幂等：没有记录也算成功）。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long postId) {
        if (postId == null) {
            throw new BusinessException(404, "文章不存在");
        }
        if (postSeoMapper.selectById(postId) != null) {
            postSeoMapper.deleteById(postId);
            log.info("[SEO] 清除 SEO 元数据：postId={}", postId);
        }
    }

    /** 空白 → {@code null}（落库为 NULL），非空白去首尾空格并校验长度。 */
    static String normalize(String raw, int max, String label) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        if (value.length() > max) {
            throw new BusinessException(400, label + "不能超过 " + max + " 个字符");
        }
        return value;
    }

    /**
     * 分享图地址校验：允许站内相对路径（{@code /uploads/a.jpg}）或 http(s) 绝对地址。
     *
     * <p>拒绝 {@code //host}（协议相对地址会把图片指到外站）、{@code javascript:}、
     * 以及反斜杠/控制字符等容易在浏览器里被解释成别的协议的写法。</p>
     */
    static void validateOgImage(String value) {
        if (value == null) {
            return;
        }
        if (value.startsWith("//")) {
            throw new BusinessException(400, "分享图地址不能使用 // 开头的协议相对地址");
        }
        if (value.startsWith("/")) {
            if (value.startsWith("/\\") || value.contains("\\")) {
                throw new BusinessException(400, "分享图地址包含非法字符");
            }
            for (int i = 0; i < value.length(); i++) {
                if (Character.isISOControl(value.charAt(i))) {
                    throw new BusinessException(400, "分享图地址包含非法字符");
                }
            }
            return;
        }
        if (!HTTP_URL.matcher(value).matches()) {
            throw new BusinessException(400, "分享图地址需以 http(s):// 或 / 开头");
        }
    }
}
