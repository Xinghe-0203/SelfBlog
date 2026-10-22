package com.selfblog.service.ai;

import com.selfblog.common.exception.BusinessException;
import com.selfblog.service.seo.LlmUpstreamException;
import com.selfblog.service.seo.OpenAiCompatibleLlmClient;
import com.selfblog.service.seo.OpenAiCompatibleLlmClient.Message;
import com.selfblog.service.seo.SeoLlmConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * 「调一次模型 + 解析失败就修复一次」的统一编排。
 *
 * <p>与 {@code SeoGenerationService} 同一套语义：模型偶尔会输出带解释的 JSON 或漏字段，
 * 直接报错对管理员不友好，因此把具体问题回给模型重试一次；第二次仍不合规才转成
 * 契约 §0 的 502 文案。上游异常（鉴权/额度/超时）不重试，直接翻译成 502。</p>
 *
 * <p>刻意不加 {@code @Transactional}：模型调用是跨网络 IO，不能占用数据库连接。</p>
 */
@Slf4j
@Component
public class AiModelCaller {

    /** 契约 §0：模型返回不合规时的统一文案。 */
    public static final String INVALID_OUTPUT_MESSAGE = "模型返回的内容不符合要求，请重试或手动填写";

    private final OpenAiCompatibleLlmClient client;

    public AiModelCaller(OpenAiCompatibleLlmClient client) {
        this.client = client;
    }

    /**
     * 纯文本补全 + 宽容解析。
     *
     * @param parser 把模型原始输出转成目标结构；结构不合规时抛 {@link AiOutputException}
     */
    public <T> T generate(SeoLlmConfig config, String systemPrompt, String userPrompt,
                          Function<String, T> parser) {
        return run(config, systemPrompt, userPrompt, null, parser, AiModelCaller::safeMessage);
    }

    /**
     * 带图像输入的补全 + 宽容解析（E 图片描述专用）。
     *
     * <p>图像以 {@code data:} URL 内联在 user 消息的多模态数组里，模型端不需要能访问本站。
     * 修复重试同样带上原图，否则模型看不到图就改不出更准确的描述。</p>
     *
     * @param failureTranslator 把上游异常翻译成给管理员看的文案（E 用它把「参数被拒」
     *                          翻译成「当前模型不支持图像输入」）
     */
    public <T> T generateVision(SeoLlmConfig config, String systemPrompt, String userPrompt,
                                String imageDataUrl, Function<String, T> parser,
                                Function<LlmUpstreamException, String> failureTranslator) {
        return run(config, systemPrompt, userPrompt, imageDataUrl, parser, failureTranslator);
    }

    private <T> T run(SeoLlmConfig config, String systemPrompt, String userPrompt,
                      String imageDataUrl, Function<String, T> parser,
                      Function<LlmUpstreamException, String> failureTranslator) {
        String raw = complete(config, systemPrompt, userPrompt, imageDataUrl, failureTranslator);
        try {
            return parser.apply(raw);
        } catch (AiOutputException first) {
            log.info("[AI] 首次输出不合规，回给模型修复一次：{}", first.getMessage());
            String repaired = complete(config, systemPrompt,
                    repairPrompt(userPrompt, raw, first.getMessage()), imageDataUrl, failureTranslator);
            try {
                return parser.apply(repaired);
            } catch (AiOutputException second) {
                log.warn("[AI] 修复后仍不合规：{}", second.getMessage());
                throw new BusinessException(502, INVALID_OUTPUT_MESSAGE);
            }
        }
    }

    private String complete(SeoLlmConfig config, String systemPrompt, String userPrompt,
                            String imageDataUrl,
                            Function<LlmUpstreamException, String> failureTranslator) {
        try {
            if (imageDataUrl == null) {
                return client.complete(config, List.of(
                        new Message("system", systemPrompt),
                        new Message("user", userPrompt)));
            }
            return client.completeVision(config, systemPrompt, userPrompt, imageDataUrl);
        } catch (LlmUpstreamException e) {
            throw new BusinessException(502, failureTranslator.apply(e));
        }
    }

    private static String safeMessage(LlmUpstreamException e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? "模型服务暂时不可用，请稍后重试" : message;
    }

    /** 修复提示：把上一轮原始输出与具体问题回给模型，要求只修结构不改内容。 */
    static String repairPrompt(String userPrompt, String previousOutput, String problem) {
        return userPrompt
                + "\n\n你上一次的输出无法通过校验，问题：" + problem
                + "\n上一次输出如下（可能包含代码块或多余文字）：\n"
                + (previousOutput == null ? "" : previousOutput)
                + "\n\n请重新输出一个严格符合系统要求的 JSON 对象，只输出 JSON，不要任何解释。";
    }
}
