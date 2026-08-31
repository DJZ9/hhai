package tech.aiflowy.admin.controller.auth;

import org.springframework.web.bind.annotation.GetMapping;
import tech.aiflowy.auth.entity.LoginDTO;
import tech.aiflowy.auth.entity.LoginVO;
import tech.aiflowy.auth.service.AuthService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.jsonbody.JsonBody;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/")
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("login")
    public Result<LoginVO> login(@JsonBody LoginDTO loginDTO) {
        LoginVO res = authService.login(loginDTO);
        return Result.ok(res);
    }

    @PostMapping("logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @GetMapping("getPermissions")
    public Result<List<String>> getPermissions() {
        List<String> permissionList = StpUtil.getPermissionList();
        return Result.ok(permissionList);
    }
}
