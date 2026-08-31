package tech.aiflowy.common.satoken.util;

import cn.dev33.satoken.session.SaSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tech.aiflowy.common.constant.Constants;
import tech.aiflowy.common.entity.LoginAccount;
import cn.dev33.satoken.stp.StpUtil;
import tech.aiflowy.common.vo.PublicApiEntity;

public class SaTokenUtil {

    public static LoginAccount getLoginAccount() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No current HTTP request");
        }
        // 如果不为空，说明是public api的请求
        Object attribute = attributes.getRequest().getSession().getAttribute(Constants.CURRENT_KEY);
        if (attribute != null) {
            PublicApiEntity apiEntity = (PublicApiEntity) attribute;
            LoginAccount loginAccount = new LoginAccount();
            loginAccount.setId(apiEntity.getCreatedBy());
            loginAccount.setDeptId(apiEntity.getDeptId());
            loginAccount.setTenantId(apiEntity.getTenantId());
            loginAccount.setLoginName(apiEntity.getApiKey());
            return loginAccount;
        }
        SaSession session = StpUtil.getSession();
        return session.getModel(Constants.LOGIN_USER_KEY, LoginAccount.class);
    }
}
