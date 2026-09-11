package tech.aiflowy.admin.controller.datacenter;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.FastExcel;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Row;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;
import tech.aiflowy.ai.service.TableAiService;
import tech.aiflowy.common.domain.Result;
import tech.aiflowy.common.entity.DatacenterQuery;
import tech.aiflowy.common.entity.LoginAccount;
import tech.aiflowy.common.filestorage.FileStorageService;
import tech.aiflowy.common.satoken.util.SaTokenUtil;
import tech.aiflowy.common.web.controller.BaseCurdController;
import tech.aiflowy.common.web.exceptions.BusinessException;
import tech.aiflowy.datacenter.entity.DatacenterTable;
import tech.aiflowy.datacenter.entity.DatacenterTableField;
import tech.aiflowy.datacenter.entity.DatacenterTableImportRecord;
import tech.aiflowy.datacenter.entity.DatacenterTableParseTask;
import tech.aiflowy.datacenter.entity.vo.AiImportPreviewRequestVo;
import tech.aiflowy.datacenter.entity.vo.HeaderVo;
import tech.aiflowy.datacenter.entity.vo.TableConfirmVo;
import tech.aiflowy.datacenter.entity.vo.TablePreviewVo;
import tech.aiflowy.datacenter.excel.ImportTableListener;
import tech.aiflowy.datacenter.excel.ReadDataListener;
import tech.aiflowy.datacenter.excel.ReadResVo;
import tech.aiflowy.datacenter.service.*;
import tech.aiflowy.datacenter.utils.SqlInjectionUtils;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.*;

/**
 * 数据中枢表 控制层。
 *
 * @author ArkLight
 * @since 2025-07-10
 */
@RestController
@RequestMapping("/api/v1/datacenterTable")
public class DatacenterTableController extends BaseCurdController<DatacenterTableService, DatacenterTable> {

    @Resource
    private DatacenterTableFieldService fieldsService;

    @Resource
    private TableAiService tableAiService;

    @Resource
    private TableImportAsyncService tableImportAsyncService;

    @Resource
    private TableParseAsyncService tableParseAsyncService;

    @Resource
    private DatacenterTableParseTaskService parseTaskService;

    @Resource
    private DatacenterTableImportRecordService importRecordService;

    @Resource(name = "default")
    private FileStorageService fileStorageService;

    public DatacenterTableController(DatacenterTableService service) {
        super(service);
    }

    @Override
    protected Result onSaveOrUpdateBefore(DatacenterTable entity, boolean isSave) {
        LoginAccount loginUser = SaTokenUtil.getLoginAccount();
        if (isSave) {
            commonFiled(entity, loginUser.getId(), loginUser.getTenantId(), loginUser.getDeptId());
        } else {
            entity.setModifiedBy(loginUser.getId());
        }
        return super.onSaveOrUpdateBefore(entity, isSave);
    }

    @PostMapping("/saveTable")
    @SaCheckPermission("/api/v1/datacenterTable/save")
    public Result<Void> saveTable(@RequestBody DatacenterTable entity) {
        LoginAccount loginUser = SaTokenUtil.getLoginAccount();
        List<DatacenterTableField> fields = entity.getFields();
        if (CollectionUtil.isEmpty(fields)) {
            return Result.fail(99, "字段不能为空");
        }
        BigInteger id = entity.getId();
        if (id == null) {
            commonFiled(entity, loginUser.getId(), loginUser.getTenantId(), loginUser.getDeptId());
        } else {
            entity.setModified(new Date());
            entity.setModifiedBy(loginUser.getId());
        }
        service.saveTable(entity, loginUser);
        return Result.ok();
    }

    @GetMapping("/detailInfo")
    @SaCheckPermission("/api/v1/datacenterTable/query")
    public Result<DatacenterTable> detailInfo(BigInteger tableId) {
        DatacenterTable table = service.getById(tableId);
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.eq(DatacenterTableField::getTableId, tableId);
        wrapper.orderBy("id");
        List<DatacenterTableField> fields = fieldsService.list(wrapper);
        table.setFields(fields);
        return Result.ok(table);
    }

    @GetMapping("/removeTable")
    @SaCheckPermission("/api/v1/datacenterTable/remove")
    public Result<Void> removeTable(BigInteger tableId) {
        service.removeTable(tableId);
        return Result.ok();
    }

