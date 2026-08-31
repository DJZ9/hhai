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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


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

    /**
     * 递归删除文件或目录（支持删除非空目录）
     * @param file 要删除的文件或目录
     */
    private void deleteRecursively(File file) throws Exception {
        if (file == null || !file.exists()) {
            LOG.warn("文件/目录不存在: {}", file);
            return;
        }

        // 如果是目录，先递归删除子文件和子目录
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) { // 防止null（目录可能被其他进程修改）
                for (File child : children) {
                    deleteRecursively(child); // 递归删除子内容
                }
            }
        }

        // 删除当前文件或空目录
        boolean deleted = file.delete();
        if (!deleted) {
            throw new Exception("无法删除文件/目录: " + file.getAbsolutePath());
        }
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
