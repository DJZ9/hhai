package tech.aiflowy.datacenter.service;

import cn.idev.excel.FastExcel;
import cn.idev.excel.read.listener.ReadListener;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.metadata.data.ReadCellData;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.datacenter.entity.vo.FieldPreviewVo;
import tech.aiflowy.datacenter.entity.vo.TablePreviewVo;

import jakarta.annotation.Resource;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 异步解析服务，用于AI智能导入的表结构解析
 */
@Service
public class TableParseAsyncService {

    private static final Logger log = LoggerFactory.getLogger(TableParseAsyncService.class);

    @Resource
    private DatacenterTableParseTaskService parseTaskService;

    @Resource(name = "default")
    private FileStorageService fileStorageService;

    /**
     * 异步执行解析任务
     * 传递主线程上下文到异步线程
     *
     * @param taskId            任务ID
     * @param files             文件列表
     * @param categoryId        分类ID
     * @param aiGenerateFunction AI生成函数（从Controller传入，避免模块依赖问题）
     * @param requestAttributes 主线程的 RequestAttributes（用于共享上下文）
     */
    @Async("datacenterImportThreadPool")
    public void asyncParseTables(BigInteger taskId, List<FileItemVo> files, BigInteger categoryId,
                                  Function<AiGenerateInput, TablePreviewVo> aiGenerateFunction,
                                  RequestAttributes requestAttributes) {
        log.info("开始异步解析任务: taskId={}", taskId);

        // 设置主线程上下文到异步线程
        RequestContextHolder.setRequestAttributes(requestAttributes, true);
        try {
            // 更新进度：开始解析
            parseTaskService.updateProgress(taskId, 10, "正在准备解析文件...");

            List<TablePreviewVo> tables = new ArrayList<>();
            int totalFiles = files.size();
            int processedFiles = 0;

            for (FileItemVo fileItem : files) {
                processedFiles++;
                int baseProgress = 10 + (int)((processedFiles - 1) * 70.0 / totalFiles);

                // 更新进度：解析单个文件
                parseTaskService.updateProgress(taskId, baseProgress,
                        "正在解析文件: " + fileItem.getOriginalFileName() + " (" + processedFiles + "/" + totalFiles + ")");

                try {
                    TablePreviewVo tablePreview = parseSingleFile(fileItem, categoryId, aiGenerateFunction);
                    tables.add(tablePreview);

                    // 更新进度：单个文件解析完成
                    int fileProgress = baseProgress + (int)(70.0 / totalFiles);
                    parseTaskService.updateProgress(taskId, fileProgress,
                            "文件 " + fileItem.getOriginalFileName() + " 解析完成");

                } catch (Exception e) {
                    log.error("解析文件失败: {}", fileItem.getOriginalFileName(), e);
                    // 单个文件失败不影响其他文件，继续处理
                    TablePreviewVo failedTable = new TablePreviewVo();
                    failedTable.setOriginalFileName(fileItem.getOriginalFileName());
                    failedTable.setErrorInfo("解析失败: " + e.getMessage());
                    tables.add(failedTable);
                }
            }

            // 更新进度：保存结果
            parseTaskService.updateProgress(taskId, 90, "正在保存解析结果...");

            // 构建结果数据
            JSONObject resultData = new JSONObject();
            JSONArray tablesArray = new JSONArray();
            for (TablePreviewVo table : tables) {
                JSONObject tableJson = new JSONObject();
                tableJson.put("originalFileName", table.getOriginalFileName());
                tableJson.put("tableName", table.getTableName());
                tableJson.put("tableDesc", table.getTableDesc());
                tableJson.put("categoryId", table.getCategoryId());
                tableJson.put("tempFilePath", table.getTempFilePath());
                tableJson.put("totalRows", table.getTotalRows());
                tableJson.put("errorInfo", table.getErrorInfo());

                // 转换字段列表
                JSONArray fieldsArray = new JSONArray();
                if (table.getFields() != null) {
                    for (FieldPreviewVo field : table.getFields()) {
                        JSONObject fieldJson = new JSONObject();
                        fieldJson.put("originalName", field.getOriginalName());
                        fieldJson.put("fieldName", field.getFieldName());
                        fieldJson.put("fieldDesc", field.getFieldDesc());
                        fieldJson.put("fieldType", field.getFieldType());
                        fieldsArray.add(fieldJson);
                    }
                }
                tableJson.put("fields", fieldsArray);

                // 转换样本数据
                if (table.getSampleData() != null) {
                    tableJson.put("sampleData", table.getSampleData());
                }

                tablesArray.add(tableJson);
            }
            resultData.put("tables", tablesArray);

            // 更新任务成功
            parseTaskService.updateSuccess(taskId, resultData.toJSONString());
            log.info("解析任务完成: taskId={}", taskId);

        } catch (Exception e) {
            log.error("解析任务失败: taskId={}", taskId, e);
            parseTaskService.updateFailed(taskId, e.getMessage());
        } finally {
            // 重置上下文
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * 解析单个文件
     */
    private TablePreviewVo parseSingleFile(FileItemVo fileItem, BigInteger categoryId,
                                            Function<AiGenerateInput, TablePreviewVo> aiGenerateFunction) throws Exception {
        // 从文件存储服务获取文件流
        try (InputStream is = fileStorageService.readStream(fileItem.getPath())) {
            if (is == null) {
                throw new RuntimeException("文件不存在: " + fileItem.getPath());
            }

            // 读取表头和前几行数据（只读取前100行用于预览）
            List<String> headers = new ArrayList<>();
            List<Map<String, Object>> sampleData = new ArrayList<>();
            int maxPreviewRows = 100;

            // 使用数组来存储总行数（因为需要在匿名类中修改）
            final int[] totalRowCount = {0};

            // 读取Excel
            FastExcel.read(is, new ReadListener<Map<Integer, Object>>() {
                private int rowCount = 0;

                @Override
                public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
                    for (Map.Entry<Integer, ReadCellData<?>> entry : headMap.entrySet()) {
                        String header = entry.getValue().getStringValue();
                        if (header == null) {
                            header = String.valueOf(entry.getValue().getNumberValue());
                        }
                        headers.add(header != null ? header.trim() : "column_" + entry.getKey());
                    }
                }

                @Override
                public void invoke(Map<Integer, Object> data, AnalysisContext context) {
                    rowCount++;
                    totalRowCount[0] = rowCount;
                    if (rowCount <= maxPreviewRows) {
                        if (rowCount <= 5) { // 只保存前5行作为样本数据
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 0; i < headers.size(); i++) {
                                row.put(headers.get(i), data.get(i));
                            }
                            sampleData.add(row);
                        }
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();

            // 使用传入的AI生成函数生成表名、描述、字段名
            AiGenerateInput input = new AiGenerateInput(headers, sampleData);
            TablePreviewVo aiPreview = aiGenerateFunction.apply(input);

            // 构建预览VO
            TablePreviewVo preview = new TablePreviewVo();
            preview.setOriginalFileName(fileItem.getOriginalFileName());
            preview.setTableName(aiPreview.getTableName());
            preview.setTableDesc(aiPreview.getTableDesc());
            preview.setCategoryId(categoryId);
            preview.setFields(aiPreview.getFields());
            preview.setTotalRows(totalRowCount[0]);
            preview.setSampleData(sampleData);
            preview.setTempFilePath(fileItem.getPath());

            return preview;
        }
    }

    /**
     * 文件项VO
     */
    public static class FileItemVo {
        private String path;
        private String originalFileName;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }
    }

    /**
     * AI生成输入参数
     */
    public static class AiGenerateInput {
        private List<String> headers;
        private List<Map<String, Object>> sampleData;

        public AiGenerateInput(List<String> headers, List<Map<String, Object>> sampleData) {
            this.headers = headers;
            this.sampleData = sampleData;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<Map<String, Object>> getSampleData() {
            return sampleData;
        }
    }
}
