package tech.aiflowy.ai.doc.extractor;

import com.agentsflex.core.document.DocumentImagePublisher;
import com.agentsflex.doc.DocumentExtractionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class DocumentExtractorConfig {

    @Bean
    public DocumentExtractionService documentExtractionService(DocumentImagePublisher documentImagePublisher) {
        return new DocumentExtractionService(documentImagePublisher);
    }
}
