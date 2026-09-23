package tech.aiflowy.ai.doc.extractor;

import com.agentsflex.core.document.DocumentImagePublisher;
import com.agentsflex.doc.DocumentExtractionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DocumentExtractorConfig {

    @Bean
    public DocumentExtractionService documentExtractionService(DocumentImagePublisher documentImagePublisher) {
        return new DocumentExtractionService(documentImagePublisher);
    }

    /**
     * 临时图片提取器：提取的图片存入临时目录（由临时文件清理任务回收），
     * 适用于不希望图片长期保留的解析场景。
     */
    @Bean
    public DocumentExtractionService tempDocumentExtractionService(
            @Qualifier("tempImagePublisher") DocumentImagePublisher documentImagePublisher) {
        return new DocumentExtractionService(documentImagePublisher);
    }
}
