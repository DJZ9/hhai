package tech.aiflowy.datacenter.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.datacenter.entity.DatacenterSourceField;

import java.util.List;

/**
 * 数据源-字段 服务层。
 *
 * @author John
 * @since 2026-03-10
 */
public interface DatacenterSourceFieldService extends IService<DatacenterSourceField> {

    /**
     * 根据表名获取数据源字段列表
     */
    List<DatacenterSourceField> getFieldsByTableName(String tableName);

}
