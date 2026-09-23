package tech.aiflowy.ai.doc.extractor;

import com.agentsflex.core.document.DocumentImagePublisher;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import tech.aiflowy.common.filestorage.CustomMultipartFile;
import tech.aiflowy.common.filestorage.FileStorageService;

import java.io.IOException;

@Component("tempImagePublisher")
public class FileStorageDocumentImagePublisher implements DocumentImagePublisher {

    @Resource(name = "default")
    private FileStorageService fileStorageService;

    @Override
    public String publish(byte[] imageBytes, String mimeType, String fileName) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }
        String originalFilename = (fileName != null && !fileName.isEmpty()) ? fileName : "embedded-image";
        String contentType = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "application/octet-stream";
        CustomMultipartFile multipartFile = new CustomMultipartFile(
                imageBytes, originalFilename, originalFilename, contentType);
        return fileStorageService.saveTemp(multipartFile);
    }
}
