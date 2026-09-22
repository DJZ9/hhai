package tech.aiflowy.ai.node;

import cn.hutool.core.util.StrUtil;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.BaseNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.common.audio.core.AudioServiceManager;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.util.HashMap;
import java.util.Map;


/**
 * 语音转文本节点。
 * <p>
 * 直接复用系统设置「语音」标签页中配置的供应商凭证（{@link AudioServiceManager}），
 * 识别输入音频（audioUrl）并输出文本。无需在模型管理中维护 STT 模型。
 * <p>
 * 当前实现使用 {@link AudioServiceManager#audioToTextByUrl(String, String)}，
 * 由 agents-flex 内部下载音频字节后发送给服务商。如需直接传 InputStream / File，
 * 可后续扩展节点参数。
 */
public class SpeechToTextNode extends BaseNode {

    private static final Logger log = LoggerFactory.getLogger(SpeechToTextNode.class);

    /**
     * 音频格式，如 mp3 / wav / pcm，默认 mp3。
     */
    private String format;

    /**
     * 采样率，可选，默认 16000。
     */
    private Integer sampleRate;

    public SpeechToTextNode() {
    }

    @Override
    public Map<String, Object> execute(Chain chain) {
        Map<String, Object> params = chain.getState().resolveParameters(this);
        Map<String, Object> res = new HashMap<>();

        // 1. 校验音频地址
        String audioUrl = asString(params.get("audioUrl"));
        if (StrUtil.isBlank(audioUrl)) {
            throw new BusinessException("语音识别节点：audioUrl 不能为空");
        }

        // 2. 调用 AudioServiceManager 识别音频（凭证来自系统设置）
        AudioServiceManager audioServiceManager = SpringContextUtil.getBean(AudioServiceManager.class);
        String fmt = StrUtil.isNotBlank(this.format) ? this.format : "mp3";
        String text;
        try {
            text = audioServiceManager.audioToTextByUrl(audioUrl, fmt);
        } catch (Exception e) {
            throw new BusinessException("语音识别调用异常：" + e.getMessage());
        }

        res.put("text", text != null ? text : "");
        log.info("语音识别成功，结果长度：{}", text == null ? 0 : text.length());
        return res;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }
}
