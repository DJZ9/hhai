
package tech.aiflowy.admin.controller.ai;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alicp.jetcache.Cache;
import com.mybatisflex.core.keygen.impl.SnowFlakeIDKeyGenerator;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tech.aiflowy.ai.agentsflex.listener.PromptChoreChatStreamListener;
import tech.aiflowy.ai.entity.*;
import tech.aiflowy.ai.service.*;
import tech.aiflowy.ai.service.impl.BotServiceImpl;
import tech.aiflowy.common.audio.core.AudioServiceManager;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.common.web.jsonbody.JsonBody;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;
import tech.aiflowy.core.chat.protocol.sse.ChatSseUtil;
import tech.aiflowy.wiki.entity.BotWiki;
import tech.aiflowy.wiki.entity.Wiki;
import tech.aiflowy.wiki.service.BotWikiService;

import jakarta.annotation.Resource;
import tech.aiflowy.wiki.service.WikiService;

import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 控制层。
 *
 * @author michael
 * @since 2024-08-23
 */
@RestController
@RequestMapping("/api/v1/bot")
public class BotController extends BaseCurdController<BotService, Bot> {

    private final ModelService modelService;
    private final BotWorkflowService botWorkflowService;
    private final BotDocumentCollectionService botDocumentCollectionService;
    @Resource
    private BotService botService;
    @Autowired
    @Qualifier("defaultCache") // 指定 Bean 名称
    private Cache<String, Object> cache;
    @Resource
    private AudioServiceManager audioServiceManager;
    @Resource
    private BotRecentlyUsedService botRecentlyUsedService;
    @Resource
    private BotMessageService botMessageService;
    @Resource
    private BotMcpService botMcpService;
    @Resource
    private BotWikiService botWikiService;
    @Resource
    private PluginService pluginService;
    @Resource
    private WorkflowService workflowService;
    @Resource
    private DocumentCollectionService documentCollectionService;
    @Resource
    private McpService mcpService;
    @Resource
    private WikiService wikiService;

    public BotController(BotService service, ModelService modelService, BotWorkflowService botWorkflowService,
                         BotDocumentCollectionService botDocumentCollectionService, BotMessageService botMessageService) {
        super(service);
        this.modelService = modelService;
        this.botWorkflowService = botWorkflowService;
        this.botDocumentCollectionService = botDocumentCollectionService;
        this.botMessageService = botMessageService;
    }

    @Resource
    private BotPluginService botPluginService;

    @GetMapping("/generateConversationId")
    public Result<Long> generateConversationId() {
        long nextId = new SnowFlakeIDKeyGenerator().nextId();
        return Result.ok(nextId);
    }

    @PostMapping("updateOptions")
    @SaCheckPermission("/api/v1/bot/save")
    public Result<Void> updateOptions(@JsonBody("id") BigInteger id,
                                      @JsonBody("options") Map<String, Object> options) {
        Bot aiBot = service.getById(id);
        Map<String, Object> existOptions = aiBot.getOptions();
        if (existOptions == null) {
            existOptions = new HashMap<>();
        }
        if (options != null) {
            existOptions.putAll(options);
        }
        aiBot.setOptions(existOptions);
        service.updateById(aiBot);
        return Result.ok();
    }

    @PostMapping("updateLlmOptions")
    @SaCheckPermission("/api/v1/bot/save")
    public Result<Void> updateLlmOptions(@JsonBody("id")
                                         BigInteger id, @JsonBody("llmOptions")
                                         Map<String, Object> llmOptions) {
        Bot aiBot = service.getById(id);
        Map<String, Object> existLlmOptions = aiBot.getModelOptions();
        if (existLlmOptions == null) {
            existLlmOptions = new HashMap<>();
        }
        if (llmOptions != null) {
            existLlmOptions.putAll(llmOptions);
        }
        aiBot.setModelOptions(existLlmOptions);
        service.updateById(aiBot);
        return Result.ok();
    }

