package tech.aiflowy.common.ai.rag;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.read.listener.ReadListener;
import com.alibaba.fastjson2.JSON;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 将 Excel（xlsx）文件内容解析为 JSON 二维数组字符串，供 {@link ExcelDocumentSplitter} 按行分段使用。
 * <p>
 * 输出格式：{@code [["表头1","表头2"],["值1","值2"], ...]}
 *
 * @author wgq
 */
public final class ExcelTableExtractor {

    private ExcelTableExtractor() {
    }

    /**
     * 读取第一个 sheet，转换为 JSON 二维数组字符串。
     *
     * @param inputStream Excel 文件流
     * @return JSON 字符串；无有效数据时返回 {@code "[]"}
     */
    public static String toJsonTable(InputStream inputStream) {
        List<LinkedHashMap<Integer, Object>> rawRows = new ArrayList<>();

        FastExcel.read(inputStream, new ReadListener<LinkedHashMap<Integer, Object>>() {
            @Override
            public void invoke(LinkedHashMap<Integer, Object> data, AnalysisContext context) {
                rawRows.add(data);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                // do nothing
            }
        }).headRowNumber(0).sheet(0).doRead();

        return JSON.toJSONString(toTable(rawRows));
    }

    /**
     * 将按列索引存放的行数据整理为等宽的二维字符串表格，跳过全空行。
     */
    private static List<List<String>> toTable(List<LinkedHashMap<Integer, Object>> rawRows) {
        if (rawRows.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算最大列数，保证每行列数一致（Markdown 表格需要对齐）
        int maxColumns = 0;
        for (LinkedHashMap<Integer, Object> row : rawRows) {
            for (Integer columnIndex : row.keySet()) {
                if (columnIndex != null) {
                    maxColumns = Math.max(maxColumns, columnIndex + 1);
                }
            }
        }

        List<List<String>> table = new ArrayList<>(rawRows.size());
        for (LinkedHashMap<Integer, Object> row : rawRows) {
            List<String> line = new ArrayList<>(maxColumns);
            boolean blank = true;
            for (int i = 0; i < maxColumns; i++) {
                Object value = row.get(i);
                String text = value == null ? "" : String.valueOf(value);
                if (!text.isBlank()) {
                    blank = false;
                }
                line.add(text);
            }
            if (!blank) {
                table.add(line);
            }
        }
        return table;
    }
}