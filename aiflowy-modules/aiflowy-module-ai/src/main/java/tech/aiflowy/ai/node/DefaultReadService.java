package tech.aiflowy.ai.node;

import com.agentsflex.doc.DocumentExtractionService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component("defaultReader")
public class DefaultReadService implements ReadDocService {

    @Resource
    private DocumentExtractionService documentExtractionService;

    @Override
    public String read(String fileName, InputStream is) {
        return documentExtractionService.extract(is, fileName, null);
    }
}