package tech.aiflowy.ai.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.agentsflex.agent.Agent;
import com.agentsflex.agent.AgentContextTokenEstimator;
import com.agentsflex.agent.AgentRunner;
import com.agentsflex.agent.AgentTurnOptions;
import com.agentsflex.agent.compression.AgentContextCompressionPolicy;
import com.agentsflex.core.memory.ChatMemory;
import com.agentsflex.core.message.Message;
import com.agentsflex.core.message.UserMessage;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.agentsflex.core.prompt.MemoryPrompt;
import com.agentsflex.skill.runtime.SkillRuntime;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tech.aiflowy.ai.agentsflex.AttachmentHelper;
import tech.aiflowy.ai.agentsflex.ChatOptionsBuilder;
import tech.aiflowy.ai.agentsflex.compression.AgentCompressionHelper;
import tech.aiflowy.ai.agentsflex.execution.AgentExecutionPolicyHelper;
import tech.aiflowy.ai.agentsflex.listener.AgentChatEventListener;
import tech.aiflowy.ai.agentsflex.loader.DynamicAgentLoader;
import tech.aiflowy.ai.agentsflex.memory.BotMessageMemory;
import tech.aiflowy.ai.agentsflex.memory.DefaultBotMessageMemory;
import tech.aiflowy.ai.agentsflex.memory.PublicBotMessageMemory;
import tech.aiflowy.ai.agentsflex.middleware.AgentChatToolMiddleware;
import tech.aiflowy.ai.agentsflex.tool.BotToolBuilder;
import tech.aiflowy.ai.entity.*;
import tech.aiflowy.ai.mapper.BotMapper;
import tech.aiflowy.ai.mapper.BotMessageMapper;
import tech.aiflowy.ai.service.*;
import tech.aiflowy.ai.utils.CustomBeanUtils;
import tech.aiflowy.ai.utils.RegexUtils;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.filestorage.utils.PathGeneratorUtil;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.util.MapUtil;
import tech.aiflowy.common.util.Maps;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;
import tech.aiflowy.core.chat.protocol.sse.ChatSseUtil;

import java.math.BigInteger;
import java.util.*;


/**
 *  服务层实现。
 *
 * @author michael
 * @since 2024-08-23
 */
@Service
public class BotServiceImpl extends ServiceImpl<BotMapper, Bot> implements BotService {

    private static final Logger log = LoggerFactory.getLogger(BotServiceImpl.class);

    /**
     * 上下文 Token 估算器：按消息文本长度的 1/4 估算，用于「挂载 Token 上限」。
     */
    private static final AgentContextTokenEstimator CONTEXT_TOKEN_ESTIMATOR = messages -> {
        if (messages == null) {
            return 0;
        }
        long total = 0;
        for (Message message : messages) {
            if (message == null) {
                continue;
            }
            String text = message.getTextContent();
            if (text != null) {
                total += text.length() / 4L;
            }
        }
        return total;
    };

    public static class ChatCheckResult {
        private Bot aiBot;
        private Map<String, Object> modelOptions;
        private ChatModel chatModel;
        private ChatModel vlChatModel;
        private String conversationIdStr;
        private boolean hasImageAttachment;

        public Bot getAiBot() {return aiBot;}

        public void setAiBot(Bot aiBot) {this.aiBot = aiBot;}

        public Map<String, Object> getModelOptions() {return modelOptions;}

        public void setModelOptions(Map<String, Object> modelOptions) {this.modelOptions = modelOptions;}

        public ChatModel getChatModel() {return chatModel;}

        public void setChatModel(ChatModel chatModel) {this.chatModel = chatModel;}

        public ChatModel getVlChatModel() {return vlChatModel;}

        public void setVlChatModel(ChatModel vlChatModel) {this.vlChatModel = vlChatModel;}

        public String getConversationIdStr() {return conversationIdStr;}

        public void setConversationIdStr(String conversationIdStr) {this.conversationIdStr = conversationIdStr;}

        public boolean hasImageAttachment() {return hasImageAttachment;}

        public void setHasImageAttachment(boolean hasImageAttachment) {this.hasImageAttachment = hasImageAttachment;}
    }

    @Resource
    private BotMessageService botMessageService;

    @Resource
    private BotMessageMapper botMessageMapper;

    @Resource
    private BotConversationService botConversationService;

    @Resource(name = "sseThreadPool")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Resource(name = "commonExecutor")
    private java.util.concurrent.Executor commonExecutor;

    @Resource
    private ModelService modelService;

