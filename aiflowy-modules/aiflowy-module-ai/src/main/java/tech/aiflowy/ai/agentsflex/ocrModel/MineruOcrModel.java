package tech.aiflowy.ai.agentsflex.ocrModel;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.common.constant.enums.EnumOcrTaskStatus;
import tech.aiflowy.common.filestorage.CustomMultipartFile;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.util.OkHttpUtil;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * MinerU OCR 模型实现类
 * 用于调用 MinerU 文档解析 API
 */
public class MineruOcrModel extends MineruOcrModelConfig implements OcrModel {

    private static final Logger log = LoggerFactory.getLogger(MineruOcrModel.class);

    // 任务状态常量
    private static final String STATE_DONE = "done";
    private static final String STATE_PENDING = "pending";
    private static final String STATE_RUNNING = "running";
    private static final String STATE_FAILED = "failed";
    private static final String STATE_CONVERTING = "converting";

    // 轮询配置
    private static final int MAX_POLL_RETRY = 180;  // 最大轮询次数
    private static final long POLL_INTERVAL_SECONDS = 10;  // 轮询间隔（秒）

    // API 路径
    private static final String CREATE_TASK_PATH = "/api/v4/extract/task";
    private static final String QUERY_TASK_PATH_TEMPLATE = "/api/v4/extract/task/%s";

    public MineruOcrModel() {
        super();
    }

    public MineruOcrModel(MineruOcrModelConfig config) {
        super();
        if (config != null) {
            this.setModelVersion(config.getModelVersion());
            this.setEnableTable(config.getEnableTable());
            this.setEnableFormula(config.getEnableFormula());
            this.setIsOcr(config.getIsOcr());
            this.setLanguage(config.getLanguage());
            this.setPageRanges(config.getPageRanges());
            this.setNoCache(config.getNoCache());
            this.setCacheTolerance(config.getCacheTolerance());
        }
    }

    @Override
    public String getText(String filePath) {
        try {
            // 1. 获取文件 URL
            String fileUrl = getFileUrl(filePath);

            // 2. 创建解析任务
            String taskId = createExtractTask(fileUrl);

            // 3. 轮询任务状态
            String resultZipUrl = pollTaskResult(taskId);

            // 4. 下载并解析 ZIP 结果
            String markdownContent = downloadAndExtractMarkdown(resultZipUrl);

            return markdownContent;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("MinerU 任务轮询被中断: " + e.getMessage());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinerU 文档解析失败", e);
            throw new BusinessException("MinerU 文档解析失败: " + e.getMessage());
        }
    }

    @Override
    public String runAsync(String filePath) {
        try {
            String fileUrl = getFileUrl(filePath);
            String taskId = createExtractTask(fileUrl);
            log.info("MinerU 异步任务创建成功，task_id: {}", taskId);
            return taskId;
        } catch (Exception e) {
            log.error("MinerU 创建异步任务失败", e);
            throw new BusinessException("MinerU 创建任务失败: " + e.getMessage());
        }
    }

    @Override
    public ParseResult queryTask(String taskId) {
        ParseResult result = new ParseResult();
        try {
            String queryUrl = getEndpoint() + String.format(QUERY_TASK_PATH_TEMPLATE, taskId);
            log.info("查询 MinerU 任务状态，URL: {}", queryUrl);

            String response = OkHttpUtil.get(queryUrl, getHeaders());
            if (response == null || response.isEmpty()) {
                result.setStatus(EnumOcrTaskStatus.IN_PROGRESS.getCode());
                return result;
            }

            JSONObject resObj = JSON.parseObject(response);
            int code = resObj.getIntValue("code");
            if (code != 0) {
                result.setStatus(EnumOcrTaskStatus.FAILED.getCode());
                result.setErrorMsg(resObj.getString("msg"));
                return result;
            }

            JSONObject data = resObj.getJSONObject("data");
            String state = data.getString("state");

            if (STATE_DONE.equals(state)) {
                result.setStatus(EnumOcrTaskStatus.COMPLETED.getCode());
                String zipUrl = data.getString("full_zip_url");
                if (zipUrl != null && !zipUrl.isEmpty()) {
                    String markdownContent = downloadAndExtractMarkdown(zipUrl);
                    result.setContent(markdownContent);
                }
            } else if (STATE_FAILED.equals(state)) {
                result.setStatus(EnumOcrTaskStatus.FAILED.getCode());
                result.setErrorMsg(data.getString("err_msg"));
            } else {
                // pending, running, converting 都视为进行中
                result.setStatus(EnumOcrTaskStatus.IN_PROGRESS.getCode());
            }

            return result;
        } catch (Exception e) {
            log.error("查询 MinerU 任务状态失败", e);
            result.setStatus(EnumOcrTaskStatus.FAILED.getCode());
            result.setErrorMsg(e.getMessage());
            return result;
        }
    }

