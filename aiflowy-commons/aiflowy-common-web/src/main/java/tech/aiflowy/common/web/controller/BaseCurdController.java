package tech.aiflowy.common.web.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.query.SqlOperators;
import com.mybatisflex.core.service.IService;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.mybatisflex.core.util.StringUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.tree.Tree;
import tech.aiflowy.common.util.SqlOperatorsUtil;
import tech.aiflowy.common.util.SqlUtil;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.common.web.exceptions.ProgramException;
import tech.aiflowy.common.web.jsonbody.JsonBody;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

public class BaseCurdController<S extends IService<M>, M> extends BaseController {
    
    /**
     * 发布状态：已发布
     */
    protected static final Integer PUBLISH_STATUS_PUBLISHED = 1;

    protected final S service;

    public BaseCurdController(S service) {
        this.service = service;
    }

    /**
     * 添加（保存）数据
     *
     * @param entity 表生成内容配置
     * @return {@code Result.errorCode == 0} 添加成功，否则添加失败
     */
    @PostMapping("save")
    public Result<?> save(@JsonBody M entity) {
        Result<?> result = onSaveOrUpdateBefore(entity, true);
        if (result != null) return result;

        if (entity == null) {
            throw new NullPointerException("entity is null");
        }
        LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
        commonFiled(entity, loginAccount.getId(), loginAccount.getTenantId(), loginAccount.getDeptId());
        service.save(entity);
        onSaveOrUpdateAfter(entity, true);
        TableInfo tableInfo = TableInfoFactory.ofEntityClass(entity.getClass());
        Object[] pkArgs = tableInfo.buildPkSqlArgs(entity);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", pkArgs);
        return Result.ok(resultMap);
    }

    /**
     * 根据主键删除数据，id 需通过 json 传入，例如：
     * <pre>
     * {
     *   "id":123
     * }
     * <pre/>
     *
     *
     * @param id 主键
     * @return {@code Result.errorCode == 0} 删除成功，否则删除失败
     */
    @PostMapping("remove")
    @Transactional
    public Result<?> remove(@JsonBody(value = "id", required = true) Serializable id) {
        List<Serializable> ids = Collections.singletonList(id);
        Result<?> result = onRemoveBefore(ids);
        if (result != null) return result;
        boolean success = service.removeById(id);
        onRemoveAfter(ids);
        return Result.ok(success);
    }


