package tech.aiflowy.skill.service.impl;

import com.agentsflex.skill.artifact.SkillArtifact;
import com.agentsflex.skill.artifact.SkillArtifactStore;
import com.agentsflex.skill.artifact.SkillInstallRequest;
import com.agentsflex.skill.artifact.PathSkillPackage;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.util.OkHttpUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.skill.config.SkillArtifactConfig;
import tech.aiflowy.skill.dto.FileTreeNode;
import tech.aiflowy.skill.entity.Skill;
import tech.aiflowy.skill.mapper.SkillMapper;
import tech.aiflowy.skill.service.SkillService;

import java.io.*;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.stream.Stream;

/**
 * 技能 服务层实现。
 *
 * @author wgq
 * @since 2026-07-21
 */
@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, Skill> implements SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillServiceImpl.class);

    @Resource
    private SkillArtifactStore skillArtifactStore;

    @Resource
    private SkillArtifactConfig skillArtifactConfig;

    /**
     * Skill 文件存储根目录（从 aiflowy.skill.artifact.local.root 读取）
     * 默认值：data/skills（相对路径）
     */
    @Value("${aiflowy.skill.artifact.local.root:data/skills}")
    private String skillBasePath;

    /**
     * SKILL.md 文件名
     */
    private static final String SKILL_MD_FILE = "SKILL.md";

    /**
     * 获取 Skill 存储基础路径
     */
    private String getSkillBasePath() {
        // 如果是相对路径，转为绝对路径
        Path path = Paths.get(skillBasePath);
        if (!path.isAbsolute()) {
            // 使用用户主目录下的路径
            path = Paths.get(System.getProperty("user.home"), "aiflowy", "skills");
        }
        return path.toString();
    }

    @Override
    public Skill parseUpload(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 验证 ZIP 文件
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("只支持 ZIP 格式文件");
        }

        // 生成临时目录（用于存储解压的文件）
        String tempId = "temp_" + System.currentTimeMillis();
        Path tempDir = Paths.get(getSkillBasePath(), tempId);

        try {
            Files.createDirectories(tempDir);

            // 统计文件数量和大小
            int fileCount = 0;
            long totalSize = 0L;

            // 解压 ZIP 文件
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path filePath = tempDir.resolve(entry.getName());

                    // 防止 ZIP 滑动攻击
                    if (!filePath.normalize().startsWith(tempDir.normalize())) {
                        throw new IOException("ZIP 文件包含非法路径: " + entry.getName());
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        // 确保父目录存在
                        if (filePath.getParent() != null) {
                            Files.createDirectories(filePath.getParent());
                        }
                        Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                        fileCount++;
                        totalSize += entry.getSize() > 0 ? entry.getSize() : 0;
                    }
                    zis.closeEntry();
                }
            }

            // 向下逐级查找 SKILL.md，以其所在目录作为技能根目录（上层包装目录忽略）
            File skillMdFile = findSkillMdFile(tempDir.toFile());
            if (skillMdFile == null) {
                throw new FileNotFoundException("压缩包中未找到 SKILL.md 文件，请确认包内包含 SKILL.md");
            }
            normalizeSkillRoot(tempDir, skillMdFile);

            // 重新统计（排除上层包装目录）
            fileCount = 0;
            totalSize = 0L;
            try (Stream<Path> stream = Files.walk(tempDir)) {
                for (Path p : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                    fileCount++;
                    totalSize += Files.size(p);
                }
            }

            // 解析 SKILL.md 文件获取元数据
            Skill skill = parseSkillMd(tempDir.toFile());

            // 设置临时路径和文件信息
            skill.setSkillPath(tempDir.toString());
            skill.setFileCount(fileCount);
            skill.setSkillSize(totalSize);

            log.info("parseUpload 完成: tempDir={}, fileCount={}, totalSize={}", tempDir, fileCount, totalSize);

            return skill;
        } catch (Exception e) {
            // 发生异常时清理临时目录
            deleteDirectory(tempDir.toFile());
            throw e;
        }
    }

    /**
     * 以 SKILL.md 所在目录作为技能根目录：将其内容上移到解压根目录，忽略上层包装目录
     *
     * @param tempDir      解压根目录
     * @param skillMdFile  找到的 SKILL.md 文件
     */
    private void normalizeSkillRoot(Path tempDir, File skillMdFile) throws IOException {
        File skillRootDir = skillMdFile.getParentFile();
        if (skillRootDir.toPath().equals(tempDir)) {
            return; // SKILL.md 已在根目录，无需处理
        }
        // 上移 SKILL.md 所在目录的全部内容到根目录
        moveDirectoryContents(skillRootDir, tempDir.toFile());
        // 删除空的包装目录链（相对路径首段即最外层包装目录）
        Path relative = tempDir.relativize(skillRootDir.toPath());
        deleteDirectory(tempDir.resolve(relative.getName(0)).toFile());
    }

    @Override
    public Skill saveWithTempPath(Skill skill) throws Exception {
        if (skill == null) {
            throw new IllegalArgumentException("Skill 不能为空");
        }

        // 获取临时路径
        String tempPath = skill.getSkillPath();
        if (tempPath == null || tempPath.isEmpty()) {
            throw new IllegalArgumentException("临时路径不能为空");
        }

        Path tempDir = Paths.get(tempPath);
        if (!Files.exists(tempDir)) {
            throw new IllegalArgumentException("临时目录不存在: " + tempPath);
        }

        boolean isUpdate = skill.getId() != null && skill.getId().toString().length() > 0;

        if (!isUpdate) {
            setCommonFields(skill);
            save(skill);
        }

        String artifactType = skillArtifactConfig.getType();
        log.info("Skill 存储类型: {}", artifactType);

        try {
            if ("local".equalsIgnoreCase(artifactType)) {
                // 本地存储：使用 用户/日期/ 分层目录结构
                String relativePath = buildSkillRelativePath(skill.getId(), skill.getName());
                Path finalDir = Paths.get(getSkillBasePath(), relativePath);
                Files.createDirectories(finalDir);
                // 移动临时目录下的文件到最终目录
                moveDirectoryContents(tempDir.toFile(), finalDir.toFile());
                skill.setSkillPath(finalDir.toString());
                skill.setStorageKey(relativePath);
            } else {
                // OSS 存储：上传临时目录到 OSS
                uploadTempDirToOss(tempDir, skill);
                // OSS 存储不需要本地路径
                skill.setSkillPath(null);
            }

            // 删除本地临时目录
            deleteDirectory(tempDir.toFile());

            updateById(skill);
            return skill;

        } catch (Exception e) {
            log.error("保存 Skill 失败: id={}", skill.getId(), e);
            if (!isUpdate) {
                removeById(skill.getId());
            }
            // 清理临时目录（无论成功还是失败都清理）
            try {
                deleteDirectory(tempDir.toFile());
                log.info("已清理临时目录: {}", tempPath);
            } catch (Exception cleanError) {
                log.warn("清理临时目录失败: {}", tempPath, cleanError);
            }
            throw new RuntimeException("保存失败：" + e.getMessage(), e);
        }
    }

    /**
     * 上传临时目录到 OSS
     */
    private void uploadTempDirToOss(Path tempDir, Skill skill) throws Exception {
        log.info("开始上传 Skill 到对象存储: name={}", skill.getName());

        // 确保 OSS 缓存目录存在（解决 NoSuchFileException 问题）
        ensureCacheDirectories();

        // 1. 将临时目录重新打包为 ZIP
        Path tempZipFile = Files.createTempFile("skill-upload-", ".zip");
        try {
            // 压缩临时目录（使用正斜杠作为 ZIP 路径分隔符，兼容 Windows 和 Linux）
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
                Files.walk(tempDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        // 统一使用正斜杠作为 ZIP 条目路径（ZIP 规范要求）
                        // Windows: pptx\agents\openai.yaml -> pptx/agents/openai.yaml
                        // Linux:   pptx/agents/openai.yaml -> pptx/agents/openai.yaml (无变化)
                        String entryPath = tempDir.relativize(path).toString().replace('\\', '/');
                        ZipEntry zipEntry = new ZipEntry(entryPath);
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException("压缩文件失败: " + path, e);
                        }
                    });
            }

            // 2. 设置 Skill 元数据
            if (skill.getVersion() == null || skill.getVersion().isEmpty()) {
                skill.setVersion("1.0.0");
            }
            // 使用 用户/日期/ 分层路径作为 storageKey
            String storageKey = buildSkillRelativePath(skill.getId(), skill.getName());
            skill.setStorageKey(storageKey);

            // 3. 创建 SkillArtifact 对象
            SkillArtifact artifact = new SkillArtifact(
                skill.getName(),           // name
                skill.getVersion(),        // version
                null,                      // digest（暂无）
                storageKey                 // storageKey
            );

            // 4. 创建 SkillInstallRequest
            SkillInstallRequest installRequest = new SkillInstallRequest(
                artifact,
                new PathSkillPackage(tempZipFile)
            );

            // 5. 使用 SkillArtifactStore 上传
            SkillArtifact installedArtifact = skillArtifactStore.install(installRequest);

            log.info("Skill 上传成功: name={}, version={}, digest={}",
                installedArtifact.getName(),
                installedArtifact.getVersion(),
                installedArtifact.getDigest());

            // 6. 更新 Skill 信息
            skill.setDigest(installedArtifact.getDigest());

        } finally {
            // 清理临时 ZIP 文件
            try {
                Files.deleteIfExists(tempZipFile);
            } catch (Exception e) {
                log.warn("删除临时 ZIP 文件失败: {}", tempZipFile, e);
            }
        }
    }

    /**
     * 确保对象存储缓存目录存在
     */
    private void ensureCacheDirectories() throws IOException {
        String artifactType = skillArtifactConfig.getType();
        String cacheDirPath = null;

        // 根据存储类型获取缓存目录路径
        switch (artifactType.toLowerCase()) {
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
        }

        if (cacheDirPath != null && !cacheDirPath.isEmpty()) {
            Path cacheDir = Paths.get(cacheDirPath).toAbsolutePath().normalize();
            Path downloadsDir = cacheDir.resolve("downloads");
            Path locksDir = cacheDir.resolve("locks");
            Path objectsDir = cacheDir.resolve("objects");
            
            if (!Files.exists(downloadsDir)) {
                Files.createDirectories(downloadsDir);
                log.info("创建 OSS downloads 目录: {}", downloadsDir);
            }
            if (!Files.exists(locksDir)) {
                Files.createDirectories(locksDir);
                log.info("创建 OSS locks 目录: {}", locksDir);
            }
            if (!Files.exists(objectsDir)) {
                Files.createDirectories(objectsDir);
                log.info("创建 OSS objects 目录: {}", objectsDir);
            }
        }
    }

    @Override
    public Skill uploadAndExtract(MultipartFile file, Skill skill) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 验证 ZIP 文件
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("只支持 ZIP 格式文件");
        }

        // 确保 Skill 有 ID
        if (skill.getId() == null) {
            throw new IllegalArgumentException("Skill ID 不能为空");
        }

        // 创建目标目录
        String skillDirPath = getSkillBasePath() + "/" + skill.getId();
        Path skillDir = Paths.get(skillDirPath);
        
        // 如果目录已存在，先删除
        if (Files.exists(skillDir)) {
            deleteDirectory(skillDir.toFile());
        }
        
        Files.createDirectories(skillDir);

        // 统计文件数量和大小
        int fileCount = 0;
        long totalSize = 0L;

        // 解压 ZIP 文件
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = skillDir.resolve(entry.getName());
                
                // 防止 ZIP 滑动攻击
                if (!filePath.normalize().startsWith(skillDir.normalize())) {
                    throw new IOException("ZIP 文件包含非法路径: " + entry.getName());
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    // 确保父目录存在
                    if (filePath.getParent() != null) {
                        Files.createDirectories(filePath.getParent());
                    }
                    
                    // 写入文件
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                    fileCount++;
                    totalSize += entry.getSize() > 0 ? entry.getSize() : 0;
                }
                zis.closeEntry();
            }
        }

        // 更新 Skill 信息
        skill.setSkillPath(skillDirPath);
        skill.setFileCount(fileCount);
        skill.setSkillSize(totalSize);

        // 向下逐级查找 SKILL.md，以其所在目录作为技能根目录（上层包装目录忽略）
        File skillMdFile = findSkillMdFile(skillDir.toFile());
        if (skillMdFile == null) {
            throw new FileNotFoundException("压缩包中未找到 SKILL.md 文件，请确认包内包含 SKILL.md");
        }
        normalizeSkillRoot(skillDir, skillMdFile);

        // 解析 SKILL.md 文件
        parseSkillMd(skillDir.toFile());

        return skill;
    }

    @Override
    public Skill parseSkillMd(File skillDir) throws Exception {
        if (skillDir == null || !skillDir.exists() || !skillDir.isDirectory()) {
            throw new IllegalArgumentException("Skill 目录无效");
        }

        // 向下逐级查找 SKILL.md 文件
        File skillMdFile = findSkillMdFile(skillDir);
        if (skillMdFile == null) {
            throw new FileNotFoundException("压缩包中未找到 SKILL.md 文件，请确认包内包含 SKILL.md");
        }

        // 读取 SKILL.md 文件内容
        String content = readFileContent(skillMdFile);

        // 解析元数据
        Skill skill = new Skill();
        parseMetadata(content, skill);

        return skill;
    }

    @Override
    public void deleteSkillDirectory(String skillPath) {
        if (skillPath == null || skillPath.isEmpty()) {
            return;
        }

        Path path = Paths.get(skillPath);
        if (Files.exists(path)) {
            try {
                deleteDirectory(path.toFile());
                log.info("成功删除 Skill 目录: {}", skillPath);
            } catch (Exception e) {
                log.error("删除 Skill 目录失败: {}", skillPath, e);
            }
        }
    }

    /**
     * 递归查找 SKILL.md 文件
     *
     * @param directory 目录
     * @return SKILL.md 文件，未找到返回 null
     */
    private File findSkillMdFile(File directory) {
        // 首先检查当前目录
        File skillMd = new File(directory, SKILL_MD_FILE);
        if (skillMd.exists()) {
            return skillMd;
        }

        // 递归查找子目录
        File[] subDirs = directory.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                File found = findSkillMdFile(subDir);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }


    /**
     * 读取文件内容
     *
     * @param file 文件
     * @return 文件内容
     * @throws IOException IO 异常
     */
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 解析 SKILL.md 文件中的元数据
     *
     * @param content 文件内容
     * @param skill Skill 对象
     */
    private void parseMetadata(String content, Skill skill) {
        // 解析 Markdown 格式的元数据
        // 常见格式：
        // # Skill Name
        // 描述内容...
        // 或者使用 YAML front matter:
        // ---
        // name: Skill Name
        // description: Skill Description
        // ---

        String[] lines = content.split("\n");
        boolean inFrontMatter = false;
        StringBuilder descriptionBuilder = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // 处理 YAML front matter
            if (trimmedLine.equals("---")) {
                inFrontMatter = !inFrontMatter;
                continue;
            }

            if (inFrontMatter) {
                // 解析 YAML 格式的元数据
                if (trimmedLine.startsWith("name:") || trimmedLine.startsWith("Name:")) {
                    String name = trimmedLine.substring(trimmedLine.indexOf(":") + 1).trim();
                    skill.setName(name);
                } else if (trimmedLine.startsWith("description:") || trimmedLine.startsWith("Description:")) {
                    String desc = trimmedLine.substring(trimmedLine.indexOf(":") + 1).trim();
                    skill.setDescription(desc);
                } else if (trimmedLine.startsWith("title:") || trimmedLine.startsWith("Title:")) {
                    String title = trimmedLine.substring(trimmedLine.indexOf(":") + 1).trim();
                    skill.setTitle(title);
                } else if (trimmedLine.startsWith("englishName:") || trimmedLine.startsWith("english_name:")) {
                    String englishName = trimmedLine.substring(trimmedLine.indexOf(":") + 1).trim();
                    skill.setEnglishName(englishName);
                }
            } else {
                // 解析 Markdown 标题作为名称
                if (trimmedLine.startsWith("# ") && skill.getName() == null) {
                    skill.setName(trimmedLine.substring(2).trim());
                }
                // 收集描述内容
                if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#") && !trimmedLine.startsWith("---")) {
                    if (descriptionBuilder.length() > 0) {
                        descriptionBuilder.append("\n");
                    }
                    descriptionBuilder.append(trimmedLine);
                }
            }
        }

        // 如果没有设置描述，使用收集的内容
        if (skill.getDescription() == null && descriptionBuilder.length() > 0) {
            skill.setDescription(descriptionBuilder.toString().trim());
        }
    }

    /**
     * 递归删除目录及其内容
     *
     * @param directory 目录
     */
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            log.warn("无法删除文件: {}", directory.getAbsolutePath());
        }
    }

    @Override
    public List<FileTreeNode> getSkillFiles(Long skillId) {
        // 查询 Skill 信息
        Skill skill = this.getById(skillId);
        if (skill == null) {
            throw new BusinessException("Skill 不存在");
        }

        // 获取 Skill 目录路径
        String skillPath = skill.getSkillPath();
        if (skillPath == null || skillPath.isEmpty()) {
            throw new BusinessException("Skill 路径不存在");
        }

        File skillDir = new File(skillPath);
        if (!skillDir.exists() || !skillDir.isDirectory()) {
            throw new BusinessException("Skill 目录不存在");
        }

        // 构建文件树
        List<FileTreeNode> fileTree = new ArrayList<>();
        buildFileTree(skillDir, skillDir, fileTree);

        return fileTree;
    }

    @Override
    public void downloadSkill(Long skillId, HttpServletResponse response) throws Exception {
        Skill skill = this.getById(skillId);
        if (skill == null) {
            throw new BusinessException("Skill 不存在");
        }

        // 1. 获取本地文件路径
        Path localPath = getSkillLocalPath(skill);

        // 2. 如果 materialize 返回的是目录，打包为临时 ZIP
        Path zipPath;
        boolean isTempZip = false;
        if (Files.isDirectory(localPath)) {
            zipPath = Files.createTempFile("skill-download-", ".zip");
            zipDirectory(localPath, zipPath);
            isTempZip = true;
        } else {
            zipPath = localPath;
        }

        // 3. 设置响应头
        String filename = (skill.getName() != null ? skill.getName() : "skill") + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

        // 4. 流式输出
        try (OutputStream out = response.getOutputStream();
             InputStream in = Files.newInputStream(zipPath)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } finally {
            // 清理临时 ZIP
            if (isTempZip) {
                Files.deleteIfExists(zipPath);
            }
        }
    }

    @Override
    public Skill saveWithFile(Skill skill, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 1. 解压 ZIP 到临时目录
        Path tempDir = extractToTempDir(file);

        // 2. 解析 SKILL.md 元数据并合并到 skill（仅当表单未填写且解析值非空时才覆盖）
        Skill parsed = parseSkillMd(tempDir.toFile());
        if ((skill.getName() == null || skill.getName().isEmpty()) && parsed.getName() != null) {
            skill.setName(parsed.getName());
        }
        if ((skill.getTitle() == null || skill.getTitle().isEmpty()) && parsed.getTitle() != null) {
            skill.setTitle(parsed.getTitle());
        }
        if ((skill.getDescription() == null || skill.getDescription().isEmpty()) && parsed.getDescription() != null) {
            skill.setDescription(parsed.getDescription());
        }
        if ((skill.getEnglishName() == null || skill.getEnglishName().isEmpty()) && parsed.getEnglishName() != null) {
            skill.setEnglishName(parsed.getEnglishName());
        }

        // 确保 name 和 title 不为空（SKILL.md 可能不含这些字段）
        if (skill.getName() == null || skill.getName().isEmpty()) {
            String fallbackName = file.getOriginalFilename();
            if (fallbackName != null && fallbackName.toLowerCase().endsWith(".zip")) {
                fallbackName = fallbackName.substring(0, fallbackName.length() - 4);
            }
            skill.setName(fallbackName != null ? fallbackName : "untitled");
        }
        if (skill.getTitle() == null || skill.getTitle().isEmpty()) {
            skill.setTitle(skill.getName());
        }

        // 3. 设置临时路径并统计文件信息
        skill.setSkillPath(tempDir.toString());
        int fileCount = 0;
        long totalSize = 0L;
        File[] allFiles = tempDir.toFile().listFiles();
        if (allFiles != null) {
            fileCount = allFiles.length;
            // 简单的大小统计（不递归，与 parseUpload 一致）
        }
        skill.setFileCount(fileCount);

        // 4. 复用 saveWithTempPath 逻辑
        return saveWithTempPath(skill);
    }

    @Override
    public Skill updateWithFile(Skill skill, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 1. 查出旧数据
        Skill oldSkill = this.getById(skill.getId());
        if (oldSkill == null) {
            throw new BusinessException("Skill 不存在");
        }

        // 2. 删除旧的 artifact
        deleteSkillArtifact(oldSkill);

        // 3. 处理新文件
        return saveWithFile(skill, file);
    }

    @Override
    public Skill installFromMarket(String downloadUrl, Skill skill) throws Exception {
        // 流式下载 ZIP 到临时文件
        Path tempZip = Files.createTempFile("skill-market-download-", ".zip");
        Path tempDir = null;
        try {
            long downloadedSize = OkHttpUtil.downloadToFile(downloadUrl, tempZip);
            if (downloadedSize <= 0) {
                throw new BusinessException("下载ZIP失败：" + downloadUrl);
            }
            // 从临时文件流式解压到临时目录
            tempDir = extractToTempDir(tempZip);

            // 解析 SKILL.md 元数据
            Skill parsed = parseSkillMd(tempDir.toFile());

            //合并元数据：商城传入的优先，SKILL.md 补充空缺
            if (skill == null) {
                skill = new Skill();
            }
            if ((skill.getName() == null || skill.getName().isEmpty()) && parsed.getName() != null) {
                skill.setName(parsed.getName());
            }
            if ((skill.getTitle() == null || skill.getTitle().isEmpty()) && parsed.getTitle() != null) {
                skill.setTitle(parsed.getTitle());
            }
            if ((skill.getDescription() == null || skill.getDescription().isEmpty()) && parsed.getDescription() != null) {
                skill.setDescription(parsed.getDescription());
            }
            if ((skill.getEnglishName() == null || skill.getEnglishName().isEmpty()) && parsed.getEnglishName() != null) {
                skill.setEnglishName(parsed.getEnglishName());
            }
            // 确保 name/title 非空
            if (skill.getName() == null || skill.getName().isEmpty()) {
                throw new BusinessException("技能名称不能为空");
            }
            if (skill.getTitle() == null || skill.getTitle().isEmpty()) {
                skill.setTitle(skill.getName());
            }

            // 幂等性：同名技能已存在则走更新流程（删除旧 artifact + 安装新文件）
            Skill existing = getOne(QueryWrapper.create().eq(Skill::getName, skill.getName()).limit(1));
            if (existing != null) {
                skill.setId(existing.getId());
                deleteSkillArtifact(existing);
            }

            // 设置临时路径与文件统计
            skill.setSkillPath(tempDir.toString());
            int fileCount = countFilesRecursive(tempDir.toFile());
            long totalSize = calculateDirectorySize(tempDir.toFile());
            skill.setFileCount(fileCount);
            skill.setSkillSize(totalSize);

            // 复用 saveWithTempPath 保存到最终位置
            return saveWithTempPath(skill);
        } catch (Exception e) {
            // 失败时清理临时目录
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir.toFile());
                } catch (Exception ignored) {
                }
            }
            throw e;
        } finally {
            // 无论成功失败都删除临时 ZIP 文件
            try {
                Files.deleteIfExists(tempZip);
            } catch (Exception ignored) {
            }
        }
    }


    /**
     * 递归统计目录下的文件数量
     */
    private int countFilesRecursive(File dir) {
        if (dir == null || !dir.exists()) {
            return 0;
        }
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    count += countFilesRecursive(f);
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 递归计算目录大小（字节）
     */
    private long calculateDirectorySize(File dir) {
        if (dir == null || !dir.exists()) {
            return 0L;
        }
        long size = 0L;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    size += calculateDirectorySize(f);
                } else {
                    size += f.length();
                }
            }
        }
        return size;
    }

    /**
     * 解压上传的 ZIP 文件到临时目录
     */
    private Path extractToTempDir(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("只支持 ZIP 格式文件");
        }
        return extractToTempDir(file.getInputStream());
    }

    /**
     * 解压 ZIP 输入流到临时目录（供 installFromMarket 等场景复用）
     */
    private Path extractToTempDir(InputStream zipInputStream) throws IOException {
        String tempId = "temp_" + System.currentTimeMillis();
        Path tempDir = Paths.get(getSkillBasePath(), tempId);
        Files.createDirectories(tempDir);

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = tempDir.resolve(entry.getName());

                // 防止 ZIP 滑动攻击
                if (!filePath.normalize().startsWith(tempDir.normalize())) {
                    throw new IOException("ZIP 文件包含非法路径: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    if (filePath.getParent() != null) {
                        Files.createDirectories(filePath.getParent());
                    }
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
        return tempDir;
    }

    /**
     * 解压本地 ZIP 文件到临时目录（适用于大文件流式下载后的解压，避免内存驻留）
     */
    private Path extractToTempDir(Path zipFile) throws IOException {
        try (InputStream in = Files.newInputStream(zipFile)) {
            return extractToTempDir(in);
        }
    }

    /**
     * 获取 Skill 的本地文件路径
     */
    private Path getSkillLocalPath(Skill skill) throws Exception {
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

    /**
     * 将目录打包为 ZIP 文件
     */
    private void zipDirectory(Path sourceDir, Path targetZip) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(targetZip))) {
            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
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

    @Override
    public void deleteSkillArtifact(Skill skill) {
        if (skill == null) {
            return;
        }

        String artifactType = skillArtifactConfig.getType();

        if ("local".equalsIgnoreCase(artifactType)) {
            // 本地存储：删除 skillPath 指向的目录
            String skillPath = skill.getSkillPath();
            if (skillPath != null && !skillPath.isEmpty()) {
                deleteSkillDirectory(skillPath);
            }
        } else {
            // OSS 存储：通过 SkillArtifactStore.delete() 删除
            String storageKey = skill.getStorageKey();
            if (storageKey == null || storageKey.isEmpty()) {
                log.warn("Skill storageKey 为空，跳过 OSS Artifact 删除: skillId={}", skill.getId());
                return;
            }

            // 构造 SkillArtifact（与 uploadTempDirToOss 中 install 时保持一致）
            SkillArtifact artifact = new SkillArtifact(
                    skill.getName(),
                    skill.getVersion(),
                    skill.getDigest(),
                    storageKey
            );

            try {
                skillArtifactStore.delete(artifact);
                log.info("Skill Artifact 删除成功: name={}, storageKey={}",
                        skill.getName(), storageKey);
            } catch (Exception e) {
                log.error("Skill Artifact 删除失败: name={}, storageKey={}",
                        skill.getName(), storageKey, e);
            }
        }
    }

    /**
     * 设置 Skill 的公共字段（createdBy, tenantId, deptId, modifiedBy, created, modified）
     */
    private void setCommonFields(Skill skill) {
        LoginAccount account = SaTokenUtil.getLoginAccount();
        skill.setCreatedBy(account.getId());
        skill.setTenantId(account.getTenantId());
        skill.setDeptId(account.getDeptId());
        skill.setModifiedBy(account.getId());
        skill.setCreated(new Date());
        skill.setModified(new Date());
    }

    /**
     * 构建 Skill 的相对存储路径
     * 格式：{userId}/{year}/{month}/{day}/{skillId}/{skillName}/
     * 参照 PathGeneratorUtil.generateUserPath() 的模式
     *
     * @param skillId   Skill ID
     * @param skillName Skill 名称
     * @return 相对路径，如 "123/2026/7/24/1723456789012345678/my-skill/"
     */
    private String buildSkillRelativePath(BigInteger skillId, String skillName) {
        // 获取当前用户 ID
        BigInteger userId = SaTokenUtil.getLoginAccount().getId();

        // 获取当前日期
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();

        // 构建相对路径：userId/年/月/日/skillId/skillName/
        return String.format("%d/%d/%d/%d/%s/%s/",
                userId, year, month, day, skillId.toString(), skillName);
    }

    /**
     * 移动目录下的所有内容（文件+子目录）到目标目录
     */
    private void moveDirectoryContents(File sourceDir, File targetDir) throws IOException {
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                Path target = targetDir.toPath().resolve(file.getName());
                Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * 构建文件树
     *
     * @param rootDir 根目录
     * @param currentDir 当前目录
     * @param nodes 文件树节点列表
     */
    private void buildFileTree(File rootDir, File currentDir, List<FileTreeNode> nodes) {
        File[] files = currentDir.listFiles();
        if (files == null) {
            return;
        }

        // 按文件名排序：目录优先
        Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) {
                return -1;
            } else if (!a.isDirectory() && b.isDirectory()) {
                return 1;
            } else {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });

        for (File file : files) {
            FileTreeNode node = new FileTreeNode();
            node.setLabel(file.getName());
            node.setIsFile(file.isFile());

            // 计算相对路径
            String relativePath = rootDir.toPath().relativize(file.toPath()).toString();
            node.setPath(relativePath.replace("\\", "/"));

            if (file.isFile()) {
                node.setSize(file.length());
            } else {
                // 递归处理子目录
                List<FileTreeNode> children = new ArrayList<>();
                buildFileTree(rootDir, file, children);
                node.setChildren(children);
            }

            nodes.add(node);
        }
    }
}