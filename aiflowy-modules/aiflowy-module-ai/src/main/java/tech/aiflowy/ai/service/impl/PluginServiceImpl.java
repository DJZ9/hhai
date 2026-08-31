package tech.aiflowy.ai.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import tech.aiflowy.ai.entity.*;
import tech.aiflowy.ai.mapper.PluginCategoryMappingMapper;
import tech.aiflowy.ai.mapper.PluginMapper;
import tech.aiflowy.ai.service.BotPluginService;
import tech.aiflowy.ai.service.PluginService;
import org.springframework.stereotype.Service;
import tech.aiflowy.ai.service.PluginItemService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.exceptions.BusinessException;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务层实现。
 *
 * @author WangGangqiang
 * @since 2025-04-25
 */
@Service
public class PluginServiceImpl extends ServiceImpl<PluginMapper, Plugin> implements PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginServiceImpl.class);

    @Resource
    PluginMapper pluginMapper;

    @Resource
    PluginCategoryMappingMapper pluginCategoryMappingMapper;

    @Resource
    private BotPluginService botPluginService;

    @Resource
    private PluginItemService pluginItemService;

    @Override
    public boolean savePlugin(Plugin plugin) {
        plugin.setCreated(new Date());
        int insert = pluginMapper.insert(plugin);
        if (insert <= 0) {
            throw new BusinessException("保存失败");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean removePlugin(String id) {
        if (id == null) {
            throw new BusinessException("id不能未空");
        }
        List<PluginItem> pluginItems = pluginItemService.getByPluginId(id);
        List<BigInteger> pluginToolIds = new ArrayList<>();

        if (pluginItems != null && !pluginItems.isEmpty()) {

            pluginToolIds = pluginItems.stream().map(PluginItem::getId).collect(Collectors.toList());
            QueryWrapper queryWrapper = QueryWrapper.create();
            queryWrapper.in(BotPlugin::getPluginItemId, pluginToolIds);
            botPluginService.remove(queryWrapper);
        }

        if ( !pluginToolIds.isEmpty()) {
            pluginItemService.removeByIds(pluginToolIds);
        }

        pluginMapper.deleteById(id);

        return true;

    }

    @Override
    public List<Plugin> getList() {
        QueryWrapper queryWrapper = QueryWrapper.create().select();
        return pluginMapper.selectListByQueryAs(queryWrapper, Plugin.class);
    }

    @Override
    public Result<Page<Plugin>> pageByCategory(Long pageNumber, Long pageSize, int category) {
        // 通过分类查询插件
        QueryWrapper queryWrapper = QueryWrapper.create().select(PluginCategoryMapping::getPluginId)
                .eq(PluginCategoryMapping::getCategoryId, category);
        // 分页查询该分类中的插件
        Page<BigInteger> pagePluginIds = pluginCategoryMappingMapper.paginateAs(
                new Page<>(pageNumber, pageSize), queryWrapper, BigInteger.class);

        List<Plugin> plugins = Collections.emptyList();
        if (pagePluginIds.getRecords().isEmpty()) {
            return Result.ok(new Page<>(plugins, pageNumber, pageSize, pagePluginIds.getTotalRow()));
        }
        List<BigInteger> pluginIds = pagePluginIds.getRecords();
        // 查询对应的插件信息
        QueryWrapper queryPluginWrapper = QueryWrapper.create().select()
                .in(Plugin::getId, pluginIds);
        plugins = pluginMapper.selectListByQuery(queryPluginWrapper);
        Page<Plugin> aiPluginPage = new Page<>(plugins, pageNumber, pageSize, pagePluginIds.getTotalRow());
        return Result.ok(aiPluginPage);
    }

    @Override
    public boolean updatePlugin(Plugin plugin) {
        pluginMapper.update(plugin);
        return true;
    }


}
