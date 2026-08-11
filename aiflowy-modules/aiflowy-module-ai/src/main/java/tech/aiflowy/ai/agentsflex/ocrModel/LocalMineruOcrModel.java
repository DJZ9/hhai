package tech.aiflowy.ai.agentsflex.ocrModel;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import tech.aiflowy.common.constant.enums.EnumOcrTaskStatus;
import tech.aiflowy.common.filestorage.CustomMultipartFile;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.util.OkHttpUtil;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 本地 MinerU OCR 模型实现类
 * 用于调用本地部署的 MinerU 文档解析 API（FastAPI 服务）
 */
public class LocalMineruOcrModel extends LocalMineruOcrModelConfig implements OcrModel {

    private static final Logger log = LoggerFactory.getLogger(LocalMineruOcrModel.class);

    // 任务状态常量
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_ERROR = "error";

    // API 路径
    private static final String SYNC_PARSE_PATH = "/file_parse";
    private static final String CREATE_TASK_PATH = "/tasks";
    private static final String QUERY_TASK_PATH_TEMPLATE = "/tasks/%s";
    private static final String RESULT_TASK_PATH_TEMPLATE = "/tasks/%s/result";

    public LocalMineruOcrModel() {
        super();
    }

    public LocalMineruOcrModel(LocalMineruOcrModelConfig config) {
        super();
        if (config != null) {
            BeanUtils.copyProperties(config, this);
        }
    }