    @GetMapping("/getHeaders")
    @SaCheckPermission("/api/v1/datacenterTable/query")
    public Result<List<HeaderVo>> getHeaders(BigInteger tableId) {
        List<HeaderVo> res = service.getHeaders(tableId);
        return Result.ok(res);
    }

    @GetMapping("/getPageData")
    @SaCheckPermission("/api/v1/datacenterTable/query")
    public Result<Page<Row>> getPageData(DatacenterQuery where) {
        Page<Row> res = service.getPageData(where);
        return Result.ok(res);
    }

    @PostMapping("/saveValue")
    @SaCheckPermission("/api/v1/datacenterTable/save")
    public Result<Void> saveValue(@RequestParam Map<String, Object> map) {
        JSONObject object = new JSONObject(map);
        BigInteger tableId = object.getBigInteger("tableId");
        LoginAccount account = SaTokenUtil.getLoginAccount();
        if (tableId == null) {
            return Result.fail(99, "参数错误");
        }
        service.saveValue(tableId, object, account);
        return Result.ok();
    }

    @PostMapping("/removeValue")
    @SaCheckPermission("/api/v1/datacenterTable/remove")
    public Result<Void> removeValue(@RequestParam Map<String, Object> map) {
        JSONObject object = new JSONObject(map);
        BigInteger tableId = object.getBigInteger("tableId");
        BigInteger id = object.getBigInteger("id");
        if (tableId == null || id == null) {
            return Result.fail(99, "参数错误");
        }
        LoginAccount account = SaTokenUtil.getLoginAccount();
        service.removeValue(tableId, id, account);
        return Result.ok();
    }

    /**
     * 导入数据
     */
    @PostMapping("/importData")
    @SaCheckPermission("/api/v1/datacenterTable/save")
    public Result<ReadResVo> importData(MultipartFile file, @RequestParam Map<String, Object> map) throws Exception {
        Object tableId = map.get("tableId");
        DatacenterTable record = service.getById(tableId.toString());
        if (record == null) {
            throw new RuntimeException("数据表不存在");
        }
        InputStream is = file.getInputStream();
        List<DatacenterTableField> fields = service.getFields(record.getId());

        LoginAccount account = SaTokenUtil.getLoginAccount();
        ReadDataListener listener = new ReadDataListener(record.getId(), fields, account);
        FastExcel.read(is, listener)
                .sheet()
                .doRead();
        int totalCount = listener.getTotalCount();
        int errorCount = listener.getErrorCount();
        int successCount = listener.getSuccessCount();
        List<JSONObject> errorRows = listener.getErrorRows();
        return Result.ok(new ReadResVo(successCount, errorCount, totalCount, errorRows));
    }

