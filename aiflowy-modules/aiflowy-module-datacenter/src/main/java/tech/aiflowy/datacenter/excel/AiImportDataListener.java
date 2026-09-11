package tech.aiflowy.datacenter.excel;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.read.listener.ReadListener;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.datacenter.entity.vo.TableConfirmVo;
import tech.aiflowy.datacenter.service.DatacenterTableService;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI智能导入数据监听器
 */
public class AiImportDataListener implements ReadListener<Map<Integer, Object>> {

    private static final Logger log = LoggerFactory.getLogger(AiImportDataListener.class);

    private final BigInteger tableId;
    private final List<TableConfirmVo.FieldConfirmVo> fields;
    private final Map<String, String> fieldNameMapping; // 字段名映射（原始字段名 -> 最终字段名）
    private final LoginAccount loginAccount;
    private final DatacenterTableService tableService;

    private final Map<String, Integer> fieldIndexMap = new HashMap<>();

    private int successCount = 0;
    private int errorCount = 0;
    private int totalCount = 0;
    private final List<JSONObject> errorRows = new ArrayList<>();

    public AiImportDataListener(BigInteger tableId, List<TableConfirmVo.FieldConfirmVo> fields,
                                  Map<String, String> fieldNameMapping, LoginAccount loginAccount,
                                  DatacenterTableService tableService) {
        this.tableId = tableId;
        this.fields = fields;
        this.fieldNameMapping = fieldNameMapping;
        this.loginAccount = loginAccount;
        this.tableService = tableService;

        // 构建字段索引映射（使用最终字段名）
        for (int i = 0; i < fields.size(); i++) {
            String originalFieldName = fields.get(i).getFieldName();
            String finalFieldName = fieldNameMapping.getOrDefault(originalFieldName, originalFieldName);
            fieldIndexMap.put(finalFieldName, i);
        }
    }

    @Override
    public void invokeHead(Map<Integer, cn.idev.excel.metadata.data.ReadCellData<?>> headMap, AnalysisContext context) {
        // 跳过表头处理，因为表结构已经由AI生成
    }

    @Override
    public void invoke(Map<Integer, Object> data, AnalysisContext context) {
        JSONObject obj = new JSONObject();
        for (int i = 0; i < fields.size(); i++) {
            TableConfirmVo.FieldConfirmVo field = fields.get(i);
            String originalFieldName = field.getFieldName();
            // 使用映射后的最终字段名
            String finalFieldName = fieldNameMapping.getOrDefault(originalFieldName, originalFieldName);
            Object value = data.get(i);
            obj.put(finalFieldName, value);
        }

        try {
            tableService.saveValue(tableId, obj, loginAccount);
            successCount++;
        } catch (Exception e) {
            errorCount++;
            log.error("导入数据失败: {}", obj, e);
            errorRows.add(obj);
        }
        totalCount++;
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("AI智能导入完成，总行数: {}, 成功: {}, 失败: {}", totalCount, successCount, errorCount);
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
