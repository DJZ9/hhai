package tech.aiflowy.ai.agentsflex.tool;

import com.agentsflex.core.model.chat.tool.Tool;
import com.agentsflex.core.model.chat.tool.ToolScanner;
import com.agentsflex.core.prompt.MemoryPrompt;
import com.agentsflex.skill.SkillsTool;
import com.agentsflex.skill.artifact.SkillArtifact;
import com.agentsflex.skill.artifact.SkillArtifactStore;
import com.agentsflex.skill.file.FilePublisher;
import com.agentsflex.skill.local.LocalSkillRuntime;
import com.agentsflex.skill.runtime.SkillRuntime;
import com.agentsflex.tool.todowrite.TodoWriteTool;
import com.agentsflex.tool.webfetch.WebFetchTool;
import com.agentsflex.wiki.WikiTool;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.entity.*;
import tech.aiflowy.ai.service.*;
import tech.aiflowy.skill.config.SkillRuntimeConfig;
import tech.aiflowy.skill.entity.BotSkill;
import tech.aiflowy.skill.entity.Skill;
import tech.aiflowy.skill.service.BotSkillService;
import tech.aiflowy.skill.service.SkillService;
import tech.aiflowy.wiki.agentsflex.AIFlowyWikiProvider;
import tech.aiflowy.wiki.entity.BotWiki;
import tech.aiflowy.wiki.entity.Wiki;
import tech.aiflowy.wiki.service.BotWikiService;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.*;

/**
 * Bot 工具列表构建器。
 * <p>
 * 从 BotServiceImpl 中抽取的工具构建逻辑，负责构建 Bot 的全部工具列表，
 * 包括工作流、知识库、插件、MCP、Wiki 和 Skill 工具，
 * 以及按 Bot 配置注入的其他技能工具（联网搜索 / 网页抓取 / 任务分解）。
 * </p>
 *
 * @author wgq
 * @since 2024-08-23
 */
@Component
public class BotToolBuilder {

    private static final Logger log = LoggerFactory.getLogger(BotToolBuilder.class);

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
    @Resource
    private BotWikiService botWikiService;
    @Resource
    private AIFlowyWikiProvider aiFlowyWikiProvider;
    @Resource
    private BotSkillService botSkillService;
    @Resource
    private SkillService skillService;
    @Resource
    private SkillRuntimeConfig skillRuntimeConfig;
    @Resource
    private FilePublisher filePublisher;
    @Resource
    private SkillArtifactStore skillArtifactStore;
    @Resource
    private WebSearchToolHelper webSearchToolHelper;

