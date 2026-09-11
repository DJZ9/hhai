package tech.aiflowy.datacenter.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import tech.aiflowy.datacenter.entity.DatacenterTableCategory;
import tech.aiflowy.datacenter.mapper.DatacenterTableCategoryMapper;
import tech.aiflowy.datacenter.service.DatacenterTableCategoryService;
import org.springframework.stereotype.Service;

/**
 * 数据中枢表分类 服务层实现。
 *
 * @author 12076
 * @since 2026-04-09
 */
@Service
public class DatacenterTableCategoryServiceImpl extends ServiceImpl<DatacenterTableCategoryMapper, DatacenterTableCategory>  implements DatacenterTableCategoryService{

}
