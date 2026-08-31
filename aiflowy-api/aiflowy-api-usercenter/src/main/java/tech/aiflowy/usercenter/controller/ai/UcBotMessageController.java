package tech.aiflowy.usercenter.controller.ai;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.ai.entity.BotMessage;
import tech.aiflowy.ai.service.BotMessageService;
import tech.aiflowy.ai.vo.ChatMessageVO;
import tech.aiflowy.common.annotation.UsePermission;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.common.web.controller.BaseCurdController;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Bot 消息记录表 控制层。
 *
 * @author michael
 * @since 2024-11-04
 */
@RestController
@RequestMapping("/userCenter/botMessage")
@UsePermission(moduleName = "/api/v1/bot")
public class UcBotMessageController extends BaseCurdController<BotMessageService, BotMessage> {
    private final BotMessageService botMessageService;

    public UcBotMessageController(BotMessageService service, BotMessageService botMessageService) {
        super(service);
        this.botMessageService = botMessageService;
    }

    @GetMapping("/getMessages")
    @SaIgnore
    public Result<List<ChatMessageVO>> getMessages(BigInteger botId, BigInteger conversationId) {
        List<ChatMessageVO> res = new ArrayList<>();
        QueryWrapper w = QueryWrapper.create();
        w.eq(BotMessage::getBotId, botId);
        w.eq(BotMessage::getConversationId, conversationId);
        w.ne(BotMessage::getRole, "tool");
        List<BotMessage> list = botMessageService.list(w);
        if (CollectionUtil.isNotEmpty(list)) {
            for (BotMessage message : list) {
                JSONObject msgContent = JSON.parseObject(message.getContent());
                String content = msgContent.getString("textContent");
                if (StringUtil.noText(content)) {
                    continue;
                }
                ChatMessageVO vo = new ChatMessageVO();
                vo.setKey(message.getId().toString());
                vo.setRole(message.getRole());
                vo.setContent(content);
                vo.setPlacement("user".equals(message.getRole()) ? "end" : "start");
                vo.setCreated(message.getCreated());
                res.add(vo);
            }
        }
        return Result.ok(res);
    }
}
