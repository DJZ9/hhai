package tech.aiflowy.datacenter.service;

import cn.idev.excel.FastExcel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.constant.enums.EnumFieldType;
import tech.aiflowy.common.constant.enums.EnumYesOrNo;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.datacenter.entity.DatacenterTable;
import tech.aiflowy.datacenter.entity.DatacenterTableField;
import tech.aiflowy.datacenter.entity.vo.TableConfirmVo;
import tech.aiflowy.datacenter.excel.AiImportDataListener;

import jakarta.annotation.Resource;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步导入服务，用于AI智能导入后的批量数据导入
 */
@Service
public class TableImportAsyncService {

    private static final Logger log = LoggerFactory.getLogger(TableImportAsyncService.class);

    @Resource
    private DatacenterTableService tableService;

    @Resource
    private DatacenterTableImportRecordService importRecordService;

    @Resource(name = "default")
    private FileStorageService fileStorageService;

    /**
     * 异步导入多个表
     * 传递主线程上下文到异步线程
     *
     * @param confirmList 用户确认的表列表
     * @param account     当前登录用户（从主线程传递）
     * @param requestAttributes 主线程的 RequestAttributes（用于共享上下文）
     */
    @Async("datacenterImportThreadPool")
    public void asyncImportTables(List<TableConfirmVo> confirmList, LoginAccount account, RequestAttributes requestAttributes) {
        // 设置主线程上下文到异步线程
        RequestContextHolder.setRequestAttributes(requestAttributes, true);
        try {
            for (TableConfirmVo confirm : confirmList) {
                try {
                    asyncImportSingleTable(confirm, account);
                } catch (Exception e) {
                    // 捕获单个表导入异常，记录日志后继续处理下一个表
                    log.error("导入表 {} 失败，将继续处理后续表: {}", confirm.getTableName(), e.getMessage(), e);
                }
            }
            // 重置上下文
            RequestContextHolder.resetRequestAttributes();
        } catch (Exception e) {
            RequestContextHolder.resetRequestAttributes();
            throw e;
        }
    }

    /**
     * 异步导入单个表
     *
     * @param confirm 用户确认的表信息
     * @param account 当前登录用户
     */
    private void asyncImportSingleTable(TableConfirmVo confirm, LoginAccount account) {
        // 创建导入记录
        BigInteger recordId = importRecordService.createRecord(
                confirm.getOriginalFileName(),
                confirm.getTableName(),
                confirm.getCategoryId(),
                account
        );

        try {
            // 1. 创建表结构
            DatacenterTable table = createTableStructure(confirm, account);
            tableService.saveTable(table, account);

            // 2. 构建字段名映射表（用于数据导入）
            Map<String, String> fieldNameMapping = buildFieldNameMapping(confirm.getFields());

            // 3. 导入数据 - 从文件存储服务读取文件
            try (InputStream is = fileStorageService.readStream(confirm.getTempFilePath())) {
                if (is != null) {
                    AiImportDataListener listener = new AiImportDataListener(
                            table.getId(),
                            confirm.getFields(),
                            fieldNameMapping,
                            account,
                            tableService
                    );

                    FastExcel.read(is, listener)
                            .sheet()
                            .doRead();

                    // 更新导入记录
                    importRecordService.updateRecordSuccess(
                            recordId,
                            listener.getTotalCount(),
                            listener.getSuccessCount(),
                            listener.getErrorCount()
                    );
                } else {
                    log.warn("文件不存在: {}", confirm.getTempFilePath());
                    importRecordService.updateRecordFailed(recordId, "文件不存在");
                }
            }

        } catch (Exception e) {
            log.error("导入表失败: {}", confirm.getTableName(), e);
            importRecordService.updateRecordFailed(recordId, e.getMessage());
        }
    }

    /**
     * 创建表结构
     */
    private DatacenterTable createTableStructure(TableConfirmVo confirm, LoginAccount account) {
        DatacenterTable table = new DatacenterTable();

        // 在表名后方添加下划线和6位随机数
        java.util.Random random = new java.util.Random();
        int randomNumber = 100000 + random.nextInt(900000); // 生成100000-999999之间的6位随机数
        String tableNameWithSuffix = confirm.getTableName() + "_" + randomNumber;

        table.setTableName(tableNameWithSuffix);
        table.setTableDesc(confirm.getTableDesc());
        table.setCategoryId(confirm.getCategoryId());
        table.setDeptId(account.getDeptId());
        table.setTenantId(account.getTenantId());
        table.setCreated(new Date());
        table.setCreatedBy(account.getId());
        table.setModified(new Date());
        table.setModifiedBy(account.getId());

        // 系统保留字段列表（这些字段由系统自动添加，用户字段不能使用）
        List<String> reservedFields = java.util.Arrays.asList(
            "id", "dept_id", "tenant_id", "created", "created_by", "modified", "modified_by", "remark"
        );

        // 字段名映射表（原始字段名 -> 最终字段名）
        Map<String, String> fieldNameMapping = new HashMap<>();

        // 创建字段列表
        List<DatacenterTableField> fields = new ArrayList<>();
        for (TableConfirmVo.FieldConfirmVo fieldConfirm : confirm.getFields()) {
            String originalFieldName = fieldConfirm.getFieldName();
            String finalFieldName = originalFieldName;

            // 检测字段名是否与保留字段冲突
            if (reservedFields.contains(originalFieldName.toLowerCase())) {
                // 自动重命名冲突字段：添加 "user_" 前缀
                finalFieldName = "user_" + originalFieldName;
                log.warn("字段名 '{}' 与系统保留字段冲突，自动重命名为 '{}'", originalFieldName, finalFieldName);
            }

            // 记录字段名映射
            fieldNameMapping.put(originalFieldName, finalFieldName);

            DatacenterTableField field = new DatacenterTableField();
            field.setFieldName(finalFieldName);
            field.setFieldDesc(fieldConfirm.getFieldDesc());
            field.setFieldType(fieldConfirm.getFieldType() != null ? fieldConfirm.getFieldType() : EnumFieldType.STRING.getCode());
            field.setRequired(EnumYesOrNo.NO.getCode());
            field.setCreated(new Date());
            field.setCreatedBy(account.getId());
            fields.add(field);
        }
        table.setFields(fields);

        return table;
    }

    /**
     * 构建字段名映射表（用于数据导入时映射原始字段名到最终字段名）
     */
    private Map<String, String> buildFieldNameMapping(List<TableConfirmVo.FieldConfirmVo> originalFields) {
        // 系统保留字段列表
        List<String> reservedFields = java.util.Arrays.asList(
            "id", "dept_id", "tenant_id", "created", "created_by", "modified", "modified_by", "remark"
        );

        Map<String, String> fieldNameMapping = new HashMap<>();
        for (TableConfirmVo.FieldConfirmVo field : originalFields) {
            String originalName = field.getFieldName();
            String finalName = originalName;
            if (reservedFields.contains(originalName.toLowerCase())) {
                finalName = "user_" + originalName;
            }
            fieldNameMapping.put(originalName, finalName);
        }
        return fieldNameMapping;
    }
}
