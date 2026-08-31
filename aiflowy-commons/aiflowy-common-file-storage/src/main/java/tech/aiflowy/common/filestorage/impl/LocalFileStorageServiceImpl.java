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


    private File getLocalFile(String path) throws IOException {
        if (this.root == null || this.root.isEmpty()) {
            throw new RuntimeException("请指定存储根目录");
        }
        return new File(this.root, path.replace(prefix, ""));
    }

    @Override
    public String save(MultipartFile file, String prePath) {
        return save(file);
    }
}
