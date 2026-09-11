package tech.aiflowy.datacenter.entity;

import com.mybatisflex.annotation.Table;
import tech.aiflowy.datacenter.entity.base.DatacenterTableParseTaskBase;


/**
 * 数据中枢解析任务 实体类。
 *
 * @author 12076
 * @since 2026-06-26
 */
@Table(value = "tb_datacenter_table_parse_task", comment = "数据中枢解析任务")
public class DatacenterTableParseTask extends DatacenterTableParseTaskBase {
    // 状态常量
    public static final int STATUS_PROCESSING = 0;
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 2;

    // 任务类型常量
    public static final String TASK_TYPE_AI_TABLE_PARSE = "AI_TABLE_PARSE";
}
