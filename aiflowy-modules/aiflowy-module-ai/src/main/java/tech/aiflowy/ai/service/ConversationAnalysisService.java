package tech.aiflowy.ai.service;

import java.math.BigInteger;
import java.util.Map;

/**
 * 会话分析服务
 * 用于分析会话，生成对话主题、核心诉求、用户画像等
 *
 * @author Administrator
 * @since 2026-05-07
 */
public interface ConversationAnalysisService {

    /**
     * 分析会话
     *
     * @param conversationId 会话ID
     * @param botId Bot ID
     * @param accountId 用户ID
     */
    void analyzeConversation(BigInteger conversationId, BigInteger botId, BigInteger accountId);

    /**
     * 获取会话分析结果
     *
     * @param conversationId 会话ID
     * @return 分析结果Map
     */
    Map<String, Object> getAnalysisResult(BigInteger conversationId);

    /**
     * 保存会话分析结果
     *
     * @param conversationId 会话ID
     * @param analysisResult 分析结果
     */
    void saveAnalysisResult(BigInteger conversationId, Map<String, Object> analysisResult);
}
