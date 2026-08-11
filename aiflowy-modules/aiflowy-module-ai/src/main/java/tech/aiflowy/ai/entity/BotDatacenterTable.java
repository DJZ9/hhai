package tech.aiflowy.ai.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.entity.base.BotDatacenterTableBase;
import tech.aiflowy.datacenter.entity.DatacenterTable;

import java.math.BigInteger;
import java.util.List;


/**
 * bot绑定的数据中枢 实体类。
 *
 * @author John
 * @since 2026-03-02
 */
@Table(value = "tb_bot_datacenter_table", comment = "bot绑定的数据中枢")
public class BotDatacenterTable extends BotDatacenterTableBase {

    @Column(ignore = true)
    private List<BigInteger> datacenterTableIds;

    @RelationOneToOne(selfField = "tableId", targetField = "id")
    private DatacenterTable datacenterTable;

    public List<BigInteger> getDatacenterTableIds() {
        return datacenterTableIds;
    }

    public void setDatacenterTableIds(List<BigInteger> datacenterTableIds) {
        this.datacenterTableIds = datacenterTableIds;
    }

    public DatacenterTable getDatacenterTable() {
        return datacenterTable;
    }

    public void setDatacenterTable(DatacenterTable datacenterTable) {
        this.datacenterTable = datacenterTable;
    }
}
