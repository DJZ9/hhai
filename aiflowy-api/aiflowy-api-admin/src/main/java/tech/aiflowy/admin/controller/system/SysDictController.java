package tech.aiflowy.admin.controller.system;

import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.util.SpringContextUtil;

import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.common.dict.DictManager;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.system.entity.SysDict;
import tech.aiflowy.system.service.SysDictService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 系统配置表 控制层。
 *
 * @author michael
 * @since 2024-03-05
 */
@RestController
@RequestMapping("/api/v1/sysDict")
public class SysDictController extends BaseCurdController<SysDictService, SysDict> {

    public SysDictController(SysDictService service) {
        super(service);
    }

    /**
     * 保存 / 更新前校验数据表字典的表名、列名配置。
     * <p>
     * 这些字段会被拼进 SQL，不合法的取值在这里就被拒绝，不会写入 tb_sys_dict，
     * 也不会注册成字典加载器。
     */
    @Override
    protected Result<?> onSaveOrUpdateBefore(SysDict entity, boolean isSave) {
        if (entity == null) {
            return null;
        }
        try {
            entity.checkTableDictOptions();
        } catch (BusinessException e) {
            return Result.fail(e.getMessage());
        }
        return null;
    }

    @Override
    protected void onSaveOrUpdateAfter(SysDict entity, boolean isSave) {
        DictManager dictManager = SpringContextUtil.getBean(DictManager.class);
        dictManager.putLoader(entity.buildLoader());
    }

    @Override
    protected Result onRemoveBefore(Collection<Serializable> ids) {
        List<SysDict> sysDicts = service.list(QueryWrapper.create().in("id", ids));
        if (sysDicts != null) {
            DictManager dictManager = SpringContextUtil.getBean(DictManager.class);
            sysDicts.forEach(sysDict -> dictManager.removeLoader(sysDict.getCode()));
        }
        return null;
    }
}
