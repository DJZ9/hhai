package tech.aiflowy.admin.controller.skill;

import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.skill.entity.BotSkill;
import tech.aiflowy.skill.entity.Skill;
import tech.aiflowy.skill.service.BotSkillService;
import tech.aiflowy.skill.service.SkillService;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Serializable;
import java.util.Collection;


/**
 * Skill 控制器。
 *
 * @author wgq
 * @since 2026-07-21
 */
@RestController
@RequestMapping(value = "/api/v1/skill", produces = MediaType.APPLICATION_JSON_VALUE)
public class SkillController extends BaseCurdController<SkillService, Skill> {

    private static final Logger log = LoggerFactory.getLogger(SkillController.class);

    @Resource
    private BotSkillService botSkillService;

    public SkillController(SkillService service) {
        super(service);
    }

    /**
     * 解析 Skill ZIP 包元数据
     * 只解析 SKILL.md 文件内容，不保存到数据库
     *
     * @param file 上传的 ZIP 文件
     * @return 解析出的 Skill 元数据
     */
    @PostMapping(value = "parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> parse(@RequestParam("file") MultipartFile file) {
        // 验证文件是否为空
        if (file == null || file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }

        // 验证文件是否为 ZIP 格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            return Result.fail("只支持 ZIP 格式的文件");
        }

        try {
            // 调用 Service 层解析 ZIP 文件元数据
            Skill skill = service.parseUpload(file);
            return Result.ok(skill);
        } catch (Exception e) {
            return Result.fail("解析失败：" + e.getMessage());
        }
    }

    /**
     * 保存 Skill（支持新增带文件和编辑重新上传）
     *
     * @param skill Skill 实体（JSON 部分）
     * @param file  上传的 ZIP 文件（新增时必传，编辑时可选）
     * @return 保存结果
     */
    @PostMapping(value = "save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> saveSkill(@RequestPart("skill") Skill skill,
                               @RequestPart(value = "file", required = false) MultipartFile file) {
        try {
            if (skill.getId() == null || skill.getId().toString().isEmpty()) {
                // 新增：必须有文件
                if (file == null || file.isEmpty()) {
                    return Result.fail("新增 Skill 时必须上传 ZIP 文件");
                }
                Skill savedSkill = service.saveWithFile(skill, file);
                return Result.ok(savedSkill);
            } else {
                // 编辑
                if (file != null && !file.isEmpty()) {
                    // 编辑 + 重新上传：删除旧文件 + 上传新文件
                    Skill updatedSkill = service.updateWithFile(skill, file);
                    return Result.ok(updatedSkill);
                } else {
                    // 编辑 + 无文件：仅更新元数据
                    service.updateById(skill);
                    return Result.ok(skill);
                }
            }
        } catch (Exception e) {
            return Result.fail("保存失败：" + e.getMessage());
        }
    }

    /**
     * 下载 Skill ZIP 包
     *
     * @param id       Skill ID
     * @param response HTTP 响应
     */
    @GetMapping("download/{id}")
    public void download(@PathVariable("id") Long id, HttpServletResponse response) {
        try {
            service.downloadSkill(id, response);
        } catch (Exception e) {
            log.error("下载 Skill 失败: id={}", id, e);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":500,\"msg\":\"下载失败：" + e.getMessage() + "\"}");
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 从技能商城安装 Skill
     *
     * @param downloadUrl 技能商城下载 URL
     * @param skill       商城传入的技能元数据
     * @return 安装后的 Skill
     */
    @PostMapping("installFromMarket")
    public Result<?> installFromMarket(@RequestParam String downloadUrl,@RequestBody Skill skill) {
        try {
            if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
                return Result.fail("downloadUrl 不能为空");
            }
            Skill installed = service.installFromMarket(downloadUrl, skill);
            return Result.ok(installed);
        } catch (Exception e) {
            return Result.fail("安装失败：" + e.getMessage());
        }
    }

    @Override
    protected Result<?> onRemoveBefore(Collection<Serializable> ids) {
        // 1. 删除关联的 BotSkill 记录
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.in(BotSkill::getSkillId, ids);
        botSkillService.remove(queryWrapper);

        // 2. 删除存储文件（本地目录或 OSS Artifact）
        if (ids != null) {
            for (Serializable id : ids) {
                try {
                    Skill skill = service.getById(id);
                    if (skill != null) {
                        service.deleteSkillArtifact(skill);
                    }
                } catch (Exception e) {
                    log.error("删除 Skill 存储文件失败: skillId={}", id, e);
                }
            }
        }

        return null;
    }
}