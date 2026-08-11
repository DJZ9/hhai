package tech.aiflowy.datacenter.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.datacenter.entity.DatacenterSource;
import tech.aiflowy.datacenter.entity.DatacenterSourceTable;

import java.math.BigInteger;
import java.util.List;

/**
 * 数据源 服务层。
 *
 * @author John
 * @since 2026-03-02
 */
public interface DatacenterSourceService extends IService<DatacenterSource> {

    /**
     * 测试数据库连接
     *
     * @param entity 数据源实体
     * @return true-连接成功，false-连接失败
     */
    boolean testConnection(DatacenterSource entity);

    void updateTableInfo(List<DatacenterSourceTable> saveList);

    void saveSingleTableInfo(DatacenterSourceTable entity);

    void deleteTableInfo(BigInteger tableId);

    void deleteSource(BigInteger sourceId);

    /**
     * 分页查询数据源列表并填充部门权限信息
     *
     * @param page 分页结果
     * @return 填充部门权限后的分页结果
     */
    Result<Page<DatacenterSource>> pageDatacenterSource(Result<Page<DatacenterSource>> page);

    /**
     * 判断用户是否有权限访问该数据源
     *
     * @param userId 用户ID
     * @param datacenterSource 数据源
     * @return true-有权限，false-无权限
     */
    boolean hasAuthority(BigInteger userId, DatacenterSource datacenterSource);
}