    /**
     * 获取文件 URL
     * 如果 filePath 已经是 URL，直接返回
     * 否则需要根据系统配置生成可访问的 URL
     */
    private String getFileUrl(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }

        // 如果已经是 HTTP URL，直接返回
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }

        // 本地文件路径，需要获取可访问的 URL
        // 这里假设系统配置中有 file_access_url 配置
        // 实际实现可能需要根据具体部署环境调整
        throw new BusinessException("MinerU 需要文件的可访问 URL，当前文件路径: " + filePath + " 不是有效的 URL。请确保文件存储服务返回完整的 URL。");
    }

    /**
     * 创建解析任务
     */
    private String createExtractTask(String fileUrl) throws IOException {
        String createUrl = getEndpoint() + CREATE_TASK_PATH;
        log.info("创建 MinerU 任务，URL: {}", createUrl);

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("url", fileUrl);
        requestBody.put("model_version", getModelVersion());

        // 可选参数
        if (getEnableTable() != null) {
            requestBody.put("enable_table", getEnableTable());
        }
        if (getEnableFormula() != null) {
            requestBody.put("enable_formula", getEnableFormula());
        }
        if (getIsOcr() != null) {
            requestBody.put("is_ocr", getIsOcr());
        }
        if (getLanguage() != null) {
            requestBody.put("language", getLanguage());
        }
        if (getPageRanges() != null && !getPageRanges().isEmpty()) {
            requestBody.put("page_ranges", getPageRanges());
        }
        if (getNoCache() != null) {
            requestBody.put("no_cache", getNoCache());
        }
        if (getCacheTolerance() != null) {
            requestBody.put("cache_tolerance", getCacheTolerance());
        }

        String jsonBody = JSON.toJSONString(requestBody);
        log.debug("MinerU 请求体: {}", jsonBody);

        // 发送请求
        Map<String, String> headers = new HashMap<>(getHeaders());
        headers.put("Content-Type", "application/json");

        String response = OkHttpUtil.post(createUrl, headers, jsonBody);
        if (response == null || response.isEmpty()) {
            throw new BusinessException("MinerU 创建任务响应为空");
        }

        JSONObject resObj = JSON.parseObject(response);
        int code = resObj.getIntValue("code");
        if (code != 0) {
            String msg = resObj.getString("msg");
            throw new BusinessException("MinerU 创建任务失败: " + msg);
        }

        JSONObject data = resObj.getJSONObject("data");
        String taskId = data.getString("task_id");
        if (taskId == null || taskId.isEmpty()) {
            throw new BusinessException("MinerU 未返回 task_id");
        }

        return taskId;
    }

    /**
     * 轮询任务结果
     */
    private String pollTaskResult(String taskId) throws InterruptedException, IOException {
        for (int retry = 1; retry <= MAX_POLL_RETRY; retry++) {
            TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);

            String queryUrl = getEndpoint() + String.format(QUERY_TASK_PATH_TEMPLATE, taskId);
            log.debug("轮询 MinerU 任务状态，第 {} 次，URL: {}", retry, queryUrl);

            String response = OkHttpUtil.get(queryUrl, getHeaders());
            if (response == null || response.isEmpty()) {
                continue;
            }

            JSONObject resObj = JSON.parseObject(response);
            int code = resObj.getIntValue("code");
            if (code != 0) {
                String msg = resObj.getString("msg");
                throw new BusinessException("MinerU 查询任务失败: " + msg);
            }

            JSONObject data = resObj.getJSONObject("data");
            String state = data.getString("state");
            log.debug("MinerU 任务状态: {}", state);

            if (STATE_DONE.equals(state)) {
                String zipUrl = data.getString("full_zip_url");
                if (zipUrl == null || zipUrl.isEmpty()) {
                    throw new BusinessException("MinerU 任务完成但未返回结果 URL");
                }
                return zipUrl;
            } else if (STATE_FAILED.equals(state)) {
                String errMsg = data.getString("err_msg");
                throw new BusinessException("MinerU 任务失败: " + errMsg);
            } else if (STATE_CONVERTING.equals(state)) {
                log.info("MinerU 任务正在进行格式转换");
            }
            // pending, running 继续轮询
        }

        throw new BusinessException("MinerU 任务轮询超时，最大重试次数: " + MAX_POLL_RETRY);
    }

    /**
     * 下载 ZIP 并提取 Markdown 内容
     * 同时上传图片到服务器并替换 Markdown 中的图片路径
     */
    private String downloadAndExtractMarkdown(String zipUrl) throws IOException {
        // 下载 ZIP 文件
        InputStream zipInputStream = OkHttpUtil.getInputStream(zipUrl);
        if (zipInputStream == null) {
            throw new BusinessException("下载 MinerU 结果 ZIP 失败");
        }

        // 用于存储图片相对路径与上传后 URL 的映射
        Map<String, String> imageUrlMap = new HashMap<>();
        // 用于存储 Markdown 内容
        String markdownContent = null;

        // 第一遍：上传所有图片文件
        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                log.debug("遍历 ZIP 条目: {}", entryName);

                // 判断是否是图片文件（images 目录下的图片）
                if (isImageEntry(entryName)) {
                    String imageUrl = uploadImageFromZip(zis, entryName);
                    if (imageUrl != null) {
                        // 提取相对路径（如 images/xxx.jpg）
                        String relativePath = extractRelativeImagePath(entryName);
                        imageUrlMap.put(relativePath, imageUrl);
                        log.info("图片上传成功: {} -> {}", relativePath, imageUrl);
                    } else {
                        log.warn("图片上传失败: {}", entryName);
                    }
                }
            }
        }

        log.info("图片上传完成，共上传 {} 张图片", imageUrlMap.size());

        // 重新下载 ZIP 文件（因为流只能读取一次）
        zipInputStream = OkHttpUtil.getInputStream(zipUrl);
        if (zipInputStream == null) {
            throw new BusinessException("重新下载 MinerU 结果 ZIP 失败");
        }

        // 第二遍：提取 Markdown 内容
        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            StringBuilder contentBuilder = new StringBuilder();

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // 查找 full.md 文件（Markdown 结果）
                if (entryName.endsWith("full.md")) {
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
            throw new BusinessException("ZIP 文件中未找到 full.md 文件");
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
        // 从完整路径中提取 images/xxx.jpg 格式的相对路径
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
            // 读取图片数据到字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = zis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = baos.toByteArray();

            // 获取文件名
            String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);

            // 获取文件类型
            String contentType = getContentType(fileName);

            // 使用 CustomMultipartFile 创建上传文件
            CustomMultipartFile multipartFile = new CustomMultipartFile(
                    imageBytes,
                    "file",
                    fileName,
                    contentType
            );

            // 获取 FileStorageService 并上传
            FileStorageService storageService = SpringContextUtil.getBean("default", FileStorageService.class);
            String uploadedUrl = storageService.saveTemp(multipartFile);

            return uploadedUrl;
        } catch (Exception e) {
            log.error("上传图片失败: {}", entryName, e);
            return null;
        }
    }

    /**
     * 根据文件名获取 Content-Type
     */
    private String getContentType(String fileName) {
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
        // 匹配 Markdown 图片语法: ![alt](path)
        Pattern pattern = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
        Matcher matcher = pattern.matcher(markdown);
        StringBuffer result = new StringBuffer();

        log.info("开始替换图片路径，映射表: {}", imageUrlMap);

        while (matcher.find()) {
            String alt = matcher.group(1);
            String originalPath = matcher.group(2);
            log.debug("匹配到图片: alt={}, path={}", alt, originalPath);

            // 查找替换后的 URL
            String replacement = imageUrlMap.get(originalPath);

            if (replacement != null) {
                // 替换为新的 URL
                log.info("图片路径替换: {} -> {}", originalPath, replacement);
                matcher.appendReplacement(result, "![" + alt + "](" + replacement + ")");
            } else {
                // 尝试模糊匹配
                for (Map.Entry<String, String> entry : imageUrlMap.entrySet()) {
                    if (originalPath.contains(entry.getKey()) || entry.getKey().contains(originalPath)) {
                        log.info("图片路径模糊匹配替换: {} -> {}", originalPath, entry.getValue());
                        matcher.appendReplacement(result, "![" + alt + "](" + entry.getValue() + ")");
                        replacement = entry.getValue();
                        break;
                    }
                }
                // 如果没有找到匹配，保持原样
                if (replacement == null) {
                    log.warn("图片路径未找到匹配: {}, 保持原样", originalPath);
                    matcher.appendReplacement(result, matcher.group(0));
                }
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }
}