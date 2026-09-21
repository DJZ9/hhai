# MCP 快速开始

## 1. 安全模式说明

前端 MCP 页面只允许手动添加 HTTP、SSE、Streamable HTTP 等远程 MCP。

`npx`、`python`、`docker` 等本地命令型 MCP 不再允许由前端直接录入，必须由管理员预置到 classpath 清单：

```yaml
aiflowy:
  mcp:
    admin-config-classpath: mcp/aiflowy-mcp-catalog.json
```

清单文件在服务启动时加载，修改后需要重新构建并部署。

## 2. 前端添加远程 MCP

1. 在 MCP 管理页面点击“添加”。
2. 选择“手动添加 HTTP MCP”页签。
3. 填写名称、描述和远程 MCP 配置：

```json
{
  "mcpServers": {
    "remote-mcp": {
      "type": "http-sse",
      "url": "https://mcp.example.com/sse",
      "headers": {
        "Authorization": "Bearer <token>"
      }
    }
  }
}
```

4. 保存后，在列表中启动 MCP，并通过编辑页的“工具”页签查看工具列表。

## 3. Classpath 清单安装

管理员在 `src/main/resources/mcp/aiflowy-mcp-catalog.json` 中按 `entries` 添加 MCP：

```json
{
  "entries": {
    "local-filesystem": {
      "title": "本地文件系统",
      "description": "随应用发布的共享 stdio MCP",
      "enabled": true,
      "config": {
        "mcpServers": {
          "filesystem": {
            "command": "npx",
            "args": ["-y", "@modelcontextprotocol/server-filesystem", "/data"]
          }
        }
      }
    }
  }
}
```

重新构建并重启服务后，前端用户在“添加 -> 从后台目录安装”页签中选择并安装。安装后的 MCP 默认停用，确认后再手动启动。

> **注意**：目录条目由管理员统一维护，所有安装用户共享同一个服务实例和认证信息；任意用户停止共享 MCP 都会影响其他使用者。

## 4. 可选：外部目录覆盖

如果部署后需要不改包修改配置，可以额外设置外部目录：

```yaml
aiflowy:
  mcp:
    admin-config-dir: /www/aiflowy/mcp
```

外部目录中每个 JSON 文件定义一个 MCP，文件名去掉 `.json` 后就是 `catalogKey`。同 key 的外部目录条目会覆盖 classpath 清单。不需要外部目录时不要配置该项，保持默认空值即可。

## 5. 清单约束

- `entries` 的每个 key 就是 `catalogKey`。
- 每个条目只能包含一个 `config.mcpServers` 条目。
- `enabled=false` 的条目不会出现在前端目录页签。
- 支持 stdio 以及 HTTP/SSE 等远程 MCP。
- classpath 清单随构建产物发布，运行期修改需要重新构建和部署。

