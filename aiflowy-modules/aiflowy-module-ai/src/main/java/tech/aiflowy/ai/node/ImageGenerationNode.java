package tech.aiflowy.ai.node;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.agentsflex.core.model.image.GenerateImageRequest;
import com.agentsflex.core.model.image.Image;
import com.agentsflex.core.model.image.ImageModel;
import com.agentsflex.core.model.image.ImageResponse;
import com.alibaba.fastjson2.JSON;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.BaseNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.common.filestorage.FileStorageManager;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.math.BigInteger;
import java.util.*;

/**
 * 图片生成节点：调用 agents-flex 图片模型生成图片，并保存到文件存储后返回访问地址。
 */
public class ImageGenerationNode extends BaseNode {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationNode.class);

    private BigInteger modelId;

    public ImageGenerationNode() {
    }

    public ImageGenerationNode(BigInteger modelId) {
        this.modelId = modelId;
    }

    @Override
    public Map<String, Object> execute(Chain chain) {
        Map<String, Object> res = new HashMap<>();
        Map<String, Object> params = chain.getState().resolveParameters(this);

        String prompt = getStringParam(params, "prompt");
        if (StrUtil.isBlank(prompt)) {
            throw new BusinessException("图片生成提示词(prompt)不能为空");
        }

        ModelService modelService = SpringContextUtil.getBean(ModelService.class);
        Model modelInstance = modelService.getModelInstance(modelId);
        if (modelInstance == null) {
            throw new BusinessException("图片生成模型不存在");
        }

        ImageModel imageModel = modelInstance.toImageModel();
        if (imageModel == null) {
            throw new BusinessException("图片生成模型创建失败");
        }

        GenerateImageRequest request = new GenerateImageRequest();
        request.setPrompt(prompt);
        request.setNegativePrompt(getStringParam(params, "negativePrompt"));
        request.setSizeString(getStringParam(params, "size"));
        request.setResolution(getStringParam(params, "resolution"));
        request.setQuality(getStringParam(params, "quality"));
        request.setStyle(getStringParam(params, "style"));
        request.setSeed(getIntegerParam(params, "seed"));
        request.setN(1);

        // 输入图片：用于参考图生成、多图融合或图片编辑。
        String inputImages = getStringParam(params, "inputImages");
        if (StrUtil.isNotBlank(inputImages)) {
            String trimmedInput = inputImages.trim();
            List<String> urls;
            if (trimmedInput.startsWith("[")) {
                // JSON 数组格式
                urls = JSON.parseArray(trimmedInput, String.class);
            } else {
                // 逗号分隔的纯字符串
                urls = Arrays.asList(trimmedInput.split(","));
            }
            for (String url : urls) {
                String trimmed = url == null ? "" : url.trim();
                if (StrUtil.isNotBlank(trimmed)) {
                    request.addInputImage(Image.ofUrl(trimmed));
                }
            }
        }

        ImageResponse response = imageModel.generate(request);
        if (response == null || response.isError()) {
            String msg = response == null ? "图片生成无响应" : response.getErrorMessage();
            throw new BusinessException("图片生成失败：" + msg);
        }
        if (response.getImages() == null || response.getImages().isEmpty()) {
            throw new BusinessException("图片生成失败：未返回图片数据");
        }

        Image image = response.getImages().get(0);
        byte[] bytes = resolveImageBytes(image);
        String suffix = resolveSuffix(image);
        String fileName = IdUtil.fastSimpleUUID() + "." + suffix;

        FileStorageManager manager = SpringContextUtil.getBean(FileStorageManager.class);
        String url = manager.save(new CustomFile(fileName, bytes));

        res.put("url", url);
        log.info("图片生成成功，已保存至：{}", url);
        return res;
    }

    /**
     * 从响应图片对象中解析出二进制字节。
     * <p>优先级：原始字节 > Base64 > 远程 URL 下载。</p>
     */
    private byte[] resolveImageBytes(Image image) {
        if (image.getBytes() != null && image.getBytes().length > 0) {
            return image.getBytes();
        }
        if (StrUtil.isNotBlank(image.getB64Json())) {
            return Base64.getDecoder().decode(image.getB64Json());
        }
        if (StrUtil.isNotBlank(image.getUrl())) {
            return HttpUtil.downloadBytes(image.getUrl());
        }
        throw new BusinessException("图片数据为空，无法保存");
    }

    /**
     * 根据图片 MIME 类型推断文件后缀，默认为 png。
     */
    private String resolveSuffix(Image image) {
        String mimeType = image.getMimeType();
        if (StrUtil.isBlank(mimeType)) {
            return "png";
        }
        switch (mimeType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/webp":
                return "webp";
            case "image/gif":
                return "gif";
            case "image/bmp":
                return "bmp";
            default:
                return "png";
        }
    }

    private static String getStringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value == null ? null : value.toString();
    }

    private static Integer getIntegerParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public BigInteger getModelId() {
        return modelId;
    }

    public void setModelId(BigInteger modelId) {
        this.modelId = modelId;
    }
}
