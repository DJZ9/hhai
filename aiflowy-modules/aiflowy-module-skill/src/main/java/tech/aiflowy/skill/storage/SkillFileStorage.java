package tech.aiflowy.skill.storage;

import java.nio.file.Path;
import tech.aiflowy.skill.entity.Skill;

/**
 * Skill 文件存储适配接口，隔离本地与对象存储的文件物化/持久化差异。
 *
 * @author wgq
 * @since 2026-09-28
 */
public interface SkillFileStorage {

    /**
     * 将 Skill 的存储文件物化为本地可访问目录。
     * 本地存储返回真实安装目录；对象存储下载解压到缓存目录。
     *
     * @param skill Skill 实体
     * @return 本地可写目录
     * @throws Exception 物化异常
     */
    Path materialize(Skill skill) throws Exception;

    /**
     * 将工作目录的修改持久化到最终存储。
     * 本地存储为空操作；对象存储重新打包上传并回填 digest。
     *
     * @param workingDir 修改后的工作目录
     * @param skill       Skill 实体（可能被回填 digest）
     * @throws Exception 持久化异常
     */
    void persist(Path workingDir, Skill skill) throws Exception;
}