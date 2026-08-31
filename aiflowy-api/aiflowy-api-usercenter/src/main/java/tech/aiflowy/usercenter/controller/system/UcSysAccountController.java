package tech.aiflowy.usercenter.controller.system;

import cn.hutool.crypto.digest.BCrypt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.web.jsonbody.JsonBody;
import tech.aiflowy.system.entity.SysAccount;
import tech.aiflowy.system.service.SysAccountService;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.Date;

/**
 * 用户
 *
 * @author ArkLight
 * @since 2025-03-14
 */
@RestController
@RequestMapping("/userCenter/sysAccount")
public class UcSysAccountController {

    @Resource
    private SysAccountService service;

    /**
     * 获取用户的信息
     */
    @GetMapping("/myProfile")
    public Result<SysAccount> myProfile() {
        LoginAccount account = SaTokenUtil.getLoginAccount();
        SysAccount sysAccount = service.getById(account.getId());
        return Result.ok(sysAccount);
    }

    /**
     * 修改用户信息
     */
    @PostMapping("/updateProfile")
    public Result<Void> updateProfile(@JsonBody SysAccount account) {
        BigInteger loginAccountId = SaTokenUtil.getLoginAccount().getId();
        SysAccount update = new SysAccount();
        update.setId(loginAccountId);
        update.setNickname(account.getNickname());
        update.setMobile(account.getMobile());
        update.setEmail(account.getEmail());
        update.setAvatar(account.getAvatar());
        update.setModified(new Date());
        update.setModifiedBy(loginAccountId);
        service.updateById(update);
        return Result.ok();
    }

    /**
     * 修改密码
     *
     * @param password        用户的旧密码
     * @param newPassword     新密码
     * @param confirmPassword 确认密码
     */
    @PostMapping("/updatePassword")
    public Result<Void> updatePassword(@JsonBody(value = "password", required = true) String password,
                                       @JsonBody(value = "newPassword", required = true) String newPassword,
                                       @JsonBody(value = "confirmPassword", required = true) String confirmPassword) {
        BigInteger loginAccountId = SaTokenUtil.getLoginAccount().getId();
        SysAccount record = service.getById(loginAccountId);
        if (record == null) {
            return Result.fail("修改失败");
        }
        String pwdDb = record.getPassword();
        if (!BCrypt.checkpw(password, pwdDb)) {
            return Result.fail(1, "密码不正确");
        }
        if (!newPassword.equals(confirmPassword)) {
            return Result.fail(2, "两次密码不一致");
        }
        SysAccount update = new SysAccount();
        update.setId(loginAccountId);
        update.setPassword(BCrypt.hashpw(newPassword));
        update.setModified(new Date());
        update.setModifiedBy(loginAccountId);
        service.updateById(update);
        return Result.ok();
    }
}