package tech.aiflowy.ai.service.impl;

import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.alibaba.fastjson.JSON;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tech.aiflowy.ai.entity.BotConversation;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.mapper.BotConversationMapper;
import tech.aiflowy.ai.mapper.BotMapper;
import tech.aiflowy.ai.mapper.BotMessageMapper;
import tech.aiflowy.ai.service.ConversationAnalysisService;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.ai.service.userProfileService.DataScopeManager;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tech.aiflowy.ai.entity.table.BotMessageTableDef.BOT_MESSAGE;

/**
 * 会话分析服务实现
 *
 * @author Administrator
 * @since 2026-05-07
 */
@Service
public class ConversationAnalysisServiceImpl implements ConversationAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ConversationAnalysisServiceImpl.class);

    @Resource
    private BotConversationMapper botConversationMapper;

    @Resource
    private BotMessageMapper botMessageMapper;

    @Resource
    private BotMapper botMapper;

    @Resource
    private ModelService modelService;

    @Override
    public void analyzeConversation(BigInteger conversationId, BigInteger botId, BigInteger accountId) {
        try {
            DataScopeManager.ignoreDataScopeCondition();

            // 1. 查询会话信息
            BotConversation conversation = botConversationMapper.selectOneById(conversationId);
            if (conversation == null) {
                log.warn("会话不存在: {}", conversationId);
                return;
            }

            // 2. 查询会话消息
            QueryWrapper qw = QueryWrapper.create();
            qw.eq(BotMessage::getConversationId, conversationId);
            qw.orderBy(BOT_MESSAGE.ID.asc());
            List<BotMessage> messages = botMessageMapper.selectListByQuery(qw);

            // 3. 获取系统模型
            Model model = modelService.getSystemModel();
            if (model == null) {
                log.warn("系统模型不存在，请先配置系统模型");
                return;
            }

            // 4. 构建消息历史（按轮次处理，每轮取第一个用户提问和最后一个AI回复
            StringBuilder messageHistory = new StringBuilder();
            
            // 按roundNumber分组处理
            Map<Integer, List<BotMessage>> roundMessages = new HashMap<>();
            for (BotMessage msg : messages) {
                Integer roundNumber = msg.getRoundNumber();
                if (roundNumber == null) {
                    roundNumber = 0;
                }
                roundMessages.computeIfAbsent(roundNumber, k -> new java.util.ArrayList<>()).add(msg);
            }
            
            // 按轮次顺序处理
            List<Integer> sortedRounds = new java.util.ArrayList<>(roundMessages.keySet());
            java.util.Collections.sort(sortedRounds);
            
            for (Integer round : sortedRounds) {
                List<BotMessage> roundMsgList = roundMessages.get(round);
                
                // 找到该轮第一个user消息
                BotMessage firstUserMsg = null;
                // 找到该轮最后一个assistant消息
                BotMessage lastAssistantMsg = null;
                
                for (BotMessage msg : roundMsgList) {
                    String role = msg.getRole();
                    if ("user".equals(role) && firstUserMsg == null) {
                        firstUserMsg = msg;
                    }
                    if ("assistant".equals(role)) {
                        lastAssistantMsg = msg;
                    }
                }
                
                // 组装消息
                if (firstUserMsg != null) {
                    String userContent = extractMessageContent(firstUserMsg);
                    if (StringUtils.hasText(userContent)) {
                        messageHistory.append("用户：").append(userContent).append("\n");
                    }
                }
                if (lastAssistantMsg != null) {
                    String aiContent = extractMessageContent(lastAssistantMsg);
                    if (StringUtils.hasText(aiContent)) {
                        messageHistory.append("AI：").append(aiContent).append("\n");
                    }
                }
            }

            // 5. 调用AI分析
            ChatModel chatModel = model.toChatModel(null);
            if (chatModel == null) {
                log.warn("模型转换失败: {}", model.getId());
                return;
            }

            String prompt = buildAnalysisPrompt(messageHistory.toString());
            ChatOptions options = new ChatOptions();
            options.setTemperature(0.3f);

            String result = chatModel.chat(prompt, options);
            if (!StringUtils.hasText(result)) {
                log.warn("AI分析结果为空: {}", conversationId);
                return;
            }

            // 6. 解析并保存结果
            Map<String, Object> analysisResult = parseAnalysisResult(result);
            if (analysisResult != null && !analysisResult.isEmpty()) {
                saveAnalysisResult(conversationId, analysisResult);
                log.info("会话分析完成: {}", conversationId);
            }

        } catch (Exception e) {
            log.warn("分析会话失败: {}", conversationId, e);
        } finally {
            DataScopeManager.restoreDataScopeCondition();
        }
    }

    @Override
    public Map<String, Object> getAnalysisResult(BigInteger conversationId) {
        BotConversation conversation = botConversationMapper.selectOneById(conversationId);
        if (conversation == null || conversation.getOptions() == null) {
            return new HashMap<>();
        }

        Object analysis = conversation.getOptions().get("analysis");
        if (analysis instanceof Map) {
            return (Map<String, Object>) analysis;
        }
        return new HashMap<>();
    }

    @Override
    public void saveAnalysisResult(BigInteger conversationId, Map<String, Object> analysisResult) {
        BotConversation conversation = botConversationMapper.selectOneById(conversationId);
        if (conversation == null) {
            return;
        }

        Map<String, Object> options = conversation.getOptions();
        if (options == null) {
            options = new HashMap<>();
        }

        // 添加分析时间
        analysisResult.put("analysisTime", System.currentTimeMillis());
        options.put("analysis", analysisResult);

        conversation.setOptions(options);
        botConversationMapper.update(conversation);
    }

    /**
     * 提取消息内容
     */
    private String extractMessageContent(BotMessage message) {
        try {
            // 如果是AI消息且有summary字段，优先使用summary
            if ("assistant".equals(message.getRole()) && StringUtils.hasText(message.getSummary())) {
                return message.getSummary();
            }
            
            com.agentsflex.core.message.Message msg = message.getContentAsMessage();
            if (msg != null) {
                // 如果是UserMessage，获取content
                if (msg instanceof com.agentsflex.core.message.UserMessage) {
                    com.agentsflex.core.message.UserMessage userMessage = (com.agentsflex.core.message.UserMessage) msg;
                    String content = userMessage.getContent();
                    if (StringUtils.hasText(content)) {
                        return content;
                    }
                }
                // 如果是AiMessage，尝试获取fullContent
                else if (msg instanceof com.agentsflex.core.message.AiMessage) {
                    com.agentsflex.core.message.AiMessage aiMessage = (com.agentsflex.core.message.AiMessage) msg;
                    // 先尝试getFullContent
                    String fullContent = aiMessage.getFullContent();
                    if (StringUtils.hasText(fullContent)) {
                        return fullContent;
                    }
                }
                // 否则使用getTextContent作为备用
                else {
                    String textContent = msg.getTextContent();
                    if (StringUtils.hasText(textContent)) {
                        return textContent;
                    }
                }
            }

        } catch (Exception e) {
            // 忽略解析错误
        }
        return "";
    }

    /**
     * 构建分析Prompt
     */
    private String buildAnalysisPrompt(String messageHistory) {
        return "你是一个专业的对话分析师，请基于以下对话历史进行分析：\n\n" +
                "【对话历史】\n" +
                messageHistory + "\n\n" +
                "请以JSON格式输出分析结果：\n" +
                "{\n" +
                "  \"conversationTheme\": \"对话主题（10-20字）\",\n" +
                "  \"coreDemand\": \"用户核心诉求（简洁明了）\",\n" +
                "  \"userProfile\": {\n" +
                "    \"occupation\": \"职业（如不确定可写'未知'）\",\n" +
                "    \"interest\": \"兴趣爱好（如不确定可写'未知'）\",\n" +
                "    \"personality\": \"性格特点（如不确定可写'未知'）\",\n" +
                "    \"communicationStyle\": \"沟通风格（如不确定可写'未知'）\"\n" +
                "  },\n" +
                "  \"summary\": \"对话摘要（100字以内）\"\n" +
                "}\n\n" +
                "要求：\n" +
                "1. 基于事实分析，不做无根据猜测\n" +
                "2. 语言简洁准确\n" +
                "3. 只返回JSON，不要其他内容";
    }

    /**
     * 解析AI返回的分析结果
     */
    private Map<String, Object> parseAnalysisResult(String result) {
        try {
            // 尝试提取JSON部分
            int jsonStart = result.indexOf("{");
            int jsonEnd = result.lastIndexOf("}");
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = result.substring(jsonStart, jsonEnd + 1);
                return JSON.parseObject(jsonStr, Map.class);
            }
            // 如果没有明确的JSON标记，尝试直接解析
            return JSON.parseObject(result, Map.class);
        } catch (Exception e) {
            log.warn("解析分析结果失败: {}", result, e);
            return null;
        }
    }
}