    /**
     * 构建 Bot 的工具列表。
     * <p>
     * 入参契约：{@code botId} 必填；{@code needEnglishName} 缺省为 false（影响工具名语言）；
     * {@code conversationId} 可选，用于创建 SkillRuntime。
     * </p>
     * <p>
     * 注意：当 Bot 关联了 Skill 时，本方法会把创建出的 {@link SkillRuntime} 写回
     * {@code buildParams} 的 {@code skillRuntime} 键，调用方可在对话结束时关闭它。
     * </p>
     *
     * @param buildParams 构建参数
     * @return 工具列表
     */
    public List<Tool> buildFunctionList(Map<String, Object> buildParams) {

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
                WorkflowTool function = (WorkflowTool) botWorkflow.getWorkflow().toFunction(needEnglishName);
                // 设置Bot自定义输出模板
                String customTemplate = botWorkflow.getOutputTemplate();
                if (customTemplate != null && !customTemplate.isEmpty()) {
                    function.setBotOutputTemplate(customTemplate);
                }
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
        queryWrapper.eq(BotPlugin::getBotId, botId);
        List<BotPlugin> botPlugins = botPluginService.getMapper()
                .selectListWithRelationsByQuery(queryWrapper);
        if (botPlugins != null && !botPlugins.isEmpty()) {
            for (BotPlugin botPlugin : botPlugins) {
                PluginItem pluginItem = pluginItemService.getById(botPlugin.getPluginItemId());
                if (pluginItem != null) {
                    PluginTool function = (PluginTool) pluginItem.toFunction();
                    // 设置Bot自定义输出模板
                    String customTemplate = botPlugin.getOutputTemplate();
                    if (customTemplate != null && !customTemplate.isEmpty()) {
                        function.setBotOutputTemplate(customTemplate);
                    }
                    functionList.add(function);
                }
            }
        }

        // MCP function 集合
        queryWrapper = QueryWrapper.create();
        queryWrapper.eq(BotMcp::getBotId, botId);
        List<BotMcp> botMcpList = botMcpService.getMapper().selectListWithRelationsByQuery(queryWrapper);
        botMcpList.forEach(botMcp -> {
            Tool tool = mcpService.toFunction(botMcp);
            if (tool != null) {
                functionList.add(tool);
            } else {
                log.warn("MCP 工具不可用，已跳过。botId: {}, mcpId: {}, toolName: {}",
                        botId, botMcp.getMcpId(), botMcp.getMcpToolName());
            }
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

       // 加载 Bot 关联的 Skills
        List<BotSkill> botSkills = botSkillService.listByBotId(botId);
        if (botSkills != null && !botSkills.isEmpty()) {
            SkillRuntime runtime = createSkillRuntime((BigInteger) buildParams.get("conversationId"));
            buildParams.put("skillRuntime", runtime);

            SkillsTool.Builder skillToolBuilder = SkillsTool.builder()
                    .runtime(runtime)
                    .filePublisher(filePublisher);
            boolean hasSkill = false;
            try {
                for (BotSkill botSkill : botSkills) {
                    Skill skill = skillService.getById(botSkill.getSkillId());
                    if (skill != null && skill.getName() != null) {
                        try {
                            // 优先使用 SkillArtifactStore，如果可用
                            if (skillArtifactStore != null && skill.getStorageKey() != null) {
                                // 创建 SkillArtifact
                                SkillArtifact artifact = new SkillArtifact(
                                        skill.getName(),
                                        skill.getVersion() != null ? skill.getVersion() : "1.0.0",
                                        skill.getDigest() != null ? skill.getDigest() : "sha256-" + skill.getId(),
                                        skill.getStorageKey()
                                );
                                // 将所有 Skill 的 artifact 累积到同一个 builder，统一构建
                                skillToolBuilder.addSkillArtifact(skillArtifactStore, artifact);
                                hasSkill = true;
                            } else {
                                // 回退到使用目录加载
                                if (skill.getSkillPath() == null) {
                                    log.warn("Skill [{}] 没有 skillPath，跳过加载", skill.getName());
                                    continue;
                                }
                                // 将所有 Skill 的目录累积到同一个 builder，统一构建
                                skillToolBuilder.addSkillsDirectory(String.valueOf(Paths.get(skill.getSkillPath())));
                                hasSkill = true;
                            }
                        } catch (Exception e) {
                            log.error("加载 Skill [{}] 失败: {}", skill.getName(), e.getMessage(), e);
                        }
                    }
                }
                  // 统一构建所有 Skill 的 Tools，确保 runtime 中保留所有 Skill 的上下文
                if (hasSkill) {
                    List<Tool> skillTools = skillToolBuilder.buildTools();
                    if (skillTools != null) {
                        functionList.addAll(skillTools);
                    }
                }
            } catch (Exception e) {
                log.error("加载 Skill 工具失败", e);
            }
        }

        return functionList;
    }

    /**
     * 添加「其他技能」工具（联网搜索、网页抓取、任务分解）。
     * <p>
     * 开关读取自 Bot 的 {@code options.otherSkills}：{@code webSearchEnabled} /
     * {@code webFetchEnabled} / {@code todoWriteEnabled}，未开启或配置缺失时不注入任何工具。
     * </p>
     *
     * @param memoryPrompt 目标 Prompt，工具直接追加到它上面
     * @param aiBot        Bot 实体，为 null 时不做任何处理
     */
    @SuppressWarnings("unchecked")
    public void addOtherSkillsTools(MemoryPrompt memoryPrompt, Bot aiBot) {
        Map<String, Object> botOptions = aiBot == null ? null : aiBot.getOptions();
        if (botOptions == null) {
            return;
        }
        Object otherSkillsObj = botOptions.get("otherSkills");
        if (!(otherSkillsObj instanceof Map<?, ?> otherSkills)) {
            return;
        }
        Map<String, Object> otherSkillsMap = (Map<String, Object>) otherSkills;
        // 搜索引擎工具
        if (Boolean.TRUE.equals(otherSkills.get("webSearchEnabled"))) {
            webSearchToolHelper.addWebSearchTool(memoryPrompt, otherSkillsMap);
        }
        // 网页抓取工具
        if (Boolean.TRUE.equals(otherSkills.get("webFetchEnabled"))) {
            memoryPrompt.addTools(ToolScanner.scan(WebFetchTool.builder().useDefaultProviders().build()));
        }
        // 任务分解工具
        if (Boolean.TRUE.equals(otherSkills.get("todoWriteEnabled"))) {
            memoryPrompt.addTools(ToolScanner.scan(TodoWriteTool.builder().build()));
        }
    }

    private SkillRuntime createSkillRuntime(BigInteger conversationId) {
        String stringConversationId = conversationId != null ? conversationId.toString() : UUID.randomUUID().toString();
        return LocalSkillRuntime.builder()
                .conversationId(stringConversationId)
                .conversationsRoot(skillRuntimeConfig.getLocal().getConversationsRoot())
                .build();
    }
}
