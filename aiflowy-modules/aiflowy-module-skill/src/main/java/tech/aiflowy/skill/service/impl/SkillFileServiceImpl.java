package tech.aiflowy.skill.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.skill.dto.FileTreeNode;
import tech.aiflowy.skill.dto.SkillFileContent;
import tech.aiflowy.skill.entity.Skill;
import tech.aiflowy.skill.service.SkillFileService;
import tech.aiflowy.skill.service.SkillService;
import tech.aiflowy.skill.storage.SkillFileStorage;
import tech.aiflowy.skill.storage.SkillFileStorageProvider;
import tech.aiflowy.skill.support.SkillFileAccessor;
import tech.aiflowy.skill.support.SkillFileTreeBuilder;

/**
 * Skill 文件管理服务实现。
 *
 * @author wgq
 * @since 2026-09-28
 */
@Service
public class SkillFileServiceImpl implements SkillFileService {

    private static final Logger log = LoggerFactory.getLogger(SkillFileServiceImpl.class);

    private final SkillService skillService;
    private final SkillFileStorageProvider storageProvider;
    private final SkillFileAccessor accessor;
    private final SkillFileTreeBuilder treeBuilder;

    public SkillFileServiceImpl(SkillService skillService,
                                SkillFileStorageProvider storageProvider,
                                SkillFileAccessor accessor,
                                SkillFileTreeBuilder treeBuilder) {
        this.skillService = skillService;
        this.storageProvider = storageProvider;
        this.accessor = accessor;
        this.treeBuilder = treeBuilder;
    }

    @Override
    public List<FileTreeNode> getTree(Long skillId) {
        Skill skill = requireSkill(skillId);
        try {
            Path dir = storage().materialize(skill);
            if (dir == null || !Files.isDirectory(dir)) {
                throw new BusinessException("Skill 文件目录不存在");
            }
            return treeBuilder.build(dir.toFile());
        } catch (Exception e) {
            throw wrap(e);
        }
    }

    @Override
    public SkillFileContent readFile(Long skillId, String path) {
        Skill skill = requireSkill(skillId);
        try {
            Path dir = storage().materialize(skill);
            Path target = accessor.resolve(dir, path);
            if (!Files.isRegularFile(target)) {
                throw new BusinessException("文件不存在或不是文件: " + path);
            }
            byte[] bytes = accessor.readBytes(target);
            boolean isBinary = accessor.isBinary(bytes);

            SkillFileContent content = new SkillFileContent();
            content.setPath(normalizePath(path));
            content.setSize(target.toFile().length());
            content.setIsBinary(isBinary);
            content.setContent(isBinary ? null : accessor.decodeText(bytes));
            return content;
        } catch (Exception e) {
            throw wrap(e);
        }
    }

    @Override
    public Skill writeFile(Long skillId, String path, String content) {
        Skill skill = requireSkill(skillId);
        try {
            Path dir = storage().materialize(skill);
            Path target = accessor.resolve(dir, path);
            accessor.writeText(target, content);
            storage().persist(dir, skill);
            skillService.updateById(skill);
            return skill;
        } catch (Exception e) {
            throw wrap(e);
        }
    }

    @Override
    public Skill deleteFile(Long skillId, String path) {
        Skill skill = requireSkill(skillId);
        try {
            accessor.ensureDeletable(path);
            Path dir = storage().materialize(skill);
            Path target = accessor.resolve(dir, path);
            if (!Files.exists(target)) {
                throw new BusinessException("文件不存在: " + path);
            }
            accessor.deleteNode(target);
            storage().persist(dir, skill);
            skillService.updateById(skill);
            return skill;
        } catch (Exception e) {
            throw wrap(e);
        }
    }

    private Skill requireSkill(Long skillId) {
        Skill skill = skillService.getById(skillId);
        if (skill == null) {
            throw new BusinessException("Skill 不存在: " + skillId);
        }
        return skill;
    }

    private SkillFileStorage storage() {
        return storageProvider.resolve();
    }

    private String normalizePath(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }

    private RuntimeException wrap(Exception e) {
        log.error("Skill 文件操作失败", e);
        if (e instanceof BusinessException) {
            return (BusinessException) e;
        }
        String detail = e.getMessage() != null && !e.getMessage().isEmpty()
                ? e.getMessage()
                : e.getClass().getSimpleName();
        if (e instanceof IOException) {
            return new BusinessException("文件操作失败: " + detail);
        }
        return new BusinessException("Skill 文件操作失败: " + detail);
    }
}