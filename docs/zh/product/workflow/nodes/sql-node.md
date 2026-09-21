# sql 查询
![sql-node.png](../resource/sql-node.png)

## 参数说明
### 输入参数
点击输入参数后面的"+"即可新增参数
### 占位符

```text
{{?param}}：值占位符，会被替换为 JDBC 的 "?"，取值通过参数绑定传入
   示例：SELECT id, login_name FROM tb_sys_account WHERE id = {{?id}}
{{param}}：标识符占位符，用于表名、列名等无法用 "?" 占位的位置，取值必须是合法的表名或列名
   示例：SELECT id FROM {{tableName}} WHERE id = {{?id}}
```

- 查询条件等值的位置使用值占位符，取值不参与 SQL 解析，可以承载任意字符串。
- 标识符占位符的取值只能是合法标识符：字母、数字、下划线或 `$`，最多 3 段点分（`db.table`、`db.schema.table`），每段可用一对反引号包裹；包含空白、引号、括号、逗号、分号、注释符等内容时会被拒绝，节点报错且不会执行 SQL。
- 一个模板中可以同时使用两类占位符，只有值占位符会生成 JDBC 参数。

### 输出参数
- queryData：查询数据，数组类型
- queryData 可以增加子属性，属性名为查询结果的具体字段，图中的示例增加了 title 和 id，皆为查询表中的字段，子属性可以被下一个节点读取

## 简单示例
### 示例一：直接读取查询结果
![sql-node-example1.png](../resource/sql-node-example1.png)
上图工作流执行结果如下
```json
{
    "result": {
        "queryData": [
            {
                "id": "277546322004111360",
                "dept_id": 1,
                "tenant_id": 1000000,
                "title": "xxxxx",
                "brand": "openai",
                "support_embed": true,
                "llm_endpoint": "xxxxx",
                "llm_model": "Doubao-pro-4k",
                "llm_api_key": "xxxxx",
                "llm_extra_config": "xxxxx"
            }
        ]
    },
    "errorCode": 0
}
```
### 示例二：读取 sql 查询节点定义的子属性
![sql-node-example2.png](../resource/sql-node-example2.png)
上图工作流中，内容模板节点读取了 sql 查询节点中定义的子属性 id 和 title ，执行结果如下
```json
{
    "result": {
        "queryData": "[277546322004111360],[Doubao-pro-4k]"
    },
    "errorCode": 0
}
```

## 使用约束

- 最终执行的 SQL 必须是单条查询语句：多语句与非查询语句都会被拒绝。
- 标识符占位符的取值会在服务端校验为合法标识符，校验不通过时节点直接报错，不会执行 SQL。
- 请不要在查询条件位置使用标识符占位符，这类位置请使用值占位符。

### 升级提示

历史工作流如果在查询条件位置使用了标识符占位符，执行会直接报错：

```text
SELECT id, login_name FROM tb_sys_account WHERE id = {{userId}}
报错信息：SQL 参数 [userId] 只能是合法的表名或列名；条件值请改用 {{?userId}} 占位符
```

请把条件值改成值占位符（带 "?"）后重新保存：

```text
SELECT id, login_name FROM tb_sys_account WHERE id = {{?userId}}
```

## 注意事项
> sql 查询节点仅支持查询语句
