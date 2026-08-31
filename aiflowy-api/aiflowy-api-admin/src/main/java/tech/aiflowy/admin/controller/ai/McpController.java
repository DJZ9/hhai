package tech.aiflowy.admin.controller.ai;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.aiflowy.ai.dto.GetMcpToolsRequestDTO;
import tech.aiflowy.ai.entity.BotMcp;
import tech.aiflowy.ai.entity.Mcp;
import tech.aiflowy.ai.service.BotMcpService;
import tech.aiflowy.ai.service.McpService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.common.web.dto.RemoveRequestDTO;

import jakarta.annotation.Resource;
import java.io.Serializable;

/**
 *  控制层。
 *
 * @author wangGangQiang
 * @since 2026-01-04
 */
@RestController
@RequestMapping("/api/v1/mcp")
public class McpController extends BaseCurdController<McpService, Mcp> {
    public McpController(McpService service) {
        super(service);
    }

    @Resource
    private BotMcpService botMcpService;
    @Override
    public Result<?> save(Mcp entity) {
        return service.saveMcp(entity);
    }

    @Override
    public Result<?> update(Mcp entity) {
        return service.updateMcp(entity);
    }

    @Override
    @Transactional
    public Result<?> remove(Serializable id) {
        service.removeMcp(id);
        botMcpService.remove(QueryWrapper.create().eq(BotMcp::getMcpId, id));
        return Result.ok();
    }

    @Override
    public Result<Page<Mcp>> page(HttpServletRequest request, String sortKey, String sortType, Long pageNumber, Long pageSize) {
        Result<Page<Mcp>> page = super.page(request, sortKey, sortType, pageNumber, pageSize);
        return service.pageMcp(page);
    }

    /**
     * 获取MCP工具
     *
     * @param request 请求参数
     * @return MCP信息
     */
    @PostMapping("/getMcpTools")
    public Result<Mcp> getMcpTools(@RequestBody GetMcpToolsRequestDTO request) {

        return Result.ok(service.getMcpTools(request.getId()));
    }


    @GetMapping("pageTools")
    public Result<Page<Mcp>> pageTools(HttpServletRequest request, String sortKey, String sortType, Long pageNumber, Long pageSize) {
        if (pageNumber == null || pageNumber < 1) {
            pageNumber = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }

        QueryWrapper queryWrapper = buildQueryWrapper(request);
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        Page<Mcp> mcpPage = queryPage(new Page<>(pageNumber, pageSize), queryWrapper);

        return Result.ok(service.pageTools(mcpPage));
    }

    @GetMapping("/getTinyFlowData")
    public Result<?> getTinyFlowData(java.math.BigInteger mcpId, String toolName) {
        com.alibaba.fastjson2.JSONObject nodeData = service.getMcpToolData(mcpId, toolName);
        com.alibaba.fastjson2.JSONArray parameters = nodeData.getJSONArray("parameters");
        if (parameters != null) {
            handleArray(parameters);
        }
        return Result.ok(nodeData);
    }

    private void handleArray(com.alibaba.fastjson2.JSONArray array) {
        for (Object o : array) {
            com.alibaba.fastjson2.JSONObject obj = (com.alibaba.fastjson2.JSONObject) o;
            obj.put("id", cn.hutool.core.util.IdUtil.simpleUUID());
            obj.put("nameDisabled", true);
            obj.put("dataTypeDisabled", true);
            obj.put("deleteDisabled", true);
            obj.put("addChildDisabled", true);
            com.alibaba.fastjson2.JSONArray children = obj.getJSONArray("children");
            if (children != null) {
                handleArray(children);
            }
        }
    }
}