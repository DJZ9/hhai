package tech.aiflowy.ai.agentsflex.tool;

import com.agentsflex.core.model.chat.tool.ToolScanner;
import com.agentsflex.core.prompt.MemoryPrompt;
import com.agentsflex.websearch.WebSearchTool;
import com.agentsflex.websearch.baidu.BaiduQianfanSearchProvider;
import com.agentsflex.websearch.bocha.BochaSearchProvider;
import com.agentsflex.websearch.brave.BraveSearchProvider;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tech.aiflowy.common.util.MapUtil;
import tech.aiflowy.system.entity.SysOption;
import tech.aiflowy.system.service.SysOptionService;

import java.util.Map;

/**
 * 搜索引擎工具助手
 */
@Component
public class WebSearchToolHelper {

    private static final Logger log = LoggerFactory.getLogger(WebSearchToolHelper.class);

    @Resource
    private SysOptionService sysOptionService;

    /**
     * 添加搜索引擎工具
     */
    public void addWebSearchTool(MemoryPrompt memoryPrompt, Map<String, Object> otherSkills) {
        try {
            // 优先从 Bot 配置中获取搜索引擎提供商
            String searchEngineProvider = MapUtil.getString(otherSkills, "webSearchProvider");

            // 如果 Bot 未配置，则从系统配置获取
            if (!StringUtils.hasText(searchEngineProvider)) {
                SysOption providerOption = sysOptionService.getByOptionKey("search_engine_provider");
                if (providerOption != null) {
                    searchEngineProvider = providerOption.getValue();
                }
            }

            if (!StringUtils.hasText(searchEngineProvider)) {
                log.warn("搜索引擎未配置，请在系统设置或 Bot 配置中配置搜索引擎提供商");
                return;
            }

            WebSearchTool webSearchTool = null;

            switch (searchEngineProvider) {
                case "qianfan" -> {
                    SysOption apiKeyOption = sysOptionService.getByOptionKey("search_engine_qianfan_api_key");
                    String apiKey = apiKeyOption != null ? apiKeyOption.getValue() : null;
                    if (!StringUtils.hasText(apiKey)) {
                        log.warn("百度千帆搜索引擎 API Key 未配置");
                        return;
                    }
                    webSearchTool = WebSearchTool.builder()
                            .provider(new BaiduQianfanSearchProvider(apiKey))
                            .build();
                }
                case "bocha" -> {
                    SysOption apiKeyOption = sysOptionService.getByOptionKey("search_engine_bocha_api_key");
                    String apiKey = apiKeyOption != null ? apiKeyOption.getValue() : null;
                    if (!StringUtils.hasText(apiKey)) {
                        log.warn("博查搜索引擎 API Key 未配置");
                        return;
                    }
                    webSearchTool = WebSearchTool.builder()
                            .provider(new BochaSearchProvider(apiKey))
                            .build();
                }
                case "brave" -> {
                    SysOption apiKeyOption = sysOptionService.getByOptionKey("search_engine_brave_api_key");
                    String apiKey = apiKeyOption != null ? apiKeyOption.getValue() : null;
                    if (!StringUtils.hasText(apiKey)) {
                        log.warn("Brave 搜索引擎 API Key 未配置");
                        return;
                    }
                    webSearchTool = WebSearchTool.builder()
                            .provider(new BraveSearchProvider(apiKey))
                            .build();
                }
                default -> {
                    log.warn("不支持的搜索引擎提供商: {}", searchEngineProvider);
                    return;
                }
            }

            if (webSearchTool != null) {
                memoryPrompt.addTools(ToolScanner.scan(webSearchTool));
                log.debug("搜索引擎工具已添加: {}", searchEngineProvider);
            }
        } catch (Exception e) {
            log.error("添加搜索引擎工具失败", e);
        }
    }
}
