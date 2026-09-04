package tech.aiflowy.admin.controller.ai;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.ai.entity.BotToolCallRecord;
import tech.aiflowy.ai.service.BotToolCallRecordService;
import tech.aiflowy.common.annotation.UsePermission;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.controller.BaseCurdController;

/**
 * 控制层。
 *
 * @author wgq
 * @since 2025-06-23
 */
@RestController
@UsePermission(moduleName = "/api/v1/bot")
@RequestMapping("/api/v1/bot/toolCallRecord")
public class BotToolCallRecordController extends BaseCurdController<BotToolCallRecordService, BotToolCallRecord> {

    public BotToolCallRecordController(BotToolCallRecordService service) {
        super(service);
    }

    @Override
    public Result<Page<BotToolCallRecord>> page(HttpServletRequest request, String sortKey, String sortType, Long pageNumber, Long pageSize) {
        if (pageNumber == null || pageNumber < 1) {
            pageNumber = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }

        QueryWrapper queryWrapper = buildQueryWrapper(request);
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, "created desc"));
        Page<BotToolCallRecord> page = service.getMapper().paginateWithRelations(pageNumber, pageSize, queryWrapper);
        return Result.ok(page);
    }
}
