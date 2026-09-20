package tech.aiflowy.ai.node;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.BaseNode;
import com.agentsflex.core.model.image.Image;
import com.agentsflex.core.model.video.GenerateVideoRequest;
import com.agentsflex.core.model.video.Video;
import com.agentsflex.core.model.video.VideoGenerationMode;
import com.agentsflex.core.model.video.VideoModel;
import com.agentsflex.core.model.video.VideoResponse;
import com.agentsflex.core.model.video.VideoTaskStatus;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.common.filestorage.FileStorageManager;
import tech.aiflowy.common.util.SpringContextUtil;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * 视频生成节点。
 * <p>
 * 通过 {@link ModelService} 按 modelId 加载 {@link Model}，调用 {@link Model#toVideoModel()}
 * 取得 agents-flex 的 {@link VideoModel}，再用 {@link VideoModel#generateAndWait} 阻塞等待
 * 服务商返回视频资源。生成结果会下载并转存到本地文件存储，输出稳定的 url。
 * <p>
 * 支持文生视频、图生视频、首尾帧、参考图、视频编辑和音频驱动全部 6 种生成模式；
 * 模式可由前端显式指定，未指定时由 agents-flex 框架根据已设置的素材自动推断。
 */
public class VideoGenerationNode extends BaseNode {

    /**
     * tb_model 主键，指向 modelType=videoModel 的视频模型记录。
     */
    private BigInteger modelId;

    /**
     * 视频分辨率，如 720p、1080p。来自前端 forms 配置。
     */
    private String resolution;

    /**
     * 视频宽高比，如 16:9、9:16。来自前端 forms 配置。
     */
    private String aspectRatio;

    /**
     * 视频时长（秒）。来自前端 forms 配置。
     */
    private Integer duration;

    /**
     * 生成模式，可空。来自前端 forms 配置。
     */
    private String generationMode;

    // 轮询间隔
    private final static long POLL_INTERVAL_MILLIS = 10_000L;
    // 超时时间（30分钟）
    private final static long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    public VideoGenerationNode() {
    }

    public VideoGenerationNode(BigInteger modelId) {
        this.modelId = modelId;
    }

    @Override
    public Map<String, Object> execute(Chain chain) {
        Map<String, Object> params = chain.getState().resolveParameters(this);
        Map<String, Object> res = new HashMap<>();

        // 1. 加载 Model 实例并转换为 VideoModel
        if (modelId == null) {
            res.put("error", "视频模型未配置");
            res.put("status", VideoTaskStatus.FAILED.name());
            return res;
        }
        ModelService modelService = SpringContextUtil.getBean(ModelService.class);
        Model model = modelService.getModelInstance(modelId);
        if (model == null) {
            res.put("error", "视频模型不存在：" + modelId);
            res.put("status", VideoTaskStatus.FAILED.name());
            return res;
        }
        if (!"videoModel".equals(model.getModelType())) {
            res.put("error", "该模型不是视频模型：" + modelId);
            res.put("status", VideoTaskStatus.FAILED.name());
            return res;
        }

        VideoModel videoModel;
        try {
            videoModel = model.toVideoModel();
        } catch (Exception e) {
            res.put("error", "视频模型配置失败：" + e.getMessage());
            res.put("status", VideoTaskStatus.FAILED.name());
            return res;
        }

        // 2. 构造 GenerateVideoRequest
        GenerateVideoRequest req = new GenerateVideoRequest();
        req.setPrompt(asString(params.get("prompt")));
        if (StrUtil.isNotBlank(model.getModelName())) {
            req.setModel(model.getModelName());
        }
        if (StrUtil.isNotBlank(this.aspectRatio)) req.setAspectRatio(this.aspectRatio);
        if (StrUtil.isNotBlank(this.resolution)) req.setResolution(this.resolution.toUpperCase());
        if (this.duration != null) req.setDuration(this.duration);
        String audioUrl = asString(params.get("audioUrl"));
        if (StrUtil.isNotBlank(audioUrl)) req.setAudioUrl(audioUrl);

        // 图片 / 视频输入：前端传 URL 字符串，这里转 agents-flex Image / Video
        String firstFrameUrl = asString(params.get("firstFrame"));
        if (StrUtil.isNotBlank(firstFrameUrl)) req.setFirstFrame(Image.ofUrl(firstFrameUrl));
        String lastFrameUrl = asString(params.get("lastFrame"));
        if (StrUtil.isNotBlank(lastFrameUrl)) req.setLastFrame(Image.ofUrl(lastFrameUrl));
        String sourceVideoUrl = asString(params.get("sourceVideo"));
        if (StrUtil.isNotBlank(sourceVideoUrl)) req.setSourceVideo(Video.ofUrl(sourceVideoUrl));
        String refs = asString(params.get("referenceImages"));
        if (StrUtil.isNotBlank(refs)) {
            for (String url : refs.split(",")) {
                String trimmed = url.trim();
                if (StrUtil.isNotBlank(trimmed)) req.addReferenceImage(Image.ofUrl(trimmed));
            }
        }

        // 生成模式显式指定（可空，框架自动推断）
        String modeName = this.generationMode;
        if (StrUtil.isNotBlank(modeName)) {
            try {
                req.setGenerationMode(VideoGenerationMode.valueOf(modeName));
            } catch (IllegalArgumentException e) {
                res.put("error", "不支持的生成模式：" + modeName);
                res.put("status", VideoTaskStatus.FAILED.name());
                return res;
            }
        }

        VideoResponse response;
        try {
            response = videoModel.generateAndWait(req, TIMEOUT_MILLIS, POLL_INTERVAL_MILLIS);
        } catch (Exception e) {
            res.put("error", "视频生成调用异常：" + e.getMessage());
            res.put("status", VideoTaskStatus.FAILED.name());
            return res;
        }
        if (response == null) {
            res.put("error", "视频生成返回空响应");
            res.put("status", VideoTaskStatus.FAILED.name());
            return res;
        }

        // 4. 输出
        if (response.getTaskId() != null) res.put("taskId", response.getTaskId());
        res.put("status", response.getStatus() != null ? response.getStatus().name() : null);
        if (response.isError()) {
            res.put("error", response.getErrorMessage());
            return res;
        }

        // 5. 把远端临时 URL 下载并转存到本地 OSS，返回稳定地址
        Video video = response.getVideo();
        if (video != null && StrUtil.isNotBlank(video.getUrl())) {
            try {
                byte[] bytes = video.readBytes();
                if (bytes != null && bytes.length > 0) {
                    String fileName = IdUtil.fastSimpleUUID() + ".mp4";
                    CustomFile file = new CustomFile(fileName, bytes);
                    FileStorageManager manager = SpringContextUtil.getBean(FileStorageManager.class);
                    String url = manager.save(file);
                    res.put("url", url);
                } else {
                    // 下载失败时回退到原始临时 URL
                    res.put("url", video.getUrl());
                    res.put("warning", "视频转存失败，已回退到服务商临时地址");
                }
            } catch (Exception e) {
                // 转存失败不阻塞流程，回退到原始 URL
                res.put("url", video.getUrl());
                res.put("warning", "视频转存异常：" + e.getMessage());
            }
        }
        return res;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
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

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getGenerationMode() {
        return generationMode;
    }

    public void setGenerationMode(String generationMode) {
        this.generationMode = generationMode;
    }
}
