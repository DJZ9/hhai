package tech.aiflowy.usercenter.controller.ai;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.ai.entity.BotConversation;
import tech.aiflowy.ai.service.BotConversationService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.common.web.exceptions.BusinessException;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/userCenter/botConversation")
@SaIgnore
public class UcBotConversationController extends BaseCurdController<BotConversationService, BotConversation> {

    @Resource
    private BotConversationService conversationMessageService;

    public UcBotConversationController(BotConversationService service) {
        super(service);
    }

    /**
     * 删除指定会话
     */
    @GetMapping("/deleteConversation")
    public Result<Void> deleteConversation(String botId, String conversationId) {
        LoginAccount account = SaTokenUtil.getLoginAccount();
        conversationMessageService.deleteConversation(botId, conversationId, account.getId());
        return Result.ok();
    }

    /**
     * 更新会话标题
     */
    @GetMapping("/updateConversation")
    public Result<Void> updateConversation(String botId, String conversationId, String title) {
        LoginAccount account = SaTokenUtil.getLoginAccount();
        conversationMessageService.updateConversation(botId, conversationId, title, account.getId());
        return Result.ok();
    }

    @Override
    public Result<List<BotConversation>> list(BotConversation entity, Boolean asTree, String sortKey, String sortType) {
        entity.setAccountId(SaTokenUtil.getLoginAccount().getId());
        sortKey = "created";
        sortType = "desc";
        return super.list(entity, asTree, sortKey, sortType);
    }

    @Override
    protected Result<?> onSaveOrUpdateBefore(BotConversation entity, boolean isSave) {
        entity.setAccountId(SaTokenUtil.getLoginAccount().getId());
        entity.setCreated(new Date());
        return super.onSaveOrUpdateBefore(entity, isSave);
    }

    /**
     * 分页查询会话列表
     *
     * @param request    查询数据
     * @param sortKey    排序字段
     * @param sortType   排序方式 asc | desc
     * @param pageNumber 当前页码
     * @param pageSize   每页的数据量
     * @return
     */
    @GetMapping("pageList")
    public Result<Page<BotConversation>> page(HttpServletRequest request, String sortKey, String sortType, Long pageNumber, Long pageSize) {
        if (pageNumber == null || pageNumber < 1) {
            pageNumber = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }

        QueryWrapper queryWrapper = buildQueryWrapper(request);
        queryWrapper.eq(BotConversation::getAccountId, SaTokenUtil.getLoginAccount().getId());
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        Page<BotConversation> botConversationPage = service.getMapper().paginateWithRelations(pageNumber, pageSize, queryWrapper);
        return Result.ok(botConversationPage);
    }

    /**
     * 根据表主键查询数据详情。
     *
     * @param id 主键值
     * @return 内容详情
     */
    @GetMapping("detail")
    @SaIgnore
    public Result<BotConversation> detail(String id) {
        if (tech.aiflowy.common.util.StringUtil.noText(id)) {
            throw new BusinessException("id must not be null");
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(BotConversation::getId, id)
                .eq(BotConversation::getAccountId, SaTokenUtil.getLoginAccount().getId());
        if (service.getOne(queryWrapper) == null) {
            return Result.ok(null);
        }
        return Result.ok(service.getMapper().selectOneWithRelationsById(id));
    }
}
