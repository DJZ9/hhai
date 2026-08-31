package tech.aiflowy.system.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.system.entity.SysApiKey;
import tech.aiflowy.system.entity.SysApiKeyResource;
import tech.aiflowy.system.entity.SysApiKeyResourceMapping;
import tech.aiflowy.system.mapper.SysApiKeyMapper;
import tech.aiflowy.system.service.SysApiKeyResourceMappingService;
import tech.aiflowy.system.service.SysApiKeyResourceService;
import tech.aiflowy.system.service.SysApiKeyService;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

/**
 * 服务层实现。
 *
 * @author Administrator
 * @since 2025-04-18
 */
@Service
public class SysApiKeyServiceImpl extends ServiceImpl<SysApiKeyMapper, SysApiKey> implements SysApiKeyService {

    @Resource
    private SysApiKeyResourceMappingService mappingService;
    @Resource
    private SysApiKeyResourceService resourceService;

    @Override
    public void checkApikeyPermission(SysApiKey apiKey, String requestURI) {
        QueryWrapper w = QueryWrapper.create();
        w.eq(SysApiKeyResource::getRequestInterface, requestURI);
        SysApiKeyResource resource = resourceService.getOne(w);
        if (resource == null) {
            throw new BusinessException("该接口不存在");
        }
        QueryWrapper wm = QueryWrapper.create();
        wm.eq(SysApiKeyResourceMapping::getApiKeyId, apiKey.getId());
        wm.eq(SysApiKeyResourceMapping::getApiKeyResourceId, resource.getId());
        long count = mappingService.count(wm);
        if (count == 0) {
            throw new BusinessException("该apiKey无权限访问该接口");
        }
    }

    @Override
    public SysApiKey getSysApiKey(String apiKey) {
        QueryWrapper w = QueryWrapper.create();
        w.eq(SysApiKey::getApiKey, apiKey);
        SysApiKey one = getOne(w);
        if (one == null || one.getStatus() == 0) {
            throw new BusinessException("apiKey 不存在或已禁用");
        }
        if (one.getExpiredAt() != null && one.getExpiredAt().getTime() < new Date().getTime()) {
            throw new BusinessException("apiKey 已过期");
        }
        return one;
    }

}
