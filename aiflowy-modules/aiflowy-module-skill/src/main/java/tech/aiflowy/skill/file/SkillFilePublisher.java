package tech.aiflowy.skill.file;

import com.agentsflex.skill.file.FilePublishRequest;
import com.agentsflex.skill.file.FilePublisher;
import com.agentsflex.skill.file.PublishedFile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tech.aiflowy.common.filestorage.FileStorageService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Skill 文件发布器实现
 * 将 Skill Runtime 生成的文件上传到临时存储，返回可访问的 URL
 */
@Component
public class SkillFilePublisher implements FilePublisher {

    private final FileStorageService fileStorageService;

    public SkillFilePublisher(@Qualifier("default") FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public PublishedFile publish(FilePublishRequest request) {
        try {
            // 创建临时文件
            String fileName = request.getFileName();
            File tempFile = Files.createTempFile("skill_", "_" + fileName).toFile();

            // 将 InputStream 写入临时文件（必须同步消费）
            try (InputStream is = request.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // 创建 MultipartFile 适配器
            MultipartFile multipartFile = new TempMultipartFile(tempFile, fileName, request.getContentType());

            // 调用 saveTemp 上传到临时存储
            String url = fileStorageService.saveTemp(multipartFile);

            // 删除本地临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }

            // 构建返回结果
            return PublishedFile.builder()
                    .url(url)
                    .fileName(fileName)
                    .contentType(request.getContentType())
                    .contentLength(request.getContentLength() >= 0 ? request.getContentLength() : tempFile.length())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to publish skill file: " + request.getFileName(), e);
        }
    }

    /**
     * 临时文件的 MultipartFile 适配器
     */
    private static class TempMultipartFile implements MultipartFile {

        private final File tempFile;
        private final String fileName;
        private final String contentType;

        public TempMultipartFile(File tempFile, String fileName, String contentType) {
            this.tempFile = tempFile;
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return fileName;
        }

        @Override
        public String getOriginalFilename() {
            return fileName;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return tempFile.length() == 0;
        }

        @Override
        public long getSize() {
            return tempFile.length();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(tempFile.toPath());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(getBytes());
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.copy(tempFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}