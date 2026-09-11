package tech.aiflowy.ai.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.agentsflex.core.model.chat.response.AiMessageResponse;
import com.agentsflex.core.prompt.SimplePrompt;
import com.mybatisflex.core.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.common.constant.enums.EnumFieldType;
import tech.aiflowy.datacenter.entity.DatacenterTable;
import tech.aiflowy.datacenter.entity.vo.FieldPreviewVo;
import tech.aiflowy.datacenter.entity.vo.TablePreviewVo;
import tech.aiflowy.datacenter.mapper.DatacenterTableMapper;
import tech.aiflowy.datacenter.utils.SqlInjectionUtils;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI智能生成服务，用于数据表导入时的表名、描述、字段名生成
 */
@Service
public class TableAiService {

    private static final Logger log = LoggerFactory.getLogger(TableAiService.class);

    /**
     * 最大重试次数（所有候选名均被占时重新生成）
     */
    private static final int MAX_RETRY = 3;

    @Resource
    private ModelService modelService;

    @Resource
    private DatacenterTableMapper tableMapper;

    /**
     * 一次性生成表名、描述和字段名。
     * AI返回10+个候选表名，自动选取第一个不存在的。
     * 若全部被占用，将所有失败表名一并发送给AI重新生成，最多重试3次。
     *
     * @param headers 列名列表
     * @param sampleData 示例数据
     * @return 表预览VO（包含表名、描述、字段列表）
     */
    public TablePreviewVo generateTablePreview(List<String> headers, List<Map<String, Object>> sampleData) {
        Model systemModel = modelService.getSystemModel();
        ChatModel chatModel = systemModel.toChatModel(null);
        ChatOptions options = new ChatOptions();

        // 第一次：完整生成（10+候选表名 + 描述 + 字段）
        String promptText = buildCombinedPrompt(headers, sampleData);
        JSONObject result;
        try {
            AiMessageResponse response = chatModel.chat(new SimplePrompt(promptText), options);
            if (response.isError()) {
                log.error("AI生成预览数据失败: {}", response.getErrorMessage());
                return generateFallbackPreview(headers, sampleData);
            }
            result = parseAiCombinedResponse(response.getMessage().getContent().trim());
        } catch (Exception e) {
            log.error("AI生成预览数据异常", e);
            return generateFallbackPreview(headers, sampleData);
        }

        String tableDesc = result.getString("tableDesc");

        // 从第一批候选中挑可用表名
        String tableName = pickAvailableFromResult(result);
        if (tableName != null) {
            return buildPreview(headers, sampleData, tableName, tableDesc, result);
        }

        // 全部被占用：收集所有失败的已存在表名
        Set<String> failedNames = collectExistingNames(result);
        log.warn("AI首批{}个候选表名均已被占用，开始重试", failedNames.size());

        // 重试：每次将所有失败表名一并发送
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            String failedNamesStr = failedNames.stream()
                    .sorted()
                    .collect(Collectors.joining(", "));
            log.info("第{}次重试，失败表名: {}", retry + 1, failedNamesStr);

            try {
                String renamePrompt = buildRenamePrompt(headers, sampleData, failedNamesStr);
                AiMessageResponse renameResponse = chatModel.chat(new SimplePrompt(renamePrompt), options);
                if (renameResponse.isError()) {
                    log.error("AI重命名失败: {}", renameResponse.getErrorMessage());
                    break;
                }

                List<String> newCandidates = parseCandidateNames(renameResponse.getMessage().getContent().trim());
                log.info("AI返回{}个新候选名", newCandidates.size());

                // 新增失败表名到排除集合
                failedNames.addAll(newCandidates);

                // 从新候选中挑第一个不存在的
                for (String candidate : newCandidates) {
                    String sanitized = sanitizeTableName(candidate);
                    if (!sanitized.isEmpty() && !isTableNameExists(sanitized)) {
                        log.info("第{}次重试选中表名: {}", retry + 1, sanitized);
                        return buildPreview(headers, sampleData, sanitized, tableDesc, result);
                    }
                }

                log.warn("第{}次重试的候选名也均被占用，累计失败{}个", retry + 1, failedNames.size());
            } catch (Exception e) {
                log.error("AI重命名异常，第{}次重试", retry + 1, e);
                break;
            }
        }

