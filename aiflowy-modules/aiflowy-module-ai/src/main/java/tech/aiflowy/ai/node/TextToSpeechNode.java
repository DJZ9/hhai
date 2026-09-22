package tech.aiflowy.ai.node;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.node.BaseNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.common.audio.core.AudioServiceManager;
import tech.aiflowy.common.filestorage.FileStorageManager;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.util.HashMap;
import java.util.Map;

/**
 * 文本转语音节点。
 * <p>
 * 直接复用系统设置「语音」标签页中配置的供应商凭证（{@link AudioServiceManager}），
 * 将文本合成为音频字节后转存到本地文件存储，输出稳定的访问 url。无需在模型管理中维护 TTS 模型。
 * <p>
 * 支持的合成参数（来自前端 forms 配置 / 节点入参）：
 * <ul>
 *   <li>voice：发音人，由具体服务商定义</li>
 *   <li>format：音频格式，默认 mp3</li>
 *   <li>speed：语速</li>
 *   <li>volume：音量</li>
 *   <li>sampleRate：采样率</li>
 * </ul>
 */
public class TextToSpeechNode extends BaseNode {

    private static final Logger log = LoggerFactory.getLogger(TextToSpeechNode.class);

    /**
     * 发音人，由具体服务商定义（如阿里云 xiaoyun、火山 zh_female_vv_uranus_bigtts）。
     */
    private String voice;

    /**
     * 输出音频格式，如 mp3 / wav / pcm，默认 mp3。
     */
    private String format;

    /**
     * 语速，可选。
     */
    private Double speed;

    /**
     * 音量，可选。
     */
    private Integer volume;

    /**
     * 采样率，可选，默认由服务商决定。
     */
    private Integer sampleRate;

    public TextToSpeechNode() {
    }

    @Override
    public Map<String, Object> execute(Chain chain) {
        Map<String, Object> params = chain.getState().resolveParameters(this);
        Map<String, Object> res = new HashMap<>();

        // 1. 校验输入文本
        String text = asString(params.get("text"));
        if (StrUtil.isBlank(text)) {
            throw new BusinessException("文本转语音节点：text 不能为空");
        }

        // 2. 调用 AudioServiceManager 合成音频（凭证来自系统设置）
        AudioServiceManager audioServiceManager = SpringContextUtil.getBean(AudioServiceManager.class);
        String fmt = StrUtil.isNotBlank(this.format) ? this.format : "mp3";
        byte[] audioBytes;
        try {
            audioBytes = audioServiceManager.textToSpeech(
                    text, this.voice, fmt, this.speed, this.volume, this.sampleRate);
        } catch (Exception e) {
            throw new BusinessException("语音合成调用异常：" + e.getMessage());
        }
        if (audioBytes == null || audioBytes.length == 0) {
            throw new BusinessException("语音合成失败：未返回音频数据，请检查系统设置中的语音供应商配置");
        }

        // 3. 转存到本地文件存储，输出访问 url
        String fileName = IdUtil.fastSimpleUUID() + "." + fmt;
        try {
            FileStorageManager manager = SpringContextUtil.getBean(FileStorageManager.class);
            String url = manager.save(new CustomFile(fileName, audioBytes));
            res.put("url", url);
            log.info("文本转语音成功，已保存至：{}", url);
        } catch (Exception e) {
            throw new BusinessException("语音合成结果保存失败：" + e.getMessage());
        }
        return res;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }
}
