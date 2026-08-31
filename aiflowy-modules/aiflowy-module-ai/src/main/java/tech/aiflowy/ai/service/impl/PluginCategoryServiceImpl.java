package tech.aiflowy.ai.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import tech.aiflowy.ai.entity.PluginCategory;
import tech.aiflowy.ai.entity.PluginCategoryMapping;
import tech.aiflowy.ai.mapper.PluginCategoryMapper;
import tech.aiflowy.ai.mapper.PluginCategoryMappingMapper;
import tech.aiflowy.ai.service.PluginCategoryService;
import org.springframework.stereotype.Service;
import tech.aiflowy.common.web.exceptions.BusinessException;

import javax.annotation.Resource;
import java.math.BigInteger;

/**
 *  服务层实现。
 *
 * @author Administrator
 * @since 2025-05-21
 */
@Service
public class PluginCategoryServiceImpl extends ServiceImpl<PluginCategoryMapper, PluginCategory>  implements PluginCategoryService {

    @Resource
    private PluginCategoryMappingMapper relationMapper;

    @Resource
    private PluginCategoryMapper pluginCategoryMapper;

    @Override
    public boolean doRemoveCategory(BigInteger id) {
        QueryWrapper queryWrapper = QueryWrapper.create().select()
                .eq(PluginCategoryMapping::getCategoryId, id);
        long relationCount = relationMapper.selectCountByQuery(queryWrapper);
        if (relationCount > 0){
            int deletePluginRelation = relationMapper.deleteByQuery(queryWrapper);
            if (deletePluginRelation <= 0){
                throw new BusinessException("删除失败");
            }
        }

        int deleteCategory = pluginCategoryMapper.deleteById(id);
        if (deleteCategory <= 0){
            throw new BusinessException("删除失败");
        }
        return true;
    }
}
