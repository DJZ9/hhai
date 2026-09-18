
package tech.aiflowy.ai.service.impl;

import org.springframework.core.io.ClassPathResource;
import cn.hutool.core.util.StrUtil;
import com.agentsflex.core.document.Document;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.chat.ChatOptions;
import com.agentsflex.core.model.embedding.EmbeddingModel;
import com.agentsflex.core.model.image.GenerateImageRequest;
import com.agentsflex.core.model.image.ImageModel;
import com.agentsflex.core.model.image.ImageResponse;
import com.agentsflex.core.model.ocr.OcrResponse;
import com.agentsflex.core.model.rerank.RerankModel;
import com.agentsflex.core.store.VectorData;
import com.alicp.jetcache.Cache;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tech.aiflowy.ai.asynctask.OcrTaskService;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.entity.ModelProvider;
import tech.aiflowy.ai.mapper.ModelMapper;
import tech.aiflowy.ai.service.ModelProviderService;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.common.filestorage.CustomMultipartFile;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.system.entity.SysOption;
import tech.aiflowy.system.service.SysOptionService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务层实现。
 *
 * @author michael
 * @since 2024-08-23
 */
@Service
public class ModelServiceImpl extends ServiceImpl<ModelMapper, Model> implements ModelService {

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    ModelProviderService modelProviderService;
    @Resource
    private SysOptionService sysOptionService;
    @Resource
    private Cache<String, Object> cache;
    @Resource
    private OcrTaskService ocrTaskService;

    @Resource(name = "default")
    private FileStorageService fileStorageService;


    @Override
    public boolean addAiLlm(Model entity) {
        int insert = modelMapper.insert(entity);
        if (insert <= 0) {
            return false;
        }
        return true;
    }

    private static final Logger log = LoggerFactory.getLogger(ModelServiceImpl.class);

    @Override
    public Map<String, Object> verifyModelConfig(Model model) {
        String modelType = model.getModelType();
        Map<String, Object> resMap = new HashMap<>();
        // 走聊天验证逻辑
        if (Model.MODEL_TYPES[0].equals(modelType)) {
            verifyChatLlm(model);
            return null;
        }
        // 走向量化验证逻辑
        if (Model.MODEL_TYPES[1].equals(modelType)) {
            int dimension = verifyEmbedLlm(model);
            resMap.put("dimension", dimension);
            return resMap;
        }
        // 走重排验证逻辑
        if (Model.MODEL_TYPES[2].equals(modelType)) {
            verifyRerankLlm(model);
            return null;

        }

        // 走图片生成验证逻辑
        if (Model.MODEL_TYPES[4].equals(modelType)) {
            verifyImageModel(model);
            return null;
        }

        // 以上不满足，视为验证失败
        throw new BusinessException("校验失败！");

    }

    @Override
    public Map<String, Map<String, List<Model>>> getList(Model entity) {
        Map<String, Map<String, List<Model>>> result = new HashMap<>();

        QueryWrapper queryWrapper = new QueryWrapper()
                .eq(Model::getProviderId, entity.getProviderId());
        queryWrapper.eq(Model::getWithUsed, entity.getWithUsed());
        List<Model> totalList = modelMapper.selectListWithRelationsByQuery(queryWrapper);
        for (String modelType : Model.MODEL_TYPES) {
            Map<String, List<Model>> groupMap = groupLlmByGroupName(totalList, modelType);
            if (!CollectionUtils.isEmpty(groupMap)) {
                result.put(modelType, groupMap);
            }
        }

        return result;
    }

    private Map<String, List<Model>> groupLlmByGroupName(List<Model> totalList, String targetModelType) {
        if (CollectionUtils.isEmpty(totalList)) {
            return Collections.emptyMap();
        }

        return totalList.stream()
                .filter(aiLlm -> targetModelType.equals(aiLlm.getModelType())
                        && aiLlm.getGroupName() != null)
                .collect(Collectors.groupingBy(Model::getGroupName));
    }


