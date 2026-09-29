package tech.aiflowy.skill.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import tech.aiflowy.common.web.exceptions.BusinessException;

/**
 * Skill 文件访问工具：路径安全解析、二进制识别、文本读写、删除与大小守卫。
 *
 * @author wgq
 * @since 2026-09-28
 */
@Component
public class SkillFileAccessor {

    /** 允许读取的单文件最大字节数，避免大文件占用过多内存 */
    private static final long MAX_TEXT_SIZE = 2L * 1024 * 1024;

    /** 禁止删除的根标记（空路径） */
    private static final String ROOT_MARK = ".";

    /** 禁止删除的 SKILL.md 文件名 */
    private static final String SKILL_MD = "SKILL.md";

    /**
     * 在根目录下安全解析相对路径，防止路径穿越。
     *
     * @param root    技能根目录
     * @param relPath 相对路径
     * @return 规范化后的目标路径
     */
    public Path resolve(Path root, String relPath) {
        if (relPath == null || relPath.trim().isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }
        Path rel;
        try {
            rel = root.getFileSystem().getPath(relPath).normalize();
        } catch (Exception e) {
            throw new BusinessException("非法文件路径");
        }
        if (rel.isAbsolute()) {
            throw new BusinessException("非法文件路径");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path target = normalizedRoot.resolve(rel).normalize();
        if (!target.startsWith(normalizedRoot)) {
            throw new BusinessException("非法文件路径");
        }
        return target;
    }

    /**
     * 读取文件全部字节，超出大小上限时抛出业务异常。
     *
     * @param file 目标文件
     * @return 文件字节
     * @throws IOException IO 异常
     */
    public byte[] readBytes(Path file) throws IOException {
        long size = Files.size(file);
        if (size > MAX_TEXT_SIZE) {
            throw new BusinessException("文件过大，无法预览/编辑:" + file.getFileName());
        }
        return Files.readAllBytes(file);
    }

    /**
     * 判断字节是否为二进制（存在 NUL 字节则认为二进制）。
     *
     * @param bytes 文件字节
     * @return 是否二进制
     */
    public boolean isBinary(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        for (int i = 0; i < bytes.length && i < 8192; i++) {
            if (bytes[i] == 0x00) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将字节解码为 UTF-8 文本（非法字节替换，不抛错）。
     *
     * @param bytes 文件字节
     * @return 文本内容
     */
    public String decodeText(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 写入文本到文件，父目录不存在时自动创建（支持新增文件）。
     *
     * @param file    目标文件
     * @param content 文本内容
     * @throws IOException IO 异常
     */
    public void writeText(Path file, String content) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, (content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 校验路径是否可删除：禁止删除根目录与 SKILL.md。
     *
     * @param relPath 相对路径
     */
    public void ensureDeletable(String relPath) {
        if (relPath == null || relPath.trim().isEmpty() || ROOT_MARK.equals(relPath.trim())) {
            throw new BusinessException("不能删除技能根目录");
        }
        String base = Paths.get(relPath.trim()).getFileName().toString();
        if (SKILL_MD.equalsIgnoreCase(base)) {
            throw new BusinessException("不能删除 SKILL.md 文件");
        }
    }

    /**
     * 删除文件或递归删除目录。
     *
     * @param target 目标文件/目录
     * @throws IOException IO 异常
     */
    public void deleteNode(Path target) throws IOException {
        if (Files.isDirectory(target)) {
            try (Stream<Path> walk = Files.walk(target)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException e) {
                        throw new RuntimeException("删除文件失败: " + p, e);
                    }
                });
            }
        } else {
            Files.deleteIfExists(target);
        }
    }
}