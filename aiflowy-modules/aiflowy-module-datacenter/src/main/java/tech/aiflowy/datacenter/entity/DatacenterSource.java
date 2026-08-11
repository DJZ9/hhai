package tech.aiflowy.datacenter.entity;

import com.agentsflex.text2sql.entity.JdbcDataSourceInfo;
import com.agentsflex.text2sql.entity.TableInfo;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.datacenter.entity.base.DatacenterSourceBase;

import java.math.BigInteger;
import java.util.List;


/**
 * 数据源 实体类。
 *
 * @author John
 * @since 2026-03-02
 */
@Table(value = "tb_datacenter_source", comment = "数据源")
public class DatacenterSource extends DatacenterSourceBase {

    private BigInteger[] scopeOfAuthorityDeptIds;

    /**
     * 转换为 JdbcDataSourceInfo（用于统一构建 Text2SqlTools）
     */
    public JdbcDataSourceInfo toDataSourceInfo(List<TableInfo> tables) {
        JdbcDataSourceInfo dataSource = new JdbcDataSourceInfo();
        dataSource.setJdbcUrl(this.getSourceAddress());
        dataSource.setUsername(this.getUsername());
        dataSource.setPassword(this.getPassword());
        dataSource.setName(this.getSourceName());
        dataSource.setDescription(this.getDescription());

        if (tables != null && !tables.isEmpty()) {
            dataSource.setTables(tables);
        }

        return dataSource;
    }

    public BigInteger[] getScopeOfAuthorityDeptIds() {
        return scopeOfAuthorityDeptIds;
    }

    public void setScopeOfAuthorityDeptIds(BigInteger[] scopeOfAuthorityDeptIds) {
        this.scopeOfAuthorityDeptIds = scopeOfAuthorityDeptIds;
    }

}
