package tech.aiflowy.skill.service;

import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import tech.aiflowy.skill.dto.FileTreeNode;
import tech.aiflowy.skill.entity.Skill;

import java.io.File;
import java.util.List;

/**
 * 技能 服务层。
 *
 * @author wgq
 * @since 2026-07-21
 */
public interface SkillService extends IService<Skill> {

    /**
     * 解析上传的 ZIP 文件元数据（不保存到数据库）
     *
     * @param file 上传的 ZIP 文件
     * @return 解析出的 Skill 元数据
     * @throws Exception 文件解析异常
     */
    Skill parseUpload(MultipartFile file) throws Exception;

    /**
     * 保存 Skill（处理临时路径移动到正式路径）
     *
     * @param skill Skill 实体（包含临时路径）
     * @return 保存后的 Skill
     * @throws Exception 保存异常
     */
    Skill saveWithTempPath(Skill skill) throws Exception;

    /**
     * 上传并解压 Skill
     *
     * @param file 上传的 ZIP 文件
     * @param skill Skill 实体对象
     * @return 更新后的 Skill 对象
     * @throws Exception 文件处理异常
     */
    Skill uploadAndExtract(MultipartFile file, Skill skill) throws Exception;

    /**
     * 解析 SKILL.md 文件
     *
     * @param skillDir Skill 目录
     * @return 更新后的 Skill 对象
     * @throws Exception 文件解析异常
     */
    Skill parseSkillMd(File skillDir) throws Exception;

    /**
     * 删除 Skill 目录
     *
     * @param skillPath Skill 目录路径
     */
    void deleteSkillDirectory(String skillPath);

    /**
     * 获取 Skill 文件树
     *
     * @param skillId Skill ID
     * @return 文件树节点列表
     */
    List<FileTreeNode> getSkillFiles(Long skillId);

    /**
     * 删除 Skill 对应的存储文件（本地目录或 OSS 对象）
     *
     * @param skill Skill 实体
     */
    void deleteSkillArtifact(Skill skill);

    /**
     * 下载 Skill 文件为 ZIP 包
     *
     * @param skillId  Skill ID
     * @param response HTTP 响应
     * @throws Exception 下载异常
     */
    void downloadSkill(Long skillId, HttpServletResponse response) throws Exception;

    /**
     * 新增 Skill（带文件上传）
     *
     * @param skill Skill 实体
     * @param file  上传的 ZIP 文件
     * @return 保存后的 Skill
     * @throws Exception 保存异常
     */
    Skill saveWithFile(Skill skill, MultipartFile file) throws Exception;

    /**
     * 编辑 Skill（重新上传文件）
     *
     * @param skill Skill 实体（含 ID）
     * @param file  新的 ZIP 文件
     * @return 更新后的 Skill
     * @throws Exception 更新异常
     */
    Skill updateWithFile(Skill skill, MultipartFile file) throws Exception;

    /**
     * 从技能商城 URL 安装技能
     *
     * @param downloadUrl 技能商城下载 URL
     * @param skill       商城传入的技能元数据
     * @throws Exception 安装异常
     */
    Skill installFromMarket(String downloadUrl, Skill skill) throws Exception;
}