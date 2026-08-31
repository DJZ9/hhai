package tech.aiflowy.ai.agentsflex.ocrModel;

import com.agentsflex.core.model.ocr.BaseOcrModel;
import com.agentsflex.core.model.ocr.OcrRequest;
import com.agentsflex.core.model.ocr.OcrResponse;
import com.agentsflex.core.model.ocr.OcrTaskStatus;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.common.util.OkHttpUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 阿里百炼 OCR 模型实现类
 * 通过 HTTP 调用阿里百炼兼容模式 Responses API
 * 支持 PDF、图片等文档格式
 * 支持文件 URL 和 Base64 编码两种方式
 *
 * <p>阿里百炼仅支持同步识别，{@link #recognize(OcrRequest)} 直接调用 /responses 并返回
 * {@link OcrTaskStatus#SUCCEEDED} + 文本内容；{@link #getResult(String)} 返回不支持异步的错误。</p>
 */
public class AliBailianOcrModel extends BaseOcrModel<AliBailianOcrModelConfig> {

    private static final Logger log = LoggerFactory.getLogger(AliBailianOcrModel.class);

    public AliBailianOcrModel(AliBailianOcrModelConfig config) {
        super(config);
    }

    @Override
    public OcrResponse recognize(OcrRequest request) {
        OcrResponse error = validateRequest(request);
        if (error != null) {
            return error;
        }
        try {
            String apiKey = config.getApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                return OcrResponse.error("阿里百炼 OCR API Key 不能为空");
            }

            String filePath = resolveFilePath(request);
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

            String text = parseResponse(response);
            OcrResponse ocrResponse = new OcrResponse();
            ocrResponse.setStatus(OcrTaskStatus.SUCCEEDED);
            ocrResponse.setText(text);
            return ocrResponse;
        } catch (BusinessException e) {
            return OcrResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("阿里百炼 OCR 解析失败", e);
            return OcrResponse.error("阿里百炼 OCR 解析失败: " + e.getMessage());
        }
    }

    @Override
    public OcrResponse getResult(String taskId) {
        return OcrResponse.error("阿里百炼 OCR 暂不支持异步任务查询，请使用同步方式调用");
    }

    /**
     * 从请求中解析出文件路径（优先 URL，其次本地文件）。
     */
    private String resolveFilePath(OcrRequest request) {
        if (request.getFileUrl() != null && !request.getFileUrl().trim().isEmpty()) {
            return request.getFileUrl();
        }
        if (request.getFile() != null) {
            return request.getFile().getAbsolutePath();
        }
        return null;
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

        // 追加文本指令（官方示例推荐）
        JSONObject textContent = new JSONObject();
        textContent.put("type", "input_text");
        textContent.put("text", "Read all the text in the file.");
        content.add(textContent);

        userMessage.put("content", content);
        input.add(userMessage);

        requestBody.put("input", input);

        return requestBody;
    }

    /**
     * 构建文件内容：
     * 1. http/https 公网地址 → input_file + file_url（保持原有逻辑，input_file 仅支持公网 URL）
     * 2. 本地图片文件 → input_image + image_url（Base64 Data URI，官方支持）
     * 3. 本地 PDF 等非图片文件 → 不支持 Base64 传文件，抛出明确错误
     */
    private JSONObject buildFileContent(String filePath) throws IOException {
        JSONObject fileContent = new JSONObject();
        if (isHttpUrl(filePath)) {
            // URL 方式
            fileContent.put("type", "input_file");
            fileContent.put("file_url", filePath);
            return fileContent;
        }
        // 本地文件，Base64 编码
        String mimeType = detectMimeType(filePath);
        if (mimeType.startsWith("image/")) {
            fileContent.put("type", "input_image");
            fileContent.put("image_url", encodeFileToBase64(filePath, mimeType));
            return fileContent;
        }
        throw new BusinessException("阿里百炼 OCR 的 input_file 仅支持公网 URL，不支持 Base64 传文件，本地 " + mimeType + " 文件无法校验，请上传至可公网访问的存储后传入文件 URL");
    }

    /**
     * 判断是否为 http/https 地址
     */
    private boolean isHttpUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

    /**
     * 将文件编码为 Base64 data URI：
     * 本地文件直接读取；网络文件通过 URL 流读取
     */
    private String encodeFileToBase64(String pathOrUrl, String mimeType) throws IOException {
        byte[] fileBytes;
        Path path = Paths.get(pathOrUrl);
        if (Files.exists(path)) {
            // 本地文件
            fileBytes = Files.readAllBytes(path);
        } else {
            // 网络文件：通过URL流读取
            URL url = new URL(pathOrUrl);
            try (var inputStream = url.openStream()) {
                fileBytes = inputStream.readAllBytes();
            }
        }
        String base64String = Base64.getEncoder().encodeToString(fileBytes);
        return "data:" + mimeType + ";base64," + base64String;
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
     * 从 content 项中提取文本：
     * 1. output_text 直接携带 text
     * 2. 定制任务结果 ocr_result.text / ocr_result.markdown / ocr_result.content
     */
    private String extractTextFromContent(JSONObject contentItem) {
        String text = contentItem.getString("text");
        if (text != null && !text.isEmpty()) {
            return text;
        }
        JSONObject ocrResult = contentItem.getJSONObject("ocr_result");
        if (ocrResult != null) {
            for (String key : new String[]{"text", "markdown", "content"}) {
                String value = ocrResult.getString(key);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return "";
    }

}
