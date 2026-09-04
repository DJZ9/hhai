package tech.aiflowy.admin.controller.ai;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import tech.aiflowy.ai.entity.Model;
import tech.aiflowy.ai.entity.ModelProvider;
import tech.aiflowy.ai.entity.table.ModelTableDef;
import tech.aiflowy.ai.mapper.ModelMapper;
import tech.aiflowy.ai.service.ModelService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.tree.Tree;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.common.web.jsonbody.JsonBody;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 控制层。
 *
 * @author michael
 * @since 2024-08-23
 */
@RestController
@RequestMapping("/api/v1/model")
public class ModelController extends BaseCurdController<ModelService, Model> {

    public ModelController(ModelService service) {
        super(service);
    }

    @Autowired
    ModelService modelService;

    @Resource
    ModelMapper modelMapper;

    @GetMapping("list")
    @SaCheckPermission("/api/v1/model/query")
    public Result<List<Model>> list(Model entity, Boolean asTree, String sortKey, String sortType) {
        QueryWrapper queryWrapper = QueryWrapper.create(entity, buildOperators(entity));
        if (entity.getProviderId() == null) {
            queryWrapper.eq(Model::getPublishStatus, 1);
        }
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        List<Model> list = Tree.tryToTree(modelMapper.selectListWithRelationsByQuery(queryWrapper), asTree);
        list.forEach(item -> {
            String providerName = Optional.ofNullable(item.getModelProvider())
                    .map(ModelProvider::getProviderName)
                    .orElse("-");
            item.setTitle(providerName + "/" + item.getTitle());
        });
        return Result.ok(list);
    }

    @GetMapping("getList")
    @SaCheckPermission("/api/v1/model/query")
    public Result<Map<String, Map<String, List<Model>>>> getList(Model entity) {
        return Result.ok(modelService.getList(entity));
    }

    @PostMapping("/addAiLlm")
    @SaCheckPermission("/api/v1/model/save")
    public Result<Boolean> addAiLlm(Model entity) {
        LoginAccount account = SaTokenUtil.getLoginAccount();
        commonFiled(entity, account.getId(), account.getTenantId(), account.getDeptId());
        return Result.ok(modelService.addAiLlm(entity));
    }


    @GetMapping("verifyLlmConfig")
    @SaCheckPermission("/api/v1/model/save")
    public Result<?> verifyLlmConfig(@RequestParam BigInteger id) {
        Model model = service.getModelInstance(id);
        return Result.ok(modelService.verifyModelConfig(model));
    }

    /**
     * 验证 OCR 模型配置
     *
     * @param id 模型ID
     * @param fileUrl 测试文件URL
     * @return 验证结果
     */
    @GetMapping("verifyOcrModelConfig")
    @SaCheckPermission("/api/v1/model/save")
    public Result<?> verifyOcrModelConfig(
            @RequestParam BigInteger id,
            @RequestParam String fileUrl
    ) {
        Model model = service.getModelInstance(id);
        return Result.ok(modelService.verifyOcrModelConfig(model, fileUrl));
    }

    @PostMapping("/removeByEntity")
    @SaCheckPermission("/api/v1/model/remove")
    public Result<?> removeByEntity(@RequestBody Model entity) {
        modelService.removeByEntity(entity);
        return Result.ok();
    }

    @PostMapping("/updateByEntity")
    @SaCheckPermission("/api/v1/model/save")
    public Result<?> updateByEntity(@RequestBody Model entity) {
        modelService.updateByEntity(entity);
        return Result.ok();
    }

    @GetMapping("/selectLlmByProviderCategory")
    @SaCheckPermission("/api/v1/model/query")
    public Result<Map<String, List<Model>>> selectLlmByProviderCategory(Model entity, String sortKey, String sortType) {
        QueryWrapper queryWrapper = QueryWrapper.create(entity, buildOperators(entity));
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        BaseMapper<Model> mapper = service.getMapper();
        List<Model> totalList = mapper.selectListWithRelationsByQuery(queryWrapper);
        Map<String, List<Model>> groupList = totalList.stream().collect(Collectors.groupingBy(Model::getGroupName));
        return Result.ok(groupList);
    }

