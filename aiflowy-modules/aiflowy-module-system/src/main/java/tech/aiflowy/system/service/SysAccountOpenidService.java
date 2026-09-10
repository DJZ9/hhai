package tech.aiflowy.system.service;

import com.mybatisflex.core.service.IService;
import tech.aiflowy.system.entity.SysAccountOpenid;

import java.math.BigInteger;
import java.util.List;

/**
 * 账号绑定信息表 服务层。
 *
 * @author michael
 * @since 2026-06-29
 */
public interface SysAccountOpenidService extends IService<SysAccountOpenid> {

    SysAccountOpenid findByOpenId(String userKey, String platform);

    /**
     * 根据账号ID查询绑定记录
     *
     * @param accountId 账号ID
     * @return 绑定记录列表
     */
    List<SysAccountOpenid> findByAccountId(BigInteger accountId);
}
