package tech.aiflowy.admin.controller.ai;

import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.*;
import tech.aiflowy.common.annotation.UsePermission;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.tree.Tree;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.skill.dto.BotSkillUpdateRequest;
import tech.aiflowy.skill.entity.BotSkill;
import tech.aiflowy.skill.service.BotSkillService;

import java.util.List;

/**
 * 机器人技能关联 控制器。
 *
 * @author wgq
 * @since 2026-07-21
 */
@RestController
@RequestMapping("/api/v1/botSkill")
@UsePermission(moduleName = "/api/v1/bot")
public class BotSkillController extends BaseCurdController<BotSkillService, BotSkill> {


    public BotSkillController(BotSkillService service) {
        super(service);
    }

    /**
     * 获取 Bot 关联的 Skills（含关联 Skill 实体）
     *
     * @param entity   查询条件
     * @param asTree   是否返回树形结构
     * @param sortKey  排序字段
     * @param sortType 排序类型
     * @return 关联的 BotSkill 列表
     */
    @GetMapping("list")
    @Override
    public Result<List<BotSkill>> list(BotSkill entity, Boolean asTree, String sortKey, String sortType) {
        QueryWrapper queryWrapper = QueryWrapper.create(entity, buildOperators(entity));
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        List<BotSkill> botSkills = service.getMapper().selectListWithRelationsByQuery(queryWrapper);
        List<BotSkill> list = Tree.tryToTree(botSkills, asTree);
        return Result.ok(list);
    }

    /**
     * 更新 Bot 关联的 Skill IDs
     *
     * @param request 更新请求
     * @return 操作结果
     */
    @PostMapping("updateBotSkillIds")
    public Result<?> updateBotSkillIds(@RequestBody BotSkillUpdateRequest request) {
        if (request == null || request.getBotId() == null) {
            return Result.fail("请求参数不能为空");
        }
        service.updateBotSkills(request.getBotId(), request.getSkillIds());
        return Result.ok();
    }
}