    @GetMapping("/selectLlmByProviderAndModelType")
    @SaCheckPermission("/api/v1/model/query")
    public Result<Map<String, List<Model>>> selectLlmByProviderAndModelType(
            @RequestParam String modelType,
            @RequestParam BigInteger providerId,
            @RequestParam(required = false) String selectText
    ) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq(Model::getProviderId, providerId);
        queryWrapper.eq(Model::getModelType, modelType);
        if (StringUtils.hasLength(selectText)) {
            queryWrapper.and(ModelTableDef.MODEL.TITLE.like(selectText).or(ModelTableDef.MODEL.MODEL_NAME.like(selectText)));
        }
        List<Model> totalList = service.getMapper().selectListWithRelationsByQuery(queryWrapper);
        Map<String, List<Model>> groupList = totalList.stream().collect(Collectors.groupingBy(Model::getGroupName));
        return Result.ok(groupList);
    }


    /**
     * 添加所有模型
     *
     * @param entity
     * @return
     */
    @PostMapping("addAllLlm")
    public Result<?> addAllLlm(@JsonBody Model entity) {
        QueryWrapper queryWrapper = QueryWrapper.create().eq(Model::getProviderId, entity.getProviderId());
        service.update(entity, queryWrapper);
        return Result.ok();
    }

    @GetMapping("/selectLlmList")
    @SaCheckPermission("/api/v1/model/query")
    public Result<List<Model>> selectLlmList(Model entity, Boolean asTree, String sortKey, String sortType) {
        QueryWrapper queryWrapper = QueryWrapper.create(entity, buildOperators(entity));
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        List<Model> totalList = Tree.tryToTree(modelMapper.selectListWithRelationsByQuery(queryWrapper), asTree);
        totalList.forEach(aiLlm -> {
            aiLlm.setTitle(aiLlm.getModelProvider().getProviderName() + "/" + aiLlm.getTitle());
        });
        return Result.ok(totalList);
    }

    @Override
    protected Result<?> onSaveOrUpdateBefore(Model entity, boolean isSave) {
        if (isSave) {
            entity.setWithUsed(true);
        }
        return super.onSaveOrUpdateBefore(entity, isSave);
    }


    @PostMapping("removeLlmByIds")
    @Transactional
    public Result<?> removeLlm(@JsonBody(value = "id", required = true) Serializable id) {
        List<Serializable> ids = Collections.singletonList(id);
        QueryWrapper queryWrapper = QueryWrapper.create().in(Model::getId, ids);
        service.remove(queryWrapper);
        return Result.ok();
    }

    /**
     * 批量发布模型：逐个校验模型配置是否可用，校验通过后将发布状态置为已发布
     *
     * @param ids       模型ID列表，支持批量
     * @param modelType 模型类型（chatModel/embeddingModel/rerankModel），可选，仅用于校验
     * @return 发布结果：success-发布成功列表，fail-发布失败列表（含原因），allSuccess-是否全部成功
     */
    @PostMapping("batchPublish")
    @SaCheckPermission("/api/v1/model/save")
    public Result<Map<String, Object>> batchPublish(@RequestParam List<BigInteger> ids,
                                                    @RequestParam(required = false) String modelType) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("模型ID不能为空");
        }

        List<Map<String, Object>> successList = new ArrayList<>();
        List<Map<String, Object>> failList = new ArrayList<>();

        for (BigInteger id : ids) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", id);
            try {
                Model model = modelService.getModelInstance(id);
                if (model == null) {
                    item.put("reason", "模型不存在");
                    failList.add(item);
                    continue;
                }
                item.put("title", model.getTitle());

                // 校验模型是否可用（校验失败会抛出 BusinessException）
                Model verifyModel = new Model();
                BeanUtils.copyProperties(model, verifyModel);
                if (StringUtils.hasLength(modelType)) {
                    verifyModel.setModelType(modelType);
                }
                if (modelType != null && modelType.equals("ocrModel")) {
                    modelService.verifyOcrModelConfig(verifyModel, null);
                } else {
                    modelService.verifyModelConfig(verifyModel);
                }

                // 校验通过，发布状态置为已发布
                model.setPublishStatus(1);
                service.updateById(model);
                successList.add(item);
            } catch (Exception e) {
                item.put("reason", e.getMessage());
                failList.add(item);
            }
        }

        boolean allSuccess = failList.isEmpty();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("allSuccess", allSuccess);
        resultMap.put("message", allSuccess ? "全部发布成功" : "部分发布失败");
        resultMap.put("success", successList);
        resultMap.put("fail", failList);
        return Result.ok(resultMap);
    }

    /**
     * 单个模型发布：校验模型配置是否可用，校验通过后将发布状态置为已发布
     *
     * @param id        模型ID
     * @return 操作结果
     */
    @PostMapping("singlePublish")
    @SaCheckPermission("/api/v1/model/save")
    public Result<?> singlePublish(@RequestParam BigInteger id) {
        if (id == null) {
            throw new BusinessException("模型ID不能为空");
        }
        Model model = modelService.getModelInstance(id);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }

        // 校验模型是否可用（校验失败会抛出 BusinessException）
        Model verifyModel = new Model();
        BeanUtils.copyProperties(model, verifyModel);

        if (model.getModelType() != null && "ocrModel".equals(model.getModelType())) {
            modelService.verifyOcrModelConfig(verifyModel, null);
        } else {
            modelService.verifyModelConfig(verifyModel);
        }

        // 校验通过，发布状态置为已发布
        model.setPublishStatus(1);
        service.updateById(model);
        return Result.ok();
    }

    /**
     * 单个模型下线：将发布状态置为未发布
     *
     * @param id 模型ID
     * @return 操作结果
     */
    @PostMapping("modelOffline")
    @SaCheckPermission("/api/v1/model/save")
    public Result<?> modelOffline(@RequestParam BigInteger id) {
        if (id == null) {
            throw new BusinessException("模型ID不能为空");
        }
        Model model = service.getById(id);
        if (model == null) {
            throw new BusinessException("模型不存在");
        }
        model.setPublishStatus(0);
        service.updateById(model);
        return Result.ok();
    }
}