    @PostMapping("voiceInput")
    @SaIgnore
    public Result<String> voiceInput(@RequestParam("audio")
                                     MultipartFile audioFile) {

        String recognize = null;
        try {
            recognize = audioServiceManager.audioToText(audioFile.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Result.ok("", recognize);
    }

    /**
     * 处理聊天请求的接口方法
     *
     * @param prompt    用户输入的聊天内容，必须提供
     * @param botId     聊天机器人的唯一标识符，必须提供
     * @param conversationId 会话ID，用于标识当前对话会话，必须提供
     * @param messages  历史消息，用于提供上下文，可选
     * @return 返回SseEmitter对象，用于服务器向客户端推送聊天响应数据
     */
    @PostMapping("chat")
    @SaIgnore
    public SseEmitter chat(
            @JsonBody(value = "prompt", required = true) String prompt,
            @JsonBody(value = "botId", required = true) BigInteger botId,
            @JsonBody(value = "conversationId", required = true) BigInteger conversationId,
            @JsonBody(value = "messages") List<Map<String, String>>  messages,
            @JsonBody(value = "attachments") List<String> attachments

    ) {
        BotServiceImpl.ChatCheckResult chatCheckResult = new BotServiceImpl.ChatCheckResult();

        // 前置校验：失败则直接返回错误SseEmitter
        SseEmitter errorEmitter = botService.checkChatBeforeStart(botId, prompt, conversationId.toString(), chatCheckResult);
        if (errorEmitter != null) {
            return errorEmitter;
        }
        return botService.startChat(botId, prompt, conversationId, messages, chatCheckResult, attachments);
    }

    @PostMapping("updateLlmId")
    @SaCheckPermission("/api/v1/bot/save")
    public Result<Void> updateBotLlmId(@RequestBody
                                       Bot aiBot) {
        service.updateBotLlmId(aiBot);
        return Result.ok();
    }

    /**
     * 导入Bot
     *
     * @param bot Bot基本信息
     * @param jsonFile JSON配置文件
     * @return 操作结果
     */
    @PostMapping("/importBot")
    @SaCheckPermission("/api/v1/bot/save")
    public Result<Void> importBot(Bot bot, MultipartFile jsonFile) {
        if (jsonFile == null || jsonFile.isEmpty()) {
            throw new BusinessException("请上传JSON配置文件");
        }

        try {
            InputStream is = jsonFile.getInputStream();
            String content = IoUtil.read(is, StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(content);

            // 从JSON中提取配置
            if (json.containsKey("options")) {
                bot.setOptions(json.getJSONObject("options").getInnerMap());
            } else {
                bot.setOptions(getDefaultOptions());
            }

            if (json.containsKey("modelOptions")) {
                bot.setModelOptions(json.getJSONObject("modelOptions").getInnerMap());
            } else {
                bot.setModelOptions(getDefaultLlmOptions());
            }

            // 从JSON中提取modelId
            if (json.containsKey("modelId") && json.getBigInteger("modelId") != null) {
                BigInteger modelId = json.getBigInteger("modelId");
                // 验证模型是否存在
                Model model = modelService.getById(modelId);
                if (model != null) {
                    bot.setModelId(modelId);
                }
            }

            // 生成新的别名，避免冲突
            if (!StringUtils.hasLength(bot.getAlias())) {
                bot.setAlias(IdUtil.fastSimpleUUID());
            } else {
                // 检查别名是否存在，存在则生成新的
                Bot existBot = service.getByAlias(bot.getAlias());
                if (existBot != null) {
                    bot.setAlias(IdUtil.fastSimpleUUID());
                }
            }

            // 设置公共字段（dept_id, tenant_id, created_by 等）
            LoginAccount account = SaTokenUtil.getLoginAccount();
            commonFiled(bot, account.getId(), account.getTenantId(), account.getDeptId());

            // 保存Bot
            service.save(bot);

            // 导入技能模块关联数据
            importSkillModules(bot.getId(), json);

            return Result.ok();
        } catch (Exception e) {
            throw new BusinessException("导入失败：" + e.getMessage());
        }
    }

    /**
     * 导出Bot
     *
     * @param id Bot ID
     * @return Bot配置JSON
     */
    @GetMapping("/exportBot")
    @SaCheckPermission("/api/v1/bot/save")
    public Result<String> exportBot(@RequestParam BigInteger id) {
        Bot bot = service.getById(id);
        if (bot == null) {
            throw new BusinessException("Bot不存在");
        }

        // 构建导出JSON
        JSONObject json = new JSONObject();
        json.put("title", bot.getTitle());
        json.put("alias", bot.getAlias());
        json.put("description", bot.getDescription());
        json.put("icon", bot.getIcon());
        json.put("categoryId", bot.getCategoryId());
        json.put("status", bot.getStatus());
        json.put("modelId", bot.getModelId());
        json.put("options", bot.getOptions());
        json.put("modelOptions", bot.getModelOptions());

        // 导出技能模块关联数据
        exportSkillModules(id, json);

        return Result.ok("", json.toJSONString());
    }


    @GetMapping("getDetail")
    @SaIgnore
    public Result<Bot> getDetail(String id) {
        return Result.ok(botService.getDetail(id));
    }

    @Override
    @SaIgnore
    public Result<Bot> detail(String id) {
        Bot data = botService.getDetail(id);
        if (data == null) {
            return Result.ok(data);
        }

        Map<String, Object> llmOptions = data.getModelOptions();
        if (llmOptions == null) {
            llmOptions = new HashMap<>();
        }

        if (data.getModelId() == null) {
            return Result.ok(data);
        }

        BigInteger llmId = data.getModelId();
        Model llm = modelService.getById(llmId);

        if (llm == null) {
            data.setModelId(null);
            return Result.ok(data);
        }

        Map<String, Object> options = llm.getOptions();

        if (options != null && !options.isEmpty()) {

            // 获取是否多模态
            Boolean multimodal = (Boolean) options.get("multimodal");
            llmOptions.put("multimodal", multimodal != null && multimodal);

        }

        return Result.ok(data);
    }

    @Override
    protected Result<?> onSaveOrUpdateBefore(Bot entity, boolean isSave) {

        String alias = entity.getAlias();

        if (StringUtils.hasLength(alias)) {
            Bot aiBot = service.getByAlias(alias);


            if (aiBot != null && isSave) {
                throw new BusinessException("别名已存在！");
            }

            if (aiBot != null && aiBot.getId().compareTo(entity.getId()) != 0) {
                throw new BusinessException("别名已存在！");
            }

        }


        if (isSave) {
            // 设置默认值
            entity.setModelOptions(getDefaultLlmOptions());
        }
        return super.onSaveOrUpdateBefore(entity, isSave);
    }

    private ChatOptions getChatOptions(Map<String, Object> llmOptions) {
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

    private Map<String, Object> getDefaultLlmOptions() {
        Map<String, Object> defaultLlmOptions = new HashMap<>();
        defaultLlmOptions.put("temperature", 0.7);
        defaultLlmOptions.put("topK", 4);
        defaultLlmOptions.put("maxReplyLength", 20000);
        defaultLlmOptions.put("topP", 0.7);
        defaultLlmOptions.put("maxMessageCount", 100);
        return defaultLlmOptions;
    }

    private Map<String, Object> errorRespnseMsg(int errorCode, String message) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("error", errorCode);
        result.put("message", message);
        return result;
    }

    @Override
    protected Result<?> onRemoveBefore(Collection<Serializable> ids) {
        QueryWrapper queryWrapperKnowledge = QueryWrapper.create().in(BotDocumentCollection::getBotId, ids);
        botDocumentCollectionService.remove(queryWrapperKnowledge);
        QueryWrapper queryWrapperBotWorkflow = QueryWrapper.create().in(BotWorkflow::getBotId, ids);
        botWorkflowService.remove(queryWrapperBotWorkflow);
        QueryWrapper queryWrapperBotPlugins = QueryWrapper.create().in(BotPlugin::getBotId, ids);
        botPluginService.remove(queryWrapperBotPlugins);
        botRecentlyUsedService.removeRecentlyAllUsed(ids.stream()
                .filter(Objects::nonNull)
                .map(id -> new BigInteger(String.valueOf(id)))
                .collect(Collectors.toList()));
        // 删除bot对应的所有用户的消息
        botMessageService.remove(QueryWrapper.create().in(BotMessage::getBotId, ids));
        // 删除bot对应mcp
        botMcpService.remove(QueryWrapper.create().in(BotMcp::getBotId, ids));
        // 删除bot对应wiki
        botWikiService.remove(QueryWrapper.create().in(BotWiki::getBotId, ids));
        return super.onRemoveBefore(ids);
    }

    /**
     * 系统提示词优化
     *
     * @param prompt
     * @return
     */
    @PostMapping("prompt/chore/chat")
    @SaIgnore
    public SseEmitter chat(
            @JsonBody(value = "prompt", required = true) String prompt,
            @JsonBody(value = "botId", required = true) BigInteger botId
    ){
        if (!StringUtils.hasLength(prompt)) {
            throw new BusinessException("提示词不能为空！");
        }
        String promptChore = "# 角色与目标\n" +
                "\n" +
                "你是一位专业的提示词工程师（Prompt Engineer）。你的任务是，分析我提供的“用户原始提示词”，并将其优化成一个结构清晰、指令明确、效果最优的“系统提示词（System Prompt）”。\n" +
                "\n" +
                "这个优化后的系统提示词将直接用于引导一个AI助手，使其能够精准、高效地完成用户的请求。\n" +
                "\n" +
                "# 优化指南 (请严格遵循)\n" +
                "\n" +
                "在优化过程中，请遵循以下原则，以确保最终提示词的质量：\n" +
                "\n" +
                "1.  **角色定义 (Role Definition)**：\n" +
                "    *   为AI助手明确一个具体、专业的角色。这个角色应该与任务高度相关。\n" +
                "    *   例如：“你是一位资深的软件架构师”、“你是一位经验丰富的产品经理”。\n" +
                "\n" +
                "2.  **任务与目标 (Task & Goal)**：\n" +
                "    *   清晰、具体地描述AI需要完成的任务。\n" +
                "    *   明确指出期望的最终输出是什么，以及输出的目标和用途。\n" +
                "    *   避免模糊不清的指令。\n" +
                "\n" +
                "3.  **输入输出格式 (Input/Output Format)**：\n" +
                "    *   如果任务涉及到处理特定格式的数据，请明确说明输入数据的格式。\n" +
                "    *   **至关重要**：请为AI的输出指定一个清晰、结构化的格式。这能极大地提升结果的可用性。\n" +
                "    *   例如：“请以Markdown表格的形式输出”、“请分点列出，每点不超过20字”。\n" +
                "\n" +
                "4.  **背景与上下文 (Context & Background)**：\n" +
                "    *   提供完成任务所必需的背景信息。\n" +
                "    *   例如：项目的阶段、目标用户、使用的技术栈、相关的约束条件等。\n" +
                "\n" +
                "5.  **语气与风格 (Tone & Style)**：\n" +
                "    *   指定AI回答时应采用的语气和风格。\n" +
                "    *   例如：“专业且简洁”、“通俗易懂，避免使用专业术语”。\n" +
                "\n" +
                "6.  **约束与规则 (Constraints & Rules)**：\n" +
                "    *   设定AI在回答时必须遵守的规则和限制。\n" +
                "    *   例如：“回答必须基于提供的文档”、“禁止猜测用户未提及的信息”。\n" +
                "\n" +
                "7.  **思考过程 (Chain-of-Thought)**：\n" +
                "    *   对于复杂的推理任务，可以引导AI展示其思考过程。\n" +
                "    *   例如：“请先分析问题的关键点，然后给出解决方案，并解释你的推理步骤。”\n" +
                "\n" +
                "# 输出要求\n" +
                "\n" +
                "请你直接输出优化后的完整系统提示词。不要包含任何额外的解释或说明。\n" +
                "\n" +
                "---\n" +
                "\n" +
                "## 用户原始提示词\n" +
                "[" + prompt + "]\n";
        Bot aiBot = service.getById(botId);
        if (aiBot == null) {
            return ChatSseUtil.sendSystemError(null, "聊天助手不存在");
        }
        ChatSseEmitter sseEmitter = new ChatSseEmitter();
        Model model = modelService.getModelInstance(aiBot.getModelId());
        if (model == null) {
            return ChatSseUtil.sendSystemError(null, "模型不存在");
        }
        ChatModel chatModel = model.toChatModel(null);
        if (chatModel == null) {
            return ChatSseUtil.sendSystemError(null, "模型不存在");
        }
        PromptChoreChatStreamListener promptChoreChatStreamListener = new PromptChoreChatStreamListener(sseEmitter);
        chatModel.chatStream(promptChore, promptChoreChatStreamListener);
        return sseEmitter.getEmitter();
    }

    private Map<String, Object> getDefaultOptions() {
        Map<String, Object> defaultOptions = new HashMap<>();
        defaultOptions.put(Bot.KEY_EXPOSE_DEEP_THINKING, true);
        // 默认开启附件功能
        defaultOptions.put(Bot.KEY_ATTACHMENT_ENABLED, true);
        // 默认使用当前对话模型作为VL模型
        defaultOptions.put(Bot.KEY_VL_MODEL_ID, "current");
        return defaultOptions;
    }

    /**
     * 导入技能模块关联数据
     */
    private void importSkillModules(BigInteger botId, JSONObject json) {
        // 导入插件关联
        if (json.containsKey("plugins")) {
            com.alibaba.fastjson.JSONArray plugins = json.getJSONArray("plugins");
            if (plugins != null) {
                for (int i = 0; i < plugins.size(); i++) {
                    JSONObject pluginJson = plugins.getJSONObject(i);
                    if (pluginJson.containsKey("pluginItemId")) {
                        BigInteger pluginItemId = pluginJson.getBigInteger("pluginItemId");
                        // 校验插件是否存在
                        Plugin plugin = pluginService.getById(pluginItemId);
                        if (plugin == null) {
                            continue; // 插件不存在，跳过该条数据
                        }
                        BotPlugin botPlugin = new BotPlugin();
                        botPlugin.setBotId(botId);
                        botPlugin.setPluginItemId(pluginItemId);
                        if (pluginJson.containsKey("options")) {
                            botPlugin.setOptions(pluginJson.getJSONObject("options").getInnerMap());
                        }
                        botPluginService.save(botPlugin);
                    }
                }
            }
        }

        // 导入工作流关联
        if (json.containsKey("workflows")) {
            com.alibaba.fastjson.JSONArray workflows = json.getJSONArray("workflows");
            if (workflows != null) {
                for (int i = 0; i < workflows.size(); i++) {
                    JSONObject workflowJson = workflows.getJSONObject(i);
                    if (workflowJson.containsKey("workflowId")) {
                        BigInteger workflowId = workflowJson.getBigInteger("workflowId");
                        // 校验工作流是否存在
                        Workflow workflow = workflowService.getById(workflowId);
                        if (workflow == null) {
                            continue; // 工作流不存在，跳过该条数据
                        }
                        BotWorkflow botWorkflow = new BotWorkflow();
                        botWorkflow.setBotId(botId);
                        botWorkflow.setWorkflowId(workflowId);
                        if (workflowJson.containsKey("options")) {
                            botWorkflow.setOptions(workflowJson.getJSONObject("options").getInnerMap());
                        }
                        botWorkflowService.save(botWorkflow);
                    }
                }
            }
        }

        // 导入知识库关联
        if (json.containsKey("documentCollections")) {
            com.alibaba.fastjson.JSONArray documentCollections = json.getJSONArray("documentCollections");
            if (documentCollections != null) {
                for (int i = 0; i < documentCollections.size(); i++) {
                    JSONObject dcJson = documentCollections.getJSONObject(i);
                    if (dcJson.containsKey("documentCollectionId")) {
                        BigInteger documentCollectionId = dcJson.getBigInteger("documentCollectionId");
                        // 校验知识库是否存在
                        DocumentCollection documentCollection = documentCollectionService.getById(documentCollectionId);
                        if (documentCollection == null) {
                            continue; // 知识库不存在，跳过该条数据
                        }
                        BotDocumentCollection botDc = new BotDocumentCollection();
                        botDc.setBotId(botId);
                        botDc.setDocumentCollectionId(documentCollectionId);
                        if (dcJson.containsKey("options")) {
                            botDc.setOptions(dcJson.getJSONObject("options").getInnerMap());
                        }
                        botDocumentCollectionService.save(botDc);
                    }
                }
            }
        }

        // 导入MCP关联
        if (json.containsKey("mcps")) {
            com.alibaba.fastjson.JSONArray mcps = json.getJSONArray("mcps");
            if (mcps != null) {
                for (int i = 0; i < mcps.size(); i++) {
                    JSONObject mcpJson = mcps.getJSONObject(i);
                    if (mcpJson.containsKey("mcpId")) {
                        BigInteger mcpId = mcpJson.getBigInteger("mcpId");
                        // 校验MCP是否存在
                        Mcp mcp = mcpService.getById(mcpId);
                        if (mcp == null) {
                            continue; // MCP不存在，跳过该条数据
                        }
                        BotMcp botMcp = new BotMcp();
                        botMcp.setBotId(botId);
                        botMcp.setMcpId(mcpId);
                        if (mcpJson.containsKey("mcpToolName")) {
                            botMcp.setMcpToolName(mcpJson.getString("mcpToolName"));
                        }
                        if (mcpJson.containsKey("mcpToolDescription")) {
                            botMcp.setMcpToolDescription(mcpJson.getString("mcpToolDescription"));
                        }
                        botMcpService.save(botMcp);
                    }
                }
            }
        }

        // 导入Wiki关联
        if (json.containsKey("wikis")) {
            com.alibaba.fastjson.JSONArray wikis = json.getJSONArray("wikis");
            if (wikis != null) {
                for (int i = 0; i < wikis.size(); i++) {
                    JSONObject wikiJson = wikis.getJSONObject(i);
                    if (wikiJson.containsKey("wikiId")) {
                        BigInteger wikiId = wikiJson.getBigInteger("wikiId");
                        // 校验Wiki是否存在
                        Wiki wiki = wikiService.getById(wikiId);
                        if (wiki == null) {
                            continue; // Wiki不存在，跳过该条数据
                        }
                        BotWiki botWiki = new BotWiki();
                        botWiki.setBotId(botId);
                        botWiki.setWikiId(wikiId);
                        if (wikiJson.containsKey("options")) {
                            botWiki.setOptions(wikiJson.getJSONObject("options").getInnerMap());
                        }
                        botWikiService.save(botWiki);
                    }
                }
            }
        }
    }

    /**
     * 导出技能模块关联数据
     */
    private void exportSkillModules(BigInteger botId, JSONObject json) {
        // 导出插件关联
        QueryWrapper pluginQuery = QueryWrapper.create().eq(BotPlugin::getBotId, botId);
        List<BotPlugin> plugins = botPluginService.list(pluginQuery);
        if (plugins != null && !plugins.isEmpty()) {
            com.alibaba.fastjson.JSONArray pluginsArray = new com.alibaba.fastjson.JSONArray();
            for (BotPlugin plugin : plugins) {
                JSONObject pluginJson = new JSONObject();
                pluginJson.put("pluginItemId", plugin.getPluginItemId());
                pluginJson.put("options", plugin.getOptions());
                pluginsArray.add(pluginJson);
            }
            json.put("plugins", pluginsArray);
        }

        // 导出工作流关联
        QueryWrapper workflowQuery = QueryWrapper.create().eq(BotWorkflow::getBotId, botId);
        List<BotWorkflow> workflows = botWorkflowService.list(workflowQuery);
        if (workflows != null && !workflows.isEmpty()) {
            com.alibaba.fastjson.JSONArray workflowsArray = new com.alibaba.fastjson.JSONArray();
            for (BotWorkflow workflow : workflows) {
                JSONObject workflowJson = new JSONObject();
                workflowJson.put("workflowId", workflow.getWorkflowId());
                workflowJson.put("options", workflow.getOptions());
                workflowsArray.add(workflowJson);
            }
            json.put("workflows", workflowsArray);
        }

        // 导出知识库关联
        QueryWrapper dcQuery = QueryWrapper.create().eq(BotDocumentCollection::getBotId, botId);
        List<BotDocumentCollection> documentCollections = botDocumentCollectionService.list(dcQuery);
        if (documentCollections != null && !documentCollections.isEmpty()) {
            com.alibaba.fastjson.JSONArray dcArray = new com.alibaba.fastjson.JSONArray();
            for (BotDocumentCollection dc : documentCollections) {
                JSONObject dcJson = new JSONObject();
                dcJson.put("documentCollectionId", dc.getDocumentCollectionId());
                dcJson.put("options", dc.getOptions());
                dcArray.add(dcJson);
            }
            json.put("documentCollections", dcArray);
        }

        // 导出MCP关联
        QueryWrapper mcpQuery = QueryWrapper.create().eq(BotMcp::getBotId, botId);
        List<BotMcp> mcps = botMcpService.list(mcpQuery);
        if (mcps != null && !mcps.isEmpty()) {
            com.alibaba.fastjson.JSONArray mcpsArray = new com.alibaba.fastjson.JSONArray();
            for (BotMcp mcp : mcps) {
                JSONObject mcpJson = new JSONObject();
                mcpJson.put("mcpId", mcp.getMcpId());
                mcpJson.put("mcpToolName", mcp.getMcpToolName());
                mcpJson.put("mcpToolDescription", mcp.getMcpToolDescription());
                mcpsArray.add(mcpJson);
            }
            json.put("mcps", mcpsArray);
        }

        // 导出Wiki关联
        QueryWrapper wikiQuery = QueryWrapper.create().eq(BotWiki::getBotId, botId);
        List<BotWiki> wikis = botWikiService.list(wikiQuery);
        if (wikis != null && !wikis.isEmpty()) {
            com.alibaba.fastjson.JSONArray wikisArray = new com.alibaba.fastjson.JSONArray();
            for (BotWiki wiki : wikis) {
                JSONObject wikiJson = new JSONObject();
                wikiJson.put("wikiId", wiki.getWikiId());
                wikiJson.put("options", wiki.getOptions());
                wikisArray.add(wikiJson);
            }
            json.put("wikis", wikisArray);
        }
    }

}
