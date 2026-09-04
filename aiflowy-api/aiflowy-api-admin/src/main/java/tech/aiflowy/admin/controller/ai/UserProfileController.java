package tech.aiflowy.admin.controller.ai;

import cn.dev33.satoken.annotation.SaIgnore;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import tech.aiflowy.ai.entity.BotConversation;
import tech.aiflowy.ai.service.BotConversationService;
import tech.aiflowy.ai.service.userProfileService.UserProfileService;
import tech.aiflowy.common.annotation.UsePermission;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.web.controller.BaseCurdController;

import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bot/userProfile")
@UsePermission(moduleName = "/api/v1/bot")
public class UserProfileController extends BaseCurdController<BotConversationService, BotConversation> {

    public UserProfileController(BotConversationService service) {
        super(service);
    }

    @Resource
    @Qualifier("UserProfileService")
    private UserProfileService userProfileService;

    @Override
    public Result<Page<BotConversation>> page(HttpServletRequest request, String sortKey, String sortType, Long pageNumber, Long pageSize) {
        if (pageNumber == null || pageNumber < 1) {
            pageNumber = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }

        QueryWrapper queryWrapper = buildQueryWrapper(request);
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        Page<BotConversation> page = service.getMapper().paginateWithRelations(pageNumber, pageSize, queryWrapper);
        return Result.ok(page);
    }

    /**
     * 触发会话分析
     */
    @PostMapping("{conversationId}/analyze")
    @SaIgnore
    public Result<?> analyzeConversation(@PathVariable("conversationId") BigInteger conversationId) {
        BotConversation conversation = service.getById(conversationId);
        if (conversation == null) {
            return Result.fail("会话不存在");
        }

        LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
        userProfileService.analyzeConversation(conversationId, conversation.getBotId(), loginAccount.getId());
        return Result.ok();
    }

    /**
     * 获取会话分析结果
     */
    @GetMapping("{conversationId}/analysis")
    @SaIgnore
    public Result<?> getAnalysisResult(@PathVariable("conversationId") BigInteger conversationId) {
        BotConversation conversation = service.getById(conversationId);
        if (conversation == null) {
            return Result.fail("会话不存在");
        }

        Map<String, Object> analysisResult = userProfileService.getAnalysisResult(conversationId);
        return Result.ok(analysisResult);
    }

    /**
     * 分析指定用户的所有会话画像
     */
    @PostMapping("analyzeUser/{accountId}")
    @SaIgnore
    public Result<?> analyzeUserConversations(@PathVariable("accountId") BigInteger accountId) {
        userProfileService.analyzeUserConversations(accountId);
        return Result.ok();
    }

    /**
     * 分析当天的所有会话画像
     */
    @PostMapping("analyzeToday")
    @SaIgnore
    public Result<?> analyzeTodayConversations() {
        userProfileService.analyzeTodayConversations();
        return Result.ok();
    }
}
