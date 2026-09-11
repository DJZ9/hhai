package tech.aiflowy.ai.agentsflex;

import com.agentsflex.doc.DocumentExtractionService;
import com.agentsflex.doc.source.HttpDocumentSource;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.filestorage.utils.PathGeneratorUtil;
import tech.aiflowy.common.util.UrlEncoderUtil;

import java.util.List;

/**
 * 附件处理帮助类，提供附件图片检测与附件内容文本转换能力。
 */
@Component
public class AttachmentHelper {

    @Resource(name = "default")
    private FileStorageService storageService;

    @Resource
    private DocumentExtractionService documentExtractionService;

    /**
     * 检测attachments中是否有图片
     * 图片格式包括：jpg, jpeg, png, gif, bmp, webp, svg
     */
    public boolean hasImageInAttachments(List<String> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return false;
        }
        // 图片文件扩展名
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg"};
        for (String attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            String lowerCaseUrl = attachment.toLowerCase();
            for (String ext : imageExtensions) {
                if (lowerCaseUrl.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String attachmentsToString(List<String> fileList) {
        StringBuilder messageBuilder = new StringBuilder();
        if (fileList != null && !fileList.isEmpty()) {
            for (int i = 0; i < fileList.size(); i++) {
                String fileUrl = fileList.get(i);
                String encodedUrl = UrlEncoderUtil.getEncodedUrl(fileUrl);
                String result = documentExtractionService.extract(new HttpDocumentSource(encodedUrl));
                if (result != null) {
                    if (i > 0) {
                        messageBuilder.append("\n\n");
                    }
                    messageBuilder.append("附件").append(i + 1).append("，文件名为：").append(PathGeneratorUtil.getPureFileName(fileUrl)).append("，访问地址为：").append(fileUrl).append("，内容为：  \n").append(result);
                }
                storageService.delete(fileUrl);
            }
        }
        return messageBuilder.toString();
    }
}
