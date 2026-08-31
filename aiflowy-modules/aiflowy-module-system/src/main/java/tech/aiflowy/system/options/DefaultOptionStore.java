package tech.aiflowy.system.options;

import com.mybatisflex.core.query.QueryWrapper;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.options.SysOptionStore;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.system.entity.SysOption;
import tech.aiflowy.system.service.SysOptionService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class DefaultOptionStore implements SysOptionStore {

    @Resource
    private SysOptionService optionService;


    @Override
    public void save(String key, Object value) {
        if (value == null || !StringUtil.hasText(value.toString())) {
            optionService.remove(QueryWrapper.create().eq(SysOption::getKey, key));
            return;
        }

        String newValue = value.toString().trim();
        SysOption option = optionService.getByOptionKey(key);
        LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
        if (option == null) {
            option = new SysOption(key, newValue);
            option.setTenantId(loginAccount.getTenantId());
            optionService.save(option);
        } else {
            option.setValue(newValue);
            QueryWrapper queryWrapper = QueryWrapper.create().eq(SysOption::getKey, key);
            optionService.update(option, queryWrapper);
        }
    }

    @Override
    public String get(String key) {
        SysOption option = optionService.getById(key);
        return option != null ? option.getValue() : null;
    }
}
