package tech.aiflowy.ai.agentsflex.ocrModel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.common.constant.enums.EnumOcrTaskStatus;
import tech.aiflowy.common.filestorage.xFileStorage.StorageConfig;
import tech.aiflowy.common.util.OkHttpUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 阿里百炼 OCR 模型实现类
 * 通过 HTTP 调用阿里百炼兼容模式 Responses API
 * 支持 PDF、图片等文档格式
 * 支持文件 URL 和 Base64 编码两种方式
 */
public class AliBailianOcrModel extends AliBailianOcrModelConfig implements OcrModel {

    private static final Logger log = LoggerFactory.getLogger(AliBailianOcrModel.class);

    private final AliBailianOcrModelConfig config;

    public AliBailianOcrModel() {
        super();
        this.config = this;
    }

    public AliBailianOcrModel(AliBailianOcrModelConfig config) {
        super();
        this.config = config;
        if (config != null) {
            this.setOcrTask(config.getOcrTask());
            this.setModelName(config.getModelName());
            this.setWorkspaceId(config.getWorkspaceId());
            this.setApiKey(config.getApiKey());
            this.setEndpoint(config.getEndpoint());
        }
    }

    @Override
    public String getText(String filePath) {
        try {
            String apiKey = this.getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                throw new BusinessException("阿里百炼 OCR API Key 不能为空");
            }

            // 构建请求体
            JSONObject requestBody = buildRequestBody(filePath);

            // 调用 API
            String url = config.getEndpoint() + "/responses";
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + apiKey);

            log.info("阿里百炼 OCR 请求 URL: {}", url);
            log.info("阿里百炼 OCR 请求模型: {}", config.getModelName());
            log.debug("阿里百炼 OCR 请求体: {}", requestBody.toJSONString());

            String response = OkHttpUtil.post(url, headers, requestBody.toJSONString());

            log.info("阿里百炼 OCR 响应: {}", response);

