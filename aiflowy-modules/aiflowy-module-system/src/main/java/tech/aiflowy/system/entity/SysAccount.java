package tech.aiflowy.system.entity;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.Table;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.system.entity.base.SysAccountBase;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户表 实体类。
 *
 * @author ArkLight
 * @since 2025-03-14
 */

@Table(value = "tb_sys_account", comment = "用户表")
public class SysAccount extends SysAccountBase {

    @RelationManyToMany(joinTable = "tb_sys_account_role"
            , joinSelfColumn = "account_id"
            , joinTargetColumn = "role_id"
            , targetTable = "tb_sys_role"
            , targetField = "id"
            , valueField = "id"
    )
    private List<BigInteger> roleIds;

    @RelationManyToMany(joinTable = "tb_sys_account_position"
            , joinSelfColumn = "account_id"
            , joinTargetColumn = "position_id"
            , targetTable = "tb_sys_position"
            , targetField = "id"
            , valueField = "id"
    )
    private List<BigInteger> positionIds;

    public List<BigInteger> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<BigInteger> roleIds) {
        this.roleIds = roleIds;
    }

    public List<BigInteger> getPositionIds() {
        return positionIds;
    }

    public void setPositionIds(List<BigInteger> positionIds) {
        this.positionIds = positionIds;
    }

    @Override
    @JSONField(serialize = false)
    public String getPassword() {
        return super.getPassword();
    }

    /**
     * 转为登录用户
     */
    public LoginAccount toLoginAccount() {
        LoginAccount loginAccount = new LoginAccount();
        BeanUtil.copyProperties(this, loginAccount);
        return loginAccount;
    }

    /**
     * 获取元数据的键值对 Map
     * 解析 metaData Map，提取 value 字段，忽略 description
     *
     * @return 键值对 Map，key 为变量名，value 为变量值
     */
    public Map<String, Object> getMetadataMap() {
        Map<String, Object> metaDataMap = getMetaData();
        if (metaDataMap == null || metaDataMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : metaDataMap.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Map) {
                // 格式: {"key": {"value": "xxx", "description": "yyy"}}
                Map<?, ?> valMap = (Map<?, ?>) val;
                Object valueObj = valMap.get("value");
                if (valueObj != null) {
                    result.put(entry.getKey(), valueObj.toString());
                }
            } else if (val != null) {
                // 格式: {"key": "xxx"}
                result.put(entry.getKey(), val.toString());
            }
        }
        return result;
    }
}
