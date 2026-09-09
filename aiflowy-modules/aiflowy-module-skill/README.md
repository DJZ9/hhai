# aiflowy-modules

## 模块定位

业务领域聚合模块，承载 AIFlowy 的核心业务逻辑、实体、Mapper、Service 和领域配置。API 层通过这里的服务访问数据和执行能力。

## 子模块

`module-ai`、`module-system`、`module-auth`、`module-tenant`、`module-datascope`、`module-datacenter`、`module-datamark`、`module-sensitive-word`、`module-skill`、`module-wiki`、`module-job`、`module-log` 和 `module-autoconfig`。

## 依赖方向

业务模块可以依赖公共模块和低层业务模块，但不应依赖 API Controller。涉及跨领域调用时优先使用 Service 接口，并注意租户、部门和权限上下文的传递。
## 业务模块的统一组成

大多数业务模块遵循 config、entity、mapper、service、service.impl 的结构：Entity 对应表模型，Mapper 负责 MyBatis Flex 查询，Service 暴露领域操作，Impl 实现事务和关系维护，Config 负责 Mapper 扫描或自动配置。

## 开发顺序

新增领域时先确定表和租户字段，再编写 Entity / Mapper / Service，最后由 API 模块暴露 Controller。涉及资源关系时同时补充数据范围、审计日志、API Key 和初始化菜单。
