package tech.aiflowy.datacenter.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.datacenter.entity.DatacenterTableField;

import java.util.List;

/**
 * 服务层。
 *
 * @author ArkLight
 * @since 2025-07-10
 */
public interface DatacenterTableFieldService extends IService<DatacenterTableField> {

    /**
     * 根据表名获取字段列表
     *
     * @param tableName 表名
     * @return 字段列表
     */
    List<DatacenterTableField> getFieldsByTableName(String tableName);

}
