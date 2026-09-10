package tech.aiflowy.usercenter.controller.auth;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alicp.jetcache.Cache;
import com.mybatisflex.core.tenant.TenantManager;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import tech.aiflowy.ai.service.userProfileService.DataScopeManager;
import tech.aiflowy.common.constant.CacheKey;
import tech.aiflowy.common.constant.Constants;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.third.auth.core.OAuthRequest;
import tech.aiflowy.common.third.auth.entity.OAuthConfigVo;
import tech.aiflowy.common.third.auth.entity.OAuthPlatformVo;
import tech.aiflowy.common.third.auth.entity.PlatformUser;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.system.entity.SysAccount;
import tech.aiflowy.system.entity.SysAccountOpenid;
import tech.aiflowy.system.entity.SysOption;
import tech.aiflowy.system.service.SysAccountOpenidService;
import tech.aiflowy.system.service.SysAccountService;
import tech.aiflowy.system.service.SysOptionService;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/userCenter/thirdAuth")
public class UcThirdAuthController {

    private static final Logger log = LoggerFactory.getLogger(UcThirdAuthController.class);
    @Resource
    private SysOptionService sysOptionService;
    @Resource(name = "defaultCache")
    private Cache<String, Object> defaultCache;
    @Value("${aiflowy.front-page-domain}")
    public String adminPageDomain;
    @Resource
    private SysAccountService accountService;
    @Resource
    private SysAccountOpenidService openidService;

    /**
     * 获取当前用户已绑定的第三方账号列表
     */
    @GetMapping("/getBoundAccounts")
    public Result<List<SysAccountOpenid>> getBoundAccounts() {
        String loginIdAsString = StpUtil.getLoginIdAsString();
        if (!StringUtil.hasText(loginIdAsString)) {
            throw new BusinessException("用户未登录");
        }
        BigInteger accountId = new BigInteger(loginIdAsString);
        List<SysAccountOpenid> boundAccounts = openidService.findByAccountId(accountId);
        return Result.ok(boundAccounts);
    }

    /**
     * 解绑第三方账号
     */
    @PostMapping("/unbind/{platform}")
    public Result<Void> unbindAccount(@PathVariable String platform) {
        String loginIdAsString = StpUtil.getLoginIdAsString();
        if (!StringUtil.hasText(loginIdAsString)) {
            throw new BusinessException("用户未登录");
        }
        BigInteger accountId = new BigInteger(loginIdAsString);

        // 查找该平台下的绑定记录
        List<SysAccountOpenid> boundAccounts = openidService.findByAccountId(accountId);
        SysAccountOpenid toDelete = null;
        for (SysAccountOpenid account : boundAccounts) {
            if (platform.equals(account.getOpenidType())) {
                toDelete = account;
                break;
            }
        }

        if (toDelete == null) {
            throw new BusinessException("未找到该平台的绑定记录");
        }

        openidService.removeById(toDelete.getId());
        return Result.ok();
    }

    /**
     * 获取第三方平台简要列表（仅含 id、platform、displayName，无敏感信息）
     */
    @GetMapping("/getPlatformList")
    public Result<List<OAuthPlatformVo>> getPlatformList() {
        SysOption oauthConfig = sysOptionService.getByOptionKey("oauthConfig");
        List<OAuthPlatformVo> res = new ArrayList<>();
        if (oauthConfig != null && StringUtil.hasLength(oauthConfig.getValue())) {
            JSONArray array = JSON.parseArray(oauthConfig.getValue());
            for (Object o : array) {
                JSONObject obj = (JSONObject) o;
                OAuthPlatformVo vo = new OAuthPlatformVo();
                vo.setId(obj.getString("id"));
                vo.setPlatform(obj.getString("platform"));
                vo.setDisplayName(obj.getString("displayName"));
                res.add(vo);
            }
        }
        return Result.ok(res);
    }

