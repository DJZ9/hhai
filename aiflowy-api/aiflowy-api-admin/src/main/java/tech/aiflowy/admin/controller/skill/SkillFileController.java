package tech.aiflowy.admin.controller.skill;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.skill.service.SkillFileService;

/**
 * Skill 文件管理控制器：文件树、预览/编辑、新增、删除。
 *
 * @author wgq
 * @since 2026-09-28
 */
@RestController
@RequestMapping(value = "/api/v1/skill/file", produces = MediaType.APPLICATION_JSON_VALUE)
public class SkillFileController {

    private final SkillFileService skillFileService;

    public SkillFileController(SkillFileService skillFileService) {
        this.skillFileService = skillFileService;
    }

    /**
     * 获取文件树。
     */
    @GetMapping("tree/{id}")
    public Result<?> tree(@PathVariable Long id) {
        try {
            return Result.ok(skillFileService.getTree(id));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 读取文件内容。
     */
    @GetMapping("content")
    public Result<?> read(@RequestParam Long skillId, @RequestParam String path) {
        try {
            return Result.ok(skillFileService.readFile(skillId, path));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 写入文件内容（不存在则新增）。
     */
    @PostMapping("content")
    public Result<?> write(@RequestBody WriteRequest request) {
        try {
            return Result.ok(skillFileService.writeFile(request.getSkillId(), request.getPath(), request.getContent()));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 删除文件或目录。
     */
    @DeleteMapping("content")
    public Result<?> delete(@RequestParam Long skillId, @RequestParam String path) {
        try {
            return Result.ok(skillFileService.deleteFile(skillId, path));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 写入文件请求体。
     */
    public static class WriteRequest {

        private Long skillId;
        private String path;
        private String content;

        public Long getSkillId() {
            return skillId;
        }

        public void setSkillId(Long skillId) {
            this.skillId = skillId;
        }

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
    }
}