package tech.aiflowy.skill.dto;

/**
 * Skill 文件内容 DTO。
 *
 * @author wgq
 * @since 2026-09-28
 */
public class SkillFileContent {

    /**
     * 文件相对路径（以技能根目录为基准）
     */
    private String path;

    /**
     * 文件文本内容（isBinary 为 true 时为 null）
     */
    private String content;

    /**
     * 是否为二进制文件
     */
    private Boolean isBinary;

    /**
     * 文件大小（字节）
     */
    private Long size;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsBinary() {
        return isBinary;
    }

    public void setIsBinary(Boolean isBinary) {
        this.isBinary = isBinary;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}