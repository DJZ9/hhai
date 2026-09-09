package tech.aiflowy.skill.dto;

import java.util.List;

/**
 * 文件树节点 DTO。
 *
 * @author wgq
 * @since 2025-07-21
 */
public class FileTreeNode {

    /**
     * 文件名/目录名
     */
    private String label;

    /**
     * 子节点
     */
    private List<FileTreeNode> children;

    /**
     * 是否是文件
     */
    private Boolean isFile;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 文件路径
     */
    private String path;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<FileTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<FileTreeNode> children) {
        this.children = children;
    }

    public Boolean getIsFile() {
        return isFile;
    }

    public void setIsFile(Boolean isFile) {
        this.isFile = isFile;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}