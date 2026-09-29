package tech.aiflowy.skill.storage.impl;

import com.agentsflex.skill.artifact.PathSkillPackage;
import com.agentsflex.skill.artifact.SkillArtifact;
import com.agentsflex.skill.artifact.SkillArtifactStore;
import com.agentsflex.skill.artifact.SkillInstallRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.skill.config.SkillArtifactConfig;
import tech.aiflowy.skill.entity.Skill;
import tech.aiflowy.skill.storage.AbstractSkillFileStorage;

/**
 * 对象存储实现：materialize() 拉到缓存目录，persist() 重新打包上传并回填 digest。
 *
 * @author wgq
 * @since 2026-09-28
 */
@Component
public class ObjectStorageSkillFileStorage extends AbstractSkillFileStorage {

    private final SkillArtifactConfig skillArtifactConfig;

    public ObjectStorageSkillFileStorage(SkillArtifactStore skillArtifactStore,
                                         SkillArtifactConfig skillArtifactConfig) {
        super(skillArtifactStore);
        this.skillArtifactConfig = skillArtifactConfig;
    }

    @Override
    public void persist(Path workingDir, Skill skill) throws Exception {
        String oldStorageKey = skill.getStorageKey();
        if (oldStorageKey == null || oldStorageKey.isEmpty()) {
            throw new BusinessException("Skill storageKey 为空，无法重新上传对象存储");
        }
        if (skill.getVersion() == null || skill.getVersion().isEmpty()) {
            skill.setVersion("1.0.0");
        }

        // 确保对象存储本地缓存目录存在（下载/锁/对象子目录），与上传路径 uploadTempDirToOss 保持一致
        ensureCacheDirectories();

        // 对象存储 install 对已存在同名 storageKey 的直接覆盖不被接受，
        // 因此每次编辑后安装到一个全新的 storageKey，成功后再删除旧对象并更新 Skill 的 storageKey。
        String newStorageKey = buildNewStorageKey(oldStorageKey);

        Path tempZipFile = Files.createTempFile("skill-edit-", ".zip");
        try {
            zipDirectory(workingDir, tempZipFile);

            SkillArtifact artifact = new SkillArtifact(skill.getName(), skill.getVersion(), null, newStorageKey);
            SkillArtifact installed = skillArtifactStore.install(new SkillInstallRequest(artifact, new PathSkillPackage(tempZipFile)));

            // 安装新对象成功后再删除旧对象，保证即便后续失败也不丢失远端数据
            if (!newStorageKey.equals(oldStorageKey)) {
                SkillArtifact old = new SkillArtifact(skill.getName(), skill.getVersion(), null, oldStorageKey);
                try {
                    skillArtifactStore.delete(old);
                } catch (Exception e) {
                    log.warn("删除旧 Skill 对象失败: {}", e.getMessage());
                }
            }

            skill.setStorageKey(newStorageKey);
            skill.setDigest(installed.getDigest());
            log.info("Skill 重新上传成功: name={}, storageKey={}, digest={}", skill.getName(), newStorageKey, installed.getDigest());
        } finally {
            try {
                Files.deleteIfExists(tempZipFile);
            } catch (IOException e) {
                log.warn("删除临时 ZIP 失败: {}", tempZipFile, e);
            }
        }
    }

    /**
     * 基于原有 storageKey 追加唯一后缀，生成全新的 storageKey，确保每次编辑 install 到全新对象。
     * 不依赖登录上下文，避免在非请求线程中获取登录态失败（SaTokenUtil.getLoginAccount() 可能为 null）。
     */
    private String buildNewStorageKey(String oldStorageKey) {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String base = oldStorageKey.endsWith("/") ? oldStorageKey : oldStorageKey + "/";
        return base + uniqueSuffix + "/";
    }

    /**
     * 确保对象存储本地缓存目录存在（downloads/locks/objects 子目录）。
     * 与 SkillServiceImpl#ensureCacheDirectories 保持一致的准备逻辑，避免 install 因缓存路径不可用而失败。
     */
    private void ensureCacheDirectories() throws IOException {
        String cacheDirPath = null;
        switch (skillArtifactConfig.getType().toLowerCase()) {
            case "aliyun-oss":
                cacheDirPath = skillArtifactConfig.getAliyunOss().getCacheDirectory();
                break;
            case "tencent-cos":
                cacheDirPath = skillArtifactConfig.getTencentCos().getCacheDirectory();
                break;
            case "huawei-obs":
                cacheDirPath = skillArtifactConfig.getHuaweiObs().getCacheDirectory();
                break;
            case "volcengine-tos":
                cacheDirPath = skillArtifactConfig.getVolcengineTos().getCacheDirectory();
                break;
            case "aws-s3":
                cacheDirPath = skillArtifactConfig.getAwsS3().getCacheDirectory();
                break;
            default:
                return;
        }

        if (cacheDirPath == null || cacheDirPath.isEmpty()) {
            return;
        }
        Path cacheDir = Paths.get(cacheDirPath).toAbsolutePath().normalize();
        Path downloadsDir = cacheDir.resolve("downloads");
        Path locksDir = cacheDir.resolve("locks");
        Path objectsDir = cacheDir.resolve("objects");

        if (!Files.exists(downloadsDir)) {
            Files.createDirectories(downloadsDir);
            log.info("创建对象存储 downloads 目录: {}", downloadsDir);
        }
        if (!Files.exists(locksDir)) {
            Files.createDirectories(locksDir);
            log.info("创建对象存储 locks 目录: {}", locksDir);
        }
        if (!Files.exists(objectsDir)) {
            Files.createDirectories(objectsDir);
            log.info("创建对象存储 objects 目录: {}", objectsDir);
        }
    }

    private void zipDirectory(Path sourceDir, Path targetZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(targetZip))) {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        // 统一使用正斜杠作为 ZIP 条目路径（ZIP 规范要求）
                        String entryPath = sourceDir.relativize(path).toString().replace('\\', '/');
                        try {
                            zos.putNextEntry(new ZipEntry(entryPath));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("打包失败: " + path, e);
                        }
                    });
        }
    }
}