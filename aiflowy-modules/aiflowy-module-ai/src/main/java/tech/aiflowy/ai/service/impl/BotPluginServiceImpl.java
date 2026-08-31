package tech.aiflowy.ai.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.ai.entity.BotPlugin;
import tech.aiflowy.ai.entity.Plugin;
import tech.aiflowy.ai.mapper.BotPluginMapper;
import tech.aiflowy.ai.mapper.PluginMapper;
import tech.aiflowy.ai.service.BotPluginService;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static tech.aiflowy.ai.entity.table.BotPluginTableDef.BOT_PLUGIN;

/**
 *  服务层实现。
 *
 * @author michael
 * @since 2025-04-07
 */
@Service
public class BotPluginServiceImpl extends ServiceImpl<BotPluginMapper, BotPlugin> implements BotPluginService {

    @Resource
    private BotPluginMapper botPluginMapper;

    @Resource
    private PluginMapper pluginMapper;

    @Override
    public List<Plugin> getList(String botId) {
        QueryWrapper w = QueryWrapper.create();
        w.select(BOT_PLUGIN.PLUGIN_ITEM_ID)
                .where(BOT_PLUGIN.BOT_ID.eq(botId));
        List<BigInteger> pluginIds = botPluginMapper.selectListByQueryAs(w, BigInteger.class);
        return pluginMapper.selectListByIds(pluginIds);
    }

    @Override
    public boolean doRemove(String botId, String pluginToolId) {
        QueryWrapper w = QueryWrapper.create();
        w.select(BOT_PLUGIN.ID)
                .where(BOT_PLUGIN.BOT_ID.eq(botId))
                .and(BOT_PLUGIN.PLUGIN_ITEM_ID.eq(pluginToolId));
        BigInteger id = botPluginMapper.selectOneByQueryAs(w, BigInteger.class);
        int delete = botPluginMapper.deleteById(id);
        return delete > 0;
    }

    @Override
    public List<BigInteger> getBotPluginToolIds(String botId) {
        QueryWrapper queryWrapper = QueryWrapper.create().select(BOT_PLUGIN.PLUGIN_ITEM_ID).where(BOT_PLUGIN.BOT_ID.eq(botId));
        return botPluginMapper.selectListByQueryAs(queryWrapper, BigInteger.class);
    }

    @Override
    public void saveBotAndPluginTool(BigInteger botId, BigInteger[] pluginToolIds) {
        this.remove(QueryWrapper.create().eq(BotPlugin::getBotId, botId));
        List<BotPlugin> list = new ArrayList<>(pluginToolIds.length);
        for (BigInteger pluginToolId : pluginToolIds) {
            BotPlugin aiBotPluginTool = new BotPlugin();
            aiBotPluginTool.setBotId(botId);
            aiBotPluginTool.setPluginItemId(pluginToolId);
            list.add(aiBotPluginTool);
        }
        this.saveBatch(list);
    }
}
