package tech.aiflowy.datacenter.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.datacenter.entity.DatacenterTableImportRecord;

import java.math.BigInteger;

/**
 * 数据中枢表导入记录 Service
 */
public interface DatacenterTableImportRecordService extends IService<DatacenterTableImportRecord> {

    /**
     * 创建导入记录（状态为处理中）
     */
    BigInteger createRecord(String fileName, String tableName, BigInteger categoryId, LoginAccount account);

    /**
     * 更新导入记录为成功
     */
    void updateRecordSuccess(BigInteger recordId, Integer totalCount, Integer successCount, Integer errorCount);

    /**
     * 更新导入记录为失败
     */
    void updateRecordFailed(BigInteger recordId, String errorInfo);
}
