package tech.aiflowy.skill.storage;

import org.springframework.stereotype.Component;
import tech.aiflowy.skill.config.SkillArtifactConfig;
import tech.aiflowy.skill.storage.impl.LocalSkillFileStorage;
import tech.aiflowy.skill.storage.impl.ObjectStorageSkillFileStorage;

/**
 * Skill 文件存储选择器：根据 {@link SkillArtifactConfig#getType()} 返回对应的存储适配器。
 *
 * @author wgq
 * @since 2026-09-28
 */
@Component
public class SkillFileStorageProvider {

    private final LocalSkillFileStorage localStorage;
    private final ObjectStorageSkillFileStorage remoteStorage;
    private final SkillArtifactConfig skillArtifactConfig;

    public SkillFileStorageProvider(LocalSkillFileStorage localStorage,
                                    ObjectStorageSkillFileStorage remoteStorage,
                                    SkillArtifactConfig skillArtifactConfig) {
        this.localStorage = localStorage;
        this.remoteStorage = remoteStorage;
        this.skillArtifactConfig = skillArtifactConfig;
    }

    /**
     * 依据配置的存储类型解析对应的存储适配器。
     *
     * @return 存储适配器
     */
    public SkillFileStorage resolve() {
        return "local".equalsIgnoreCase(skillArtifactConfig.getType()) ? localStorage : remoteStorage;
    }
}