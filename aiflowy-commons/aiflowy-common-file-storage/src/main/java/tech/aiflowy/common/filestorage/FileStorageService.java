package tech.aiflowy.common.filestorage;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

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

    /**
     * 保存临时文件到临时目录（用于定时清理）
     * @param file 文件
     * @return 文件url
     */
    String saveTemp(MultipartFile file);


    /**
     * 清理指定日期的临时文件
     * @param date 要清理的日期
     * @return 清理的文件数量，返回-1表示不支持清理
     */
    default int cleanTempFiles(LocalDate date) {
        return -1;
    }
}
