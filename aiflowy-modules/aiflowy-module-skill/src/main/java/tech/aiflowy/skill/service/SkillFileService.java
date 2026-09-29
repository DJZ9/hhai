package tech.aiflowy.skill.service;

import java.util.List;
import tech.aiflowy.skill.dto.FileTreeNode;
import tech.aiflowy.skill.dto.SkillFileContent;
import tech.aiflowy.skill.entity.Skill;

/**
 * Skill 文件管理服务：文件树、预览/编辑、新增、删除。
 *
 * @author wgq
 * @since 2026-09-28
 */
public interface SkillFileService {

    /**
     * 获取 Skill 文件树。
     *
     * @param skillId Skill ID
     * @return 文件树节点列表
     */
    List<FileTreeNode> getTree(Long skillId);

    /**
     * 读取文件内容。
     *
     * @param skillId Skill ID
     * @param path    文件相对路径
     * @return 文件内容
     */
    SkillFileContent readFile(Long skillId, String path);

    /**
     * 写入文件内容（不存在则新增，父目录自动创建）。
     *
     * @param skillId Skill ID
     * @param path    文件相对路径
     * @param content 文件文本内容
     * @return 更新后的 Skill
     */
    Skill writeFile(Long skillId, String path, String content);

    /**
     * 删除文件或目录（禁止删除根目录与 SKILL.md）。
     *
     * @param skillId Skill ID
     * @param path    相对路径
     * @return 更新后的 Skill
     */
    Skill deleteFile(Long skillId, String path);
}