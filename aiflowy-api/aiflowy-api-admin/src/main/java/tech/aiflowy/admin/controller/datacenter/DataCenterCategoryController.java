package tech.aiflowy.admin.controller.datacenter;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.common.annotation.UsePermission;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.datacenter.entity.DatacenterTableCategory;
import tech.aiflowy.datacenter.service.DatacenterTableCategoryService;

/**
 * bot分类 控制层。
 *
 * @author Wgq
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/api/v1/datacenterCategory")
@UsePermission(moduleName = "/api/v1/datacenterTable")
public class DataCenterCategoryController extends BaseCurdController<DatacenterTableCategoryService, DatacenterTableCategory> {
    public DataCenterCategoryController(DatacenterTableCategoryService service) {
        super(service);
    }
}