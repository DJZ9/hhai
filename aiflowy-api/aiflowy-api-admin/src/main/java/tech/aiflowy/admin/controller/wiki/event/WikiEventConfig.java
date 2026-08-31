package tech.aiflowy.admin.controller.wiki.event;

import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.prompt.SimplePrompt;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.common.constant.ModelJsonSchema;
import tech.aiflowy.common.constant.enums.EnumOcrTaskStatus;
import tech.aiflowy.wiki.entity.Wiki;
import tech.aiflowy.wiki.service.WikiService;

@Component
public class WikiEventConfig {

    private static final String PROMPT = "请根据内容提炼出标题和摘要并返回指定的 JSON 格式数据，内容如下：\n";
    private static final String WIKI_INFO = """
            {
                 "type": "object",
                 "properties": {
                     "title": {
                         "type": "string",
                         "description": "标题，不超过100个字符"
                     },
                     "description": {
                         "type": "string",
                         "description": "摘要，不超过500个字符"
                     }
                 },
                 "required": [
                     "title",
                     "description"
                 ]
             }
            """;
    private static final Logger log = LoggerFactory.getLogger(WikiEventConfig.class);

    @Resource
    private ModelService modelService;
    @Resource
    private WikiService wikiService;

    @Async
    @EventListener
    public void updateWiki(WikiUpdateEvent event) {
        String content = event.getContent();
        Wiki update = new Wiki();
        update.setId(event.getWikiId());
        try {
            ChatModel chatModel = modelService.getSystemModel().toChatModel();
            SimplePrompt prompt = new SimplePrompt(PROMPT + content + "\n JSON 格式如下：\n" + WIKI_INFO);

            ChatOptions options = new ChatOptions.Builder()
                    .responseFormat(ModelJsonSchema.JSON_RESPONSE_FORMAT)
                    .build();

            AiMessageResponse response = chatModel.chat(prompt, options);
            if (response.isError()) {
                String errorMessage = response.getErrorMessage();
                log.error("Error processing wiki update event, llm error: {}", errorMessage);
                update.setTitle("---");
                update.setDescription("---");
                update.setTaskStatus(EnumOcrTaskStatus.FAILED.getCode());
                update.setFailReason(errorMessage);
            } else {
                JSONObject obj = JSON.parseObject(response.getMessage().getContent());
                update.setTitle(obj.getString("title"));
                update.setDescription(obj.getString("description"));
                update.setTaskStatus(EnumOcrTaskStatus.COMPLETED.getCode());
            }
        } catch (Exception e) {
            update.setTitle("---");
            update.setDescription("---");
            update.setTaskStatus(EnumOcrTaskStatus.FAILED.getCode());
            update.setFailReason(e.getMessage());
            log.error("Error processing wiki update event", e);
        }
        wikiService.updateById(update);
    }

}
