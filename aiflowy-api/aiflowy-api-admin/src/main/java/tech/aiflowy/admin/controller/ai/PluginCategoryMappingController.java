package tech.aiflowy.admin.controller.ai;

import org.springframework.web.bind.annotation.*;
import tech.aiflowy.ai.entity.PluginCategory;
import tech.aiflowy.ai.entity.PluginCategoryMapping;
import tech.aiflowy.ai.service.PluginCategoryMappingService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.common.web.jsonbody.JsonBody;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 *  控制层。
 *
 * @author wangGangQiang
 * @since 2025-05-21
 */
@RestController
@RequestMapping("/api/v1/pluginCategoryMapping")
public class PluginCategoryMappingController extends BaseCurdController<PluginCategoryMappingService, PluginCategoryMapping> {
    public PluginCategoryMappingController(PluginCategoryMappingService service) {
        super(service);
    }

    @Resource
    private PluginCategoryMappingService relationService;

    @PostMapping("/updateRelation")
    public Result<Boolean> updateRelation(
            @JsonBody(value="pluginId") BigInteger pluginId,
            @JsonBody(value="categoryIds") ArrayList<BigInteger> categoryIds
    ){
        return Result.ok(relationService.updateRelation(pluginId, categoryIds));
    }

    @GetMapping("/getPluginCategories")
    public Result<List<PluginCategory>> getPluginCategories(@RequestParam(value="pluginId") BigInteger pluginId
    ){
        return Result.ok(relationService.getPluginCategories(pluginId));
    }
}