package tech.aiflowy.skill.storage;

import com.agentsflex.skill.artifact.SkillArtifact;
import com.agentsflex.skill.artifact.SkillArtifactStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.skill.entity.Skill;

/**
 * 抽象 Skill 文件存储实现，提供共用的物化逻辑（本地与对象存储一致）。
 *
 * @author wgq
 * @since 2026-09-28
 */
public abstract class AbstractSkillFileStorage implements SkillFileStorage {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final SkillArtifactStore skillArtifactStore;

    protected AbstractSkillFileStorage(SkillArtifactStore skillArtifactStore) {
        this.skillArtifactStore = skillArtifactStore;
    }

    @Override
    public Path materialize(Skill skill) throws Exception {
        String storageKey = skill.getStorageKey();
        if (storageKey != null && !storageKey.isEmpty()) {
            SkillArtifact artifact = new SkillArtifact(
                    skill.getName(),
                    skill.getVersion() != null ? skill.getVersion() : "1.0.0",
                    skill.getDigest(),
                    storageKey
            );
            return skillArtifactStore.materialize(artifact);
        }

        if (skill.getSkillPath() != null && !skill.getSkillPath().isEmpty()) {
            return Paths.get(skill.getSkillPath());
        }

        throw new BusinessException("无法获取 Skill 文件路径");
    }
}