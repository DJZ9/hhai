package tech.aiflowy.system.entity;

import tech.aiflowy.common.util.SpringContextUtil;
import tech.aiflowy.common.util.SqlIdentifierUtil;
import tech.aiflowy.common.dict.Dict;
import tech.aiflowy.common.dict.DictItem;
import tech.aiflowy.common.dict.DictLoader;
import tech.aiflowy.common.dict.DictType;
import tech.aiflowy.common.dict.loader.DatabaseDictLoader;
import tech.aiflowy.common.dict.loader.EnumDictLoader;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.system.entity.base.SysDictBase;
import tech.aiflowy.system.service.SysDictItemService;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;


/**
 * 系统配置表 实体类。
 *
 * @author michael
 * @since 2024-03-04
 */

@Table(value = "tb_sys_dict",comment = "系统字典表")
public class SysDict extends SysDictBase {

    public DictLoader buildLoader() {
        Integer dictType = getDictType();
        if (dictType == null) {
            return null;
        }

        if (dictType == DictType.TABLE.getValue()) {
            String tableName = optionValue("tableName");
            String keyColumn = optionValue("keyColumn");
            String labelColumn = optionValue("labelColumn");
            String parentColumn = optionValue("parentColumn");
            return new DatabaseDictLoader(this.getCode(), tableName, keyColumn, labelColumn, parentColumn, null);
        } else if (dictType == DictType.ENUM.getValue()) {
            String enumClassString = optionValue("enumClass");
            String keyField = optionValue("keyField");
            String labelField = optionValue("labelField");

            //noinspection rawtypes
            Class enumClass = null;
            try {
                enumClass = Class.forName(enumClassString);
            } catch (ClassNotFoundException e) {
                //ignore
                return null;
            }
            return new EnumDictLoader<>(this.getName(), this.getCode(), enumClass, keyField, labelField);
        } else if (dictType == DictType.CUSTOM.getValue()) {
            return new DictItemsLoader(this.getCode(), this.getId());
        }

        return null;
    }

    /**
     * 校验数据表字典（dict_type = 2）的 options 配置。
     * <p>
     * 表名、列名最终会被拼进 SQL，因此只允许“纯标识符”（字母、数字、下划线或 $，最多 3 段点分），
     * 不合法的配置直接拒绝保存。非数据表字典不做校验。
     *
     * @throws BusinessException 配置不合法时抛出，消息中指明是哪个字段不合法
     */
    public void checkTableDictOptions() {
        if (getDictType() == null || getDictType() != DictType.TABLE.getValue()) {
            return;
        }

        checkOption("tableName", true);
        checkOption("keyColumn", true);
        checkOption("labelColumn", true);
        checkOption("parentColumn", false);
        checkOption("orderBy", false);
    }

    private void checkOption(String field, boolean required) {
        String value = optionValue(field);
        if (!StringUtils.hasText(value)) {
            if (required) {
                throw new BusinessException("数据表字典的 " + field + " 不能为空");
            }
            return;
        }

        try {
            if ("orderBy".equals(field)) {
                SqlIdentifierUtil.checkOrderBy(field, value);
            } else {
                SqlIdentifierUtil.checkIdentifier(field, value);
            }
        } catch (IllegalArgumentException e) {
            throw new BusinessException("数据表字典配置校验失败：" + e.getMessage());
        }
    }

    /**
     * 从 options 中读取字符串配置，options 为空时返回 null。
     */
    private String optionValue(String key) {
        Map<String, Object> options = this.getOptions();
        if (options == null) {
            return null;
        }
        Object value = options.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static class DictItemsLoader implements DictLoader {
        private final String code;
        private final BigInteger dictId;
        private final SysDictItemService itemService;

        public DictItemsLoader(String code, BigInteger dictId) {
            this.code = code;
            this.dictId = dictId;
            this.itemService = SpringContextUtil.getBean(SysDictItemService.class);
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public Dict load(String keyword, Map<String, String[]> parameters) {
            QueryWrapper qw = QueryWrapper.create()
                    .eq(SysDictItem::getDictId, this.dictId)
                    .eq(SysDictItem::getStatus, 0)
                    .like(SysDictItem::getText, keyword);
            List<SysDictItem> sysDictItems = itemService.list(qw);

            Dict dict = new Dict();
            if (sysDictItems != null) {
                for (SysDictItem sysDictItem : sysDictItems) {
                    dict.addItem(new DictItem(sysDictItem.getValue(), sysDictItem.getText()));
                }
            }
            return dict;
        }
    }
}
