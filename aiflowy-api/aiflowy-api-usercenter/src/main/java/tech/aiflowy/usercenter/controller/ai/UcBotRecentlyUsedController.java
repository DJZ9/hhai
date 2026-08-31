package tech.aiflowy.usercenter.controller.ai;

import cn.hutool.core.collection.CollectionUtil;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.ai.entity.Bot;
import tech.aiflowy.ai.entity.BotRecentlyUsed;
import tech.aiflowy.ai.service.BotRecentlyUsedService;
import tech.aiflowy.ai.service.BotService;
import tech.aiflowy.common.annotation.UsePermission;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.web.controller.BaseCurdController;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 最近使用 控制层。
 *
 * @author ArkLight
 * @since 2025-12-18
 */
@RestController
@RequestMapping("/userCenter/botRecentlyUsed")
@UsePermission(moduleName = "/api/v1/bot")
public class UcBotRecentlyUsedController extends BaseCurdController<BotRecentlyUsedService, BotRecentlyUsed> {

    @Resource
    private BotService botService;

    public UcBotRecentlyUsedController(BotRecentlyUsedService service) {
        super(service);
    }

    @GetMapping("/getRecentlyBot")
    public Result<List<Bot>> getRecentlyBot() {
        LoginAccount account = SaTokenUtil.getLoginAccount();
        QueryWrapper w = QueryWrapper.create();
        w.eq(BotRecentlyUsed::getCreatedBy,account.getId());
        w.orderBy(BotRecentlyUsed::getSortNo,true);
        List<BotRecentlyUsed> list = service.list(w);
        if (CollectionUtil.isNotEmpty(list)) {
            List<BigInteger> botIds = list.stream().map(BotRecentlyUsed::getBotId).collect(Collectors.toList());
            QueryWrapper botQw = QueryWrapper.create();
            botQw.in(Bot::getId,botIds);
            List<Bot> listBot = botService.list(botQw);
            listBot.sort(Comparator.comparing(bot -> botIds.indexOf(bot.getId())));
            return Result.ok(listBot);
        }
        return Result.ok(new ArrayList<>());
    }

    @GetMapping("/removeByBotId")
    public Result<Void> removeByBotId(BigInteger botId) {
        QueryWrapper w = QueryWrapper.create();
        w.eq(BotRecentlyUsed::getBotId,botId);
        w.eq(BotRecentlyUsed::getCreatedBy,SaTokenUtil.getLoginAccount().getId());
        service.remove(w);
        return Result.ok();
    }

    @Override
    public Result<List<BotRecentlyUsed>> list(BotRecentlyUsed entity, Boolean asTree, String sortKey, String sortType) {
        LoginAccount account = SaTokenUtil.getLoginAccount();
        entity.setCreatedBy(account.getId());
        return super.list(entity, asTree, sortKey, sortType);
    }

    @Override
    protected Result<?> onSaveOrUpdateBefore(BotRecentlyUsed entity, boolean isSave) {
        entity.setCreated(new Date());
        entity.setCreatedBy(SaTokenUtil.getLoginAccount().getId());
        return super.onSaveOrUpdateBefore(entity, isSave);
    }
}