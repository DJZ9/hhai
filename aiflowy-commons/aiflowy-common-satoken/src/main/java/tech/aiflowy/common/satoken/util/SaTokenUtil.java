package tech.aiflowy.common.satoken.util;

import cn.dev33.satoken.session.SaSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tech.aiflowy.common.constant.Constants;
import tech.aiflowy.common.entity.LoginAccount;
import cn.dev33.satoken.stp.StpUtil;
import tech.aiflowy.common.vo.PublicApiEntity;

import java.lang.reflect.Method;
import java.util.Date;

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

    public static void setUpdateField(Object t) {
        Method[] methods = t.getClass().getMethods();
        LoginAccount account = getLoginAccount();
        try {
            for (Method m : methods) {
                String name = m.getName();
                if ("setModifiedBy".equals(name)) {
                    m.invoke(t, account.getId());
                }
                if ("setModified".equals(name)) {
                    m.invoke(t, new Date());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("updateFiled反射出错：" + e.getMessage());
        }
    }

    public static void setCommonField(Object t) {
        Method[] methods = t.getClass().getMethods();
        LoginAccount account = getLoginAccount();
        try {
            for (Method m : methods) {
                String name = m.getName();
                if ("setDeptId".equals(name)) {
                    m.invoke(t, account.getDeptId());
                }
                if ("setTenantId".equals(name)) {
                    m.invoke(t, account.getTenantId());
                }
                if ("setCreatedBy".equals(name)) {
                    m.invoke(t, account.getId());
                }
                if ("setModifiedBy".equals(name)) {
                    m.invoke(t, account.getId());
                }
                if ("setCreated".equals(name)) {
                    m.invoke(t, new Date());
                }
                if ("setModified".equals(name)) {
                    m.invoke(t, new Date());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("commonFiled反射出错：" + e.getMessage());
        }
    }
}
