package tech.aiflowy.common.filestorage.impl;

import org.dromara.x.file.storage.core.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.filestorage.utils.PathGeneratorUtil;
import tech.aiflowy.common.util.OkHttpUtil;

import java.io.*;

@Component("xFileStorage")
public class XFIleStorageServiceImpl implements FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(XFIleStorageServiceImpl.class);

    @Autowired
    private org.dromara.x.file.storage.core.FileStorageService fileStorageService;

    @Override
    public String save(MultipartFile file) {
        FileInfo fileInfo = fileStorageService.of(file)
                .setPath(PathGeneratorUtil.generateUserPath(""))
                .setSaveFilename(file.getOriginalFilename())
                .setContentType(getFileContentType(file))
                .upload();
        return fileInfo == null ? "上传失败！" : fileInfo.getUrl();
    }

    @Override
    public void delete(String path) {
        fileStorageService.delete(path);
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