        // 兜底：使用第一个候选名
        JSONArray tableNames = result.getJSONArray("tableNames");
        tableName = (tableNames != null && !tableNames.isEmpty())
                ? sanitizeTableName(tableNames.getString(0))
                : sanitizeTableName(result.getString("tableName"));
        log.warn("超过最大重试次数，使用兜底表名: {}", tableName);
        return buildPreview(headers, sampleData, tableName, tableDesc, result);
    }

    /**
     * 构建合并提示词（一次调用生成所有信息，包含10+个候选表名）
     */
    private String buildCombinedPrompt(List<String> headers, List<Map<String, Object>> sampleData) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个数据表设计专家。请根据以下列名和示例数据，生成表名、表描述和字段信息。\n");
        sb.append("规则：\n");
        sb.append("1. tableNames: 英文候选表名数组（10个及以上且各不相同），表名只能包含小写字母、数字和下划线，必须以小写字母开头，最多43字符\n");
        sb.append("2. tableDesc: 表描述，必须使用中文简洁描述表的用途，最多200字符\n");
        sb.append("3. fields: 字段数组，每个字段包含fieldName(英文字段名)和fieldDesc(中文字段描述)\n");
        sb.append("4. fieldName只能包含小写字母、数字和下划线，必须以小写字母开头\n");
        sb.append("5. fieldDesc必须使用中文描述字段的含义，保留原始列名的语义\n\n");
        sb.append("原始列名: ").append(headers).append("\n");
        sb.append("示例数据（前5行）: ").append(sampleData.subList(0, Math.min(5, sampleData.size()))).append("\n\n");
        sb.append("请返回JSON格式：{\"tableNames\": [\"候选1\", \"候选2\", ...共10+个], \"tableDesc\": \"中文表描述\", \"fields\": [{\"fieldName\": \"字段名\", \"fieldDesc\": \"中文字段描述\"}]}\n");
        sb.append("只返回JSON，不要包含任何解释或额外内容。");
        return sb.toString();
    }

    /**
     * 从AI结果中挑选第一个数据库中不存在的候选表名
     * 遍历过程中顺便把不存在的也返回，找到则返回；全都不存在则返回null
     */
    private String pickAvailableFromResult(JSONObject result) {
        JSONArray tableNames = result.getJSONArray("tableNames");
        if (tableNames != null && !tableNames.isEmpty()) {
            for (int i = 0; i < tableNames.size(); i++) {
                String name = sanitizeTableName(tableNames.getString(i));
                if (!name.isEmpty() && !isTableNameExists(name)) {
                    log.info("选中候选表名: {}", name);
                    return name;
                }
            }
        }
        // 兼容旧格式：单个 tableName
        String tableName = sanitizeTableName(result.getString("tableName"));
        if (!tableName.isEmpty() && !isTableNameExists(tableName)) {
            return tableName;
        }
        return null;
    }

    /**
     * 收集AI结果中所有已存在于数据库的候选表名
     */
    private Set<String> collectExistingNames(JSONObject result) {
        Set<String> existing = new HashSet<>();
        JSONArray tableNames = result.getJSONArray("tableNames");
        if (tableNames != null) {
            for (int i = 0; i < tableNames.size(); i++) {
                String name = sanitizeTableName(tableNames.getString(i));
                if (!name.isEmpty() && isTableNameExists(name)) {
                    existing.add(name);
                }
            }
        }
        // 兼容旧格式
        String tableName = sanitizeTableName(result.getString("tableName"));
        if (!tableName.isEmpty() && isTableNameExists(tableName)) {
            existing.add(tableName);
        }
        return existing;
    }

    /**
     * 构建重命名提示词：告知AI所有已存在的表名，要求生成新的候选
     */
    private String buildRenamePrompt(List<String> headers, List<Map<String, Object>> sampleData, String failedNamesStr) {
        StringBuilder sb = new StringBuilder();
        sb.append("你之前根据以下数据生成了表名候选，但以下表名全部已被占用：\n");
        sb.append(failedNamesStr).append("\n\n");
        sb.append("请重新生成10个及以上新的英文候选表名（不要与上面重复），规则同前：只能包含小写字母、数字和下划线，必须以小写字母开头，最多43字符。\n\n");
        sb.append("原始列名: ").append(headers).append("\n");
        sb.append("示例数据: ").append(sampleData.subList(0, Math.min(5, sampleData.size()))).append("\n\n");
        sb.append("只返回候选表名，每行一个，不要包含任何JSON、序号、解释或额外内容。");
        return sb.toString();
    }

    /**
     * 从AI重命名响应中解析候选表名列表（每行一个）
     */
    private List<String> parseCandidateNames(String content) {
        List<String> names = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return names;
        }
        for (String line : content.split("\\n")) {
            String name = line.trim()
                    .replaceFirst("^\\d+[.、)\\s]*", "")
                    .replaceAll("[\"'`,]", "")
                    .trim();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * 解析合并响应
     */
    private JSONObject parseAiCombinedResponse(String content) {
        // 尝试直接解析
        try {
            return JSONObject.parseObject(content);
        } catch (Exception e) {
            // 尝试提取JSON部分
            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");
            if (start >= 0 && end > start) {
                String jsonStr = content.substring(start, end + 1);
                return JSONObject.parseObject(jsonStr);
            }
        }
        return new JSONObject();
    }

    /**
     * 检查表名是否已存在于数据库中（实际表名为 tb_data_{tableName}）
     */
    private boolean isTableNameExists(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return false;
        }
        String actualTable = "tb_data_" + tableName;
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.eq(DatacenterTable::getActualTable, actualTable);
        return tableMapper.selectCountByQuery(wrapper) > 0;
    }

    /**
     * 将AI解析结果组装为 TablePreviewVo
     */
    private TablePreviewVo buildPreview(List<String> headers, List<Map<String, Object>> sampleData,
                                        String tableName, String tableDesc, JSONObject result) {
        TablePreviewVo preview = new TablePreviewVo();
        preview.setTableName(tableName);

        if (tableDesc == null || tableDesc.isEmpty()) {
            tableDesc = tableName;
        }
        if (tableDesc.length() > 200) {
            tableDesc = tableDesc.substring(0, 200);
        }
        preview.setTableDesc(tableDesc);

        JSONArray fieldsArray = result.getJSONArray("fields");
        List<FieldPreviewVo> fields = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            FieldPreviewVo field = new FieldPreviewVo();
            field.setOriginalName(headers.get(i));

            if (fieldsArray != null && i < fieldsArray.size()) {
                JSONObject fieldObj = fieldsArray.getJSONObject(i);
                field.setFieldName(sanitizeFieldName(fieldObj.getString("fieldName")));
                field.setFieldDesc(fieldObj.getString("fieldDesc"));
            } else {
                field.setFieldName(sanitizeFieldName(headers.get(i)));
                field.setFieldDesc(headers.get(i));
            }

            field.setFieldType(EnumFieldType.STRING.getCode());
            fields.add(field);
        }
        preview.setFields(fields);
        preview.setSampleData(sampleData);
        return preview;
    }

    /**
     * 清理表名，确保符合规范
     */
    private String sanitizeTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return "data_table";
        }
        // 转小写
        tableName = tableName.toLowerCase();
        // 替换非规范字符
        tableName = tableName.replaceAll("[^a-z0-9_]", "_");
        // 合并连续下划线
        tableName = tableName.replaceAll("_+", "_");
        // 去掉开头结尾下划线
        tableName = tableName.replaceAll("^_|_$", "");
        // 确保以字母开头
        if (!tableName.matches("^[a-z].*")) {
            tableName = "t_" + tableName;
        }
        // 限制长度（实际表名格式为 tb_data_{tableName}，tb_data_ = 8字符，总长度不超过64，tableName最多43字符）
        if (tableName.length() > 43) {
            tableName = tableName.substring(0, 43);
        }
        // SQL注入检查
        SqlInjectionUtils.checkIdentifier(tableName);
        return tableName;
    }

    /**
     * 清理字段名，确保符合规范
     */
    private String sanitizeFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "field";
        }
        // 转小写
        fieldName = fieldName.toLowerCase();
        // 替换非规范字符
        fieldName = fieldName.replaceAll("[^a-z0-9_]", "_");
        // 合并连续下划线
        fieldName = fieldName.replaceAll("_+", "_");
        // 去掉开头结尾下划线
        fieldName = fieldName.replaceAll("^_|_$", "");
        // 确保以字母开头
        if (!fieldName.matches("^[a-z].*")) {
            fieldName = "f_" + fieldName;
        }
        // 限制长度
        if (fieldName.length() > 50) {
            fieldName = fieldName.substring(0, 50);
        }
        return fieldName;
    }

    /**
     * 生成备用表名（当AI失败时使用）
     */
    private String generateFallbackTableName(List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "data_table";
        }
        // 使用第一个列名作为基础
        String firstHeader = headers.get(0);
        return sanitizeFieldName(firstHeader) + "_table";
    }

    /**
     * 生成备用字段名列表（当AI失败时使用）
     */
    private List<FieldPreviewVo> generateFallbackFieldNames(List<String> originalHeaders) {
        List<FieldPreviewVo> fields = new ArrayList<>();
        for (String header : originalHeaders) {
            FieldPreviewVo field = new FieldPreviewVo();
            field.setOriginalName(header);
            field.setFieldName(sanitizeFieldName(header));
            field.setFieldDesc(header);
            field.setFieldType(EnumFieldType.STRING.getCode());
            fields.add(field);
        }
        return fields;
    }

    /**
     * 生成备用预览数据（当AI失败时使用）
     */
    private TablePreviewVo generateFallbackPreview(List<String> headers, List<Map<String, Object>> sampleData) {
        TablePreviewVo preview = new TablePreviewVo();
        preview.setTableName(generateFallbackTableName(headers));
        // 使用中文描述（当AI失败时，使用原始列名作为描述）
        preview.setTableDesc("数据表-" + (headers.isEmpty() ? "默认" : headers.get(0)));
        preview.setFields(generateFallbackFieldNames(headers));
        preview.setSampleData(sampleData);
        return preview;
    }
}
