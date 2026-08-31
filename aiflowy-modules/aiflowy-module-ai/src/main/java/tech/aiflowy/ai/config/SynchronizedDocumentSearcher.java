package tech.aiflowy.ai.config;

import com.agentsflex.core.document.Document;
import com.agentsflex.search.engine.service.DocumentSearcher;

import java.util.List;
import java.util.Map;

/**
 * 按知识库串行化 Lucene 写操作的 DocumentSearcher 包装器。
 *
 * <p>Lucene 同一索引目录同一时刻只允许一个 IndexWriter，而 {@code LuceneSearcher}
 * 的 add/delete/update 每次都会新建并关闭 IndexWriter。同一知识库的多个文档异步处理任务
 * 并发调用这些写方法时，会出现 {@code LockObtainFailedException: Lock held by this virtual machine}。
 * 本包装器用一个共享写锁串行化写方法（每个知识库缓存一个实例，故锁粒度为每知识库）；
 * 读方法不锁，保持并发检索能力。</p>
 */
public class SynchronizedDocumentSearcher implements DocumentSearcher {

    private final DocumentSearcher delegate;
    private final Object writeLock = new Object();

    public SynchronizedDocumentSearcher(DocumentSearcher delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean addDocument(Document document) {
        synchronized (writeLock) {
            return delegate.addDocument(document);
        }
    }

    @Override
    public boolean deleteDocument(Object id) {
        synchronized (writeLock) {
            return delegate.deleteDocument(id);
        }
    }

    @Override
    public boolean updateDocument(Document document) {
        synchronized (writeLock) {
            return delegate.updateDocument(document);
        }
    }

    @Override
    public List<Document> searchDocuments(String keyword, int count) {
        return delegate.searchDocuments(keyword, count);
    }

    @Override
    public List<Document> searchDocuments(String keyword, int count, Map<String, Object> metadataFilters) {
        return delegate.searchDocuments(keyword, count, metadataFilters);
    }
}
