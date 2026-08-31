package tech.aiflowy.ai.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.ai.entity.BotPlugin;
import tech.aiflowy.ai.entity.Plugin;
import tech.aiflowy.ai.agentsflex.tool.PluginTool;
import tech.aiflowy.ai.entity.PluginItem;
import tech.aiflowy.ai.mapper.BotPluginMapper;
import tech.aiflowy.ai.mapper.PluginMapper;
import tech.aiflowy.ai.mapper.PluginItemMapper;
import tech.aiflowy.ai.service.PluginItemService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.exceptions.BusinessException;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.*;

import static tech.aiflowy.ai.entity.table.BotPluginTableDef.BOT_PLUGIN;

/**
 * 服务层实现。
 *
 * @author WangGangqiang
 * @since 2025-04-27
 */
@Service
public class PluginItemServiceImpl extends ServiceImpl<PluginItemMapper, PluginItem> implements PluginItemService {

    @Resource
    private PluginItemMapper pluginItemMapper;

    @Resource
    private PluginMapper pluginMapper;

    @Resource
    private BotPluginMapper botPluginMapper;

    @Override
    public boolean savePluginTool(PluginItem pluginItem) {
        pluginItem.setCreated(new Date());
        pluginItem.setRequestMethod("Post");
        int insert = pluginItemMapper.insert(pluginItem);
        if (insert <= 0) {
            throw new BusinessException("保存失败");
        }
        return true;
    }

    @Override
    public Result<?> searchPlugin(BigInteger aiPluginToolId) {
        //查询当前插件工具
        QueryWrapper queryAiPluginToolWrapper = QueryWrapper.create()
                .select()
                .eq(PluginItem::getId, aiPluginToolId);
        PluginItem pluginItem = pluginItemMapper.selectOneByQuery(queryAiPluginToolWrapper);
        // 查询当前的插件信息
        QueryWrapper queryAiPluginWrapper = QueryWrapper.create()
                .select()
                .eq(Plugin::getId, pluginItem.getPluginId());
        Plugin plugin = pluginMapper.selectOneByQuery(queryAiPluginWrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("data", pluginItem);
        result.put("aiPlugin", plugin);
        return Result.ok(result);
    }

    @Override
    public boolean updatePlugin(PluginItem pluginItem) {
        int update = pluginItemMapper.update(pluginItem);
        if (update <= 0) {
            throw new BusinessException("修改失败");
        }
        return true;
    }

    @Override
    public List<PluginItem> searchPluginToolByPluginId(BigInteger pluginId, BigInteger botId) {
        QueryWrapper queryAiPluginToolWrapper = QueryWrapper.create()
                .select()
                .eq(PluginItem::getPluginId, pluginId);
        List<PluginItem> pluginItems = pluginItemMapper.selectListByQueryAs(queryAiPluginToolWrapper, PluginItem.class);
        // 查询当前bot有哪些插件工具方法
        QueryWrapper queryBotPluginTools = QueryWrapper.create()
                .select()
                .eq(BotPlugin::getBotId, botId);
        List<BigInteger> aiBotPluginToolIds = botPluginMapper.selectListWithRelationsByQueryAs(queryBotPluginTools, BigInteger.class);
        aiBotPluginToolIds.forEach(botPluginTooId -> {
            pluginItems.forEach(item -> {
                if (Objects.equals(botPluginTooId, item.getId())) {
                    item.setJoinBot(true);
                }
            });
        });
        return pluginItems;
    }

    @Override
    public List<PluginItem> getPluginToolList(BigInteger botId) {
        QueryWrapper queryAiPluginToolWrapper = QueryWrapper.create()
                .select(BOT_PLUGIN.PLUGIN_ITEM_ID)
                .from(BOT_PLUGIN)
                .where(BOT_PLUGIN.BOT_ID.eq(botId));
        List<BigInteger> pluginToolIds = botPluginMapper.selectListByQueryAs(queryAiPluginToolWrapper, BigInteger.class);
        if (pluginToolIds == null || pluginToolIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 查询当前bots对应的有哪些pluginTool
        return pluginItemMapper.selectListByIds(pluginToolIds);
    }

    @Override
    public Result<?> pluginToolTest(String inputData, BigInteger pluginToolId) {
        PluginItem pluginItem = new PluginItem();
        pluginItem.setId(pluginToolId);
        pluginItem.setInputData(inputData);
        PluginTool pluginTool = new PluginTool(pluginItem);
        return Result.ok(pluginTool.runPluginTool(null, inputData, pluginToolId));
    }

    @Override
    public List<PluginItem> getByPluginId(String id) {

        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq(PluginItem::getPluginId, id);

        return list(queryWrapper);
    }

}
