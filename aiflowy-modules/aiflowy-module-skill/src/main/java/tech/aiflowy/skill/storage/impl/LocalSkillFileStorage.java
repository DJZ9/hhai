package tech.aiflowy.skill.storage.impl;

import com.agentsflex.skill.artifact.SkillArtifactStore;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import tech.aiflowy.skill.entity.Skill;
import tech.aiflowy.skill.storage.AbstractSkillFileStorage;

/**
 * 本地存储实现：materialize() 返回真实安装目录，文件操作直接落盘，无需额外持久化。
 *
 * @author wgq
 * @since 2026-09-28
 */
@Component
public class LocalSkillFileStorage extends AbstractSkillFileStorage {

    public LocalSkillFileStorage(SkillArtifactStore skillArtifactStore) {
        super(skillArtifactStore);
    }

    @Override
    public void persist(Path workingDir, Skill skill) {
        // 本地存储已直接写盘，无需额外持久化
    }
}