    /**
     * 导入Excel并创建数据表
     */
    @PostMapping("/importTable")
    @SaCheckPermission("/api/v1/datacenterTable/save")
    public Result<ReadResVo> importTable(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tableName") String tableName,
            @RequestParam(value = "categoryId", required = false) BigInteger categoryId) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传文件");
        }
        if (StrUtil.isBlank(tableName)) {
            throw new BusinessException("表名不能为空");
        }
        if (!tableName.matches("^[a-z][a-z0-9_]*$")) {
            throw new BusinessException("只能包含小写字母、数字和下划线，且必须以小写字母开头");
        }
        SqlInjectionUtils.checkIdentifier(tableName);

        LoginAccount account = SaTokenUtil.getLoginAccount();
        try (InputStream is = file.getInputStream()) {
            ImportTableListener listener = new ImportTableListener(tableName, account, categoryId);
            FastExcel.read(is, listener)
                    .sheet()
                    .doRead();
            return Result.ok(new ReadResVo(
                    listener.getSuccessCount(),
                    listener.getErrorCount(),
                    listener.getTotalCount(),
                    listener.getErrorRows()));
        }
    }

    @GetMapping("/getTemplate")
    public void getTemplate(BigInteger tableId, HttpServletResponse response) throws Exception {
        List<DatacenterTableField> fields = service.getFields(tableId);
        // 设置响应内容类型
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        // 设置文件名
        String fileName = URLEncoder.encode("导入模板", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 动态表头数据
        List<List<String>> headList = new ArrayList<>();

        for (DatacenterTableField field : fields) {
            List<String> head = new ArrayList<>();
            head.add(field.getFieldName());
            headList.add(head);
        }

        // 写入Excel
        EasyExcel.write(response.getOutputStream())
                .head(headList)
                .sheet("模板")
                .doWrite(new ArrayList<>()); // 写入空数据，只生成模板
    }

    @RequestMapping("/exportData")
    public void exportData(BigInteger tableId, HttpServletResponse response) throws Exception {

        DatacenterTable table = service.getById(tableId);
        if (table == null) {
            throw new BusinessException("数据表不存在");
        }
        List<DatacenterTableField> fields = service.getFields(tableId);
        // 设置响应内容类型
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        // 设置文件名
        String fileName = URLEncoder.encode(table.getTableName() + "表数据", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 动态表头数据
        List<List<String>> headList = new ArrayList<>();
        // 数据
        List<List<Object>> dataList = new ArrayList<>();
        DatacenterQuery where = new DatacenterQuery();
        where.setTableId(tableId);
        List<Row> list = service.getListData(where);

        for (DatacenterTableField field : fields) {
            List<String> head = new ArrayList<>();
            head.add(field.getFieldName());
            headList.add(head);
        }

        for (Row row : list) {
            List<Object> data = new ArrayList<>();
            for (DatacenterTableField field : fields) {
                Object o = row.get(field.getFieldName());
                data.add(o);
            }
            dataList.add(data);
        }

        EasyExcel.write(response.getOutputStream())
                .head(headList)
                .sheet("sheet1")
                .doWrite(dataList);
    }

    /**
     * AI智能导入 - 创建解析任务接口
     * 用户先上传文件，然后调用此接口创建解析任务
     * 后台异步执行解析（解析Excel + AI生成表名/描述/字段名）
     * 返回任务ID，前端轮询查询任务状态
     */
    @PostMapping("/createParseTask")
    @SaCheckPermission("/api/v1/datacenterTable/save")
    public Result<BigInteger> createParseTask(@RequestBody AiImportPreviewRequestVo request) {

        if (request.getFiles() == null || request.getFiles().isEmpty()) {
            throw new BusinessException("请上传文件");
        }

        LoginAccount account = SaTokenUtil.getLoginAccount();

        // 构建输入数据
        JSONObject inputData = new JSONObject();
        JSONArray filesArray = new JSONArray();
        for (AiImportPreviewRequestVo.FileItemVo fileItem : request.getFiles()) {
            JSONObject fileJson = new JSONObject();
            fileJson.put("path", fileItem.getPath());
            fileJson.put("originalFileName", fileItem.getOriginalFileName());
            filesArray.add(fileJson);
        }
        inputData.put("files", filesArray);
        inputData.put("categoryId", request.getCategoryId());

        // 创建任务名称
        String taskName = "AI解析-" + request.getFiles().get(0).getOriginalFileName();
        if (request.getFiles().size() > 1) {
            taskName += " 等" + request.getFiles().size() + "个文件";
        }

        // 创建解析任务
        BigInteger taskId = parseTaskService.createTask(
                taskName,
                DatacenterTableParseTask.TASK_TYPE_AI_TABLE_PARSE,
                inputData.toJSONString(),
                account.getDeptId(),
                account.getTenantId(),
                account.getId()
        );

        // 构建文件列表用于异步解析
        List<TableParseAsyncService.FileItemVo> files = new ArrayList<>();
        for (AiImportPreviewRequestVo.FileItemVo fileItem : request.getFiles()) {
            TableParseAsyncService.FileItemVo file = new TableParseAsyncService.FileItemVo();
            file.setPath(fileItem.getPath());
            file.setOriginalFileName(fileItem.getOriginalFileName());
            files.add(file);
        }

        // 异步执行解析任务，传递主线程上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        tableParseAsyncService.asyncParseTables(taskId, files, request.getCategoryId(),
                (input) -> tableAiService.generateTablePreview(input.getHeaders(), input.getSampleData()),
                requestAttributes);

        return Result.ok(taskId);
    }


    /**
     * AI智能导入 - 查询任务状态接口
     * 前端轮询此接口获取任务状态和进度
     */
    @GetMapping("/getParseTaskStatus")
    @SaCheckPermission("/api/v1/datacenterTable/save")
    public Result<TaskStatusVo> getParseTaskStatus(@RequestParam BigInteger taskId) {
        DatacenterTableParseTask task = parseTaskService.getTaskStatus(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        TaskStatusVo statusVo = new TaskStatusVo();
        statusVo.setTaskId(taskId);
        statusVo.setStatus(task.getStatus());
        statusVo.setProgress(task.getProgress());
        statusVo.setMessage(task.getProgressMessage());
        if (task.getStatus() == DatacenterTableParseTask.STATUS_FAILED) {
            statusVo.setError(task.getErrorInfo());
        }

        return Result.ok(statusVo);
    }

    /**
     * AI智能导入 - 获取任务结果接口
     * 任务成功后，前端调用此接口获取解析结果
     */
    @GetMapping("/getParseTaskResult")
    @SaCheckPermission("/api/v1/datacenterTable/save")
    public Result<List<TablePreviewVo>> getParseTaskResult(@RequestParam BigInteger taskId) {
        DatacenterTableParseTask task = parseTaskService.getTaskStatus(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        if (task.getStatus() != DatacenterTableParseTask.STATUS_SUCCESS) {
            throw new BusinessException("任务未完成或失败");
        }

        // 解析结果数据
        JSONObject resultData = JSONObject.parseObject(task.getResultData());
        JSONArray tablesArray = resultData.getJSONArray("tables");

        List<TablePreviewVo> tables = new ArrayList<>();
        for (int i = 0; i < tablesArray.size(); i++) {
            JSONObject tableJson = tablesArray.getJSONObject(i);
            TablePreviewVo table = new TablePreviewVo();
            table.setOriginalFileName(tableJson.getString("originalFileName"));
            table.setTableName(tableJson.getString("tableName"));
            table.setTableDesc(tableJson.getString("tableDesc"));
            table.setCategoryId(tableJson.getBigInteger("categoryId"));
            table.setTempFilePath(tableJson.getString("tempFilePath"));
            table.setTotalRows(tableJson.getInteger("totalRows"));
            table.setErrorInfo(tableJson.getString("errorInfo"));

            // 解析字段列表
            JSONArray fieldsArray = tableJson.getJSONArray("fields");
            List<tech.aiflowy.datacenter.entity.vo.FieldPreviewVo> fields = new ArrayList<>();
            if (fieldsArray != null) {
                for (int j = 0; j < fieldsArray.size(); j++) {
                    JSONObject fieldJson = fieldsArray.getJSONObject(j);
                    tech.aiflowy.datacenter.entity.vo.FieldPreviewVo field = new tech.aiflowy.datacenter.entity.vo.FieldPreviewVo();
                    field.setOriginalName(fieldJson.getString("originalName"));
                    field.setFieldName(fieldJson.getString("fieldName"));
                    field.setFieldDesc(fieldJson.getString("fieldDesc"));
                    field.setFieldType(fieldJson.getInteger("fieldType"));
                    fields.add(field);
                }
            }
            table.setFields(fields);

            // 解析样本数据
            if (tableJson.containsKey("sampleData")) {
                JSONArray sampleArray = tableJson.getJSONArray("sampleData");
                List<Map<String, Object>> sampleData = new ArrayList<>();
                for (int k = 0; k < sampleArray.size(); k++) {
                    JSONObject sampleObj = sampleArray.getJSONObject(k);
                    Map<String, Object> sampleMap = new LinkedHashMap<>();
                    for (String key : sampleObj.keySet()) {
                        sampleMap.put(key, sampleObj.get(key));
                    }
                    sampleData.add(sampleMap);
                }
                table.setSampleData(sampleData);
            }

            tables.add(table);
        }

        return Result.ok(tables);
    }

    /**
     * AI智能导入 - 确认导入接口
     * 用户确认后异步导入数据
     * 支持两种模式：
     * 1. 传递任务ID（taskId）- 从任务结果中获取数据
     * 2. 直接传递表结构数据
     */
    @PostMapping("/confirmImportTables")
    @SaCheckPermission("/api/v1/datacenterTable/save")
    public Result<Void> confirmImportTables(@RequestBody List<TableConfirmVo> confirmList) {
        if (confirmList == null || confirmList.isEmpty()) {
            throw new BusinessException("确认列表不能为空");
        }

        LoginAccount account = SaTokenUtil.getLoginAccount();

        // 处理任务ID模式
        for (TableConfirmVo confirm : confirmList) {
            if (confirm.getTaskId() != null) {
                // 从任务结果中获取数据
                DatacenterTableParseTask task = parseTaskService.getTaskStatus(confirm.getTaskId());
                if (task == null || task.getStatus() != DatacenterTableParseTask.STATUS_SUCCESS) {
                    throw new BusinessException("任务不存在或未完成: " + confirm.getTaskId());
                }

                // 解析任务结果
                JSONObject resultData = JSONObject.parseObject(task.getResultData());
                JSONArray tablesArray = resultData.getJSONArray("tables");

                // 找到对应的表
                for (int i = 0; i < tablesArray.size(); i++) {
                    JSONObject tableJson = tablesArray.getJSONObject(i);
                    if (tableJson.getString("originalFileName").equals(confirm.getOriginalFileName())) {
                        // 如果用户修改了表名/描述，使用用户的值
                        if (confirm.getTableName() != null) {
                            tableJson.put("tableName", confirm.getTableName());
                        }
                        if (confirm.getTableDesc() != null) {
                            tableJson.put("tableDesc", confirm.getTableDesc());
                        }
                        if (confirm.getCategoryId() != null) {
                            tableJson.put("categoryId", confirm.getCategoryId());
                        }

                        // 如果用户修改了字段，使用用户的值
                        if (confirm.getFields() != null) {
                            JSONArray userFields = new JSONArray();
                            for (TableConfirmVo.FieldConfirmVo fieldConfirm : confirm.getFields()) {
                                JSONObject fieldJson = new JSONObject();
                                fieldJson.put("originalName", fieldConfirm.getOriginalName());
                                fieldJson.put("fieldName", fieldConfirm.getFieldName());
                                fieldJson.put("fieldDesc", fieldConfirm.getFieldDesc());
                                fieldJson.put("fieldType", fieldConfirm.getFieldType());
                                userFields.add(fieldJson);
                            }
                            tableJson.put("fields", userFields);
                        }

                        // 更新confirm对象
                        confirm.setTableName(tableJson.getString("tableName"));
                        confirm.setTableDesc(tableJson.getString("tableDesc"));
                        confirm.setCategoryId(tableJson.getBigInteger("categoryId"));
                        confirm.setTempFilePath(tableJson.getString("tempFilePath"));

                        // 解析字段列表
                        JSONArray fieldsArray = tableJson.getJSONArray("fields");
                        List<TableConfirmVo.FieldConfirmVo> fields = new ArrayList<>();
                        for (int j = 0; j < fieldsArray.size(); j++) {
                            JSONObject fieldJson = fieldsArray.getJSONObject(j);
                            TableConfirmVo.FieldConfirmVo field = new TableConfirmVo.FieldConfirmVo();
                            field.setOriginalName(fieldJson.getString("originalName"));
                            field.setFieldName(fieldJson.getString("fieldName"));
                            field.setFieldDesc(fieldJson.getString("fieldDesc"));
                            field.setFieldType(fieldJson.getInteger("fieldType"));
                            fields.add(field);
                        }
                        confirm.setFields(fields);
                        break;
                    }
                }
            }
        }

        // 异步导入数据，传递主线程上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        tableImportAsyncService.asyncImportTables(confirmList, account, requestAttributes);
        return Result.ok();
    }

    /**
     * 任务状态VO
     */
    public static class TaskStatusVo {
        private BigInteger taskId;
        private Integer status;
        private Integer progress;
        private String message;
        private String error;

        public BigInteger getTaskId() {
            return taskId;
        }

        public void setTaskId(BigInteger taskId) {
            this.taskId = taskId;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }

        public Integer getProgress() {
            return progress;
        }

        public void setProgress(Integer progress) {
            this.progress = progress;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    /**
     * 查询当前用户的导入记录列表
     */
    @GetMapping("/getImportRecords")
    @SaCheckPermission("/api/v1/datacenterTable/query")
    public Result<Page<DatacenterTableImportRecord>> getImportRecords(
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        LoginAccount account = SaTokenUtil.getLoginAccount();
        QueryWrapper wrapper = QueryWrapper.create();
        wrapper.eq(DatacenterTableImportRecord::getCreatedBy, account.getId());
        wrapper.orderBy(DatacenterTableImportRecord::getCreated, false);
        Page<DatacenterTableImportRecord> page = importRecordService.page(new Page<>(pageNumber, pageSize), wrapper);
        return Result.ok(page);
    }

    /**
     * 查询指定导入记录的详情（用于轮询进度）
     */
    @GetMapping("/getImportRecordDetail")
    @SaCheckPermission("/api/v1/datacenterTable/query")
    public Result<DatacenterTableImportRecord> getImportRecordDetail(@RequestParam BigInteger recordId) {
        DatacenterTableImportRecord record = importRecordService.getById(recordId);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        LoginAccount account = SaTokenUtil.getLoginAccount();
        if (!record.getCreatedBy().equals(account.getId())) {
            throw new BusinessException("无权限查看此记录");
        }
        return Result.ok(record);
    }
}