    private void verifyRerankLlm(Model model) {
        RerankModel rerankModel = model.toRerankModel();
        List<Document> documents = new ArrayList<>();
        documents.add(Document.of("Paris is the capital of France."));
        documents.add(Document.of("London is the capital of England."));
        documents.add(Document.of("Tokyo is the capital of Japan."));
        documents.add(Document.of("Beijing is the capital of China."));
        documents.add(Document.of("Washington, D.C. is the capital of the United States."));
        documents.add(Document.of("Moscow is the capital of Russia."));
        try {
            List<Document> rerank = rerankModel.rerank("What is the capital of France?", documents);
            if (rerank == null || rerank.isEmpty()) {
                throw new BusinessException("校验未通过，请前往后端日志查看详情！");
            }
        } catch (Exception e) {
            log.error("校验失败：{}", e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    private int verifyEmbedLlm(Model model) {
        try {
            EmbeddingModel embeddingModel = model.toEmbeddingModel();
            VectorData vectorData = embeddingModel.embed("这是一条校验模型配置的文本");
            if (vectorData.getVector() == null) {
                throw new BusinessException("校验未通过，请前往后端日志查看详情！");
            }
            log.info("取到向量数据，校验结果通过");
            return vectorData.getVector().length;
        } catch (Exception e) {
            log.error("模型配置校验失败:{}", e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    private void verifyChatLlm(Model llm) {

        ChatModel chatModel = llm.toChatModel(null);
        if (chatModel == null) {
            throw new BusinessException("chatModel为空");
        }
        try {
            ChatOptions options = new ChatOptions();
            options.setThinkingEnabled(false);
            String response = chatModel.chat("我在对模型配置进行校验，你收到这条消息无需做任何思考，直接回复一个“你好”即可!", options);
            if (response == null) {
                throw new BusinessException("校验未通过，请前往后端日志查看详情！");
            }
            log.info("校验结果：{}", response);
        } catch (Exception e) {
            log.error("校验失败：{}", e.getMessage());
            throw new BusinessException(e.getMessage());
        }

    }

    /**
     * 校验图片生成模型配置：生成一张测试图片，确认模型可正常返回图片数据。
     */
    private void verifyImageModel(Model model) {
        ImageModel imageModel = model.toImageModel();
        if (imageModel == null) {
            throw new BusinessException("imageModel为空");
        }
        try {
            GenerateImageRequest request = new GenerateImageRequest();
            request.setPrompt("一只可爱的猫咪，简约插画风格");
            request.setN(1);
            ImageResponse response = imageModel.generate(request);
            if (response == null || response.isError()) {
                String msg = response == null ? "无响应" : response.getErrorMessage();
                throw new BusinessException("校验未通过：" + msg);
            }
            if (response.getImages() == null || response.getImages().isEmpty()) {
                throw new BusinessException("校验未通过：未返回图片数据");
            }
            log.info("图片生成模型校验通过，返回图片数：{}", response.getImages().size());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片生成模型校验失败：{}", e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public void removeByEntity(Model entity) {
        QueryWrapper queryWrapper = QueryWrapper.create().eq(Model::getProviderId, entity.getProviderId()).eq(Model::getGroupName, entity.getGroupName());
        modelMapper.deleteByQuery(queryWrapper);
    }

    @Override
    public Model getModelInstance(BigInteger modelId) {
        if (modelId == null) {
            throw new BusinessException("模型ID不能为空");
        }
        Model model = modelMapper.selectOneWithRelationsById(modelId);
        if (model == null) {
            return null;
        }
        ModelProvider modelProvider = model.getModelProvider();
        model.setModelProvider(modelProvider);
        if (StrUtil.isBlank(model.getApiKey())) {
            model.setApiKey(modelProvider.getApiKey());
        }
        if (StrUtil.isBlank(model.getEndpoint())) {
            model.setEndpoint(modelProvider.getEndpoint());
        }

        // 请求路径为空，从modelProvider中获取
        if (StrUtil.isBlank(model.getRequestPath())) {
            // 模型类型为chatModel
            if (model.getModelType().equals(Model.MODEL_TYPES[0])) {
                model.setRequestPath(modelProvider.getChatPath());
                // 模型类型为embeddingModel
            } else if (model.getModelType().equals(Model.MODEL_TYPES[1])) {
                model.setRequestPath(modelProvider.getEmbedPath());
                // 模型类型为rerankModel
            } else if (model.getModelType().equals(Model.MODEL_TYPES[2])) {
                model.setRequestPath(modelProvider.getRerankPath());
            }
        }

        return model;
    }

    @Override
    public void updateByEntity(Model entity) {
        QueryWrapper queryWrapper = QueryWrapper.create().eq(Model::getProviderId, entity.getProviderId())
                .eq(Model::getGroupName, entity.getGroupName());
        Model model = new Model();
        model.setWithUsed(entity.getWithUsed());
        modelMapper.updateByQuery(model, queryWrapper);
    }

    @Override
    public Model getSystemModel() {
        SysOption modelProviderName = sysOptionService.getByOptionKey("model_of_chat");
        SysOption modelName = sysOptionService.getByOptionKey("chatgpt_model_name");
        SysOption requestPath = sysOptionService.getByOptionKey("chatgpt_chatPath");
        SysOption endpoint = sysOptionService.getByOptionKey("chatgpt_endpoint");
        SysOption apiKey = sysOptionService.getByOptionKey("chatgpt_api_key");
        if (modelProviderName == null
                || modelName == null
                || requestPath == null
                || endpoint == null
                || apiKey == null) {
            throw new BusinessException("系统模型配置不完善，请检查系统模型配置！");
        }
        ModelProvider modelProvider = new ModelProvider();
        modelProvider.setProviderType(modelProviderName.getValue());
        Model model = new Model();
        model.setEndpoint(endpoint.getValue());
        model.setApiKey(apiKey.getValue());
        model.setModelType("chatModel");
        model.setRequestPath(requestPath.getValue());
        model.setModelProvider(modelProvider);
        model.setModelName(modelName.getValue());
        model.setModelProvider(modelProvider);
        return model;
    }

    @Override
    public Map<String, Object> verifyOcrModelConfig(Model model, String fileUrl) {
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        // 如果文件地址为空，生成测试图并上传到临时文件系统，得到可被 OCR 供应商拉取的 URL
        if (StrUtil.isBlank(fileUrl)) {
            fileUrl = resolveTestImageAsUrl();
        }

        try {
            String taskId = ocrTaskService.submit(model.getId(), fileUrl, null, 600_000L);
            OcrResponse ocrResponse = ocrTaskService.awaitResult(taskId, 600_000L, 3_000L);
            String result = StrUtil.isNotBlank(ocrResponse.getMarkdown()) ? ocrResponse.getMarkdown() : ocrResponse.getText();

            if (result == null || result.trim().isEmpty()) {
                throw new BusinessException("OCR 模型验证失败：解析结果为空");
            }

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("contentLength", result.length());
            resultMap.put("preview", result.substring(0, Math.min(500, result.length())));
            return resultMap;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("OCR 模型验证失败: " + e.getMessage());
        }
    }

    /**
     * 生成/定位本地测试图后，上传到临时文件系统，返回可被 OCR 供应商拉取的 URL。
     */
    private String resolveTestImageAsUrl() {
        String localPath = resolveTestImagePath();
        try {
            byte[] bytes = Files.readAllBytes(new File(localPath).toPath());
            CustomMultipartFile mf = new CustomMultipartFile(
                    bytes, "ocr_test", "ocr_test.png", "image/png");
            return fileStorageService.saveTemp(mf);
        } catch (Exception e) {
            throw new BusinessException("上传测试图片到临时文件系统失败: " + e.getMessage());
        }
    }

    /**
     * 解析测试图片路径：
     * 1. 本地运行：直接使用 classpath 下 static/ocr_test.png 物理文件
     * 2. 打包 jar 运行：内置图片复制到临时文件
     * 3. 不存在：动态生成含文字的测试图片，并写回 static 目录同名文件，下次启动直接复用
     */
    private String resolveTestImagePath() {
        Path staticImagePath = locateStaticTestImage();
        // 1. 直接使用 static 目录下的物理文件，无需复制
        if (staticImagePath != null && Files.exists(staticImagePath)) {
            return staticImagePath.toAbsolutePath().toString();
        }
        // 2. jar 内运行且内置图片存在：复制到临时文件
        ClassPathResource resource = new ClassPathResource("static/ocr_test.png");
        try {
            if (resource.exists()) {
                Path tempFile = Files.createTempFile("aiflowy_ocr_test_", ".png");
                try (InputStream in = resource.getInputStream()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return tempFile.toAbsolutePath().toString();
            }
        } catch (Exception e) {
            log.warn("复制内置测试图片失败，改用动态生成: {}", e.getMessage());
        }
        // 3. 生成测试图片，写回 static 目录同名文件
        return generateTestImage(staticImagePath);
    }

    /**
     * 定位 classpath 下 static/ocr_test.png 的物理文件路径。
     * 本地运行（classpath 为文件系统目录）时可定位；打包 jar 内运行返回 null。
     */
    private Path locateStaticTestImage() {
        try {
            File staticDir = new ClassPathResource("static").getFile();
            return new File(staticDir, "ocr_test.png").toPath();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 生成含文字的测试图片，返回本地文件路径。
     * 优先写回 static 目录同名文件（下次启动直接复用），无法写回时退回临时文件。
     * 用于 OCR 模型校验，不依赖外网资源，纯内网环境可用。
     */
    private String generateTestImage(Path preferredPath) {
        try {
            int width = 420;
            int height = 120;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
                graphics.setColor(Color.BLACK);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
                graphics.drawString("AIFlowy OCR TEST 123", 20, 75);
            } finally {
                graphics.dispose();
            }

            Path target;
            if (preferredPath != null) {
                Files.createDirectories(preferredPath.getParent());
                target = preferredPath;
            } else {
                target = Files.createTempFile("aiflowy_ocr_test_", ".png");
            }
            ImageIO.write(image, "png", target.toFile());
            log.info("OCR 校验测试图片已生成: {}", target.toAbsolutePath());
            return target.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new BusinessException("生成测试图片失败：" + e.getMessage());
        }
    }
}
