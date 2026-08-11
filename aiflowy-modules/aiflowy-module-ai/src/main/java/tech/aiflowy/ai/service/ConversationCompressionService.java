package tech.aiflowy.ai.service;

import tech.aiflowy.ai.entity.BotMessage;

import java.math.BigInteger;
import java.util.List;

public interface ConversationCompressionService {

    void compressConversationHistory(BigInteger botId, BigInteger accountId, BigInteger conversationId);

    String getExistingSummary(BigInteger botId, BigInteger accountId, BigInteger conversationId);

    String generateSummaryWithAI(BigInteger botId, String summaryPrompt);

    List<BotMessage> getMessagesForCompression(BigInteger botId, BigInteger accountId, BigInteger conversationId, int maxHistoryRounds, int compressionRounds);

    void updateMessageSummary(BigInteger messageId, String summary);
}
