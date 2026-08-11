package tech.aiflowy.datacenter.entity;

import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.datacenter.entity.base.DatacenterSourceTableBase;

import java.util.List;


/**
 * 数据源-数据表 实体类。
 *
 * @author John
 * @since 2026-03-10
 */
@Table(value = "tb_datacenter_source_table", comment = "数据源-数据表")
public class DatacenterSourceTable extends DatacenterSourceTableBase {

    @RelationOneToMany(selfField = "id", targetField = "tableId")
    private List<DatacenterSourceField> fields;

    public List<DatacenterSourceField> getFields() {
        return fields;
    }

    public void setFields(List<DatacenterSourceField> fields) {
        this.fields = fields;
    }
}
