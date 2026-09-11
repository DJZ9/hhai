package tech.aiflowy.datacenter.excel;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.metadata.data.ReadCellData;
import cn.idev.excel.read.listener.ReadListener;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.common.constant.enums.EnumFieldType;
import tech.aiflowy.common.constant.enums.EnumYesOrNo;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.datacenter.entity.DatacenterTable;
import tech.aiflowy.datacenter.entity.DatacenterTableField;
import tech.aiflowy.datacenter.service.DatacenterTableService;
import tech.aiflowy.datacenter.utils.SqlInjectionUtils;

import java.math.BigInteger;
import java.util.*;

public class ImportTableListener implements ReadListener<LinkedHashMap<Integer, Object>> {

    private static final Logger log = LoggerFactory.getLogger(ImportTableListener.class);
    private static final Set<String> RESERVED_FIELDS = new HashSet<>(Arrays.asList(
            "id",
            "dept_id",
            "tenant_id",
            "created",
            "created_by",
            "modified",
            "modified_by",
            "remark"
    ));

    private final String tableName;
    private final LoginAccount loginAccount;
    private final DatacenterTableService tableService;
    private final BigInteger categoryId;

    private BigInteger tableId;
    private List<DatacenterTableField> fields = new ArrayList<>();
    private final Map<String, Integer> headFieldIndex = new HashMap<>();

    private int successCount = 0;
    private int errorCount = 0;
    private int totalCount = 0;
    private final List<JSONObject> errorRows = new ArrayList<>();

    public ImportTableListener(String tableName, LoginAccount loginAccount, BigInteger categoryId) {
        this.tableName = tableName;
        this.loginAccount = loginAccount;
        this.categoryId = categoryId;
        this.tableService = SpringContextUtil.getBean(DatacenterTableService.class);
    }

    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
        if (headMap == null || headMap.isEmpty()) {
            throw new BusinessException("表头不能为空");
        }
        List<Map.Entry<Integer, ReadCellData<?>>> entries = new ArrayList<>(headMap.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        Set<String> fieldNameSet = new HashSet<>();
        List<DatacenterTableField> tableFields = new ArrayList<>();
        for (Map.Entry<Integer, ReadCellData<?>> entry : entries) {
            ReadCellData<?> cellData = entry.getValue();
            String raw = cellData == null ? null : cellData.getStringValue();
            String rawNumberValue = cellData == null ? null : String.valueOf(cellData.getNumberValue());
            if (StrUtil.isBlank(raw) && StrUtil.isBlank(rawNumberValue)) {
                throw new BusinessException("表头字段不能为空");
            }
            if (StrUtil.isBlank(raw)) {
                raw = rawNumberValue;
            }
            String fieldName = raw.trim().replaceAll(" ", "");
            fieldName = fieldName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
            // 把连续的下划线合并成一个
            fieldName = fieldName.replaceAll("_+", "_");
            // 去掉开头和结尾的下划线
            fieldName = fieldName.replaceAll("^_|_$", "");
            validateFieldName(fieldName);

            String fieldNameLower = fieldName.toLowerCase(Locale.ROOT);
            if (RESERVED_FIELDS.contains(fieldNameLower)) {
                throw new BusinessException("表头字段不能使用系统保留字段：" + fieldName);
            }
            if (!fieldNameSet.add(fieldNameLower)) {
                throw new BusinessException("表头字段重复：" + fieldName);
            }
            headFieldIndex.put(fieldName, entry.getKey());

            DatacenterTableField field = new DatacenterTableField();
            field.setFieldName(fieldName);
            field.setFieldType(EnumFieldType.STRING.getCode());
            field.setRequired(EnumYesOrNo.NO.getCode());
            tableFields.add(field);
        }
        if (tableFields.isEmpty()) {
            throw new BusinessException("表头字段不能为空");
        }

        DatacenterTable table = new DatacenterTable();
        table.setTableName(tableName);
        table.setFields(tableFields);
        table.setCategoryId(categoryId);
        table.setDeptId(loginAccount.getDeptId());
        table.setTenantId(loginAccount.getTenantId());
        Date now = new Date();
        table.setCreated(now);
        table.setCreatedBy(loginAccount.getId());
        table.setModified(now);
        table.setModifiedBy(loginAccount.getId());

        tableService.saveTable(table, loginAccount);
        this.tableId = table.getId();
        this.fields = tableFields;
    }

    @Override
    public void invoke(LinkedHashMap<Integer, Object> data, AnalysisContext context) {
        if (tableId == null) {
            throw new BusinessException("表结构初始化失败");
        }
        JSONObject obj = new JSONObject();
        for (DatacenterTableField field : fields) {
            Integer index = headFieldIndex.get(field.getFieldName());
            if (index != null) {
                obj.put(field.getFieldName(), data.get(index));
            }
        }
        try {
            tableService.saveValue(tableId, obj, loginAccount);
            successCount++;
        } catch (Exception e) {
            errorCount++;
            log.error("导入数据到数据中心表失败，具体值：{}", obj, e);
            errorRows.add(obj);
        }
        totalCount++;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {

    }

    private void validateFieldName(String fieldName) {
        SqlInjectionUtils.checkIdentifier(fieldName);
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<JSONObject> getErrorRows() {
        return errorRows;
    }
}
