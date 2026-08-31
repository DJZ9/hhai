package tech.aiflowy.admin.controller.ai;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.ai.entity.BotMcp;
import tech.aiflowy.ai.entity.Mcp;
import tech.aiflowy.ai.service.BotMcpService;
import tech.aiflowy.ai.service.McpService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.common.web.jsonbody.JsonBody;

import javax.annotation.Resource;
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

    @PostMapping("/getMcpTools")
    public Result<Mcp> getMcpTools(@JsonBody("id") String id) {

        return Result.ok(service.getMcpTools(id));
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
}