    @Override
    public String getText(String filePath) {
        Response response = null;
        try {
            response = uploadFile(filePath, getEndpoint() + SYNC_PARSE_PATH);
            if (Boolean.TRUE.equals(getResponseFormatZip()) || isZipResponse(response)) {
                try (InputStream is = response.body().byteStream()) {
                    return downloadAndExtractMarkdown(is);
                }
            }
            String responseBody = parseResponseBody(response);
            return extractContent(responseBody);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("本地 MinerU 同步解析失败", e);
            throw new BusinessException("本地 MinerU 同步解析失败: " + e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public String runAsync(String filePath) {
        Response response = null;
        try {
            response = uploadFile(filePath, getEndpoint() + CREATE_TASK_PATH);
            String responseBody = parseResponseBody(response);
            JSONObject resObj = JSON.parseObject(responseBody);
            String taskId = resObj.getString("task_id");
            if (taskId == null || taskId.isEmpty()) {
                throw new BusinessException("本地 MinerU 未返回 task_id");
            }
            log.info("本地 MinerU 异步任务创建成功，task_id: {}", taskId);
            return taskId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("本地 MinerU 创建异步任务失败", e);
            throw new BusinessException("本地 MinerU 创建任务失败: " + e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public ParseResult queryTask(String taskId) {
        ParseResult result = new ParseResult();
        try {
            String queryUrl = getEndpoint() + String.format(QUERY_TASK_PATH_TEMPLATE, taskId);
            log.info("查询本地 MinerU 任务状态，URL: {}", queryUrl);

            String response = OkHttpUtil.get(queryUrl, getHeaders());
            if (response == null || response.isEmpty()) {
                result.setStatus(EnumOcrTaskStatus.IN_PROGRESS.getCode());
                return result;
            }

            JSONObject statusObj = JSON.parseObject(response);
            String status = statusObj.getString("status");

            if (STATUS_COMPLETED.equals(status) || STATUS_SUCCESS.equals(status)) {
                String resultUrl = getEndpoint() + String.format(RESULT_TASK_PATH_TEMPLATE, taskId);
                log.info("本地 MinerU 任务完成，获取结果，URL: {}", resultUrl);
                String content;
                if (Boolean.TRUE.equals(getResponseFormatZip())) {
                    try (InputStream is = OkHttpUtil.getInputStream(resultUrl)) {
                        content = downloadAndExtractMarkdown(is);
                    }
                } else {
                    String resultResponse = OkHttpUtil.get(resultUrl, getHeaders());
                    content = extractContent(resultResponse);
                }
                result.setStatus(EnumOcrTaskStatus.COMPLETED.getCode());
                result.setContent(content);
            } else if (STATUS_FAILED.equals(status) || STATUS_ERROR.equals(status)) {
                result.setStatus(EnumOcrTaskStatus.FAILED.getCode());
                result.setErrorMsg(statusObj.getString("error"));
            } else {
                // pending, running, processing 都视为进行中
                result.setStatus(EnumOcrTaskStatus.IN_PROGRESS.getCode());
            }

            return result;
        } catch (Exception e) {
            log.error("查询本地 MinerU 任务状态失败", e);
            result.setStatus(EnumOcrTaskStatus.FAILED.getCode());
            result.setErrorMsg(e.getMessage());
            return result;
        }
    }

    /**
     * 上传文件并调用指定接口
     *
     * @param filePath 文件路径或 URL
     * @param url      接口地址
     */
    private Response uploadFile(String filePath, String url) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }

        File file = null;
        String fileName;
        boolean isTempFile = false;

        try {
            if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
                String tempDir = System.getProperty("java.io.tmpdir");
                fileName = extractFileNameFromUrl(filePath);
                File tempFile = new File(tempDir, fileName);
                long size = HttpUtil.downloadFile(filePath, FileUtil.file(tempFile.getAbsolutePath()));
                if (size <= 0) {
                    throw new IOException("文件下载失败或文件大小为0: " + filePath);
                }
                file = tempFile;
                isTempFile = true;
            } else {
                file = new File(filePath);
                fileName = file.getName();
            }

            if (!file.exists()) {
                throw new BusinessException("文件不存在：" + filePath);
            }
            if (!file.isFile()) {
                throw new BusinessException("不是文件：" + filePath);
            }
            if (file.length() == 0) {
                throw new BusinessException("文件为空：" + filePath);
            }

            MediaType mediaType = detectMediaType(fileName);
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("files", fileName, RequestBody.create(file, mediaType));

            // 布尔参数：return_md 默认 true
            builder.addFormDataPart("return_md", String.valueOf(getReturnMd() != null ? getReturnMd() : true));
            addBooleanParam(builder, "formula_enable", getFormulaEnable());
            addBooleanParam(builder, "table_enable", getTableEnable());
            addBooleanParam(builder, "image_analysis", getImageAnalysis());
            addBooleanParam(builder, "return_middle_json", getReturnMiddleJson());
            addBooleanParam(builder, "return_model_output", getReturnModelOutput());
            addBooleanParam(builder, "return_content_list", getReturnContentList());
            addBooleanParam(builder, "return_images", getReturnImages());
            addBooleanParam(builder, "response_format_zip", getResponseFormatZip());
            addBooleanParam(builder, "return_original_file", getReturnOriginalFile());
            addBooleanParam(builder, "client_side_output_generation", getClientSideOutputGeneration());

            // 字符串参数
            addStringParam(builder, "backend", getBackend());
            addStringParam(builder, "effort", getEffort());
            addStringParam(builder, "parse_method", getParseMethod());
            addStringParam(builder, "server_url", getServerUrl());

            // 数组/整数参数
            addListParam(builder, "lang_list", getLangList());
            addIntegerParam(builder, "start_page_id", getStartPageId());
            addIntegerParam(builder, "end_page_id", getEndPageId());

            MultipartBody multipartBody = builder.build();

            log.info("调用本地 MinerU 接口，URL: {}", url);
            Response response = OkHttpUtil.multipart(url, getHeaders(), multipartBody);
            if (response == null || !response.isSuccessful()) {
                String errorBody = response != null && response.body() != null ? response.body().string() : "";
                throw new BusinessException(
                        String.format("本地 MinerU 请求失败，状态码：%s，响应：%s",
                                response != null ? response.code() : "null", errorBody)
                );
            }
            return response;
        } finally {
            if (isTempFile && file != null) {
                FileUtil.del(file);
            }
        }
    }

    /**
     * 解析响应体为字符串
     */
    private String parseResponseBody(Response response) throws IOException {
        if (response == null || response.body() == null) {
            throw new BusinessException("本地 MinerU 响应为空");
        }
        return response.body().string();
    }

    /**
     * 从响应 JSON 中提取解析内容
     * 兼容多种返回结构：
     * 1. 顶层 markdown / content
     * 2. result.markdown / result.content
     * 3. MinerU CLI 结构：results.{filename}.md_content
     */
    private String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            throw new BusinessException("本地 MinerU 响应为空");
        }

        JSONObject resObj = JSON.parseObject(responseBody);
        if (resObj == null) {
            throw new BusinessException("本地 MinerU 响应解析失败");
        }

        if (resObj.containsKey("markdown")) {
            return resObj.getString("markdown");
        }
        if (resObj.containsKey("content")) {
            return resObj.getString("content");
        }
        if (resObj.containsKey("result")) {
            JSONObject resultObj = resObj.getJSONObject("result");
            if (resultObj != null) {
                if (resultObj.containsKey("markdown")) {
                    return resultObj.getString("markdown");
                }
                if (resultObj.containsKey("content")) {
                    return resultObj.getString("content");
                }
            }
        }

        // MinerU CLI 实际返回结构：results.{filename}.md_content
        if (resObj.containsKey("results")) {
            JSONObject resultsObj = resObj.getJSONObject("results");
            if (resultsObj != null) {
                for (String fileName : resultsObj.keySet()) {
                    JSONObject fileResult = resultsObj.getJSONObject(fileName);
                    if (fileResult != null && fileResult.containsKey("md_content")) {
                        return fileResult.getString("md_content");
                    }
                }
            }
        }

        throw new BusinessException("本地 MinerU 响应中未找到解析内容");
    }

    /**
     * 添加字符串表单参数
     */
    private void addStringParam(MultipartBody.Builder builder, String name, String value) {
        if (value != null && !value.isEmpty()) {
            builder.addFormDataPart(name, value);
        }
    }

    /**
     * 添加布尔表单参数
     */
    private void addBooleanParam(MultipartBody.Builder builder, String name, Boolean value) {
        if (value != null) {
            builder.addFormDataPart(name, String.valueOf(value));
        }
    }

    /**
     * 添加整数表单参数
     */
    private void addIntegerParam(MultipartBody.Builder builder, String name, Integer value) {
        if (value != null) {
            builder.addFormDataPart(name, String.valueOf(value));
        }
    }

    /**
     * 添加数组表单参数（FastAPI List 类型使用同名多字段）
     */
    private void addListParam(MultipartBody.Builder builder, String name, List<String> values) {
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isEmpty()) {
                    builder.addFormDataPart(name, value);
                }
            }
        }
    }

    /**
     * 判断响应是否为 ZIP 格式
     */
    private boolean isZipResponse(Response response) {
        if (response == null || response.body() == null) {
            return false;
        }
        MediaType contentType = response.body().contentType();
        if (contentType != null) {
            String type = contentType.toString().toLowerCase();
            return type.contains("zip") || type.contains("octet-stream");
        }
        return false;
    }

    /**
     * 下载并解析 ZIP 结果
     * 上传图片到文件存储并替换 Markdown 中的图片路径
     */
    private String downloadAndExtractMarkdown(InputStream zipInputStream) throws IOException {
        if (zipInputStream == null) {
            throw new BusinessException("下载本地 MinerU 结果 ZIP 失败");
        }

        // 一次性读取到内存，避免 ZIP 流只能读取一次的问题
        byte[] zipBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = zipInputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            zipBytes = baos.toByteArray();
        }

        Map<String, String> imageUrlMap = new HashMap<>();
        String markdownContent = null;

        // 第一遍：上传所有图片
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (isImageEntry(entryName)) {
                    String imageUrl = uploadImageFromZip(zis, entryName);
                    if (imageUrl != null) {
                        String relativePath = extractRelativeImagePath(entryName);
                        imageUrlMap.put(relativePath, imageUrl);
                        log.info("本地 MinerU 图片上传成功: {} -> {}", relativePath, imageUrl);
                    } else {
                        log.warn("本地 MinerU 图片上传失败: {}", entryName);
                    }
                }
            }
        }

        log.info("本地 MinerU 图片上传完成，共上传 {} 张图片", imageUrlMap.size());

        // 第二遍：提取 Markdown 内容
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            StringBuilder contentBuilder = new StringBuilder();
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entryName.endsWith(".md")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        contentBuilder.append(line).append("\n");
                    }
                    markdownContent = contentBuilder.toString();
                    log.info("提取到 Markdown 内容，长度: {}", markdownContent.length());
                    break;
                }
            }
        }

        if (markdownContent == null || markdownContent.isEmpty()) {
            throw new BusinessException("本地 MinerU ZIP 结果中未找到 Markdown 文件");
        }

        // 替换 Markdown 中的图片路径
        if (!imageUrlMap.isEmpty()) {
            markdownContent = replaceImagePaths(markdownContent, imageUrlMap);
            log.info("图片路径替换完成，共替换 {} 个图片", imageUrlMap.size());
        }

        return markdownContent;
    }

    /**
     * 判断 ZIP 条目是否是图片文件
     */
    private boolean isImageEntry(String entryName) {
        String lowerName = entryName.toLowerCase();
        return lowerName.contains("images/") &&
                (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                        || lowerName.endsWith(".png") || lowerName.endsWith(".gif")
                        || lowerName.endsWith(".webp") || lowerName.endsWith(".bmp"));
    }

    /**
     * 提取图片的相对路径（images/xxx.jpg 格式）
     */
    private String extractRelativeImagePath(String entryName) {
        int imagesIndex = entryName.indexOf("images/");
        if (imagesIndex >= 0) {
            return entryName.substring(imagesIndex);
        }
        return entryName;
    }

    /**
     * 从 ZIP 流中上传图片
     */
    private String uploadImageFromZip(ZipInputStream zis, String entryName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = zis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = baos.toByteArray();

            String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);
            String contentType = getImageContentType(fileName);

            CustomMultipartFile multipartFile = new CustomMultipartFile(
                    imageBytes,
                    "file",
                    fileName,
                    contentType
            );

            FileStorageService storageService = SpringContextUtil.getBean("default", FileStorageService.class);
            return storageService.saveTemp(multipartFile);
        } catch (Exception e) {
            log.error("本地 MinerU 上传图片失败: {}", entryName, e);
            return null;
        }
    }

    /**
     * 根据文件名获取图片 Content-Type
     */
    private String getImageContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerName.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "application/octet-stream";
    }

    /**
     * 替换 Markdown 中的图片路径
     */
    private String replaceImagePaths(String markdown, Map<String, String> imageUrlMap) {
        Pattern pattern = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(markdown);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String alt = matcher.group(1);
            String originalPath = matcher.group(2);
            String replacement = imageUrlMap.get(originalPath);

            if (replacement != null) {
                matcher.appendReplacement(result, "![" + alt + "](" + replacement + ")");
            } else {
                for (Map.Entry<String, String> entry : imageUrlMap.entrySet()) {
                    if (originalPath.contains(entry.getKey()) || entry.getKey().contains(originalPath)) {
                        matcher.appendReplacement(result, "![" + alt + "](" + entry.getValue() + ")");
                        replacement = entry.getValue();
                        break;
                    }
                }
                if (replacement == null) {
                    matcher.appendReplacement(result, matcher.group(0));
                }
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 从 URL 提取文件名
     */
    private String extractFileNameFromUrl(String url) {
        if (url.contains("/")) {
            String name = url.substring(url.lastIndexOf("/") + 1);
            if (name.contains("?")) {
                name = name.substring(0, name.indexOf("?"));
            }
            if (!name.isEmpty() && name.contains(".")) {
                return name;
            }
        }
        return "downloaded_" + System.currentTimeMillis() + ".tmp";
    }

    /**
     * 根据文件名获取 MediaType
     */
    private MediaType detectMediaType(String fileName) {
        if (fileName == null) {
            return MediaType.parse("application/octet-stream");
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return MediaType.parse("application/pdf");
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return MediaType.parse("image/jpeg");
        } else if (lowerName.endsWith(".png")) {
            return MediaType.parse("image/png");
        } else if (lowerName.endsWith(".gif")) {
            return MediaType.parse("image/gif");
        } else if (lowerName.endsWith(".bmp")) {
            return MediaType.parse("image/bmp");
        } else if (lowerName.endsWith(".tiff")) {
            return MediaType.parse("image/tiff");
        } else if (lowerName.endsWith(".docx")) {
            return MediaType.parse("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else if (lowerName.endsWith(".pptx")) {
            return MediaType.parse("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        } else if (lowerName.endsWith(".xlsx")) {
            return MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        return MediaType.parse("application/octet-stream");
    }
}
