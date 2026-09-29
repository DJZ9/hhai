package tech.aiflowy.skill.support;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import tech.aiflowy.skill.dto.FileTreeNode;

/**
 * Skill 目录转文件树的构建器。
 *
 * @author wgq
 * @since 2026-09-28
 */
@Component
public class SkillFileTreeBuilder {

    /**
     * 将目录构建为文件树，目录优先、按名称排序。
     *
     * @param rootDir 技能根目录
     * @return 文件树节点列表
     */
    public List<FileTreeNode> build(File rootDir) {
        List<FileTreeNode> tree = new ArrayList<>();
        if (rootDir == null || !rootDir.exists()) {
            return tree;
        }
        buildFileTree(rootDir, rootDir, tree);
        return tree;
    }

    private void buildFileTree(File root, File current, List<FileTreeNode> nodes) {
        File[] files = current.listFiles();
        if (files == null) {
            return;
        }
        List<File> sorted = new ArrayList<>();
        for (File file : files) {
            sorted.add(file);
        }
        sorted.sort(Comparator.comparing(File::isFile).thenComparing(File::getName));

        for (File file : sorted) {
            FileTreeNode node = new FileTreeNode();
            node.setLabel(file.getName());
            node.setIsFile(file.isFile());
            if (file.isFile()) {
                node.setSize(file.length());
            }
            node.setPath(root.toPath().relativize(file.toPath()).toString().replace('\\', '/'));
            if (file.isDirectory()) {
                node.setChildren(new ArrayList<>());
                buildFileTree(root, file, node.getChildren());
            }
            nodes.add(node);
        }
    }
}