    /**
     * 根据多个主键删数据内容，id 需通过 json 传入，例如：
     * <pre>
     * {
     *   "ids":[123, 234, 222]
     * }
     * <pre/>
     *
     * @param ids 主键
     * @return {@code Result.errorCode == 0} 删除成功，否则删除失败
     */
    @PostMapping("removeBatch")
    @Transactional
    public Result<?> removeBatch(@JsonBody(value = "ids", required = true) Collection<Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.fail("id不能为空");
        }
        Result<?> result = onRemoveBefore(ids);
        if (result != null) return result;
        boolean success = service.removeByIds(ids);
        onRemoveAfter(ids);
        return Result.ok(success);
    }

    /**
     * 根据主键更新内容
     *
     * @param entity 实体类数据
     * @return {@code Result.errorCode == 0} 更新成功，否则更新失败
     */
    @PostMapping("update")
    public Result<?> update(@JsonBody M entity) {
        Result<?> result = onSaveOrUpdateBefore(entity, false);
        if (result != null) return result;
        service.updateById(entity);
        onSaveOrUpdateAfter(entity, false);
        return Result.ok();
    }

    /**
     * 查询所有所有数据
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public Result<List<M>> list(M entity, Boolean asTree, String sortKey, String sortType) {
        QueryWrapper queryWrapper = QueryWrapper.create(entity, buildOperators(entity));
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        List<M> list = Tree.tryToTree(service.list(queryWrapper), asTree);
        return Result.ok(list);
    }


    /**
     * 根据表主键查询数据详情。
     *
     * @param id 主键值
     * @return 内容详情
     */
    @GetMapping("detail")
    public Result<M> detail(String id) {
        if (tech.aiflowy.common.util.StringUtil.noText(id)) {
            throw new BusinessException("id must not be null");
        }
        return Result.ok(service.getById(id));
    }


    /**
     * 分页查询数据列表
     *
     * @param request    查询数据
     * @param sortKey    排序字段
     * @param sortType   排序方式 asc | desc
     * @param pageNumber 当前页码
     * @param pageSize   每页的数据量
     * @return 查询的结果集
     */
    @GetMapping("page")
    public Result<Page<M>> page(HttpServletRequest request, String sortKey, String sortType, Long pageNumber, Long pageSize) {
        if (pageNumber == null || pageNumber < 1) {
            pageNumber = 1L;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10L;
        }

        QueryWrapper queryWrapper = buildQueryWrapper(request);
        onPageQueryBefore(queryWrapper);
        queryWrapper.orderBy(buildOrderBy(sortKey, sortType, getDefaultOrderBy()));
        return Result.ok(queryPage(new Page<>(pageNumber, pageSize), queryWrapper));
    }

    /**
     * 发布数据：不修改原记录，而是基于原记录新增一条子记录，建立父子版本关系。
     * <p>
     * 流程：
     * <ol>
     *   <li>从请求 entity 中提取主键，作为父记录ID</li>
     *   <li>从数据库加载完整父记录</li>
     *   <li>将请求 entity 中的非空字段合并到父记录上（保留未修改字段的原始值）</li>
     *   <li>设置 parentId 和 ancestors（若实体存在对应字段）</li>
     *   <li>清空主键ID，使数据库生成新的主键</li>
     *   <li>以新增方式保存</li>
     * </ol>
     *
     * @param entity 实体类数据（包含父记录ID及本次修改的字段）
     * @return {@code Result.errorCode == 0} 发布成功，返回新记录的ID
     */
    @PostMapping("publish")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> publish(@RequestBody M entity) {
        Result<?> result = onPublishBefore(entity);
        if (result != null) return result;

        if (entity == null) {
            throw new NullPointerException("entity is null");
        }

        Object parentId = extractParentId(entity);
        if (parentId == null) {
            throw new BusinessException("父记录ID不能为空");
        }

        M parentRecord = service.getById((Serializable) parentId);
        if (parentRecord == null) {
            throw new BusinessException("父记录不存在");
        }

        mergeNonNullFields(entity, parentRecord);

        // 查询最大版本号记录
        List<M> botList = getBotList(parentRecord, (BigInteger) parentId);
        // botList 是升序排列的，取最后一个即为最大版本号记录
        M maxVersionRecord = botList.get(botList.size() - 1);
        // 获取最大版本号，并且加1后赋值给parentRecord
        int maxVersion = getMaxVersion(maxVersionRecord);
        incrementVersion(parentRecord, maxVersion);

        applyPublishStatus(parentRecord);

        // 构建父记录和子记录的发布关系，
        buildParentRelation(parentRecord, parentId);

        clearEntityId(parentRecord);

        LoginAccount loginAccount = SaTokenUtil.getLoginAccount();
        commonFiled(parentRecord, loginAccount.getId(), loginAccount.getTenantId(), loginAccount.getDeptId());
        service.save(parentRecord);
        onPublishAfter(parentRecord);

        TableInfo tableInfo = TableInfoFactory.ofEntityClass(parentRecord.getClass());
        Object[] pkArgs = tableInfo.buildPkSqlArgs(parentRecord);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("id", pkArgs);
        return Result.ok(resultMap);
    }


    /**
     * 查询发布历史记录：botId 为版本树根节点ID（首版），
     * 通过根节点ID找到其下所有子孙版本节点。
     * <p>
     * 查询条件：id = botId（根节点本身），或 ancestors = botId（直接子节点），
     * 或 ancestors 以 "botId," 开头（任意深度的子孙节点）。
     *
     * @param id 版本树根节点ID（首版智能体的ID）
     * @return 根节点及其全部子孙版本记录，按版本号升序排列
     */
    @GetMapping("history")
    public Result<List<M>> history(@RequestParam BigInteger id) {
        M current = service.getById(id);
        if (current == null) {
            return Result.ok(Collections.emptyList());
        }

        return Result.ok(getBotList(current, id));
    }
    
    protected QueryWrapper buildQueryWrapper(HttpServletRequest request) {

        QueryWrapper queryWrapper = new QueryWrapper();

        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return queryWrapper;
        }

        String[] isQueryOrs = parameterMap.get("isQueryOr");
        boolean isQueryOrBool = false;

        if (isQueryOrs != null && isQueryOrs.length > 0) {
            String isQueryOr = isQueryOrs[0];
            isQueryOrBool = "true".equals(isQueryOr);
        }

        Map<String, String> propertyColumnMapping = TableInfoFactory.ofEntityClass(getEntityClass())
                .getPropertyColumnMapping();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramKey = entry.getKey();
            if (StringUtil.hasText(paramKey) && !paramKey.endsWith(OperatorBuilder.operatorSuffix) && propertyColumnMapping.containsKey(paramKey)) {
                String columnName = propertyColumnMapping.get(paramKey);
                String[] values = entry.getValue();
                if (values != null && values.length > 0 && StringUtil.hasText(values[0])) {
                    String op = request.getParameter(paramKey + OperatorBuilder.operatorSuffix);
                    if (StringUtil.hasText(op)) {
                        OperatorBuilder.buildOperator(queryWrapper, columnName, op.trim(), values);
                    } else {
                        if (values.length == 2) {
                            queryWrapper.between(columnName, values[0], values[1]);
                        } else {
                            String value = values[0];
                            if (StringUtil.isNumeric(value)) {
                                queryWrapper.eq(columnName, value);
                            } else {
                                if (isQueryOrBool) {
                                    queryWrapper.or((Consumer<QueryWrapper>) wrapper -> wrapper.like(columnName, value));
                                } else {
                                    queryWrapper.like(columnName, value);
                                }
                            }
                        }
                    }
                }
            }
        }

        return queryWrapper;
    }

    protected Class<?> getEntityClass() {
        Type type = getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            return (Class<M>) ((ParameterizedType) type).getActualTypeArguments()[1];
        }

        return null;
    }


    /**
     * 方便子类复写，构建自己的 SqlOperators
     *
     * @param entity 实体类
     * @return SqlOperators
     */
    protected SqlOperators buildOperators(M entity) {
        return entity == null ? SqlOperators.empty() : SqlOperatorsUtil.build(entity.getClass());
    }

    protected String getDefaultOrderBy() {
        return "id desc";
    }

    /**
     * 方便子类复写，构建自己的 orderBy
     *
     * @param sortKey        排序字段
     * @param sortType       排序类型
     * @param defaultOrderBy 默认方式内容
     * @return orderBy 的内容，返回 null 或者 空字符串，表示不参与排序
     */
    protected String buildOrderBy(String sortKey, String sortType, String defaultOrderBy) {
        sortKey = StringUtil.camelToUnderline(sortKey);
        return SqlUtil.buildOrderBy(sortKey, sortType, defaultOrderBy);
    }

    protected Page<M> queryPage(Page<M> page, QueryWrapper queryWrapper) {
        return service.page(page, queryWrapper);
    }

    protected Result<?> onSaveOrUpdateBefore(M entity, boolean isSave) {
        return null;
    }

    protected void onSaveOrUpdateAfter(M entity, boolean isSave) {
        //void
    }

    protected Result<?> onRemoveBefore(Collection<Serializable> ids) {
        return null;
    }

    protected void onRemoveAfter(Collection<Serializable> ids) {
        //void
    }

    protected void commonFiled(Object t, BigInteger userId, BigInteger tenantId, BigInteger deptId) {
        Method[] methods = t.getClass().getMethods();
        try {
            for (Method m : methods) {
                String name = m.getName();
                if ("setDeptId".equals(name)) {
                    m.invoke(t, deptId);
                }
                if ("setTenantId".equals(name)) {
                    m.invoke(t, tenantId);
                }
                if ("setCreatedBy".equals(name)) {
                    m.invoke(t, userId);
                }
                if ("setModifiedBy".equals(name)) {
                    m.invoke(t, userId);
                }
                if ("setCreated".equals(name)) {
                    m.invoke(t, new Date());
                }
                if ("setModified".equals(name)) {
                    m.invoke(t, new Date());
                }
            }
        } catch (Exception e) {
            throw new ProgramException("commonFiled反射出错：" + e.getMessage());
        }
    }

    /**
     * 发布前置钩子，子类重写以添加校验逻辑。
     * 返回非 null 的 Result 将终止发布流程并直接返回该结果。
     *
     * @param entity 待发布的实体数据
     * @return null 表示校验通过，非 null 表示校验失败
     */
    protected Result<?> onPublishBefore(M entity) {
        return null;
    }

    /**
     * 从实体中通过反射提取主键ID（父记录ID）。
     * 子类可重写以支持不同的ID获取方式。
     *
     * @param entity 实体对象
     * @return 主键ID值
     */
    protected Object extractParentId(M entity) {
        try {
            Method[] methods = entity.getClass().getMethods();
            for (Method m : methods) {
                if ("getId".equals(m.getName()) && m.getParameterCount() == 0) {
                    return m.invoke(entity);
                }
            }
        } catch (Exception e) {
            throw new ProgramException("提取父记录ID失败：" + e.getMessage());
        }
        return null;
    }

    /**
     * 将 source 中的非空字段合并到 target 上。
     * <p>
     * 核心作用：source（请求体）只包含用户修改的字段，target（数据库加载的完整记录）
     * 包含所有字段。合并后 target 拥有"完整原始数据 + 本次修改"。
     * <p>
     * 子类可重写以实现自定义的字段合并逻辑（如对 Map 类型字段做深度合并）。
     *
     * @param source 请求体中的实体（仅含修改字段）
     * @param target 从数据库加载的完整父记录
     */
    protected void mergeNonNullFields(M source, M target) {
        BeanUtil.copyProperties(source, target, CopyOptions.create().setIgnoreNullValue(true));
    }

    private List<M> getBotList(M current, BigInteger parentId) {
        String rootId = parentId.toString();

        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.and((Consumer<QueryWrapper>) w -> w.eq("id", rootId)
                .or((Consumer<QueryWrapper>) w2 -> w2.eq("parent_id", rootId)));

        if (hasVersionField(current)) {
            queryWrapper.orderBy(buildOrderBy("version", "asc", "version asc"));
        }
        return service.list(queryWrapper);
    }
    /**
     * 判断实体是否存在 version 字段
     */
    protected boolean hasVersionField(M entity) {
        Method[] methods = entity.getClass().getMethods();
        for (Method m : methods) {
            if ("getVersion".equals(m.getName()) && m.getParameterCount() == 0) {
                return true;
            }
        }
        return false;
    }

    private static <M> int getMaxVersion(M maxVersionRecord)  {
        try {
            return (int) maxVersionRecord.getClass().getMethod("getVersion").invoke(maxVersionRecord);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 版本号递增：读取当前记录版本号 +1，若不存在则设为 1。
     * 子类可重写以自定义版本号规则。
     */
    protected void incrementVersion(M entity, int maxVersion) {
        try {
            Method[] methods = entity.getClass().getMethods();

            int newVersion = maxVersion <= 0 ? 1 : maxVersion + 1;

            for (Method m : methods) {
                if ("setVersion".equals(m.getName())) {
                    m.invoke(entity, newVersion);
                    return;
                }
            }
        } catch (Exception e) {
            throw new ProgramException("版本号递增反射出错：" + e.getMessage());
        }
    }

    /**
     * 设置发布状态为已发布。
     * 若实体无 publishStatus 字段（通过 set 方法检测），则静默跳过。
     * 子类可重写以自定义发布状态规则。
     *
     * @param entity 实体对象
     */
    protected void applyPublishStatus(M entity) {
        try {
            Method[] methods = entity.getClass().getMethods();
            for (Method m : methods) {
                if ("setPublishStatus".equals(m.getName())) {
                    m.invoke(entity, PUBLISH_STATUS_PUBLISHED);
                    return;
                }
            }
        } catch (Exception e) {
            throw new ProgramException("设置发布状态反射出错：" + e.getMessage());
        }
    }

    /**
     * 构建父子关系：设置 parentId 和 alias。
     * <p>
     * alias 规则：alias + "_v" + version。
     * 若实体无 parentId / alias / version 字段（通过 set 方法检测），则静默跳过对应设置。
     * <p>
     * 子类可重写以实现自定义的父子关系逻辑。
     *
     * @param entity   已合并了修改字段的实体（此时仍持有父记录的完整数据）
     * @param parentId 父记录ID
     */
    protected void buildParentRelation(M entity, Object parentId) {
        try {
            Method[] methods = entity.getClass().getMethods();

            String currentAncestors = null;
            // 别名加上版本号，例如 "alias_v1"
            String alias = null;
            // 最新版本号
            Integer newVersion = null;

            boolean foundAlias = false;
            boolean foundVersion = false;

            for (Method m : methods) {

                if ("getAlias".equals(m.getName()) && m.getParameterCount() == 0) {
                    Object result = m.invoke(entity);
                    alias = result != null ? result.toString() : null;
                    foundAlias = true;
                }

                if ("getVersion".equals(m.getName()) && m.getParameterCount() == 0) {
                    Object result = m.invoke(entity);
                    newVersion = result != null ? ((Integer) result) : 1;
                    foundVersion = true;
                }

                if (foundAlias && foundVersion) {
                    break;
                }
            }

            String newAncestors;
            if (currentAncestors == null || currentAncestors.isEmpty()) {
                newAncestors = parentId.toString();
            } else {
                newAncestors = currentAncestors + "," + parentId;
            }

            // 别名加上版本号，例如 "alias_v1"
            String newAlias = alias != null ? alias + "_v" + newVersion : null;

            for (Method m : methods) {
                String name = m.getName();
                if ("setParentId".equals(name)) {
                    m.invoke(entity, parentId);
                }
                if ("setAlias".equals(name)) {
                    m.invoke(entity, newAlias);
                }
            }
        } catch (Exception e) {
            throw new ProgramException("构建父子关系反射出错：" + e.getMessage());
        }
    }

    /**
     * 清空实体的主键ID，使其以新增方式保存（由数据库生成新ID）。
     * 子类可重写以处理复合主键等情况。
     *
     * @param entity 实体对象
     */
    protected void clearEntityId(M entity) {
        try {
            Method[] methods = entity.getClass().getMethods();
            for (Method m : methods) {
                if ("setId".equals(m.getName())) {
                    m.invoke(entity, (Object) null);
                    return;
                }
            }
        } catch (Exception e) {
            throw new ProgramException("清空主键ID失败：" + e.getMessage());
        }
    }

    /**
     * 发布后置钩子，子类重写以添加发布后的额外处理逻辑。
     *
     * @param entity 已发布的新实体数据
     */
    protected void onPublishAfter(M entity) {
        // void
    }

    /**
     * 分页查询前置钩子，默认空实现。
     * 子类可重写以在分页查询前对 QueryWrapper 做定制（如只查询父亲节点）。
     *
     * @param queryWrapper 查询条件包装器
     */
    protected void onPageQueryBefore(QueryWrapper queryWrapper) {
        // 默认无操作
    }

}
