package tech.aiflowy.datacenter.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.aiflowy.common.constant.enums.EnumDatasourceType;
import tech.aiflowy.common.constant.enums.EnumDeptMappingAuthType;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.datacenter.entity.DatacenterSource;
import tech.aiflowy.datacenter.entity.DatacenterSourceDeptMapping;
import tech.aiflowy.datacenter.entity.DatacenterSourceField;
import tech.aiflowy.datacenter.entity.DatacenterSourceTable;
import tech.aiflowy.datacenter.mapper.DatacenterSourceDeptMappingMapper;
import tech.aiflowy.datacenter.mapper.DatacenterSourceFieldMapper;
import tech.aiflowy.datacenter.mapper.DatacenterSourceMapper;
import tech.aiflowy.datacenter.mapper.DatacenterSourceTableMapper;
import tech.aiflowy.datacenter.service.DatacenterSourceService;
import tech.aiflowy.system.entity.SysAccount;
import tech.aiflowy.system.mapper.SysAccountMapper;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据源 服务层实现。
 *
 * @author John
 * @since 2026-03-02
 */
@Service
public class DatacenterSourceServiceImpl extends ServiceImpl<DatacenterSourceMapper, DatacenterSource> implements DatacenterSourceService {

    private static final Logger log = LoggerFactory.getLogger(DatacenterSourceServiceImpl.class);

    @Resource
    private DatacenterSourceTableMapper datacenterSourceTableMapper;
    @Resource
    private DatacenterSourceFieldMapper datacenterSourceFieldMapper;
    @Resource
    private DatacenterSourceDeptMappingMapper datacenterSourceDeptMappingMapper;
    @Resource
    private SysAccountMapper sysAccountMapper;

    @Override
    public boolean testConnection(DatacenterSource entity) {
        if (entity == null || entity.getSourceType() == null) {
            return false;
        }

        EnumDatasourceType datasourceType = EnumDatasourceType.getByCode(entity.getSourceType());
        if (datasourceType == null) {
            return false;
        }

        String jdbcUrl = entity.getSourceAddress();
        if (jdbcUrl == null) {
            return false;
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, entity.getUsername(), entity.getPassword())) {
            return connection.isValid(5);
        } catch (SQLException e) {
            log.error("数据库连接测试失败", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTableInfo(List<DatacenterSourceTable> saveList) {
        // 删除原来的表信息
        DatacenterSourceTable record = saveList.get(0);
        QueryWrapper wds = QueryWrapper.create();
        wds.eq(DatacenterSourceTable::getSourceId, record.getSourceId());
        datacenterSourceTableMapper.deleteByQuery(wds);
        // 批量保存或更新数据源表信息
        for (DatacenterSourceTable table : saveList) {
            SaTokenUtil.setCommonField(table);
            datacenterSourceTableMapper.insert(table, true);
            // 删除原来的表字段信息
            saveFields(table, table.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSingleTableInfo(DatacenterSourceTable entity) {
        BigInteger tableId = entity.getId();
        if (tableId == null) {
            SaTokenUtil.setCommonField(entity);
        }
        datacenterSourceTableMapper.insertOrUpdate(entity, true);
        tableId = entity.getId();
        saveFields(entity, tableId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTableInfo(BigInteger tableId) {
        datacenterSourceTableMapper.deleteById(tableId);
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.eq(DatacenterSourceField::getTableId, tableId);
        datacenterSourceFieldMapper.deleteByQuery(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSource(BigInteger sourceId) {
        removeById(sourceId);
        QueryWrapper w = QueryWrapper.create().eq(DatacenterSourceTable::getSourceId, sourceId);
        List<DatacenterSourceTable> tables = datacenterSourceTableMapper.selectListByQuery(w);
        datacenterSourceTableMapper.deleteByQuery(w);
        if (tables != null && !tables.isEmpty()) {
            for (DatacenterSourceTable table : tables) {
                QueryWrapper wrapper = QueryWrapper.create();
                wrapper.eq(DatacenterSourceField::getTableId, table.getId());
                datacenterSourceFieldMapper.deleteByQuery(wrapper);
            }
        }
    }

    private void saveFields(DatacenterSourceTable entity, BigInteger tableId) {
        // 删除原来的表字段信息
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.eq(DatacenterSourceField::getTableId, tableId);
        datacenterSourceFieldMapper.deleteByQuery(wrapper);
        // 批量保存表字段信息
        List<DatacenterSourceField> columns = entity.getFields();
        for (DatacenterSourceField column : columns) {
            column.setTableId(tableId);
            SaTokenUtil.setCommonField(column);
            datacenterSourceFieldMapper.insert(column, true);
        }
    }

    @Override
    public Result<Page<DatacenterSource>> pageDatacenterSource(Result<Page<DatacenterSource>> page) {
        page.getData().getRecords().forEach(datacenterSource -> {
            if (datacenterSource.getAuthType() == EnumDeptMappingAuthType.CUSTOM.getCode()) {
                List<DatacenterSourceDeptMapping> datacenterSourceDeptMappings = datacenterSourceDeptMappingMapper.selectListByQuery(QueryWrapper.create().eq(DatacenterSourceDeptMapping::getDatacenterSourceId, datacenterSource.getId()));
                datacenterSource.setScopeOfAuthorityDeptIds(datacenterSourceDeptMappings.stream().map(DatacenterSourceDeptMapping::getDeptId).toArray(BigInteger[]::new));
            }
        });
        return page;
    }

    @Override
    public boolean hasAuthority(BigInteger userId, DatacenterSource datacenterSource) {
        SysAccount sysAccount = sysAccountMapper.selectOneById(userId);
        BigInteger deptId = null;
        if (sysAccount != null) {
            deptId = sysAccount.getDeptId();
        }
        if (datacenterSource.getAuthType() == EnumDeptMappingAuthType.ALL.getCode()) {
            return true;
        }
        if (datacenterSource.getAuthType() == EnumDeptMappingAuthType.CUSTOM.getCode()) {
            List<DatacenterSourceDeptMapping> datacenterSourceDeptMappings = datacenterSourceDeptMappingMapper
                    .selectListByQuery(QueryWrapper.create().eq(DatacenterSourceDeptMapping::getDatacenterSourceId, datacenterSource.getId()));
            if (datacenterSourceDeptMappings != null) {
                for (DatacenterSourceDeptMapping datacenterSourceDeptMapping : datacenterSourceDeptMappings) {
                    if (datacenterSourceDeptMapping.getDeptId().equals(deptId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
