package tech.aiflowy.datacenter.entity.base;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;

import java.io.Serializable;
import java.math.BigInteger;


public class DatacenterSourceDeptMappingBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id(keyType = KeyType.Generator, value = "snowFlakeId", comment = "主键")
    private BigInteger id;

    /**
     * 数据源ID
     */
    @Column(comment = "数据源ID")
    private BigInteger datacenterSourceId;

    /**
     * 部门ID
     */
    @Column(comment = "部门ID")
    private BigInteger deptId;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public BigInteger getDatacenterSourceId() {
        return datacenterSourceId;
    }

    public void setDatacenterSourceId(BigInteger datacenterSourceId) {
        this.datacenterSourceId = datacenterSourceId;
    }

    public BigInteger getDeptId() {
        return deptId;
    }

    public void setDeptId(BigInteger deptId) {
        this.deptId = deptId;
    }

}
