package tech.aiflowy.skill.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.skill.entity.SkillCategory;
import tech.aiflowy.skill.mapper.SkillCategoryMapper;
import tech.aiflowy.skill.service.SkillCategoryService;

/**
 * 技能分类 服务层实现。
 *
 * @author wgq
 * @since 2026-07-21
 */
@Service
public class SkillCategoryServiceImpl extends ServiceImpl<SkillCategoryMapper, SkillCategory> implements SkillCategoryService {

}