package tech.aiflowy.datacenter.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import tech.aiflowy.datacenter.entity.DatacenterSourceField;
import tech.aiflowy.datacenter.entity.DatacenterSourceTable;
import tech.aiflowy.datacenter.mapper.DatacenterSourceFieldMapper;
import tech.aiflowy.datacenter.service.DatacenterSourceFieldService;
import tech.aiflowy.datacenter.service.DatacenterSourceTableService;

import java.util.Collections;
import java.util.List;

/**
 * 数据源-字段 服务层实现。
 *
 * @author John
 * @since 2026-03-10
 */
@Service
public class DatacenterSourceFieldServiceImpl extends ServiceImpl<DatacenterSourceFieldMapper, DatacenterSourceField>  implements DatacenterSourceFieldService{

    @Resource
    private DatacenterSourceTableService datacenterSourceTableService;

    @Override
    public List<DatacenterSourceField> getFieldsByTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return Collections.emptyList();
        }

        // 先根据表名查询数据源表信息（精确匹配）
        QueryWrapper tableWrapper = QueryWrapper.create()
            .eq(DatacenterSourceTable::getName, tableName);
        DatacenterSourceTable table = datacenterSourceTableService.getOne(tableWrapper);

        // 如果精确匹配失败，尝试忽略大小写匹配
        if (table == null) {
            List<DatacenterSourceTable> allTables = datacenterSourceTableService.list();
            for (DatacenterSourceTable t : allTables) {
                if (t.getName() != null && t.getName().equalsIgnoreCase(tableName)) {
                    table = t;
                    break;
                }
            }
        }

        if (table == null) {
            return Collections.emptyList();
        }

        // 根据表ID查询字段列表
        QueryWrapper fieldWrapper = QueryWrapper.create()
            .eq(DatacenterSourceField::getTableId, table.getId())
            .orderBy("id");
        return list(fieldWrapper);
    }

}
