package tech.aiflowy.usercenter.controller.auth;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.bind.annotation.*;
import tech.aiflowy.auth.entity.LoginDTO;
import tech.aiflowy.auth.entity.LoginVO;
import tech.aiflowy.auth.service.AuthService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.jsonbody.JsonBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 认证
 */
@RestController
@RequestMapping("/userCenter/auth/")
public class UcAuthController {

    @Resource
    private AuthService authService;

    /**
     * 登录
     * @param loginDTO 登录参数
     */
    @PostMapping("login")
    public Result<LoginVO> login(@JsonBody LoginDTO loginDTO) {
        LoginVO res = authService.login(loginDTO);
        return Result.ok(res);
    }

    /**
     * 登出
     */
    @PostMapping("logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    /**
     * 获取权限
     */
    @GetMapping("getPermissions")
    public Result<List<String>> getPermissions() {
        List<String> permissionList = StpUtil.getPermissionList();
        return Result.ok(permissionList);
    }
}
