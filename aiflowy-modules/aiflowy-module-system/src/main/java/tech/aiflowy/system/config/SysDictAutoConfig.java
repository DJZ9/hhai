package tech.aiflowy.system.config;

import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.dict.DictManager;
import tech.aiflowy.common.dict.loader.DbDataLoader;
import tech.aiflowy.system.entity.SysDict;
import tech.aiflowy.system.mapper.*;
import tech.aiflowy.system.service.SysDictService;
import tech.aiflowy.common.web.exceptions.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import jakarta.annotation.Resource;
import java.util.List;

@Configuration
public class SysDictAutoConfig {

    private static final Logger logger = LoggerFactory.getLogger(SysDictAutoConfig.class);

    private SysDictService service;

    @Resource
    private SysMenuMapper sysMenuMapper;
    @Resource
    private SysDeptMapper sysDeptMapper;
    @Resource
    private SysRoleMapper sysRoleMapper;
    @Resource
    private SysPositionMapper sysPositionMapper;
    @Resource
    private SysAccountMapper sysAccountMapper;

    public SysDictAutoConfig(SysDictService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStartup() {

        DictManager dictManager = SpringContextUtil.getBean(DictManager.class);
        // 菜单表字典
        dictManager.putLoader(new DbDataLoader<>("sysMenu", sysMenuMapper, "id", "menu_title", "parent_id", "sort_no asc", false));
        // 部门表字典
        dictManager.putLoader(new DbDataLoader<>("sysDept", sysDeptMapper, "id", "dept_name", "parent_id", "sort_no asc", false));
        // 角色表字典
        dictManager.putLoader(new DbDataLoader<>("sysRole", sysRoleMapper, "id", "role_name", null, null, true));
        // 职位字典
        dictManager.putLoader(new DbDataLoader<>("sysPosition", sysPositionMapper, "id", "position_name", null, null, true));
        // 用户字典
        dictManager.putLoader(new DbDataLoader<>("sysAccount", sysAccountMapper, "id", "login_name", null, null, true));

        List<SysDict> sysDicts = service.list();
        if (sysDicts != null) {
            sysDicts.forEach(sysDict -> {
                // 库中可能存在历史数据（例如格式不规范的 options），这里只告警不阻止启动；
                // 真正执行 SQL 之前 DatabaseDictLoader 还会再校验一次并直接拒绝。
                try {
                    sysDict.checkTableDictOptions();
                } catch (BusinessException e) {
                    logger.warn("数据表字典 [{}] 配置非法，读取该字典时将被拒绝：{}", sysDict.getCode(), e.getMessage());
                }
                dictManager.putLoader(sysDict.buildLoader());
            });
        }
    }
}
