package tech.aiflowy.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.options.SysOptions;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.web.controller.BaseController;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.common.web.jsonbody.JsonBody;
import tech.aiflowy.system.entity.SysOption;
import tech.aiflowy.system.service.SysOptionService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置信息表。 控制层。
 *
 * @author michael
 * @since 2024-03-13
 */
@RestController
@RequestMapping("/api/v1/sysOption")
public class SysOptionController extends BaseController {

    @Resource
    private SysOptionService service;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(String[] keys) {
        Map<String, Object> data = new HashMap<>();
        if (keys == null || keys.length == 0) {
            return Result.ok(data);
        }
        List<SysOption> list = service.list(QueryWrapper.create().in(SysOption::getKey, (Object[]) keys));
        for (SysOption sysOption : list) {
            data.put(sysOption.getKey(), sysOption.getValue());
        }
        return Result.ok(data);
    }

    @PostMapping("/save")
    public Result<Void> save(@JsonBody Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return Result.ok();
        }
        map.forEach(SysOptions::set);
        return Result.ok();
    }

    @PostMapping("/saveOption")
    @SaCheckPermission("/api/v1/sysOption/save")
    public Result<Void> saveOption(@JsonBody SysOption sysOption) {
        String key = sysOption.getKey();
        if (key == null || key.isEmpty()) {
            throw new BusinessException("key is empty");
        }
        sysOption.setTenantId(SaTokenUtil.getLoginAccount().getTenantId());
        SysOption record = service.getByOptionKey(key);
        if (record == null) {
            service.save(sysOption);
        } else {
            QueryWrapper w = QueryWrapper.create();
            w.eq(SysOption::getKey, key);
            service.update(sysOption, w);
        }
        return Result.ok();
    }

    @GetMapping("/getByKey")
    public Result<SysOption> getByKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new BusinessException("key is empty");
        }
        return Result.ok(service.getByOptionKey(key));
    }
}