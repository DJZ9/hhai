package tech.aiflowy.common.filestorage.impl;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.get.ListFilesResult;
import org.dromara.x.file.storage.core.get.RemoteDirInfo;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.filestorage.utils.PathGeneratorUtil;
import tech.aiflowy.common.util.OkHttpUtil;

import java.io.InputStream;
import java.time.LocalDate;

@Component("xFileStorage")
public class XFIleStorageServiceImpl implements FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(XFIleStorageServiceImpl.class);

    @Autowired
    private org.dromara.x.file.storage.core.FileStorageService xFileStorageService;

    @Override
    public String save(MultipartFile file) {
        String filename = PathGeneratorUtil.getPureFileName(file.getOriginalFilename());
        FileInfo fileInfo = xFileStorageService.of(file)
                .setPath(PathGeneratorUtil.generateUserPath(""))
                .setSaveFilename(filename)
                .setContentType(getFileContentType(file))
                .upload();
        return fileInfo == null ? "上传失败！" : fileInfo.getUrl();
    }

    @Override
    public String saveTemp(MultipartFile file) {
        String path = PathGeneratorUtil.generateTempPath("");
        String filename = PathGeneratorUtil.getPureFileName(file.getOriginalFilename());
        FileInfo fileInfo = xFileStorageService.of(file)
                .setPath(path)
                .setSaveFilename(filename)
                .setContentType(getFileContentType(file))
                .upload();
        return fileInfo == null ? "上传失败！" : fileInfo.getUrl();
    }

    @Override
    public void delete(String path) {
        xFileStorageService.delete(path);
    }

    @Override
    public InputStream readStream(String fileUrl) {
        return OkHttpUtil.getInputStream(fileUrl);
    }

    /**
     * 获取S3中文件的大小（单位：字节）
     * @param path 文件路径
     * @return 文件大小（字节）
     */
    @Override
    public long getFileSize(String path) {
        try {
            return OkHttpUtil.getFileSize(path);
        } catch (Exception e) {
            LOG.error("获取文件大小失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int cleanTempFiles(LocalDate date) {
        // 临时文件路径格式: /temp/{年/月/日/uuid/文件名}
        // 构建要清理的日期路径
        String tempPath = String.format("/temp/%d/%d/%d/", date.getYear(), date.getMonthValue(), date.getDayOfMonth());

        LOG.info("开始清理云存储临时文件，路径: {}", tempPath);

        int deletedCount = cleanDirectory(tempPath);

        LOG.info("云存储临时文件清理完成，路径: {}，成功删除 {} 个文件", tempPath, deletedCount);

        return deletedCount;
    }

    /**
     * 递归清理目录下的所有文件
     */
    private int cleanDirectory(String path) {
        int deletedCount = 0;

        try {
            LOG.debug("正在扫描目录: {}", path);
            
            ListFilesResult result = xFileStorageService.listFiles()
                    .setPath(path)
                    .listFiles();

            if (result == null) {
                LOG.debug("目录为空或不存在: {}", path);
                return 0;
            }

            LOG.debug("目录 {} 扫描结果: fileList={}, dirList={}", path, 
                    result.getFileList() != null ? result.getFileList().size() : 0,
                    result.getDirList() != null ? result.getDirList().size() : 0);

            // 删除当前目录下的文件
            if (result.getFileList() != null) {
                for (RemoteFileInfo fileInfo : result.getFileList()) {
                    try {
                        LOG.info("准备删除文件: url={}, path={}, filename={}", 
                                fileInfo.getUrl(), fileInfo.getPath(), fileInfo.getFilename());
                        
                        // 使用FileInfo方式删除，避免FileRecorder.getByUrl()返回null的问题
                        FileInfo fi = new FileInfo();
                        fi.setPlatform(fileInfo.getPlatform());
                        fi.setBasePath(fileInfo.getBasePath());
                        fi.setPath(fileInfo.getPath());
                        fi.setFilename(fileInfo.getFilename());
                        
                        boolean deleted = xFileStorageService.delete(fi);
                        if (deleted) {
                            deletedCount++;
                            LOG.info("删除临时文件成功: {}", fileInfo.getUrl());
                        } else {
                            LOG.warn("删除临时文件返回false: {}", fileInfo.getUrl());
                        }
                    } catch (Exception e) {
                        LOG.warn("删除临时文件失败: {}", fileInfo.getUrl(), e);
                    }
                }
            }

            // 递归删除子目录中的文件
            if (result.getDirList() != null) {
                for (RemoteDirInfo dirInfo : result.getDirList()) {
                    // 构建完整的子目录路径: path + name + /
                    String subDirPath = dirInfo.getPath() + dirInfo.getName() + "/";
                    LOG.debug("递归进入子目录: {}", subDirPath);
                    deletedCount += cleanDirectory(subDirPath);
                }
            }

        } catch (Exception e) {
            LOG.error("清理云存储目录失败: {}", path, e);
        }

        return deletedCount;
    }

    /**
     * 获取文件的 Content-Type
     */
    public static String getFileContentType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String contentType = null;
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".txt")) {
            contentType = "text/plain; charset=utf-8";
        } else {
            // 其他类型文件可以按需设置
            contentType = file.getContentType();
        }
        return contentType;
    }
}