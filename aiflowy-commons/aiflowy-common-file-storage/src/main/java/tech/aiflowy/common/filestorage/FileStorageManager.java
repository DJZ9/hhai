package tech.aiflowy.common.filestorage;

import tech.aiflowy.common.filestorage.xFileStorage.StorageConfig;
import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.filestorage.impl.LocalFileStorageServiceImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component("default")
public class FileStorageManager implements FileStorageService {

    @Override
    public String save(MultipartFile file) {
        return getService().save(file);
    }

    @Override
    public String save(MultipartFile file,String prePath) {
        return getService().save(file,prePath);
    }
	
	@Override
    public void delete(String path) {
        getService().delete(path);
    }

    @Override
    public String save(File file, String prePath) {
        return getService().save(file, prePath);
    }

    @Override
    public InputStream readStream(String path) throws IOException {
        return getService().readStream(path);
    }

    @Override
    public long getFileSize(String path) {
        return getService().getFileSize(path);
    }

    private FileStorageService getService() {
        String type = StorageConfig.getInstance().getType();
        if (!StringUtils.hasText(type)) {
            return SpringContextUtil.getBean(LocalFileStorageServiceImpl.class);
        } else {
            return SpringContextUtil.getBean(type);
        }
    }
}
