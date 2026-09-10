package tech.aiflowy.system.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.common.util.StringUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.system.entity.SysAccountOpenid;
import tech.aiflowy.system.mapper.SysAccountOpenidMapper;
import tech.aiflowy.system.service.SysAccountOpenidService;

import java.math.BigInteger;
import java.util.List;

/**
 * 账号绑定信息表 服务层实现。
 *
 * @author michael
 * @since 2026-06-29
 */
@Service
public class SysAccountOpenidServiceImpl extends ServiceImpl<SysAccountOpenidMapper, SysAccountOpenid>  implements SysAccountOpenidService{

    @Override
    public SysAccountOpenid findByOpenId(String userKey, String platform) {
        if (StringUtil.noText(userKey) || StringUtil.noText(platform)){
            throw new BusinessException("参数错误, userKey 和 platform 不能为空");
        }

        QueryWrapper wrapper = QueryWrapper.create()
                .eq(SysAccountOpenid::getOpenidValue, userKey)
                .eq(SysAccountOpenid::getOpenidType, platform);

        return getOne(wrapper);
    }

    @Override
    public List<SysAccountOpenid> findByAccountId(BigInteger accountId) {
        if (accountId == null) {
            throw new BusinessException("参数错误, accountId 不能为空");
        }

        QueryWrapper wrapper = QueryWrapper.create()
                .eq(SysAccountOpenid::getAccountId, accountId);

        return list(wrapper);
    }
}
