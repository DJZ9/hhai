package tech.aiflowy.common.filestorage.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.filestorage.utils.PathGeneratorUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;


@Component("local")
public class LocalFileStorageServiceImpl implements FileStorageService {
    private static final Logger LOG = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);


    @Value("${aiflowy.storage.local.root:}")
    private String root;
    @Value("${aiflowy.storage.local.prefix}")
    private String prefix;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
    }


    @Override
    public String save(MultipartFile file) {
        try {
            String path = PathGeneratorUtil.generateUserPath(file.getOriginalFilename());
            File target = getLocalFile(path);
            if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
                LOG.error("创建文件失败: {} ", target.getParentFile());
            }
            file.transferTo(target);
            return prefix + path;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String saveTemp(MultipartFile file) {
        try {
            String path = PathGeneratorUtil.generateTempPath(file.getOriginalFilename());
            File target = getLocalFile(path);
            if (!target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
                LOG.error("创建临时文件失败: {} ", target.getParentFile());
            }
            file.transferTo(target);
            return prefix + path;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public InputStream readStream(String path) throws IOException {
        File target = getLocalFile(path);
        return Files.newInputStream(target.toPath());
    }

    @Override
    public long getFileSize(String path) {
        File target = null;
        try {
            target = getLocalFile(path);
        } catch (IOException e) {
            throw new RuntimeException("获取文件大小出错", e);
        }
        if (target.exists()) {
            return target.length();
        }
        return 0;
    }

    @Override
    public void delete(String path) {
        try {
            File file = getLocalFile(path);
            Files.delete(file.toPath());
        } catch (IOException e) {
            LOG.error("删除本地文件出错: {}", path, e);
            throw new RuntimeException("删除本地文件出错：",e);
        }
    }

    @Override
    public int cleanTempFiles(LocalDate date) {
        if (root == null || root.isEmpty()) {
            LOG.warn("存储根目录未配置，跳过临时文件清理");
            return 0;
        }

        File tempDir = new File(root, "temp");
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            LOG.info("临时目录不存在，无需清理: {}", tempDir.getAbsolutePath());
            return 0;
        }

        int deletedCount = 0;

        try {
            int year = date.getYear();
            int month = date.getMonthValue();
            int day = date.getDayOfMonth();

            LOG.info("准备清理本地临时文件，根目录: {}，日期: {}-{}-{}", root, year, month, day);

            // 构建目标日期目录路径: temp/年/月/日
            File yearDir = new File(tempDir, String.valueOf(year));
            File monthDir = new File(yearDir, String.valueOf(month));
            File dayDir = new File(monthDir, String.valueOf(day));

            LOG.info("目标日期目录路径: {}", dayDir.getAbsolutePath());

            if (!yearDir.exists()) {
                LOG.info("年份目录不存在: {}", yearDir.getAbsolutePath());
                return 0;
            }
            if (!monthDir.exists()) {
                LOG.info("月份目录不存在: {}", monthDir.getAbsolutePath());
                return 0;
            }
            if (!dayDir.exists()) {
                LOG.info("日期目录不存在: {}", dayDir.getAbsolutePath());
                return 0;
            }

            if (dayDir.isDirectory()) {
                deletedCount = deleteDirectory(dayDir);
                LOG.info("本地临时文件清理完成，日期 {}，删除 {} 个文件", date, deletedCount);
            }
        } catch (Exception e) {
            LOG.error("清理本地临时文件失败，日期: {}", date, e);
        }

        return deletedCount;
    }

    /**
     * 递归删除目录及其所有内容
     * @param directory 要删除的目录
     * @return 删除的文件数量
     */
    private int deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return 0;
        }

        final int[] count = {0};

        try {
            Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        Files.delete(file);
                        count[0]++;
                    } catch (IOException e) {
                        LOG.warn("删除文件失败: {}", file, e);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    try {
                        Files.delete(dir);
                    } catch (IOException e) {
                        LOG.warn("删除目录失败: {}", dir, e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.error("遍历目录失败: {}", directory.getAbsolutePath(), e);
        }

        return count[0];
    }


    /**
     * 获取存储根目录下的本地文件。
     * <p>
     * 会对路径做规范化处理，确保最终访问的文件一定落在存储根目录内。
     */
    private File getLocalFile(String path) throws IOException {
        if (this.root == null || this.root.isEmpty()) {
            throw new RuntimeException("请指定存储根目录");
        }
        String relativePath = stripStoragePrefix(path, this.prefix);
        return resolveLocalFile(this.root, relativePath, path);
    }

    /**
     * 剥离存储路径的访问前缀（例如 http://localhost:8080/attachment）。
     * 仅当路径确实以 prefix 开头时截断一次，避免使用 replace 造成的全局替换问题。
     */
    static String stripStoragePrefix(String path, String prefix) {
        if (path == null) {
            return null;
        }
        String trimmed = path.trim();
        if (prefix != null && !prefix.isEmpty() && trimmed.startsWith(prefix)) {
            return trimmed.substring(prefix.length());
        }
        return trimmed;
    }

    /**
     * 将相对路径解析到存储根目录内，并确保最终落点在根目录内。
     * <p>
     * 采用 canonical path 而非 real path：save 场景下目标文件可能尚不存在，
     * 同时 canonical path 已能解析 <code>..</code> 与已存在的符号链接。
     *
     * @param rootPath       存储根目录
     * @param relativePath   相对存储根目录的路径
     * @param originalPath   调用方传入的原始路径（仅用于日志）
     * @return 根目录内的目标文件
     * @throws SecurityException 路径不合法
     */
    static File resolveLocalFile(String rootPath, String relativePath, String originalPath) throws IOException {
        if (relativePath == null || relativePath.isEmpty() || relativePath.indexOf('\0') >= 0) {
            throw illegalPath(originalPath);
        }

        File rootDir = new File(rootPath).getCanonicalFile();
        File target;
        try {
            target = new File(rootDir, relativePath).getCanonicalFile();
        } catch (IOException e) {
            // 畸形路径（例如 Windows 上的非法盘符路径）无法规范化，直接拒绝
            throw illegalPath(originalPath);
        }

        Path rootDirPath = rootDir.toPath();
        Path targetPath = target.toPath();
        if (targetPath.equals(rootDirPath) || !targetPath.startsWith(rootDirPath)) {
            throw illegalPath(originalPath);
        }
        return target;
    }

    private static SecurityException illegalPath(String originalPath) {
        LOG.warn("检测到非法文件路径访问，已拒绝: {}", originalPath);
        return new SecurityException("非法的文件路径");
    }

    @Override
    public String save(MultipartFile file, String prePath) {
        return save(file);
    }
}
