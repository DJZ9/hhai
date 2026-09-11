package tech.aiflowy.admin.controller.datacenter;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.aiflowy.common.annotation.UsePermission;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.datacenter.entity.DatacenterTableImportRecord;
import tech.aiflowy.datacenter.service.DatacenterTableImportRecordService;

/**
 * 数据中枢表导入记录 控制层。
 *
 * @author wgq
 * @since 2026-06-29
 */
@RestController
@RequestMapping("/api/v1/datacenterTableImportRecord")
@UsePermission(moduleName = "/api/v1/datacenterTable")
public class DatacenterTableImportRecordController extends BaseCurdController<DatacenterTableImportRecordService, DatacenterTableImportRecord> {

    public DatacenterTableImportRecordController(DatacenterTableImportRecordService service) {
        super(service);
    }
}