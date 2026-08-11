package tech.aiflowy.ai.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.entity.base.BotDatasourceBase;
import tech.aiflowy.datacenter.entity.DatacenterSource;

import java.math.BigInteger;
import java.util.List;


/**
 * bot绑定的数据源 实体类。
 *
 * @author John
 * @since 2026-03-02
 */
@Table(value = "tb_bot_datasource", comment = "bot绑定的数据源")
public class BotDatasource extends BotDatasourceBase {

    @Column(ignore = true)
    private List<BigInteger> datasourceIds;

    @RelationOneToOne(selfField = "sourceId", targetField = "id")
    private DatacenterSource datacenterSource;

    public List<BigInteger> getDatasourceIds() {
        return datasourceIds;
    }

    public void setDatasourceIds(List<BigInteger> datasourceIds) {
        this.datasourceIds = datasourceIds;
    }

    public DatacenterSource getDatacenterSource() {
        return datacenterSource;
    }

    public void setDatacenterSource(DatacenterSource datacenterSource) {
        this.datacenterSource = datacenterSource;
    }
}
