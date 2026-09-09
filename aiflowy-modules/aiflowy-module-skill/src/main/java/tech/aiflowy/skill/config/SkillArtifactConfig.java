package tech.aiflowy.skill.config;

import com.agentsflex.skill.artifact.FileSystemSkillArtifactStore;
import com.agentsflex.skill.artifact.SkillArtifactStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Skill Artifact 存储配置
 * 支持多种对象存储后端：
 * - local: 本地文件系统
 * - aliyun-oss: 阿里云 OSS
 * - tencent-cos: 腾讯云 COS
 * - huawei-obs: 华为云 OBS
 * - volcengine-tos: 火山引擎 TOS
 * - aws-s3: AWS S3
 *
 * @author michael
 * @since 2026-07-21
 */
@Configuration
@ConfigurationProperties(prefix = "aiflowy.skill.artifact")
public class SkillArtifactConfig {

    private static final Logger log = LoggerFactory.getLogger(SkillArtifactConfig.class);

    /**
     * 存储类型: local / aliyun-oss / tencent-cos / huawei-obs / volcengine-tos / aws-s3
     */
    private String type = "local";

    /**
     * 本地存储配置
     */
    private LocalConfig local = new LocalConfig();

    /**
     * 阿里云 OSS 配置
     */
    private AliyunOssConfig aliyunOss = new AliyunOssConfig();

    /**
     * 腾讯云 COS 配置
     */
    private TencentCosConfig tencentCos = new TencentCosConfig();

    /**
     * 华为云 OBS 配置
     */
    private HuaweiObsConfig huaweiObs = new HuaweiObsConfig();

    /**
     * 火山引擎 TOS 配置
     */
    private VolcengineTosConfig volcengineTos = new VolcengineTosConfig();

    /**
     * AWS S3 配置
     */
    private AwsS3Config awsS3 = new AwsS3Config();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalConfig getLocal() {
        return local;
    }

    public void setLocal(LocalConfig local) {
        this.local = local;
    }

    public AliyunOssConfig getAliyunOss() {
        return aliyunOss;
    }

    public void setAliyunOss(AliyunOssConfig aliyunOss) {
        this.aliyunOss = aliyunOss;
    }

    public TencentCosConfig getTencentCos() {
        return tencentCos;
    }

    public void setTencentCos(TencentCosConfig tencentCos) {
        this.tencentCos = tencentCos;
    }

    public HuaweiObsConfig getHuaweiObs() {
        return huaweiObs;
    }

    public void setHuaweiObs(HuaweiObsConfig huaweiObs) {
        this.huaweiObs = huaweiObs;
    }

    public VolcengineTosConfig getVolcengineTos() {
        return volcengineTos;
    }

    public void setVolcengineTos(VolcengineTosConfig volcengineTos) {
        this.volcengineTos = volcengineTos;
    }

    public AwsS3Config getAwsS3() {
        return awsS3;
    }

    public void setAwsS3(AwsS3Config awsS3) {
        this.awsS3 = awsS3;
    }

    /**
     * 创建 SkillArtifactStore Bean
     * 根据配置类型创建对应的实现
     */
    @Bean
    public SkillArtifactStore skillArtifactStore() {
        switch (type.toLowerCase()) {
            case "local":
                return createLocalStore();
            case "aliyun-oss":
                return createAliyunOssStore();
            case "tencent-cos":
                return createTencentCosStore();
            case "huawei-obs":
                return createHuaweiObsStore();
            case "volcengine-tos":
                return createVolcengineTosStore();
            case "aws-s3":
                return createAwsS3Store();
            default:
                throw new IllegalArgumentException("不支持的 Skill Artifact 存储类型: " + type);
        }
    }

    /**
     * 创建本地文件系统存储
     */
    private SkillArtifactStore createLocalStore() {
        Path skillRootPath = Paths.get(local.getRoot());

        // 自动创建目录（如果不存在）
        if (!Files.exists(skillRootPath)) {
            try {
                Files.createDirectories(skillRootPath);
                log.info("Skill 存储根目录已创建: {}", skillRootPath.toAbsolutePath());
            } catch (Exception e) {
                throw new RuntimeException("创建 Skill 存储根目录失败: " + skillRootPath.toAbsolutePath(), e);
            }
        }

        return new FileSystemSkillArtifactStore(skillRootPath);
    }

