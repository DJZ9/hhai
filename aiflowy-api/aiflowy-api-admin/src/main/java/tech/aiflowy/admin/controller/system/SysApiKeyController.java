package tech.aiflowy.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.util.IdUtil;
import tech.aiflowy.common.vo.PkVo;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.system.entity.SysApiKey;
import tech.aiflowy.system.entity.SysApiKeyResourceMapping;
import tech.aiflowy.system.service.SysApiKeyResourceMappingService;
import tech.aiflowy.system.service.SysApiKeyService;

import jakarta.annotation.Resource;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 *  控制层。
 *
 * @author wangGangQiang
 * @since 2025-04-18
 */
@RestController
@RequestMapping("/api/v1/sysApiKey")
public class SysApiKeyController extends BaseCurdController<SysApiKeyService, SysApiKey> {
    public SysApiKeyController(SysApiKeyService service) {
        super(service);
    }

    @Resource
    private SysApiKeyResourceMappingService sysApiKeyResourceMappingService;
    /**
     * 添加（保存）数据
     *
     * @return {@code Result.errorCode == 0} 添加成功，否则添加失败
     */
    @PostMapping("/key/save")
    @SaCheckPermission("/api/v1/sysApiKey/save")
    public Result<PkVo> save(@RequestBody(required = false) SysApiKey req) {
        SysApiKey entity = new SysApiKey();
        entity.setApiKey(IdUtil.generateUUID());
        entity.setCreated(new Date());
        entity.setStatus(req != null && req.getStatus() != null ? req.getStatus() : 1);
        entity.setName(req != null ? req.getName() : null);
        // 默认失效时间：当前时间 + 30 天
        LocalDate expireLocal = LocalDate.now().plusDays(30);
        Date defaultExpire = Date.from(expireLocal.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
        entity.setExpiredAt(req != null && req.getExpiredAt() != null ? req.getExpiredAt() : defaultExpire);
        if (req != null) {
            entity.setPermissionIds(req.getPermissionIds());
        }
        LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
        commonFiled(entity,loginAccount.getId(),loginAccount.getTenantId(),loginAccount.getDeptId());
        service.save(entity);
        onSaveOrUpdateAfter(entity, true);
        TableInfo tableInfo = TableInfoFactory.ofEntityClass(entity.getClass());
        Object[] pkArgs = tableInfo.buildPkSqlArgs(entity);
        return Result.ok(new PkVo(pkArgs));
    }

    @Override
    protected void onSaveOrUpdateAfter(SysApiKey entity, boolean isSave) {
        if (entity.getPermissionIds() != null && !entity.getPermissionIds().isEmpty()) {
            // 保存或修改时绑定授权接口
            sysApiKeyResourceMappingService.authInterface(entity);
        }
    }

    @Override
    @GetMapping("/page")
    public Result<Page<SysApiKey>> page(HttpServletRequest request, String sortKey, String sortType, Long pageNumber, Long pageSize) {
        Result<Page<SysApiKey>> pageResult = (Result<Page<SysApiKey>>) super.page(request, sortKey, sortType, pageNumber, pageSize);
        Page<SysApiKey> data = pageResult.getData();
        List<SysApiKey> records = data.getRecords();
        records.forEach(record -> {
            QueryWrapper queryWrapper = QueryWrapper.create().select(SysApiKeyResourceMapping::getApiKeyResourceId).eq(SysApiKeyResourceMapping::getApiKeyId, record.getId());
            List<BigInteger> resourceIds = sysApiKeyResourceMappingService.listAs(queryWrapper, BigInteger.class);
            record.setPermissionIds(resourceIds);
        });
        return pageResult;
    }
}