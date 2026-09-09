package tech.aiflowy.common.audio.core;

import com.agentsflex.audio.aliyun.AliyunSpeechToTextConfig;
import com.agentsflex.audio.aliyun.AliyunSpeechToTextModel;
import com.agentsflex.audio.tencent.TencentSpeechToTextConfig;
import com.agentsflex.audio.tencent.TencentSpeechToTextModel;
import com.agentsflex.audio.volc.VolcSpeechToTextConfig;
import com.agentsflex.audio.volc.VolcSpeechToTextModel;
import com.agentsflex.core.audio.stt.SpeechToTextModel;
import com.agentsflex.core.audio.stt.SpeechToTextOptions;
import com.agentsflex.core.audio.stt.SpeechToTextRequest;
import com.agentsflex.core.audio.stt.SpeechToTextResponse;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.aiflowy.system.service.SysOptionService;

import java.io.InputStream;

/**
 * 音频服务管理器 - 基于 agents-flex 实现三平台 STT / TTS。
 * <p>
 * 参考文档：
 * <ul>
 *   <li>阿里云：https://agentsflex.com/zh/audio/aliyun.html</li>
 *   <li>腾讯云：https://agentsflex.com/zh/audio/tencent.html</li>
 *   <li>火山引擎：https://agentsflex.com/zh/audio/volc.html</li>
 * </ul>
 */
@Component
public class AudioServiceManager {

    private static final Logger log = LoggerFactory.getLogger(AudioServiceManager.class);

    @Resource
    private SysOptionService sysOptionService;

    // ==================== STT 语音识别 ====================

    /**
     * 语音转文字（默认 MP3 格式）
     */
    public String audioToText(InputStream audioStream) {
        return audioToText(audioStream, "mp3");
    }

    /**
     * 语音转文字（指定格式）
     *
     * @param audioStream 音频输入流
     * @param format      音频格式（如 wav, mp3, pcm）
     * @return 识别结果文本，失败返回空字符串
     */
    public String audioToText(InputStream audioStream, String format) {
        SpeechToTextModel model = createSttModel();
        if (model == null) {
            return "";
        }

        SpeechToTextOptions options = new SpeechToTextOptions();
        options.setFormat(format);

        SpeechToTextRequest request = new SpeechToTextRequest();
        request.setAudioStream(audioStream);
        request.setOptions(options);

        SpeechToTextResponse response = model.stt(request);
        if (response != null && response.isSuccess()) {
            return response.getResult() != null ? response.getResult() : "";
        }
        log.error("STT failed: {}", response != null ? response.getMessage() : "null response");
        return "";
    }

    // ==================== TTS 语音合成（预留） ====================

    /**
     * 文本转语音（预留接口，后续实现）
     */
    public byte[] textToSpeech(String text) {
        log.warn("textToSpeech not implemented for provider: {}", getSysOptionValue("voice_provider"));
        return new byte[0];
    }

    // ==================== 内部方法 ====================

    private SpeechToTextModel createSttModel() {
        // 从 sys_option 表读取 voice_provider
        String provider = getSysOptionValue("voice_provider");
        if (provider == null || provider.isEmpty()) {
            log.warn("voice_provider not configured in sys_option, STT not available");
            return null;
        }

        log.info("Creating STT model for provider: {}", provider);

        switch (provider) {
            case "aliyun": {
                String accessKeyId = getSysOptionValue("voice_aliyun_access_key_id");
                String accessKeySecret = getSysOptionValue("voice_aliyun_access_key_secret");
                String appKey = getSysOptionValue("voice_aliyun_app_key");
                if (accessKeyId == null || accessKeySecret == null || appKey == null) {
                    log.warn("Aliyun voice config incomplete in sys_option");
                    return null;
                }
                AliyunSpeechToTextConfig config = new AliyunSpeechToTextConfig();
                config.setAccessKeyId(accessKeyId);
                config.setAccessKeySecret(accessKeySecret);
                config.setAppKey(appKey);
                return new AliyunSpeechToTextModel(config);
            }
            case "tencent": {
                String secretId = getSysOptionValue("voice_tencent_secret_id");
                String secretKey = getSysOptionValue("voice_tencent_secret_key");
                String appId = getSysOptionValue("voice_tencent_app_id");
                if (secretId == null || secretKey == null || appId == null) {
                    log.warn("Tencent voice config incomplete in sys_option");
                    return null;
                }
                TencentSpeechToTextConfig config = new TencentSpeechToTextConfig();
                config.setSecretId(secretId);
                config.setSecretKey(secretKey);
                config.setAppId(appId);
                return new TencentSpeechToTextModel(config);
            }
            case "volc": {
                String apiKey = getSysOptionValue("voice_volc_api_key");
                if (apiKey == null) {
                    log.warn("Volc voice config incomplete in sys_option");
                    return null;
                }
                VolcSpeechToTextConfig config = new VolcSpeechToTextConfig();
                config.setApiKey(apiKey);
                return new VolcSpeechToTextModel(config);
            }
            default:
                log.warn("Unknown voice provider: {}, STT not available", provider);
                return null;
        }
    }

    private String getSysOptionValue(String key) {
        try {
            tech.aiflowy.system.entity.SysOption option = sysOptionService.getByOptionKey(key);
            return option != null ? option.getValue() : null;
        } catch (Exception e) {
            log.error("Failed to get sys_option value for key: {}", key, e);
            return null;
        }
    }
}