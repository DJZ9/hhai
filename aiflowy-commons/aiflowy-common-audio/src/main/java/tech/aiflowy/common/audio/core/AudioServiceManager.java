package tech.aiflowy.common.audio.core;

import com.agentsflex.audio.aliyun.AliyunSpeechToTextConfig;
import com.agentsflex.audio.aliyun.AliyunSpeechToTextModel;
import com.agentsflex.audio.aliyun.AliyunTextToSpeechConfig;
import com.agentsflex.audio.aliyun.AliyunTextToSpeechModel;
import com.agentsflex.audio.tencent.TencentSpeechToTextConfig;
import com.agentsflex.audio.tencent.TencentSpeechToTextModel;
import com.agentsflex.audio.tencent.TencentTextToSpeechConfig;
import com.agentsflex.audio.tencent.TencentTextToSpeechModel;
import com.agentsflex.audio.volc.VolcSpeechToTextConfig;
import com.agentsflex.audio.volc.VolcSpeechToTextModel;
import com.agentsflex.audio.volc.VolcTextToSpeechConfig;
import com.agentsflex.audio.volc.VolcTextToSpeechModel;
import com.agentsflex.core.audio.stt.SpeechToTextModel;
import com.agentsflex.core.audio.stt.SpeechToTextOptions;
import com.agentsflex.core.audio.stt.SpeechToTextRequest;
import com.agentsflex.core.audio.stt.SpeechToTextResponse;
import com.agentsflex.core.audio.tts.TextToSpeechModel;
import com.agentsflex.core.audio.tts.TextToSpeechOptions;
import com.agentsflex.core.audio.tts.TextToSpeechRequest;
import com.agentsflex.core.audio.tts.TextToSpeechResponse;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.aiflowy.system.service.SysOptionService;