    @GetMapping("/getOAuthList")
    public Result<List<OAuthConfigVo>> getOAuthList() {
        SysOption oauthConfig = sysOptionService.getByOptionKey("oauthConfig");
        List<OAuthConfigVo> res = new ArrayList<>();
        if (oauthConfig != null && StringUtil.hasLength(oauthConfig.getValue())) {
            JSONArray array = JSON.parseArray(oauthConfig.getValue());

            // 获取当前登录用户ID（用于绑定场景）
            String currentAccountId = null;
            try {
                String loginIdAsString = StpUtil.getLoginIdAsString();
                if (StringUtil.hasText(loginIdAsString)) {
                    currentAccountId = loginIdAsString;
                }
            } catch (Throwable e) {
                // ignore
            }

            for (Object o : array) {
                JSONObject obj = (JSONObject) o;
                Integer enabled = obj.getInteger("enabled");
                if (enabled == null || enabled == 0) {
                    continue;
                }
                // 使用用户端回调地址替换默认的 redirectUrl
                String userCenterRedirectUrl = obj.getString("userCenterRedirectUrl");
                if (StringUtil.hasLength(userCenterRedirectUrl)) {
                    obj.put("redirectUrl", userCenterRedirectUrl);
                }
                OAuthRequest request = new OAuthRequest(obj);
                OAuthConfigVo vo = new OAuthConfigVo();
                String platform = obj.getString("platform");
                vo.setPlatform(platform);
                vo.setDisplayName(obj.getString("displayName"));
                vo.setIcon(obj.getString("icon"));
                String state = UUID.randomUUID().toString().replaceAll("-", "");
                // 缓存state有效性，3小时过期
                defaultCache.put(CacheKey.OAUTH_STATE_KEY + platform + ":" + state, state, 3, TimeUnit.HOURS);
                // 如果用户已登录，额外缓存accountId到bind key，回调时用于获取绑定用户
                if (currentAccountId != null) {
                    defaultCache.put(CacheKey.OAUTH_STATE_KEY + platform + ":bind:" + state, currentAccountId, 3, TimeUnit.HOURS);
                }
                vo.setAuthUrl(request.getAuthUrl(state));
                res.add(vo);
            }
        }
        return Result.ok(res);
    }

    /**
     * 回调-登录-重定向前端
     */
    @GetMapping("/callback/{platform}")
    @SaIgnore
    public void callback(@PathVariable String platform, @RequestParam Map<String, Object> map, HttpServletResponse response) throws Exception {
        try {
            DataScopeManager.ignoreDataScopeCondition();
            TenantManager.ignoreTenantCondition();
            JSONObject callback = new JSONObject(map);
            String state = callback.getString("state");
            Object cache = defaultCache.get(CacheKey.OAUTH_STATE_KEY + platform + ":" + state);
            if (cache == null) {
                log.error("【{}】 ---> 无效授权：{}", platform, callback);
                writeErrorPage(response, "授权已过期", "授权链接已过期或无效，请返回登录页重新操作。", adminPageDomain + "auth/login");
                return;
            }

            // 判断是否为绑定场景：检查bind缓存中是否有accountId
            Object bindCache = defaultCache.get(CacheKey.OAUTH_STATE_KEY + platform + ":bind:" + state);
            BigInteger currentAccountId = null;
            boolean isBindScenario = false;
            if (bindCache != null) {
                isBindScenario = true;
                currentAccountId = new BigInteger(bindCache.toString());
                log.info("从缓存获取到绑定用户accountId: {}", currentAccountId);
            }

            SysOption oauthConfig = sysOptionService.getByOptionKey("oauthConfig");
            if (oauthConfig == null || !StringUtil.hasLength(oauthConfig.getValue())) {
                writeErrorPage(response, "配置错误", "系统未配置第三方登录，请联系管理员。", adminPageDomain + "auth/login");
                return;
            }
            JSONArray array = JSON.parseArray(oauthConfig.getValue());
            JSONObject config = null;
            for (Object o : array) {
                JSONObject obj = (JSONObject) o;
                String configPlatform = obj.getString("platform");
                if (platform.equals(configPlatform)) {
                    config = obj;
                    break;
                }
            }
            if (config == null) {
                writeErrorPage(response, "配置错误", "未找到平台【" + platform + "】的配置信息。", adminPageDomain + "auth/login");
                return;
            }
            OAuthRequest request = new OAuthRequest(config);
            JSONObject result = request.getAccessToken(callback);
            PlatformUser userInfo = request.getUserInfo(result);
            String userKey = userInfo.getUserKey();
            if (StringUtil.noText(userKey)) {
                log.error("userKey 无法获取: Config: {}, remote: {}", config, result);
                writeErrorPage(response, "认证失败", "获取第三方用户信息失败，请稍后重试。", adminPageDomain + "auth/login");
                return;
            }

            SysAccountOpenid sysAccountOpenid = openidService.findByOpenId(userKey, platform);
            if (sysAccountOpenid == null) {
                if ("wx_service_account".equals(platform)) {
                    sysAccountOpenid = openidService.findByOpenId(userKey, "wx_open");
                }
            }
            // 绑定场景处理
            if (isBindScenario) {

                if (sysAccountOpenid != null) {
                    // 该第三方账号已被绑定过
                    if (sysAccountOpenid.getAccountId().equals(currentAccountId)) {
                        // 已绑定到当前用户，提示但不阻止绑定其他账号
                        response.sendRedirect(adminPageDomain + "/personalCenter?bindStatus=alreadyBoundToSelf");
                        return;
                    } else {
                        // 已绑定到其他用户，拒绝绑定
                        writeErrorPage(response, "账号已被占用",
                                "该第三方账号已被其他用户绑定，请使用其他账号。", adminPageDomain + "personalCenter");
                        return;
                    }
                }

                // 创建新的绑定记录
                sysAccountOpenid = new SysAccountOpenid();
                sysAccountOpenid.setAccountId(currentAccountId);
                sysAccountOpenid.setOpenidType(platform);
                sysAccountOpenid.setOpenidValue(userKey);
                sysAccountOpenid.setNickname(userInfo.getNickname());
                sysAccountOpenid.setAvatar(userInfo.getAvatar());
                sysAccountOpenid.setCreated(new Date());
                sysAccountOpenid.setModified(new Date());
                openidService.save(sysAccountOpenid);

                // 绑定成功，重定向到个人中心
                response.sendRedirect(adminPageDomain + "/personalCenter?bindStatus=success");
                return;
            }

            // 登录场景处理
            if (sysAccountOpenid == null || sysAccountOpenid.getAccountId() == null) {
                writeErrorPage(response, "账号未绑定",
                        "该第三方账号尚未绑定系统用户，请先在个人中心完成绑定后再登录。", adminPageDomain + "auth/login");
                return;
            }
            // 正常登录流程
            SysAccount account = accountService.getById(sysAccountOpenid.getAccountId());
            if (account == null) {
                writeErrorPage(response, "用户不存在", "绑定的系统用户不存在。", adminPageDomain + "auth/login");
                return;
            }
            StpUtil.login(account.getId());
            StpUtil.getSession().set(Constants.LOGIN_USER_KEY, account.toLoginAccount());
            String tokenValue = StpUtil.getTokenValue();
            response.sendRedirect(adminPageDomain + "/oauth?token=" + tokenValue);
        }
        finally {
            DataScopeManager.restoreDataScopeCondition();
            TenantManager.restoreTenantCondition();
        }

    }

