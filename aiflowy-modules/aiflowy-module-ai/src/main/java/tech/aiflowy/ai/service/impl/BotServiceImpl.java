package tech.aiflowy.ai.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.agentsflex.core.memory.ChatMemory;
import com.agentsflex.core.message.Message;
import com.agentsflex.core.message.SystemMessage;
import com.agentsflex.core.message.UserMessage;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.agentsflex.core.model.chat.StreamResponseListener;
import com.agentsflex.core.model.chat.tool.Tool;
import com.agentsflex.core.prompt.MemoryPrompt;
import com.agentsflex.doc.DocumentExtractionService;
import com.agentsflex.wiki.WikiTool;
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
import tech.aiflowy.ai.agentsflex.listener.ChatStreamListener;
import tech.aiflowy.ai.agentsflex.memory.BotMessageMemory;
import tech.aiflowy.ai.agentsflex.memory.DefaultBotMessageMemory;
import tech.aiflowy.ai.agentsflex.memory.PublicBotMessageMemory;
import tech.aiflowy.ai.entity.*;
import tech.aiflowy.ai.mapper.BotMapper;
import tech.aiflowy.ai.service.*;
import tech.aiflowy.ai.utils.CustomBeanUtils;
import tech.aiflowy.ai.utils.RegexUtils;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.filestorage.utils.PathGeneratorUtil;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.util.MapUtil;
import tech.aiflowy.common.util.Maps;
import tech.aiflowy.common.util.UrlEncoderUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;
import tech.aiflowy.core.chat.protocol.sse.ChatSseUtil;
import tech.aiflowy.wiki.agentsflex.AIFlowyWikiProvider;
import tech.aiflowy.wiki.entity.BotWiki;
import tech.aiflowy.wiki.entity.Wiki;
import tech.aiflowy.wiki.service.BotWikiService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static tech.aiflowy.ai.entity.table.BotPluginTableDef.BOT_PLUGIN;
import static tech.aiflowy.ai.entity.table.PluginItemTableDef.PLUGIN_ITEM;

/**
 *  服务层实现。
 *
 * @author michael
 * @since 2024-08-23
 */
@Service
public class BotServiceImpl extends ServiceImpl<BotMapper, Bot> implements BotService {

    private static final Logger log = LoggerFactory.getLogger(BotServiceImpl.class);

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

    @Resource(name = "sseThreadPool")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private ModelService modelService;
    @Resource
    private BotWorkflowService botWorkflowService;
    @Resource
    private BotDocumentCollectionService botDocumentCollectionService;
    @Resource
    private BotPluginService botPluginService;
    @Resource
    private PluginItemService pluginItemService;
    @Resource
    private BotMcpService botMcpService;
    @Resource
    private McpService mcpService;
    @Resource(name = "default")
    FileStorageService storageService;
    @Resource
    private BotWikiService botWikiService;
    @Resource
    private AIFlowyWikiProvider aiFlowyWikiProvider;
    @Resource
    private DocumentExtractionService documentExtractionService;

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
        boolean hasImageAttachment = hasImageInAttachments(attachments);
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