            return parseResponse(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里百炼 OCR 解析失败", e);
            throw new BusinessException("阿里百炼 OCR 解析失败: " + e.getMessage());
        }
    }

    @Override
    public String runAsync(String filePath) {
        throw new BusinessException("阿里百炼 OCR 暂不支持异步任务，请使用同步方式调用");
    }

    @Override
    public ParseResult queryTask(String taskId) {
        ParseResult result = new ParseResult();
        result.setStatus(EnumOcrTaskStatus.FAILED.getCode());
        result.setErrorMsg("阿里百炼 OCR 暂不支持异步任务查询");
        return result;
    }

    /**
     * 构建 Responses API 请求体
     */
    private JSONObject buildRequestBody(String filePath) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", config.getModelName());

        // 构建 ocr_options
        JSONObject ocrOptions = new JSONObject();
        ocrOptions.put("task", config.getOcrTask());
        requestBody.put("ocr_options", ocrOptions);

        // 构建 input
        JSONArray input = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");

        // 构建 content
        JSONArray content = new JSONArray();
        JSONObject fileContent = buildFileContent(filePath);
        content.add(fileContent);

        userMessage.put("content", content);
        input.add(userMessage);

        requestBody.put("input", input);

        return requestBody;
    }

    /**
     * 构建文件内容
     * 支持 URL 和 Base64 两种方式
     */
    private JSONObject buildFileContent(String filePath) throws IOException {
        JSONObject fileContent = new JSONObject();
        fileContent.put("type", "input_file");
        StorageConfig instance = StorageConfig.getInstance();
        String storageType = instance.getType();
        if ("xFileStorage".equals(storageType)) {
            // URL 方式
            fileContent.put("file_url", filePath);
        } else if ("local".equals(storageType)){
            // 本地文件，Base64 编码
            String fileName = extractFileName(filePath);
            String mimeType = detectMimeType(filePath);
            String base64Data = encodeFileToBase64(filePath, mimeType);

            fileContent.put("filename", fileName);
            fileContent.put("file_data", base64Data);
        }

        return fileContent;
    }

    /**
     * 将本地文件编码为 Base64 data URI
     */
    private String encodeFileToBase64(String pathOrUrl, String mimeType) throws IOException {
        byte[] fileBytes;
            // 网络文件：通过URL流读取
            URL url = new URL(pathOrUrl);
            try (var inputStream = url.openStream()) {
                fileBytes = inputStream.readAllBytes();
            }
        String base64String = Base64.getEncoder().encodeToString(fileBytes);
        return "data:" + mimeType + ";base64," + base64String;
    }

    /**
     * 从路径提取文件名
     */
    private String extractFileName(String filePath) {
        if (filePath == null) {
            return "document.pdf";
        }
        int lastSep = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSep >= 0 && lastSep < filePath.length() - 1) {
            return filePath.substring(lastSep + 1);
        }
        return filePath;
    }

    /**
     * 根据文件扩展名检测 MIME 类型
     */
    private String detectMimeType(String filePath) {
        String lowerPath = filePath.toLowerCase();
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put(".pdf", "application/pdf");
        mimeTypes.put(".png", "image/png");
        mimeTypes.put(".jpg", "image/jpeg");
        mimeTypes.put(".jpeg", "image/jpeg");
        mimeTypes.put(".gif", "image/gif");
        mimeTypes.put(".webp", "image/webp");
        mimeTypes.put(".bmp", "image/bmp");
        mimeTypes.put(".tiff", "image/tiff");
        mimeTypes.put(".tif", "image/tiff");

        for (Map.Entry<String, String> entry : mimeTypes.entrySet()) {
            if (lowerPath.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "application/octet-stream";
    }

    /**
     * 解析响应
     */
    private String parseResponse(String response) {
        JSONObject resObj = JSON.parseObject(response);

        // 检查错误
        String errorCode = resObj.getString("code");
        if (errorCode != null && !"Success".equals(errorCode) && !"200".equals(errorCode)) {
            String errorMsg = resObj.getString("message");
            throw new BusinessException("阿里百炼 OCR API 错误 [" + errorCode + "]: " + errorMsg);
        }

        if (resObj.containsKey("error")) {
            JSONObject error = resObj.getJSONObject("error");
            String message = error != null ? error.getString("message") : "未知错误";
            throw new BusinessException("阿里百炼 OCR API 错误: " + message);
        }

        // 检查 status
        String status = resObj.getString("status");
        if ("failed".equals(status)) {
            String errorMsg = resObj.getString("error_message");
            if (errorMsg == null) {
                errorMsg = "未知错误";
            }
            throw new BusinessException("阿里百炼 OCR 任务失败: " + errorMsg);
        }

        // 解析 output 数组
        JSONArray output = resObj.getJSONArray("output");
        if (output == null || output.isEmpty()) {
            throw new BusinessException("阿里百炼 OCR 响应中没有找到输出内容");
        }

        StringBuilder result = new StringBuilder();

        // 遍历所有 output 项
        for (int i = 0; i < output.size(); i++) {
            JSONObject outputItem = output.getJSONObject(i);

            // 尝试获取 message.content（部分响应格式）
            JSONObject messageObj = outputItem.getJSONObject("message");
            JSONArray contentArray = null;
            if (messageObj != null) {
                contentArray = messageObj.getJSONArray("content");
            }

            // 如果没有 message，直接获取 content
            if (contentArray == null) {
                contentArray = outputItem.getJSONArray("content");
            }

            if (contentArray == null || contentArray.isEmpty()) {
                continue;
            }

            // 遍历所有 content 项
            for (int j = 0; j < contentArray.size(); j++) {
                JSONObject contentItem = contentArray.getJSONObject(j);
                String text = extractTextFromContent(contentItem);
                if (!text.isEmpty()) {
                    result.append(text);
                }
            }
        }

        if (!result.isEmpty()) {
            return result.toString();
        }

        throw new BusinessException("阿里百炼 OCR 响应中没有找到结果");
    }

    /**
     * 从 content 项中提取文本
     */
    private String extractTextFromContent(JSONObject contentItem) {
        // 方式1: 从 ocr_result.text 中获取
        JSONObject ocrResult = contentItem.getJSONObject("ocr_result");
        if (ocrResult != null) {
            String text = contentItem.getString("text");
            if (text != null && !text.isEmpty()) {
                return text;
            }
        }


        return "";
    }

}