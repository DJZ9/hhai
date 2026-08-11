
package tech.aiflowy.ai.entity;

import cn.hutool.core.util.StrUtil;
import com.agentsflex.core.model.chat.ChatModel;
import com.agentsflex.core.model.embedding.EmbeddingModel;
import com.agentsflex.core.model.rerank.RerankModel;
import com.agentsflex.core.store.VectorData;
import com.agentsflex.embedding.ollama.OllamaEmbeddingConfig;
import com.agentsflex.embedding.ollama.OllamaEmbeddingModel;
import com.agentsflex.embedding.openai.OpenAIEmbeddingConfig;
import com.agentsflex.embedding.openai.OpenAIEmbeddingModel;
import com.agentsflex.model.chat.deepseek.DeepseekChatModel;
import com.agentsflex.model.chat.deepseek.DeepseekConfig;
import com.agentsflex.model.chat.ollama.OllamaChatConfig;
import com.agentsflex.model.chat.ollama.OllamaChatModel;
import com.agentsflex.model.chat.openai.OpenAIChatConfig;
import com.agentsflex.model.chat.openai.OpenAIChatModel;
import com.agentsflex.rerank.DefaultRerankModel;
import com.agentsflex.rerank.DefaultRerankModelConfig;
import com.agentsflex.rerank.gitee.GiteeRerankModel;
import com.agentsflex.rerank.gitee.GiteeRerankModelConfig;
import com.alibaba.fastjson2.JSON;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.ai.agentsflex.ocrModel.*;
import tech.aiflowy.ai.entity.base.ModelBase;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;

import java.util.Map;

/**
 * 实体类。
 *
 * @author michael
 * @since 2024-08-23
 */

@Table("tb_model")
public class Model extends ModelBase {

    @RelationManyToOne(selfField = "providerId", targetField = "id")
    private ModelProvider modelProvider;

    /**
     * 模型类型
     */
    public final static String[] MODEL_TYPES = {"chatModel", "embeddingModel", "rerankModel", "ocrModel"};


    public ModelProvider getModelProvider() {
        return modelProvider;
    }