    /**
     * 检测attachments中是否有图片
     * 图片格式包括：jpg, jpeg, png, gif, bmp, webp, svg
     */
    private boolean hasImageInAttachments(List<String> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return false;
        }
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg"};
        for (String attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            String lowerCaseUrl = attachment.toLowerCase();
            for (String ext : imageExtensions) {
                if (lowerCaseUrl.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public SseEmitter startChat(BigInteger botId, String prompt, BigInteger conversationId, List<Map<String, String>> messages,
                                BotServiceImpl.ChatCheckResult chatCheckResult, List<String> attachments) {
        Map<String, Object> modelOptions = chatCheckResult.getModelOptions();
        // 如果有图片附件且配置了VL模型，使用VL模型
        ChatModel chatModel;
        if (chatCheckResult.hasImageAttachment() && chatCheckResult.getVlChatModel() != null) {
            chatModel = chatCheckResult.getVlChatModel();
            log.info("使用VL模型处理图片对话");
        } else {
            chatModel = chatCheckResult.getChatModel();
        }
        final MemoryPrompt memoryPrompt = new MemoryPrompt();
        String systemPrompt = MapUtil.getString(modelOptions, Bot.KEY_SYSTEM_PROMPT);
        Integer maxMessageCount = MapUtil.getInteger(modelOptions, Bot.KEY_MAX_MESSAGE_COUNT);
        if (maxMessageCount != null) {
            memoryPrompt.setMaxAttachedMessageCount(maxMessageCount);
        }
        if (StringUtils.hasLength(systemPrompt)) {
            memoryPrompt.setSystemMessage(SystemMessage.of(systemPrompt));
        }

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
        String nonImageAttachmentsText = attachmentsToString(nonImageAttachments);
        if (StringUtils.hasLength(nonImageAttachmentsText)) {
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
            if (StringUtils.hasLength(nonImageAttachmentsText)) {
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
        memoryPrompt.addTools(buildFunctionList(Maps.of("botId", botId).set("needEnglishName", false)));
        ChatOptions chatOptions = getChatOptions(modelOptions);
        Boolean enableDeepThinking = MapUtil.getBoolean(modelOptions, Bot.KEY_ENABLE_DEEP_THINKING, false);
        chatOptions.setThinkingEnabled(enableDeepThinking);
        Boolean exposeDeepThinking = MapUtil.getBoolean(modelOptions, Bot.KEY_EXPOSE_DEEP_THINKING, false);
        LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
        ChatSseEmitter chatSseEmitter = new ChatSseEmitter();
        SseEmitter emitter = chatSseEmitter.getEmitter();
        if (messages != null && !messages.isEmpty()) {
            ChatMemory defaultChatMemory = new DefaultBotMessageMemory(conversationId, chatSseEmitter, messages);
            memoryPrompt.setMemory(defaultChatMemory);
        } else {
            BotMessageMemory memory = new BotMessageMemory(botId, loginAccount.getId(), conversationId, botMessageService);
            memoryPrompt.setMemory(memory);
        }
        memoryPrompt.addMessage(userMessage);
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        final boolean finalExposeDeepThinking = exposeDeepThinking;
        threadPoolTaskExecutor.execute(() -> {
            ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
            RequestContextHolder.setRequestAttributes(sra, true);
            StreamResponseListener streamResponseListener = new ChatStreamListener(
                    botId.toString(),
                    loginAccount.getId().toString(),
                    conversationId.toString(),
                    chatModel,
                    memoryPrompt,
                    chatSseEmitter,
                    chatOptions,
                    finalExposeDeepThinking,
                    loginAccount);
            chatModel.chatStream(memoryPrompt, streamResponseListener, chatOptions);
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
        ChatOptions chatOptions = getChatOptions(modelOptions);
        ChatModel chatModel = chatCheckResult.getChatModel();
        UserMessage userMessage = new UserMessage(prompt);
        ChatSseEmitter chatSseEmitter = new ChatSseEmitter();
        SseEmitter emitter = chatSseEmitter.getEmitter();
        ChatMemory defaultChatMemory = new PublicBotMessageMemory(chatSseEmitter, messages);
        final MemoryPrompt memoryPrompt = new MemoryPrompt();
        memoryPrompt.setMemory(defaultChatMemory);
        memoryPrompt.addMessage(userMessage);
        memoryPrompt.addTools(buildFunctionList(Maps.of("botId", botId)
                .set("needEnglishName", false)
                .set("needAccountId", false)
        ));
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;
        threadPoolTaskExecutor.execute(() -> {
            RequestContextHolder.setRequestAttributes(sra, true);
            StreamResponseListener streamResponseListener = new ChatStreamListener(
                    botId.toString(),
                    null,
                    chatCheckResult.getConversationIdStr(),
                    chatModel,
                    memoryPrompt,
                    chatSseEmitter,
                    chatOptions,
                    false,
                    null);
            chatModel.chatStream(memoryPrompt, streamResponseListener, chatOptions);
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

    public static ChatOptions getChatOptions(Map<String, Object> llmOptions) {
        ChatOptions defaultOptions = new ChatOptions();
        if (llmOptions != null) {
            Object topK = llmOptions.get("topK");
            Object maxReplyLength = llmOptions.get("maxReplyLength");
            Object temperature = llmOptions.get("temperature");
            Object topP = llmOptions.get("topP");
            Object thinkingEnabled = llmOptions.get("thinkingEnabled");

            if (topK != null) {
                defaultOptions.setTopK(Integer.parseInt(String.valueOf(topK)));
            }
            if (maxReplyLength != null) {
                defaultOptions.setMaxTokens(Integer.parseInt(String.valueOf(maxReplyLength)));
            }
            if (temperature != null) {
                defaultOptions.setTemperature(Float.parseFloat(String.valueOf(temperature)));
            }
            if (topP != null) {
                defaultOptions.setTopP(Float.parseFloat(String.valueOf(topP)));
            }
            if (thinkingEnabled != null) {
                defaultOptions.setThinkingEnabled(Boolean.parseBoolean(String.valueOf(thinkingEnabled)));
            }

        }
        return defaultOptions;
    }

    private List<Tool> buildFunctionList(Map<String, Object> buildParams) {

        if (buildParams == null || buildParams.isEmpty()) {
            throw new IllegalArgumentException("buildParams is empty");
        }

        List<Tool> functionList = new ArrayList<>();

        BigInteger botId = (BigInteger) buildParams.get("botId");
        if (botId == null) {
            throw new IllegalArgumentException("botId is empty");
        }
        Boolean needEnglishName = (Boolean) buildParams.get("needEnglishName");
        if (needEnglishName == null) {
            needEnglishName = false;
        }

        QueryWrapper queryWrapper = QueryWrapper.create();

        // 工作流 function 集合
        queryWrapper.eq(BotWorkflow::getBotId, botId);
        List<BotWorkflow> botWorkflows = botWorkflowService.getMapper()
                .selectListWithRelationsByQuery(queryWrapper);
        if (botWorkflows != null && !botWorkflows.isEmpty()) {
            for (BotWorkflow botWorkflow : botWorkflows) {
                Tool function = botWorkflow.getWorkflow().toFunction(needEnglishName);
                functionList.add(function);
            }
        }

        // 知识库 function 集合
        queryWrapper = QueryWrapper.create();
        queryWrapper.eq(BotDocumentCollection::getBotId, botId);
        List<BotDocumentCollection> botDocumentCollections = botDocumentCollectionService.getMapper()
                .selectListWithRelationsByQuery(queryWrapper);
        if (botDocumentCollections != null && !botDocumentCollections.isEmpty()) {
            for (BotDocumentCollection botDocumentCollection : botDocumentCollections) {
                Tool function = botDocumentCollection.getKnowledge().toFunction(needEnglishName);
                functionList.add(function);
            }
        }

        // 插件 function 集合
        queryWrapper = QueryWrapper.create();
        queryWrapper.select(BOT_PLUGIN.PLUGIN_ITEM_ID).eq(BotPlugin::getBotId, botId);
        List<BigInteger> pluginToolIds = botPluginService.getMapper()
                .selectListWithRelationsByQueryAs(queryWrapper, BigInteger.class);
        if (pluginToolIds != null && !pluginToolIds.isEmpty()) {
            QueryWrapper queryTool = QueryWrapper.create()
                    .select(PLUGIN_ITEM.ALL_COLUMNS)
                    .from(PLUGIN_ITEM)
                    .where(PLUGIN_ITEM.ID.in(pluginToolIds));
            List<PluginItem> pluginItems = pluginItemService.getMapper().selectListWithRelationsByQuery(queryTool);
            if (pluginItems != null && !pluginItems.isEmpty()) {
                for (PluginItem pluginItem : pluginItems) {
                    functionList.add(pluginItem.toFunction());
                }
            }
        }

        // MCP function 集合
        queryWrapper = QueryWrapper.create();
        queryWrapper.eq(BotMcp::getBotId, botId);
        List<BotMcp> botMcpList = botMcpService.getMapper().selectListWithRelationsByQuery(queryWrapper);
        botMcpList.forEach(botMcp -> {
            Tool tool = mcpService.toFunction(botMcp);
            functionList.add(tool);
        });

        // Wiki
        queryWrapper = QueryWrapper.create();
        queryWrapper.eq(BotWiki::getBotId, botId);
        List<BotWiki> botWikis = botWikiService.getMapper().selectListWithRelationsByQuery(queryWrapper);
        if (botWikis != null && !botWikis.isEmpty()) {
            List<com.agentsflex.wiki.Wiki> afWikiList = new ArrayList<>();
            for (BotWiki botWiki : botWikis) {
                Wiki wiki = botWiki.getWiki();
                com.agentsflex.wiki.Wiki afWiki = new com.agentsflex.wiki.Wiki();
                afWiki.setPath(wiki.getId().toString());
                afWiki.setTitle(wiki.getTitle());
                afWiki.setSummary(wiki.getDescription());
                // afWiki.setContent(wiki.getContent());
                afWikiList.add(afWiki);
            }
            Tool wikiTool = WikiTool.builder()
                    .wikiProvider(aiFlowyWikiProvider)
                    .addWikis(afWikiList)
                    .build();
            functionList.add(wikiTool);
        }

        return functionList;
    }

    public String attachmentsToString(List<String> fileList) {
        StringBuilder messageBuilder = new StringBuilder();
        if (fileList != null && !fileList.isEmpty()) {
            for (int i = 0; i < fileList.size(); i++) {
                String fileUrl = fileList.get(i);
                String encodedUrl = UrlEncoderUtil.getEncodedUrl(fileUrl);
                String result = documentExtractionService.extract(new com.agentsflex.doc.source.HttpDocumentSource(encodedUrl));
                if (result != null) {
                    if (i > 0) {
                        messageBuilder.append("\n\n");
                    }
                    messageBuilder.append("附件").append(i + 1).append("，文件名为：").append(PathGeneratorUtil.getPureFileName(fileUrl)).append("，访问地址为：").append(fileUrl).append("，内容为：  \n").append(result);
                }
                storageService.delete(fileUrl);
            }
        }
        return messageBuilder.toString();
    }

}