    /**
     * 创建阿里云 OSS 存储
     */
    private SkillArtifactStore createAliyunOssStore() {
        try {
            // 创建缓存目录及其所有子目录（如果不存在）
            Path cacheDir = Paths.get(aliyunOss.getCacheDirectory()).toAbsolutePath().normalize();
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
                log.info("OSS 缓存目录已创建: {}", cacheDir);
            }
            
            // 确保 downloads、locks、objects 子目录存在
            Path downloadsDir = cacheDir.resolve("downloads");
            Path locksDir = cacheDir.resolve("locks");
            Path objectsDir = cacheDir.resolve("objects");
            
            if (!Files.exists(downloadsDir)) {
                Files.createDirectories(downloadsDir);
                log.info("OSS downloads 目录已创建: {}", downloadsDir);
            }
            if (!Files.exists(locksDir)) {
                Files.createDirectories(locksDir);
                log.info("OSS locks 目录已创建: {}", locksDir);
            }
            if (!Files.exists(objectsDir)) {
                Files.createDirectories(objectsDir);
                log.info("OSS objects 目录已创建: {}", objectsDir);
            }

            // 导入阿里云 OSS 实现类
            Class<?> storeClass = Class.forName("com.agentsflex.skill.artifact.aliyun.oss.AliyunOssSkillArtifactStore");
            Object builder = storeClass.getMethod("builder").invoke(null);

            // 设置必需参数
            builder.getClass().getMethod("region", String.class).invoke(builder, aliyunOss.getRegion());
            builder.getClass().getMethod("bucket", String.class).invoke(builder, aliyunOss.getBucket());
            builder.getClass().getMethod("keyPrefix", String.class).invoke(builder, aliyunOss.getKeyPrefix());
            builder.getClass().getMethod("cacheDirectory", Path.class).invoke(builder, cacheDir);

            // 设置可选参数
            if (aliyunOss.getEndpoint() != null && !aliyunOss.getEndpoint().trim().isEmpty()) {
                builder.getClass().getMethod("endpoint", String.class).invoke(builder, aliyunOss.getEndpoint());
            }

            // 设置凭证
            if (aliyunOss.getAccessKeyId() != null && aliyunOss.getAccessKeySecret() != null) {
                if (aliyunOss.getSecurityToken() != null && !aliyunOss.getSecurityToken().trim().isEmpty()) {
                    builder.getClass().getMethod("credentials", String.class, String.class, String.class)
                            .invoke(builder, aliyunOss.getAccessKeyId(), aliyunOss.getAccessKeySecret(), aliyunOss.getSecurityToken());
                } else {
                    builder.getClass().getMethod("credentials", String.class, String.class)
                            .invoke(builder, aliyunOss.getAccessKeyId(), aliyunOss.getAccessKeySecret());
                }
            }

            // 设置最大包大小
            builder.getClass().getMethod("maxPackageSize", long.class).invoke(builder, aliyunOss.getMaxPackageSize());

            log.info("阿里云 OSS SkillArtifactStore 创建成功: bucket={}, keyPrefix={}", 
                aliyunOss.getBucket(), aliyunOss.getKeyPrefix());

            // 构建 SkillArtifactStore
            return (SkillArtifactStore) builder.getClass().getMethod("build").invoke(builder);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("阿里云 OSS SkillArtifactStore 尚未实现，请添加依赖：agents-flex-skills-artifact-aliyun-oss");
        } catch (Exception e) {
            throw new RuntimeException("创建阿里云 OSS SkillArtifactStore 失败", e);
        }
    }

    /**
     * 创建腾讯云 COS 存储
     */
    private SkillArtifactStore createTencentCosStore() {
        try {
            // 导入腾讯云 COS 实现类
            Class<?> storeClass = Class.forName("com.agentsflex.skill.artifact.tencent.cos.TencentCosSkillArtifactStore");
            Object builder = storeClass.getMethod("builder").invoke(null);

            // 设置必需参数
            builder.getClass().getMethod("region", String.class).invoke(builder, tencentCos.getRegion());
            builder.getClass().getMethod("bucket", String.class).invoke(builder, tencentCos.getBucket());
            builder.getClass().getMethod("keyPrefix", String.class).invoke(builder, tencentCos.getKeyPrefix());
            builder.getClass().getMethod("cacheDirectory", Path.class).invoke(builder, Paths.get(tencentCos.getCacheDirectory()));

            // 设置可选参数
            if (tencentCos.getEndpoint() != null && !tencentCos.getEndpoint().trim().isEmpty()) {
                builder.getClass().getMethod("endpoint", String.class).invoke(builder, tencentCos.getEndpoint());
            }

            // 设置凭证
            if (tencentCos.getAccessKeyId() != null && tencentCos.getAccessKeySecret() != null) {
                if (tencentCos.getSecurityToken() != null && !tencentCos.getSecurityToken().trim().isEmpty()) {
                    builder.getClass().getMethod("credentials", String.class, String.class, String.class)
                            .invoke(builder, tencentCos.getAccessKeyId(), tencentCos.getAccessKeySecret(), tencentCos.getSecurityToken());
                } else {
                    builder.getClass().getMethod("credentials", String.class, String.class)
                            .invoke(builder, tencentCos.getAccessKeyId(), tencentCos.getAccessKeySecret());
                }
            }

            // 设置最大包大小
            builder.getClass().getMethod("maxPackageSize", long.class).invoke(builder, tencentCos.getMaxPackageSize());

            // 构建 SkillArtifactStore
            return (SkillArtifactStore) builder.getClass().getMethod("build").invoke(builder);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("腾讯云 COS SkillArtifactStore 尚未实现，请添加依赖：agents-flex-skills-artifact-tencent-cos");
        } catch (Exception e) {
            throw new RuntimeException("创建腾讯云 COS SkillArtifactStore 失败", e);
        }
    }

    /**
     * 创建华为云 OBS 存储
     */
    private SkillArtifactStore createHuaweiObsStore() {
        try {
            // 导入华为云 OBS 实现类
            Class<?> storeClass = Class.forName("com.agentsflex.skill.artifact.huawei.obs.HuaweiObsSkillArtifactStore");
            Object builder = storeClass.getMethod("builder").invoke(null);

            // 设置必需参数
            builder.getClass().getMethod("region", String.class).invoke(builder, huaweiObs.getRegion());
            builder.getClass().getMethod("bucket", String.class).invoke(builder, huaweiObs.getBucket());
            builder.getClass().getMethod("keyPrefix", String.class).invoke(builder, huaweiObs.getKeyPrefix());
            builder.getClass().getMethod("cacheDirectory", Path.class).invoke(builder, Paths.get(huaweiObs.getCacheDirectory()));

            // 设置可选参数
            if (huaweiObs.getEndpoint() != null && !huaweiObs.getEndpoint().trim().isEmpty()) {
                builder.getClass().getMethod("endpoint", String.class).invoke(builder, huaweiObs.getEndpoint());
            }

            // 设置凭证
            if (huaweiObs.getAccessKeyId() != null && huaweiObs.getAccessKeySecret() != null) {
                if (huaweiObs.getSecurityToken() != null && !huaweiObs.getSecurityToken().trim().isEmpty()) {
                    builder.getClass().getMethod("credentials", String.class, String.class, String.class)
                            .invoke(builder, huaweiObs.getAccessKeyId(), huaweiObs.getAccessKeySecret(), huaweiObs.getSecurityToken());
                } else {
                    builder.getClass().getMethod("credentials", String.class, String.class)
                            .invoke(builder, huaweiObs.getAccessKeyId(), huaweiObs.getAccessKeySecret());
                }
            }

            // 设置最大包大小
            builder.getClass().getMethod("maxPackageSize", long.class).invoke(builder, huaweiObs.getMaxPackageSize());

            // 构建 SkillArtifactStore
            return (SkillArtifactStore) builder.getClass().getMethod("build").invoke(builder);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("华为云 OBS SkillArtifactStore 尚未实现，请添加依赖：agents-flex-skills-artifact-huawei-obs");
        } catch (Exception e) {
            throw new RuntimeException("创建华为云 OBS SkillArtifactStore 失败", e);
        }
    }

    /**
     * 创建火山引擎 TOS 存储
     */
    private SkillArtifactStore createVolcengineTosStore() {
        try {
            // 导入火山引擎 TOS 实现类
            Class<?> storeClass = Class.forName("com.agentsflex.skill.artifact.volcengine.tos.VolcengineTosSkillArtifactStore");
            Object builder = storeClass.getMethod("builder").invoke(null);

            // 设置必需参数
            builder.getClass().getMethod("region", String.class).invoke(builder, volcengineTos.getRegion());
            builder.getClass().getMethod("bucket", String.class).invoke(builder, volcengineTos.getBucket());
            builder.getClass().getMethod("keyPrefix", String.class).invoke(builder, volcengineTos.getKeyPrefix());
            builder.getClass().getMethod("cacheDirectory", Path.class).invoke(builder, Paths.get(volcengineTos.getCacheDirectory()));

            // 设置可选参数
            if (volcengineTos.getEndpoint() != null && !volcengineTos.getEndpoint().trim().isEmpty()) {
                builder.getClass().getMethod("endpoint", String.class).invoke(builder, volcengineTos.getEndpoint());
            }

            // 设置凭证
            if (volcengineTos.getAccessKeyId() != null && volcengineTos.getAccessKeySecret() != null) {
                if (volcengineTos.getSecurityToken() != null && !volcengineTos.getSecurityToken().trim().isEmpty()) {
                    builder.getClass().getMethod("credentials", String.class, String.class, String.class)
                            .invoke(builder, volcengineTos.getAccessKeyId(), volcengineTos.getAccessKeySecret(), volcengineTos.getSecurityToken());
                } else {
                    builder.getClass().getMethod("credentials", String.class, String.class)
                            .invoke(builder, volcengineTos.getAccessKeyId(), volcengineTos.getAccessKeySecret());
                }
            }

            // 设置最大包大小
            builder.getClass().getMethod("maxPackageSize", long.class).invoke(builder, volcengineTos.getMaxPackageSize());

            // 构建 SkillArtifactStore
            return (SkillArtifactStore) builder.getClass().getMethod("build").invoke(builder);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("火山引擎 TOS SkillArtifactStore 尚未实现，请添加依赖：agents-flex-skills-artifact-volcengine-tos");
        } catch (Exception e) {
            throw new RuntimeException("创建火山引擎 TOS SkillArtifactStore 失败", e);
        }
    }

    /**
     * 创建 AWS S3 存储
     */
    private SkillArtifactStore createAwsS3Store() {
        try {
            // 导入 AWS S3 实现类
            Class<?> storeClass = Class.forName("com.agentsflex.skill.artifact.s3.S3SkillArtifactStore");
            Object builder = storeClass.getMethod("builder").invoke(null);

            // 设置必需参数
            builder.getClass().getMethod("region", String.class).invoke(builder, awsS3.getRegion());
            builder.getClass().getMethod("bucket", String.class).invoke(builder, awsS3.getBucket());
            builder.getClass().getMethod("keyPrefix", String.class).invoke(builder, awsS3.getKeyPrefix());
            builder.getClass().getMethod("cacheDirectory", Path.class).invoke(builder, Paths.get(awsS3.getCacheDirectory()));

            // 设置可选参数
            if (awsS3.getEndpoint() != null && !awsS3.getEndpoint().trim().isEmpty()) {
                builder.getClass().getMethod("endpoint", String.class).invoke(builder, awsS3.getEndpoint());
            }

            // 设置 forcePathStyle（S3 特有）
            if (awsS3.getForcePathStyle() != null) {
                builder.getClass().getMethod("forcePathStyle", boolean.class).invoke(builder, awsS3.getForcePathStyle());
            }

            // 设置凭证
            if (awsS3.getAccessKeyId() != null && awsS3.getAccessKeySecret() != null) {
                if (awsS3.getSecurityToken() != null && !awsS3.getSecurityToken().trim().isEmpty()) {
                    builder.getClass().getMethod("credentials", String.class, String.class, String.class)
                            .invoke(builder, awsS3.getAccessKeyId(), awsS3.getAccessKeySecret(), awsS3.getSecurityToken());
                } else {
                    builder.getClass().getMethod("credentials", String.class, String.class)
                            .invoke(builder, awsS3.getAccessKeyId(), awsS3.getAccessKeySecret());
                }
            }

            // 设置最大包大小
            builder.getClass().getMethod("maxPackageSize", long.class).invoke(builder, awsS3.getMaxPackageSize());

            // 构建 SkillArtifactStore
            return (SkillArtifactStore) builder.getClass().getMethod("build").invoke(builder);
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("AWS S3 SkillArtifactStore 尚未实现，请添加依赖：agents-flex-skills-artifact-s3");
        } catch (Exception e) {
            throw new RuntimeException("创建 AWS S3 SkillArtifactStore 失败", e);
        }
    }

    /**
     * 本地存储配置
     */
    public static class LocalConfig {
        /**
         * Skill 安装根目录
         */
        private String root = System.getProperty("java.io.tmpdir") + "/aiflowy/skills";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }

    /**
     * 阿里云 OSS 配置
     */
    public static class AliyunOssConfig {
        /**
         * 区域（如：cn-hangzhou）
         */
        private String region;

        /**
         * 存储桶名称
         */
        private String bucket;

        /**
         * 键前缀（默认：agents-flex/skills）
         */
        private String keyPrefix = "agents-flex/skills";

        /**
         * 缓存目录
         */
        private String cacheDirectory = System.getProperty("java.io.tmpdir") + "/aiflowy/skills-cache/aliyun-oss";

        /**
         * AccessKey ID
         */
        private String accessKeyId;

        /**
         * AccessKey Secret
         */
        private String accessKeySecret;

        /**
         * 安全令牌（可选，用于 STS 临时凭证）
         */
        private String securityToken;

        /**
         * 自定义端点（可选）
         */
        private String endpoint;

        /**
         * 最大包大小（字节）
         */
        private Long maxPackageSize = 100 * 1024 * 1024L; // 100 MB

        // Getters and Setters
        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getCacheDirectory() {
            return cacheDirectory;
        }

        public void setCacheDirectory(String cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getSecurityToken() {
            return securityToken;
        }

        public void setSecurityToken(String securityToken) {
            this.securityToken = securityToken;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Long getMaxPackageSize() {
            return maxPackageSize;
        }

        public void setMaxPackageSize(Long maxPackageSize) {
            this.maxPackageSize = maxPackageSize;
        }
    }

    /**
     * 腾讯云 COS 配置（与阿里云 OSS 相同的配置结构）
     */
    public static class TencentCosConfig extends AliyunOssConfig {
    }

    /**
     * 华为云 OBS 配置（与阿里云 OSS 相同的配置结构）
     */
    public static class HuaweiObsConfig extends AliyunOssConfig {
    }

    /**
     * 火山引擎 TOS 配置（与阿里云 OSS 相同的配置结构）
     */
    public static class VolcengineTosConfig extends AliyunOssConfig {
    }

    /**
     * AWS S3 配置
     */
    public static class AwsS3Config {
        /**
         * AWS 区域（如：us-east-1）
         */
        private String region;

        /**
         * 存储桶名称
         */
        private String bucket;

        /**
         * 键前缀（默认：agents-flex/skills）
         */
        private String keyPrefix = "agents-flex/skills";

        /**
         * 缓存目录
         */
        private String cacheDirectory = System.getProperty("java.io.tmpdir") + "/aiflowy/skills-cache/aws-s3";

        /**
         * AccessKey ID
         */
        private String accessKeyId;

        /**
         * AccessKey Secret
         */
        private String accessKeySecret;

        /**
         * 安全令牌（可选，用于 STS 临时凭证）
         */
        private String securityToken;

        /**
         * 自定义端点（可选，用于 S3-compatible 存储）
         */
        private String endpoint;

        /**
         * 是否强制使用路径样式（用于 S3-compatible 存储）
         */
        private Boolean forcePathStyle = false;

        /**
         * 最大包大小（字节）
         */
        private Long maxPackageSize = 100 * 1024 * 1024L; // 100 MB

        // Getters and Setters
        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String getCacheDirectory() {
            return cacheDirectory;
        }

        public void setCacheDirectory(String cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getSecurityToken() {
            return securityToken;
        }

        public void setSecurityToken(String securityToken) {
            this.securityToken = securityToken;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public Boolean getForcePathStyle() {
            return forcePathStyle;
        }

        public void setForcePathStyle(Boolean forcePathStyle) {
            this.forcePathStyle = forcePathStyle;
        }

        public Long getMaxPackageSize() {
            return maxPackageSize;
        }

        public void setMaxPackageSize(Long maxPackageSize) {
            this.maxPackageSize = maxPackageSize;
        }
    }
}