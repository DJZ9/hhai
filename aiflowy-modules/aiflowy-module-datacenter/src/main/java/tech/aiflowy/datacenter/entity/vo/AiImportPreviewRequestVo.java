package tech.aiflowy.datacenter.entity.vo;

import java.math.BigInteger;
import java.util.List;

/**
 * AI智能导入预览请求VO
 */
public class AiImportPreviewRequestVo {

    /**
     * 已上传的文件路径列表
     */
    private List<FileItemVo> files;

    /**
     * 分类ID
     */
    private BigInteger categoryId;

    public List<FileItemVo> getFiles() {
        return files;
    }

    public void setFiles(List<FileItemVo> files) {
        this.files = files;
    }

    public BigInteger getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(BigInteger categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * 文件项VO
     */
    public static class FileItemVo {
        /**
         * 文件路径（上传后返回的路径）
         */
        private String path;

        /**
         * 原始文件名
         */
        private String originalFileName;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }
    }
}
