package tech.aiflowy.datacenter.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import tech.aiflowy.datacenter.entity.DatacenterSourceTable;
import tech.aiflowy.datacenter.mapper.DatacenterSourceTableMapper;
import tech.aiflowy.datacenter.service.DatacenterSourceTableService;

/**
 * 数据源-数据表 服务层实现。
 *
 * @author John
 * @since 2026-03-10
 */
@Service
public class DatacenterSourceTableServiceImpl extends ServiceImpl<DatacenterSourceTableMapper, DatacenterSourceTable>  implements DatacenterSourceTableService{

}
