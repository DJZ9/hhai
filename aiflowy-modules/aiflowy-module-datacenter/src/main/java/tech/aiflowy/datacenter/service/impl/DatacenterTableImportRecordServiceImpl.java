package tech.aiflowy.datacenter.service.impl;

import com.mybatisflex.core.keygen.impl.SnowFlakeIDKeyGenerator;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.datacenter.entity.DatacenterTableImportRecord;
import tech.aiflowy.datacenter.mapper.DatacenterTableImportRecordMapper;
import tech.aiflowy.datacenter.service.DatacenterTableImportRecordService;

import java.math.BigInteger;
import java.util.Date;

/**
 * 数据中枢表导入记录 Service 实现
 */
@Service
public class DatacenterTableImportRecordServiceImpl extends ServiceImpl<DatacenterTableImportRecordMapper, DatacenterTableImportRecord> implements DatacenterTableImportRecordService {

    @Override
    public BigInteger createRecord(String fileName, String tableName, BigInteger categoryId, LoginAccount account) {
        DatacenterTableImportRecord record = new DatacenterTableImportRecord();
        long snowId = new SnowFlakeIDKeyGenerator().nextId();
        record.setId(new BigInteger(String.valueOf(snowId)));
        record.setFileName(fileName);
        record.setTableName(tableName);
        record.setCategoryId(categoryId);
        record.setStatus(0);
        record.setTotalCount(0);
        record.setSuccessCount(0);
        record.setErrorCount(0);
        record.setCreated(new Date());
        record.setDeptId(account.getDeptId());
        record.setTenantId(account.getTenantId());
        record.setCreatedBy(account.getId());
        save(record);
        return record.getId();
    }

    @Override
    public void updateRecordSuccess(BigInteger recordId, Integer totalCount, Integer successCount, Integer errorCount) {
        DatacenterTableImportRecord record = getById(recordId);
        if (record != null) {
            record.setStatus(1);
            record.setTotalCount(totalCount);
            record.setSuccessCount(successCount);
            record.setErrorCount(errorCount);
            updateById(record);
        }
    }

    @Override
    public void updateRecordFailed(BigInteger recordId, String errorInfo) {
        DatacenterTableImportRecord record = getById(recordId);
        if (record != null) {
            record.setStatus(2);
            if (errorInfo != null && errorInfo.length() > 1000) {
                errorInfo = errorInfo.substring(0, 1000);
            }
            record.setErrorInfo(errorInfo);
            updateById(record);
        }
    }
}
