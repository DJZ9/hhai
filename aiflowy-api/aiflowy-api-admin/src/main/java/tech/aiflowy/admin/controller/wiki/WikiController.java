package tech.aiflowy.admin.controller.wiki;

import com.agentsflex.core.file2text.File2TextUtil;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.agentsflex.wiki.WikiTool;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.apache.tika.io.FilenameUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tech.aiflowy.admin.controller.wiki.event.WikiUpdateEvent;
import tech.aiflowy.ai.agentsflex.listener.PromptChoreChatStreamListener;
import tech.aiflowy.ai.agentsflex.ocrModel.OcrModel;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.common.constant.enums.EnumOcrTaskStatus;
import tech.aiflowy.common.constant.enums.EnumRecognitionMode;
import tech.aiflowy.common.constant.enums.EnumWikiType;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.tree.Tree;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.core.chat.protocol.sse.ChatSseEmitter;
import tech.aiflowy.core.chat.protocol.sse.ChatSseUtil;
import tech.aiflowy.wiki.agentsflex.AIFlowyWikiProvider;
import tech.aiflowy.wiki.entity.BotWiki;
import tech.aiflowy.wiki.entity.Wiki;
import tech.aiflowy.wiki.service.BotWikiService;
import tech.aiflowy.wiki.service.WikiService;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/v1/wiki")
@RestController
public class WikiController extends BaseCurdController<WikiService, Wiki> {

    @Resource(name = "default")
    private FileStorageService storageService;
    @Resource
    private ModelService modelService;
    @Resource
    private BotWikiService botWikiService;
    @Resource
    private ApplicationEventPublisher publisher;
    @Resource
    private AIFlowyWikiProvider wikiProvider;

    public WikiController(WikiService service) {
        super(service);
    }

    /**
     * 根据子内容优化标题或描述
     */
    @PostMapping("/optimizeTitleOrDesc")
    public SseEmitter chat(@RequestBody WikiOptimizeDto dto) {
        Integer type = dto.getType();
        String field = dto.getField();
        BigInteger wikiId = dto.getWikiId();
        String prompt = "不用做任何思考内容，帮用户优化一下";
        if ("title".equals(field)) {
            prompt += "标题，不超过100个字符。";
        }
        if ("description".equals(field)) {
            prompt += "描述，根据给出的内容总结一下这些内容的概览，不超过500个字符。";
        }
        prompt += "\n原内容是：" + dto.getOriginValue();
        if (EnumWikiType.DIRECTORY.getCode().equals(type)) {
            QueryWrapper w = QueryWrapper.create();
            w.eq(Wiki::getParentId, wikiId);
            List<Wiki> children = service.list(w);
            if (children != null && !children.isEmpty()) {
                List<com.agentsflex.wiki.Wiki> wikis = wikiProvider.makeAgentsFlexWikis(children);
                String content = WikiTool.buildWikisXml(wikis);
                prompt += "\n参考的内容如下：\n" + content;
            }
        }
        if (EnumWikiType.CONTENT.getCode().equals(type)) {
            Wiki wiki = service.getById(wikiId);
            if (wiki != null && StringUtil.hasLength(wiki.getContent())) {
                prompt += "\n参考的内容如下：\n" + wiki.getContent();
            }
        }

        ChatSseEmitter sseEmitter = new ChatSseEmitter();
        Model model = modelService.getSystemModel();
        ChatModel chatModel = model.toChatModel();
        if (chatModel == null) {
            return ChatSseUtil.sendSystemError(null, "模型不存在");
        }
        ChatOptions chatOptions = new ChatOptions();
        chatOptions.setThinkingEnabled(false);
        chatOptions.setExtraBody(Map.of("enable_thinking", false,
                "thinking", Map.of("type", "disabled")));

        PromptChoreChatStreamListener listener = new PromptChoreChatStreamListener(sseEmitter);
        chatModel.chatStream(prompt, listener, chatOptions);
        return sseEmitter.getEmitter();
    }

    @Override
    protected Result<?> onSaveOrUpdateBefore(Wiki entity, boolean isSave) {
        if (isSave) {
            Integer recognitionMode = entity.getRecognitionMode();
            Integer type = entity.getType();
            ChatModel chatModel = modelService.getSystemModel().toChatModel();
            if (EnumWikiType.CONTENT.getCode().equals(type)) {
                Assert.notNull(recognitionMode, "识别模式不能为空");
                Assert.notNull(chatModel, "请先配置系统模型");
            }
            if (EnumRecognitionMode.NORMAL.getCode().equals(recognitionMode)) {
                InputStream inputStream = null;
                try {
                    inputStream = storageService.readStream(entity.getFileUrl());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String content = File2TextUtil.readFromStream(inputStream, FilenameUtils.getName(entity.getFileUrl()), null);
                entity.setContent(content);
                entity.setTaskStatus(EnumOcrTaskStatus.IN_PROGRESS.getCode());
            }
            if (EnumRecognitionMode.OCR.getCode().equals(recognitionMode)) {
                Assert.notNull(entity.getOcrModelId(), "OCR模型不能为空");
                Model model = modelService.getModelInstance(entity.getOcrModelId());
                OcrModel ocrModel = model.toOcrModel();
                String taskId = ocrModel.runAsync(entity.getFileUrl());
                entity.setTaskId(taskId);
                entity.setTaskStatus(EnumOcrTaskStatus.IN_PROGRESS.getCode());
            }
        }
        return super.onSaveOrUpdateBefore(entity, isSave);
    }

    @Override
    protected void onSaveOrUpdateAfter(Wiki entity, boolean isSave) {
        Integer recognitionMode = entity.getRecognitionMode();
        if (isSave && EnumRecognitionMode.NORMAL.getCode().equals(recognitionMode)) {
            WikiUpdateEvent event = new WikiUpdateEvent();
            event.setWikiId(entity.getId());
            event.setContent(entity.getContent());
            publisher.publishEvent(event);
        }
    }

    @Override
    @GetMapping("list")
    public Result<List<Wiki>> list(Wiki entity, Boolean asTree, String sortKey, String sortType) {
        QueryWrapper queryWrapper = QueryWrapper.create(entity, buildOperators(entity));
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        List<Wiki> list = service.list(queryWrapper);
        return Result.ok(Tree.tryToTree(list, "id", "parentId"));
    }

    @Override
    protected Result<?> onRemoveBefore(Collection<Serializable> ids) {

        for (Serializable id : ids) {
            QueryWrapper w = QueryWrapper.create();
            w.eq(Wiki::getParentId, id);
            if (service.exists(w)) {
                throw new BusinessException("请先删除子数据！");
            }
        }

        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.in(BotWiki::getWikiId, ids);
        botWikiService.remove(queryWrapper);
        return super.onRemoveBefore(ids);
    }

    @Override
    protected String getDefaultOrderBy() {
        return "type asc";
    }
}
