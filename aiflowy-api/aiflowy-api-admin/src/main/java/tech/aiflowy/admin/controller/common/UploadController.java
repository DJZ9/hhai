package tech.aiflowy.admin.controller.common;

import cn.dev33.satoken.annotation.SaIgnore;
import tech.aiflowy.common.domain.Result;

import tech.aiflowy.common.vo.UploadResVo;
import tech.aiflowy.common.filestorage.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/api/v1/commons/")
public class UploadController {

    @Resource(name = "default")
    FileStorageService storageService;

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<UploadResVo> upload(MultipartFile file) {
        String path = storageService.save(file);
        UploadResVo resVo = new UploadResVo();
        resVo.setPath(path);
        return Result.ok(resVo);
    }

    @PostMapping(value = "/uploadAntd", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<UploadResVo> uploadAntd(MultipartFile file) {
        String path = storageService.save(file);
        UploadResVo resVo = new UploadResVo();
        resVo.setPath(path);
        return Result.ok(resVo);
    }

    /**
     * 临时文件上传（用于定时清理）
     */
    @PostMapping(value = "/uploadTemp", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<UploadResVo> uploadTemp(MultipartFile file) {
        String path = storageService.saveTemp(file);
        UploadResVo resVo = new UploadResVo();
        resVo.setPath(path);
        return Result.ok(resVo);
    }

}
