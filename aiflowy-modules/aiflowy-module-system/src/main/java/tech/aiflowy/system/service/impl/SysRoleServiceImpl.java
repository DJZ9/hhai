package tech.aiflowy.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.aiflowy.common.constant.enums.EnumDataScope;
import tech.aiflowy.system.entity.SysAccountRole;
import tech.aiflowy.system.entity.SysRole;
import tech.aiflowy.system.entity.SysRoleDept;
import tech.aiflowy.system.entity.SysRoleMenu;
import tech.aiflowy.system.mapper.SysRoleDeptMapper;
import tech.aiflowy.system.mapper.SysRoleMapper;
import tech.aiflowy.system.mapper.SysRoleMenuMapper;
import tech.aiflowy.system.service.SysAccountRoleService;
import tech.aiflowy.system.service.SysRoleService;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色 服务层实现。
 *
 * @author ArkLight
 * @since 2025-03-14
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Resource
    private SysAccountRoleService sysAccountRoleService;
    @Resource
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Resource
    private SysRoleDeptMapper sysRoleDeptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleMenu(BigInteger roleId, List<String> keys) {

        QueryWrapper delW = QueryWrapper.create();
        delW.eq(SysRoleMenu::getRoleId, roleId);
        sysRoleMenuMapper.deleteByQuery(delW);

        List<SysRoleMenu> rows = new ArrayList<>(keys.size());
        keys.forEach(string -> {
            SysRoleMenu row = new SysRoleMenu();
            row.setRoleId(roleId);
            row.setMenuId(new BigInteger(string));
            rows.add(row);
        });
        sysRoleMenuMapper.insertBatch(rows);
    }

    @Override
    public List<SysRole> getRolesByAccountId(BigInteger accountId) {
        // 查询用户对应角色id集合
        QueryWrapper am = QueryWrapper.create();
        am.eq(SysAccountRole::getAccountId, accountId);
        List<BigInteger> roleIds = sysAccountRoleService.list(am).stream().map(SysAccountRole::getRoleId).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        return listByIds(roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRole(SysRole sysRole) {

        saveOrUpdate(sysRole);

        // 非自定义数据权限，则部门id集合为空
        if (!EnumDataScope.CUSTOM.getCode().equals(sysRole.getDataScope())) {
            sysRole.setDeptIds(new ArrayList<>());
        }

        List<BigInteger> menuIds = sysRole.getMenuIds();
        List<BigInteger> deptIds = sysRole.getDeptIds();

        QueryWrapper wrm = QueryWrapper.create();
        wrm.eq(SysRoleMenu::getRoleId, sysRole.getId());
        sysRoleMenuMapper.deleteByQuery(wrm);
        QueryWrapper wrd = QueryWrapper.create();
        wrd.eq(SysRoleDept::getRoleId, sysRole.getId());
        sysRoleDeptMapper.deleteByQuery(wrd);

        if (CollectionUtil.isNotEmpty(menuIds)) {
            for (BigInteger menuId : menuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(sysRole.getId());
                roleMenu.setMenuId(menuId);
                sysRoleMenuMapper.insert(roleMenu);
            }
        }

        if (CollectionUtil.isNotEmpty(deptIds)) {
            for (BigInteger deptId : deptIds) {
                SysRoleDept roleDept = new SysRoleDept();
                roleDept.setRoleId(sysRole.getId());
                roleDept.setDeptId(deptId);
                sysRoleDeptMapper.insert(roleDept);
            }
        }
    }
}
