package tech.aiflowy.ai.node;

import com.agentsflex.core.util.StringUtil;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.chain.Parameter;
import dev.tinyflow.core.node.BaseNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aiflowy.ai.utils.DocUtil;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.util.SpringContextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocNode extends BaseNode {

    private static final Logger log = LoggerFactory.getLogger(DocNode.class);

    @Override
    public Map<String, Object> execute(Chain chain) {
        Map<String, Object> map = chain.getState().resolveParameters(this);
        Map<String, Object> res = new HashMap<>();
        String url = map.get("fileUrl").toString();
        ReaderManager manager = SpringContextUtil.getBean(ReaderManager.class);
        FileStorageService storageService = SpringContextUtil.getBean("default", FileStorageService.class);
        String docContent;
        try (InputStream inputStream = storageService.readStream(url)) {
            if (inputStream == null) {
                throw new IOException("文件读取失败，请确认文件地址是否可访问：" + url);
            }
            docContent = manager.getReader().read(DocUtil.getFileNameByUrl(url), inputStream);
        } catch (IOException e) {
            log.error("读取文件失败，文件地址不可访问：{}", url, e);
            docContent = "";
        }

        String key = "content";
        List<Parameter> outputDefs = getOutputDefs();
        if (outputDefs != null && !outputDefs.isEmpty()) {
            String defName = outputDefs.get(0).getName();
            if (StringUtil.hasText(defName)) key = defName;
        }
        res.put(key, docContent);
        return res;
    }
}
