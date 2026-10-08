package tech.aiflowy.ai.service;

import tech.aiflowy.ai.entity.Document;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import tech.aiflowy.ai.entity.DocumentChunk;
import tech.aiflowy.ai.entity.DocumentCollectionSplitParams;
import tech.aiflowy.common.domain.Result;

import java.math.BigInteger;
import java.util.List;

/**
 *  服务层。
 *
 * @author michael
 * @since 2024-08-23
 */
public interface DocumentService extends IService<Document> {

    Page<Document> getDocumentList(String knowledgeId , int pageSize, int pageNum, String fileName);

    boolean removeDoc(String id);

    Result textSplit(DocumentCollectionSplitParams documentCollectionSplitParams);

    Result saveTextResult(List<DocumentChunk> documentChunks, Document document);

    /**
     * 提交文档解析任务：仅落库文档记录（WAITING）后立即返回，解析在后台异步执行。
     *
     * @param params 分段参数（含 filePath / fileOriginName / knowledgeId 及分段配置）
     * @return 提交结果
     */
    Result<?> quickSave(DocumentCollectionSplitParams params);
}