    /**
     * 输出友好的HTML错误页面
     */
    private void writeErrorPage(HttpServletResponse response, String title, String message, String backUrl) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.println("<!DOCTYPE html>");
        writer.println("<html lang=\"zh-CN\">");
        writer.println("<head>");
        writer.println("<meta charset=\"UTF-8\">");
        writer.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        writer.println("<title>" + title + "</title>");
        writer.println("<style>");
        writer.println("  *{margin:0;padding:0;box-sizing:border-box}");
        writer.println("  body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#f5f7fa;display:flex;justify-content:center;align-items:center;min-height:100vh}");
        writer.println("  .card{background:#fff;border-radius:12px;box-shadow:0 4px 24px rgba(0,0,0,0.08);padding:48px 40px;text-align:center;max-width:420px;width:90%}");
        writer.println("  .icon{width:64px;height:64px;border-radius:50%;background:#fef0f0;color:#f56c6c;display:flex;align-items:center;justify-content:center;font-size:32px;margin:0 auto 24px}");
        writer.println("  h2{font-size:20px;color:#303133;margin-bottom:12px}");
        writer.println("  p{font-size:14px;color:#909399;margin-bottom:32px;line-height:1.6}");
        writer.println("  .btn{display:inline-block;background:#409eff;color:#fff;border:none;border-radius:6px;padding:10px 32px;font-size:14px;text-decoration:none;cursor:pointer;transition:background .2s}");
        writer.println("  .btn:hover{background:#337ecc}");
        writer.println("</style>");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("<div class=\"card\">");
        writer.println("  <div class=\"icon\">&#10071;</div>");
        writer.println("  <h2>" + title + "</h2>");
        writer.println("  <p>" + message + "</p>");
        writer.println("  <a href=\"" + backUrl + "\" class=\"btn\">返回登录</a>");
        writer.println("</div>");
        writer.println("</body>");
        writer.println("</html>");
        writer.flush();
    }

}
