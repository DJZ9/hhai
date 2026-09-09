package tech.aiflowy.admin.controller.skill;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.common.annotation.UsePermission;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.skill.entity.SkillCategory;
import tech.aiflowy.skill.service.SkillCategoryService;

/**
 * Skill分类 控制器。
 *
 * @author wgq
 * @since 2026-07-21
 */
@RestController
@RequestMapping("/api/v1/skillCategory")
@UsePermission(moduleName = "/api/v1/skill")
public class SkillCategoryController extends BaseCurdController<SkillCategoryService, SkillCategory> {

    public SkillCategoryController(SkillCategoryService service) {
        super(service);
    }
}