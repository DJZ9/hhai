package tech.aiflowy.ai.service;

import com.agentsflex.core.model.chat.tool.Tool;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import tech.aiflowy.ai.entity.BotMcp;
import tech.aiflowy.ai.entity.Mcp;
import tech.aiflowy.common.domain.Result;

import java.io.Serializable;

/**
 *  服务层。
 *
 * @author wangGangQiang
 * @since 2026-01-04
 */
public interface McpService extends IService<Mcp> {

    Result<?> saveMcp(Mcp entity);

    Result<?> updateMcp(Mcp entity);

    void removeMcp(Serializable id);

    Tool toFunction(BotMcp botMcp);

    Result<Page<Mcp>> pageMcp(Result<Page<Mcp>> page);

    Mcp getMcpTools(String id);

    Page<Mcp> pageTools(Page<Mcp> mcpPage);

    com.alibaba.fastjson2.JSONObject getMcpToolData(java.math.BigInteger mcpId, String toolName);
}