import java.io.InputStream;
import java.util.List;

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

    // ==================== TTS 语音合成 ====================

    /**
     * 文本转语音（默认 mp3 格式）。
     * 凭证复用系统设置「语音」标签页中各供应商的 STT 配置。
     */
    public byte[] textToSpeech(String text) {
        return textToSpeech(text, null, "mp3", null, null, null);
    }

    /**
     * 文本转语音（指定合成参数）。
     * <p>
     * 凭证复用系统设置「语音」标签页中各供应商的 STT 配置（accessKeyId/accessKeySecret/appKey
     * 或 secretId/secretKey/appId 或 apiKey）。voice 未传时由具体服务商决定默认发音人。
     *
     * @param text       待合成文本，不可为空
     * @param voice      发音人，可选（如阿里云 xiaoyun、火山 zh_female_vv_uranus_bigtts）
     * @param format     输出音频格式，如 mp3 / wav / pcm，默认 mp3
     * @param speed     语速，可选
     * @param volume    音量，可选
     * @param sampleRate 采样率，可选
     * @return 合并后的音频字节；失败返回空数组
     */
    public byte[] textToSpeech(String text, String voice, String format,
                               Double speed, Integer volume, Integer sampleRate) {
        if (text == null || text.isEmpty()) {
            log.warn("textToSpeech: text is empty");
            return new byte[0];
        }

        TextToSpeechModel model = createTtsModel();
        if (model == null) {
            return new byte[0];
        }

        TextToSpeechOptions options = new TextToSpeechOptions();
        if (voice != null && !voice.isEmpty()) {
            options.setVoice(voice);
        }
        String fmt = (format == null || format.isEmpty()) ? "mp3" : format;
        options.setFormat(fmt);
        if (speed != null) {
            options.setSpeed(speed);
        }
        if (volume != null) {
            options.setVolume(volume);
        }
        if (sampleRate != null) {
            options.setSampleRate(sampleRate);
        }

        TextToSpeechRequest request = new TextToSpeechRequest(text, options);
        TextToSpeechResponse response;
        try {
            response = model.tts(request);
        } catch (Exception e) {
            log.error("TTS invoke failed: {}", e.getMessage(), e);
            return new byte[0];
        }
        if (response == null || !response.isSuccess()) {
            String msg = response == null ? "null response" : response.getMessage();
            log.error("TTS failed: {}", msg);
            return new byte[0];
        }
        List<byte[]> results = response.getResults();
        if (results == null || results.isEmpty()) {
            log.error("TTS failed: no audio data returned");
            return new byte[0];
        }
        return mergeBytes(results);
    }

    // ==================== STT 语音识别（按 URL） ====================

    /**
     * 语音转文字（按音频 URL，默认 mp3 格式）。
     * 由 agents-flex 内部下载音频后发送给服务商识别。
     */
    public String audioToTextByUrl(String audioUrl) {
        return audioToTextByUrl(audioUrl, "mp3");
    }

    /**
     * 语音转文字（按音频 URL，指定格式）。
     *
     * @param audioUrl 音频访问地址，不可为空
     * @param format   音频格式，如 wav / mp3 / pcm，默认 mp3
     * @return 识别结果文本，失败返回空字符串
     */
    public String audioToTextByUrl(String audioUrl, String format) {
        if (audioUrl == null || audioUrl.isEmpty()) {
            log.warn("audioToTextByUrl: audioUrl is empty");
            return "";
        }

        SpeechToTextModel model = createSttModel();
        if (model == null) {
            return "";
        }

        SpeechToTextOptions options = new SpeechToTextOptions();
        options.setFormat((format == null || format.isEmpty()) ? "mp3" : format);

        SpeechToTextRequest request = new SpeechToTextRequest();
        request.setAudioUrl(audioUrl);
        request.setOptions(options);

        SpeechToTextResponse response = model.stt(request);
        if (response != null && response.isSuccess()) {
            return response.getResult() != null ? response.getResult() : "";
        }
        log.error("STT failed: {}", response != null ? response.getMessage() : "null response");
        return "";
    }

    // ==================== 内部方法 ====================

    private static byte[] mergeBytes(List<byte[]> chunks) {
        int total = 0;
        for (byte[] c : chunks) {
            if (c != null) {
                total += c.length;
            }
        }
        byte[] merged = new byte[total];
        int offset = 0;
        for (byte[] c : chunks) {
            if (c == null || c.length == 0) {
                continue;
            }
            System.arraycopy(c, 0, merged, offset, c.length);
            offset += c.length;
        }
        return merged;
    }

    private TextToSpeechModel createTtsModel() {
        String provider = getSysOptionValue("voice_provider");
        if (provider == null || provider.isEmpty()) {
            log.warn("voice_provider not configured in sys_option, TTS not available");
            return null;
        }

        log.info("Creating TTS model for provider: {}", provider);

        switch (provider) {
            case "aliyun": {
                String accessKeyId = getSysOptionValue("voice_aliyun_access_key_id");
                String accessKeySecret = getSysOptionValue("voice_aliyun_access_key_secret");
                String appKey = getSysOptionValue("voice_aliyun_app_key");
                if (accessKeyId == null || accessKeySecret == null || appKey == null) {
                    log.warn("Aliyun voice config incomplete in sys_option");
                    return null;
                }
                AliyunTextToSpeechConfig config = new AliyunTextToSpeechConfig();
                config.setAccessKeyId(accessKeyId);
                config.setAccessKeySecret(accessKeySecret);
                config.setAppKey(appKey);
                return new AliyunTextToSpeechModel(config);
            }
            case "tencent": {
                String secretId = getSysOptionValue("voice_tencent_secret_id");
                String secretKey = getSysOptionValue("voice_tencent_secret_key");
                String appId = getSysOptionValue("voice_tencent_app_id");
                if (secretId == null || secretKey == null || appId == null) {
                    log.warn("Tencent voice config incomplete in sys_option");
                    return null;
                }
                TencentTextToSpeechConfig config = new TencentTextToSpeechConfig();
                config.setSecretId(secretId);
                config.setSecretKey(secretKey);
                config.setAppId(appId);
                return new TencentTextToSpeechModel(config);
            }
            case "volc": {
                String apiKey = getSysOptionValue("voice_volc_api_key");
                if (apiKey == null) {
                    log.warn("Volc voice config incomplete in sys_option");
                    return null;
                }
                VolcTextToSpeechConfig config = new VolcTextToSpeechConfig();
                config.setApiKey(apiKey);
                return new VolcTextToSpeechModel(config);
            }
            default:
                log.warn("Unknown voice provider: {}, TTS not available", provider);
                return null;
        }
    }

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