    public void setModelProvider(ModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    public ChatModel toChatModel(Map<String, Object> customProperties) {
        String providerType = modelProvider.getProviderType();
        if (StringUtil.noText(providerType)) {
            throw new BusinessException("模型提供者不能为空");
        }

        switch (providerType.toLowerCase()) {
            case "ollama":
                OllamaChatConfig ollamaChatConfig = new OllamaChatConfig();
                ollamaChatConfig.setEndpoint(checkAndGetEndpoint());
                if (StringUtil.hasText(getApiKey())) {
                    ollamaChatConfig.setApiKey(checkAndGetApiKey());
                }
                ollamaChatConfig.setModel(checkAndGetModelName());
                ollamaChatConfig.setProvider(getModelProvider().getProviderName());
                setChatConfig(customProperties);
                ollamaChatConfig.setCustomProperties(customProperties);
                ollamaChatConfig.setSupportImage(isTrueOr(this.getSupportImage(), this.getSupportImageB64Only()));
                return new OllamaChatModel(ollamaChatConfig);
            case "deepseek":
                DeepseekConfig deepseekConfig = new DeepseekConfig();
                deepseekConfig.setProvider(getModelProvider().getProviderName());
                deepseekConfig.setEndpoint(checkAndGetEndpoint());
                deepseekConfig.setApiKey(checkAndGetApiKey());
                deepseekConfig.setModel(checkAndGetModelName());
                deepseekConfig.setRequestPath(checkAndGetRequestPath());
                deepseekConfig.setSupportImage(isTrueOr(this.getSupportImage(), this.getSupportImageB64Only()));
                setChatConfig(customProperties);
                deepseekConfig.setCustomProperties(customProperties);
                return new DeepseekChatModel(deepseekConfig);
            default:
                OpenAIChatConfig openAIChatConfig = new OpenAIChatConfig();
                openAIChatConfig.setProvider(getModelProvider().getProviderName());
                openAIChatConfig.setEndpoint(checkAndGetEndpoint());
                openAIChatConfig.setApiKey(checkAndGetApiKey());
                openAIChatConfig.setModel(checkAndGetModelName());
                openAIChatConfig.setLogEnabled(true);
                openAIChatConfig.setRequestPath(checkAndGetRequestPath());
                openAIChatConfig.setSupportImage(isTrueOr(this.getSupportImage(), this.getSupportImageB64Only()));
                if (getSupportToolMessage() != null) {
                    openAIChatConfig.setSupportToolMessage(getSupportToolMessage());
                }
                setChatConfig(customProperties);
                openAIChatConfig.setCustomProperties(customProperties);
                return new OpenAIChatModel(openAIChatConfig);
        }
    }

    private void setChatConfig(Map<String, Object> customProperties) {
        if (customProperties != null) {
            customProperties.put("modelId", getId());
        }
    }

    public RerankModel toRerankModel() {
        switch (modelProvider.getProviderType().toLowerCase()) {
            case "gitee":
                GiteeRerankModelConfig giteeRerankModelConfig = new GiteeRerankModelConfig();
                giteeRerankModelConfig.setProvider(getModelProvider().getProviderName());
                giteeRerankModelConfig.setApiKey(checkAndGetApiKey());
                giteeRerankModelConfig.setEndpoint(checkAndGetEndpoint());
                giteeRerankModelConfig.setModel(checkAndGetModelName());
                giteeRerankModelConfig.setRequestPath(checkAndGetRequestPath());
                return new GiteeRerankModel(giteeRerankModelConfig);
            default:
                DefaultRerankModelConfig defaultRerankModelConfig = new DefaultRerankModelConfig();
                defaultRerankModelConfig.setProvider(getModelProvider().getProviderName());
                defaultRerankModelConfig.setApiKey(checkAndGetApiKey());
                defaultRerankModelConfig.setEndpoint(checkAndGetEndpoint());
                defaultRerankModelConfig.setRequestPath(checkAndGetRequestPath());
                defaultRerankModelConfig.setModel(checkAndGetModelName());
                return new DefaultRerankModel(defaultRerankModelConfig);
        }
    }

    public EmbeddingModel toEmbeddingModel() {
        String providerType = modelProvider.getProviderType();
        if (StringUtil.noText(providerType)) {
            return null;
        }
        try {
            switch (providerType.toLowerCase()) {
                case "ollama":
                    OllamaEmbeddingConfig ollamaEmbeddingConfig = new OllamaEmbeddingConfig();
                    ollamaEmbeddingConfig.setProvider(getModelProvider().getProviderName());
                    ollamaEmbeddingConfig.setEndpoint(checkAndGetEndpoint());
                    if (StringUtil.hasText(getApiKey())) {
                        ollamaEmbeddingConfig.setApiKey(getApiKey());
                    }
                    ollamaEmbeddingConfig.setModel(checkAndGetModelName());
                    ollamaEmbeddingConfig.setRequestPath(getRequestPath());
                    return new OllamaEmbeddingModel(ollamaEmbeddingConfig);
                default:
                    OpenAIEmbeddingConfig openAIEmbeddingConfig = new OpenAIEmbeddingConfig();
                    openAIEmbeddingConfig.setProvider(getModelProvider().getProviderName());
                    openAIEmbeddingConfig.setEndpoint(checkAndGetEndpoint());
                    openAIEmbeddingConfig.setApiKey(checkAndGetApiKey());
                    openAIEmbeddingConfig.setModel(checkAndGetModelName());
                    openAIEmbeddingConfig.setRequestPath(checkAndGetRequestPath());
                    return new OpenAIEmbeddingModel(openAIEmbeddingConfig);
            }
        } catch (Exception e) {
            throw new BusinessException("向量模型配置失败：" + e.getMessage());
        }
    }

    public OcrModel toOcrModel() {
        switch (modelProvider.getProviderType().toLowerCase()) {
            case "gitee":
                GiteeOcrModelConfig giteeOcrModelConfig = new GiteeOcrModelConfig();
                GiteeOcrModel giteeOcrModel = new GiteeOcrModel(giteeOcrModelConfig);
                giteeOcrModel.setApiKey(checkAndGetApiKey());
                giteeOcrModel.setEndpoint(checkAndGetEndpoint());
                if (StrUtil.isNotBlank(getRequestPath())) {
                    giteeOcrModel.setRequestPath(getRequestPath());
                }
                giteeOcrModel.setModel(checkAndGetModelName());
                return giteeOcrModel;
            case "mineru":
                MineruOcrModelConfig mineruOcrModelConfig = new MineruOcrModelConfig();
                MineruOcrModel mineruOcrModel = new MineruOcrModel(mineruOcrModelConfig);
                mineruOcrModel.setApiKey(checkAndGetApiKey());
                mineruOcrModel.setEndpoint(checkAndGetEndpoint());
                if (StrUtil.isNotBlank(getRequestPath())) {
                    mineruOcrModel.setRequestPath(getRequestPath());
                }
                mineruOcrModel.setModel(checkAndGetModelName());
                // 从 options 中读取额外配置
                Map<String, Object> options = getOptions();
                if (options != null) {
                    if (options.containsKey("modelVersion")) {
                        mineruOcrModel.setModelVersion((String) options.get("modelVersion"));
                    }
                    if (options.containsKey("enableTable")) {
                        mineruOcrModel.setEnableTable((Boolean) options.get("enableTable"));
                    }
                    if (options.containsKey("enableFormula")) {
                        mineruOcrModel.setEnableFormula((Boolean) options.get("enableFormula"));
                    }
                    if (options.containsKey("isOcr")) {
                        mineruOcrModel.setIsOcr((Boolean) options.get("isOcr"));
                    }
                    if (options.containsKey("language")) {
                        mineruOcrModel.setLanguage((String) options.get("language"));
                    }
                    if (options.containsKey("pageRanges")) {
                        mineruOcrModel.setPageRanges((String) options.get("pageRanges"));
                    }
                }
                return mineruOcrModel;
            case "localmineru":
                Map<String, Object> localOptions = getOptions();
                LocalMineruOcrModel localMineruOcrModel;
                if (localOptions != null && !localOptions.isEmpty()) {
                    localMineruOcrModel = JSON.parseObject(JSON.toJSONString(localOptions), LocalMineruOcrModel.class);
                } else {
                    localMineruOcrModel = new LocalMineruOcrModel();
                }
                localMineruOcrModel.setApiKey(getApiKey());
                localMineruOcrModel.setEndpoint(checkAndGetEndpoint());
                if (StrUtil.isNotBlank(getRequestPath())) {
                    localMineruOcrModel.setRequestPath(getRequestPath());
                }
                localMineruOcrModel.setModel(checkAndGetModelName());
                localMineruOcrModel.setBackend(checkAndGetModelName());
                return localMineruOcrModel;
            case "aliyun":
                AliBailianOcrModelConfig aliBailianOcrModelConfig = new AliBailianOcrModelConfig();
                AliBailianOcrModel aliBailianOcrModel = new AliBailianOcrModel(aliBailianOcrModelConfig);
                aliBailianOcrModel.setApiKey(checkAndGetApiKey());
                if (StrUtil.isNotBlank(checkAndGetEndpoint())) {
                    aliBailianOcrModel.setEndpoint(checkAndGetEndpoint());
                }
                // 从 options 中读取额外配置
                Map<String, Object> aliOptions = getOptions();
                if (aliOptions != null) {
                    if (aliOptions.containsKey("workspaceId")) {
                        aliBailianOcrModel.setWorkspaceId((String) aliOptions.get("workspaceId"));
                    }
                    if (aliOptions.containsKey("modelName")) {
                        aliBailianOcrModel.setModelName((String) aliOptions.get("modelName"));
                    }
                    if (aliOptions.containsKey("ocrTask")) {
                        aliBailianOcrModel.setOcrTask((String) aliOptions.get("ocrTask"));
                    }
                }
                return aliBailianOcrModel;
        }
        return null;
    }

    /**
     * 获取模型向量的维度
     *
     * @return
     */
    public static int getEmbeddingDimension(EmbeddingModel embeddingModel) {
        if (embeddingModel == null) {
            throw new BusinessException("embeddingModel不能为空");
        }
        VectorData vectorData = embeddingModel.embed("测试向量维度");
        return vectorData.getVector().length;
    }

    public String checkAndGetRequestPath() {
        if (StrUtil.isEmpty(getRequestPath())) {
            throw new BusinessException("请求地址不能为空");
        }
        return getRequestPath();
    }

    public String checkAndGetApiKey() {
        if (StrUtil.isEmpty(getApiKey())) {
            throw new BusinessException("API 密钥不能为空");
        }
        return getApiKey();
    }

    public String checkAndGetEndpoint() {
        if (StrUtil.isEmpty(getEndpoint())) {
            throw new BusinessException("API 地址不能为空");
        }
        return getEndpoint();
    }

    public String checkAndGetModelName() {
        if (StrUtil.isEmpty(getModelName())) {
            throw new BusinessException("模型名称不能为空");
        }
        return getModelName();
    }

    /**
     * 安全地判断多个 Boolean 值的 OR 结果（null 视为 false）
     * @param values Boolean 值数组
     * @return OR 运算结果
     */
    private static boolean isTrueOr(Boolean... values) {
        if (values == null) {
            return false;
        }
        for (Boolean value : values) {
            if (value != null && value) {
                return true;
            }
        }
        return false;
    }


}