    @Resource
    private AttachmentHelper attachmentHelper;
    @Resource
    private AgentCompressionHelper compressionHelper;
    @Resource
    private AgentExecutionPolicyHelper executionPolicyHelper;
    @Resource
    private DynamicAgentLoader dynamicAgentLoader;
    @Resource
    private BotToolBuilder botToolBuilder;

    @Override
    public Bot getDetail(String id) {
        Bot aiBot = null;

        if (id.matches(RegexUtils.ALL_NUMBER)) {
            aiBot = getById(id);

            if (aiBot == null) {
                aiBot = getByAlias(id);
            }

        } else {
            aiBot = getByAlias(id);
        }

        return aiBot;
    }

    @Override
    public Bot getByAlias(String alias) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq(Bot::getAlias, alias);
        return getOne(queryWrapper);
    }

    public SseEmitter checkChatBeforeStart(BigInteger botId, String prompt, String conversationId, ChatCheckResult chatCheckResult, List<String> attachments) {
        if (!StringUtils.hasLength(prompt)) {
            return ChatSseUtil.sendSystemError(conversationId, "提示词不能为空");
        }
        if (!StringUtils.hasLength(conversationId)) {
            return ChatSseUtil.sendSystemError(conversationId, "conversationId不能为空");
        }
        Bot aiBot = this.getById(botId);
        if (aiBot == null) {
            return ChatSseUtil.sendSystemError(conversationId, "聊天助手不存在");
        }
        if (aiBot.getModelId() == null) {
            return ChatSseUtil.sendSystemError(conversationId, "请配置大模型!");
        }
        boolean login = StpUtil.isLogin();
        if (!login && !aiBot.isAnonymousEnabled()) {
            return ChatSseUtil.sendSystemError(conversationId, "此聊天助手不支持匿名访问");
        }
        Map<String, Object> modelOptions = aiBot.getModelOptions();
        Model model = modelService.getModelInstance(aiBot.getModelId());
        if (model == null) {
            return ChatSseUtil.sendSystemError(conversationId, "模型不存在，请检查配置");
        }
        ChatModel chatModel = model.toChatModel(null);
        if (chatModel == null) {
            return ChatSseUtil.sendSystemError(conversationId, "对话模型获取失败，请检查配置");
        }

        // 检查附件功能是否开启
        boolean attachmentEnabled = aiBot.isAttachmentEnabled();

        // 如果附件功能未开启，忽略所有附件
        if (!attachmentEnabled && attachments != null && !attachments.isEmpty()) {
            attachments = null;
        }

        // 检测attachments中是否有图片
        boolean hasImageAttachment = attachmentHelper.hasImageInAttachments(attachments);
        chatCheckResult.setHasImageAttachment(hasImageAttachment);

        // 如果有图片，获取VL模型
        if (hasImageAttachment) {
            BigInteger vlModelId = aiBot.getVlModelId();
            ChatModel vlChatModel = null;

            // 如果选择了"使用当前对话模型"
            if (aiBot.isUseCurrentModelAsVl()) {
                Boolean supportImage = model.getSupportImage();
                if (supportImage == null || !supportImage) {
                    return ChatSseUtil.sendSystemError(conversationId, "当前对话模型不支持图片处理，请配置VL模型!");
                }
                vlChatModel = chatModel;
            }
            // 如果配置了具体的VL模型
            else if (vlModelId != null) {
                Model vlModel = modelService.getModelInstance(vlModelId);
                if (vlModel == null) {
                    return ChatSseUtil.sendSystemError(conversationId, "VL模型不存在，请检查配置!");
                }
                vlChatModel = vlModel.toChatModel(null);
                if (vlChatModel == null) {
                    return ChatSseUtil.sendSystemError(conversationId, "VL模型获取失败，请检查配置!");
                }
            }
            // 如果未配置VL模型
            else {
                return ChatSseUtil.sendSystemError(conversationId, "检测到图片附件，请先配置VL模型!");
            }

            chatCheckResult.setVlChatModel(vlChatModel);
        }

        chatCheckResult.setAiBot(aiBot);
        chatCheckResult.setModelOptions(modelOptions);
        chatCheckResult.setChatModel(chatModel);
        chatCheckResult.setConversationIdStr(conversationId);
        return null;
    }

    @Override
    public SseEmitter startChat(BigInteger botId, String prompt, BigInteger conversationId, List<Map<String, String>> messages,
                                BotServiceImpl.ChatCheckResult chatCheckResult, List<String> attachments) {
        // 获取当前登录用户信息，传递给 Agent 事件监听器
        LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
        BigInteger accountId = loginAccount.getId();
        Map<String, Object> modelOptions = chatCheckResult.getModelOptions();
        Bot aiBot = chatCheckResult.getAiBot();
        // 如果有图片附件且配置了VL模型，使用VL模型
        ChatModel chatModel;
        if (chatCheckResult.hasImageAttachment() && chatCheckResult.getVlChatModel() != null) {
            chatModel = chatCheckResult.getVlChatModel();
            log.info("使用VL模型处理图片对话");
        } else {
            chatModel = chatCheckResult.getChatModel();
        }

        // 系统提示词作为 Agent 的 instructions
        String systemPrompt = MapUtil.getString(modelOptions, Bot.KEY_SYSTEM_PROMPT);
        String instructions = StringUtils.hasText(systemPrompt) ? systemPrompt : "";

        // 分离图片附件和非图片附件
        List<String> imageAttachments = new ArrayList<>();
        List<String> nonImageAttachments = new ArrayList<>();
        if (attachments != null && !attachments.isEmpty()) {
            String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg"};
            for (String attachment : attachments) {
                if (attachment == null) {
                    continue;
                }
                String lowerCaseUrl = attachment.toLowerCase();
                boolean isImage = false;
                for (String ext : imageExtensions) {
                    if (lowerCaseUrl.endsWith(ext)) {
                        isImage = true;
                        break;
                    }
                }
                if (isImage) {
                    imageAttachments.add(attachment);
                } else {
                    nonImageAttachments.add(attachment);
                }
            }
        }

        // 处理非图片附件（转换为文本）
        String originalPrompt = prompt;
        String nonImageAttachmentsText = attachmentHelper.attachmentsToString(nonImageAttachments);
        if (StringUtils.hasText(nonImageAttachmentsText)) {
            prompt = "【用户问题】：\n" + prompt + "\n请基于用户上传的附件内容回答用户问题： \n\n" + "【用户上传的附件内容】：\n" + nonImageAttachmentsText;
        }

        // 处理图片附件（构造图片访问地址信息）
        if (!imageAttachments.isEmpty()) {
            StringBuilder imageInfoBuilder = new StringBuilder();
            for (int i = 0; i < imageAttachments.size(); i++) {
                String imageUrl = imageAttachments.get(i);
                if (i > 0) {
                    imageInfoBuilder.append("\n\n");
                }
                imageInfoBuilder.append("图片").append(i + 1).append("，文件名为：").append(PathGeneratorUtil.getPureFileName(imageUrl)).append("，访问地址为：").append(imageUrl);
            }
            if (StringUtils.hasText(nonImageAttachmentsText)) {
                prompt = prompt + "\n\n" + "【用户上传的图片信息】：\n" + imageInfoBuilder.toString();
            } else {
                prompt = "【用户问题】：\n" + prompt + "\n\n" + "【用户上传的图片信息】：\n" + imageInfoBuilder.toString();
            }
        }

        UserMessage userMessage = new UserMessage(prompt);

        // 处理图片附件（添加图片URL）
        if (!imageAttachments.isEmpty()) {
            for (String imageUrl : imageAttachments) {
                userMessage.addImageUrl(imageUrl);
                log.info("添加图片URL到消息: {}", imageUrl);
            }
        }

        // 构建 ChatOptions
        ChatOptions chatOptions = ChatOptionsBuilder.getChatOptions(modelOptions);
        Boolean enableDeepThinking = MapUtil.getBoolean(modelOptions, Bot.KEY_ENABLE_DEEP_THINKING, false);
        chatOptions.setThinkingEnabled(enableDeepThinking);
        Boolean exposeDeepThinking = MapUtil.getBoolean(aiBot.getOptions(), Bot.KEY_EXPOSE_DEEP_THINKING, false);

        // 收集工具（其他技能 + 挂载模块）
        MemoryPrompt memoryPrompt = new MemoryPrompt();
        botToolBuilder.addOtherSkillsTools(memoryPrompt, aiBot);
        Map<String, Object> buildParams = Maps.of("botId", botId)
                .set("conversationId", conversationId);
        memoryPrompt.addTools(botToolBuilder.buildFunctionList(buildParams));
        // 获取 SkillRuntime，传递给事件监听器在结束时关闭
        SkillRuntime skillRuntime = (SkillRuntime) buildParams.get("skillRuntime");
        if (skillRuntime != null) {
            Map<String, Object> contextAttributes = chatOptions.getContextAttributes();
            if (contextAttributes == null) {
                contextAttributes = new HashMap<>();
                chatOptions.setContextAttributes(contextAttributes);
            }
            contextAttributes.put("skillRuntime", skillRuntime);
        }
        chatOptions.setContextConversationId(conversationId);
        chatOptions.setContextAccountId(accountId);

        ChatSseEmitter chatSseEmitter = new ChatSseEmitter();
        SseEmitter emitter = chatSseEmitter.getEmitter();

        // 创建对话记忆
        final ChatMemory chatMemory;
        if (messages != null && !messages.isEmpty()) {
            chatMemory = new DefaultBotMessageMemory(conversationId, chatSseEmitter, messages);
        } else {
            BotMessageMemory botMessageMemory = new BotMessageMemory(botId, accountId, conversationId,
                    botMessageService, botMessageMapper, botConversationService, commonExecutor);
            botMessageMemory.setOriginalUserContent(originalPrompt);
            chatMemory = botMessageMemory;
        }

        // 构建 Agent
        Agent.Builder agentBuilder = Agent.builder("bot-" + botId)
                .instructions(instructions)
                .chatModel(chatModel)
                .maxAttachedTurns(aiBot.getMaxAttachedTurns())
                .maxAttachedMessages(aiBot.getMaxAttachedMessages())
                .chatOptions(chatOptions)
                .tools(memoryPrompt.getTools() == null ? new ArrayList<>() : memoryPrompt.getTools())
                .executionPolicy(executionPolicyHelper.buildExecutionPolicy(aiBot))
                .middleware(new AgentChatToolMiddleware(botId.toString(), accountId.toString(),
                        conversationId.toString(), chatSseEmitter, loginAccount));

        // 挂载 Token 上限（0 表示不限制）
        if (aiBot.getMaxAttachedTokens() > 0) {
            agentBuilder.maxAttachedTokens(aiBot.getMaxAttachedTokens(), CONTEXT_TOKEN_ESTIMATOR);
        }

        // 如果有图片附件，设置多模态模型
        if (chatCheckResult.hasImageAttachment() && chatCheckResult.getVlChatModel() != null) {
            agentBuilder.multimodalChatModel(chatCheckResult.getVlChatModel());
        }

        // 配置模型摘要压缩（固定策略：阈值 2 轮触发、保留最近 2 轮、摘要模型为当前对话模型）
        AgentContextCompressionPolicy compressionPolicy =
                compressionHelper.createCompressionPolicy(aiBot, conversationId.toString());
        if (compressionPolicy != null) {
            agentBuilder.compressionPolicy(compressionPolicy);
        }

        Agent agent = agentBuilder.build();
        dynamicAgentLoader.register(agent);

        // 构建 AgentRunner，配置 chatMemoryProvider 接入对话记忆
        AgentRunner runner = AgentRunner.builder()
                .chatMemoryProvider(conversationIdStr -> chatMemory)
                .build();

        // 创建事件监听器
        AgentChatEventListener eventListener = new AgentChatEventListener(
                botId.toString(), accountId.toString(), conversationId.toString(),
                chatSseEmitter, exposeDeepThinking);
        if (skillRuntime != null) {
            eventListener.setSkillRuntime(skillRuntime);
        }
        runner.addEventListener(eventListener);

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
        threadPoolTaskExecutor.execute(() -> {
            boolean requestAttributesSet = false;
            try {
                RequestContextHolder.setRequestAttributes(sra, true);
                requestAttributesSet = true;
                runner.run(agent, conversationId.toString(), userMessage,
                        AgentTurnOptions.builder().streaming(true).build());
            } catch (Exception e) {
                log.error("Agent 对话执行异常", e);
                eventListener.sendSystemError("对话处理失败: " + e.getMessage());
            } finally {
                if (requestAttributesSet) {
                    RequestContextHolder.resetRequestAttributes();
                }
            }
        });

        return emitter;
    }

    /**
     * 第三方使用Apikey访问聊天
     * @param botId
     * @return
     */
    @Override
    public SseEmitter startPublicChat(BigInteger botId, String prompt,  List<Message> messages, BotServiceImpl.ChatCheckResult chatCheckResult) {
        Map<String, Object> modelOptions = chatCheckResult.getModelOptions();
        ChatOptions chatOptions = ChatOptionsBuilder.getChatOptions(modelOptions);
        ChatModel chatModel = chatCheckResult.getChatModel();
        // 将 botId 和 conversationId 加入上下文，供运行时对象回查使用
        chatOptions.setContextBotId(botId);
        chatOptions.setContextConversationId(chatCheckResult.getConversationIdStr());
        UserMessage userMessage = new UserMessage(prompt);
        ChatSseEmitter chatSseEmitter = new ChatSseEmitter();
        SseEmitter emitter = chatSseEmitter.getEmitter();
        final ChatMemory publicChatMemory = new PublicBotMessageMemory(chatSseEmitter, messages);

        // 收集工具
        MemoryPrompt memoryPrompt = new MemoryPrompt();
        Bot publicBot = getById(botId);
        botToolBuilder.addOtherSkillsTools(memoryPrompt, publicBot);
        // 会话ID：用于 SkillRuntime 等运行时对象，非数字时传 null
        BigInteger publicConversationId = parseConversationId(chatCheckResult.getConversationIdStr());
        memoryPrompt.addTools(botToolBuilder.buildFunctionList(Maps.of("botId", botId)
                .set("needEnglishName", false)
                .set("conversationId", publicConversationId)
        ));

        String instructions = StringUtils.hasText(MapUtil.getString(modelOptions, Bot.KEY_SYSTEM_PROMPT))
                ? MapUtil.getString(modelOptions, Bot.KEY_SYSTEM_PROMPT) : "";

        // 构建 Agent
        Agent.Builder agentBuilder = Agent.builder("bot-" + botId)
                .instructions(instructions)
                .chatModel(chatModel)
                .maxAttachedTurns(publicBot != null ? publicBot.getMaxAttachedTurns() : 10)
                .maxAttachedMessages(publicBot != null ? publicBot.getMaxAttachedMessages() : 100)
                .chatOptions(chatOptions)
                .tools(memoryPrompt.getTools() == null ? new ArrayList<>() : memoryPrompt.getTools())
                .executionPolicy(executionPolicyHelper.buildExecutionPolicy(publicBot))
                .middleware(new AgentChatToolMiddleware(botId.toString(), null,
                        chatCheckResult.getConversationIdStr(), chatSseEmitter, null));

        if (publicBot != null && publicBot.getMaxAttachedTokens() > 0) {
            agentBuilder.maxAttachedTokens(publicBot.getMaxAttachedTokens(), CONTEXT_TOKEN_ESTIMATOR);
        }

        // 配置模型摘要压缩（固定策略：阈值 2 轮触发、保留最近 2 轮、摘要模型为当前对话模型）
        AgentContextCompressionPolicy compressionPolicy =
                compressionHelper.createCompressionPolicy(publicBot, chatCheckResult.getConversationIdStr());
        if (compressionPolicy != null) {
            agentBuilder.compressionPolicy(compressionPolicy);
        }

        Agent agent = agentBuilder.build();
        dynamicAgentLoader.register(agent);

        AgentRunner runner = AgentRunner.builder()
                .chatMemoryProvider(conversationIdStr -> publicChatMemory)
                .build();

        AgentChatEventListener eventListener = new AgentChatEventListener(
                botId.toString(), null, chatCheckResult.getConversationIdStr(),
                chatSseEmitter, true);
        runner.addEventListener(eventListener);

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
        threadPoolTaskExecutor.execute(() -> {
            boolean requestAttributesSet = false;
            try {
                RequestContextHolder.setRequestAttributes(sra, true);
                requestAttributesSet = true;
                runner.run(agent, chatCheckResult.getConversationIdStr(), userMessage,
                        AgentTurnOptions.builder().streaming(true).build());
            } catch (Exception e) {
                log.error("Agent 对话执行异常（startPublicChat）", e);
                eventListener.sendSystemError("对话处理失败: " + e.getMessage());
            } finally {
                if (requestAttributesSet) {
                    RequestContextHolder.resetRequestAttributes();
                }
            }
        });

        return emitter;
    }

    @Override
    public void updateBotLlmId(Bot aiBot) {
        Bot bot = getById(aiBot.getId());

        if (bot == null) {
            log.error("修改bot的llmId失败，bot不存在！");
            throw new BusinessException("bot不存在！");
        }

        bot.setModelId(aiBot.getModelId());

        updateById(bot, false);

    }


    @Override
    public boolean updateById(Bot entity) {
        Bot aiBot = getById(entity.getId());
        if (aiBot == null) {
            throw new BusinessException("bot 不存在");
        }

        CustomBeanUtils.copyPropertiesIgnoreNull(entity, aiBot);

        if ("".equals(aiBot.getAlias())) {
            aiBot.setAlias(null);
        }


        return super.updateById(aiBot, false);
    }

    /**
     * 将会话ID字符串解析为 BigInteger，非数字或为空时返回 null。
     */
    private BigInteger parseConversationId(String conversationIdStr) {
        if (!StringUtils.hasText(conversationIdStr)) {
            return null;
        }
        try {
            return new BigInteger(conversationIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
