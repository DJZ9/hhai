package tech.aiflowy.common.filestorage;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {


    String save(MultipartFile file);


    void delete(String path);

    /**
     * 上传文件
     * @param file 文件
     * @param prePath 存储桶和文件名中间的路径（不用加斜杠）
     * @return 文件url
     */
    default String save(MultipartFile file, String prePath){
        return "";
    }

    default String save(File file, String prePath){
        return "";
    }

    InputStream readStream(String path) throws IOException;

    /**
     * 获取文件大小
     * @param path
     * @return 文件大小 单位字节
     */
    public long getFileSize(String path);
}
