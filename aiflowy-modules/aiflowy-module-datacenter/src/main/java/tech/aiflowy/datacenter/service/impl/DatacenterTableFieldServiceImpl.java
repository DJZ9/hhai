package tech.aiflowy.datacenter.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import tech.aiflowy.datacenter.entity.DatacenterTable;
import tech.aiflowy.datacenter.entity.DatacenterTableField;
import tech.aiflowy.datacenter.mapper.DatacenterTableFieldMapper;
import tech.aiflowy.datacenter.service.DatacenterTableFieldService;
import tech.aiflowy.datacenter.service.DatacenterTableService;

import java.util.Collections;
import java.util.List;

/**
 * 服务层实现。
 *
 * @author ArkLight
 * @since 2025-07-10
 */
@Service
public class DatacenterTableFieldServiceImpl extends ServiceImpl<DatacenterTableFieldMapper, DatacenterTableField> implements DatacenterTableFieldService {

    @Resource
    private DatacenterTableService datacenterTableService;

    @Override
    public List<DatacenterTableField> getFieldsByTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return Collections.emptyList();
        }

        // 先根据表名查询表信息（精确匹配）
        QueryWrapper tableWrapper = QueryWrapper.create()
                .eq(DatacenterTable::getActualTable, tableName);
        DatacenterTable table = datacenterTableService.getOne(tableWrapper);

        // 如果精确匹配失败，尝试忽略大小写匹配
        if (table == null) {
            List<DatacenterTable> allTables = datacenterTableService.list();
            for (DatacenterTable t : allTables) {
                if (t.getActualTable() != null && t.getActualTable().equalsIgnoreCase(tableName)) {
                    table = t;
                    break;
                }
            }
        }

        if (table == null) {
            return Collections.emptyList();
        }

        // 复用 DatacenterTableService 的 getFields 方法查询字段列表
        return datacenterTableService.getFields(table.getId());

    }

}
