package com.selfblog.service.ai;

/**
 * 模型输出未通过结构校验。
 *
 * <p>消息是「问题描述」而不是给管理员的最终文案：调用方会把它回给模型要求修复一次，
 * 只有修复后仍不合规才转成 {@code BusinessException(502, 模型返回的内容不符合要求…)}。</p>
 */
public class AiOutputException extends RuntimeException {

    public AiOutputException(String problem) {
        super(problem);
    }
}
