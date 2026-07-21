# 权限说明

## 默认权限模型

本插件基于 Mirai Console 的权限系统。命令注册时会自动创建以下权限节点：

| 命令 | 默认权限 ID | 默认拥有者 |
|------|------------|-----------|
| `/grw` | `net.xchen446.mirai.grw:command.grw` | 控制台 |
| `/watch` | `net.xchen446.mirai.grw:command.watch` | 控制台 |

普通用户默认**不拥有**上述权限，需管理员通过 `/permission permit <权限ID> <目标>` 显式授权。

## 敏感操作权限

`/grw` 内部进一步区分了普通与敏感操作。敏感子命令除了 `command.grw`，还另需 **`net.xchen446.mirai.grw:admin`** 权限：

| 子命令 | 需要权限 | 说明 |
|--------|---------|------|
| `/grw enable` / `/grw disable` | `command.grw` | 启停轮询 |
| `/grw set interval ...` | `command.grw` | 调整轮询间隔 |
| `/grw set timeout ...` | `command.grw` | 调整 HTTP 超时 |
| `/grw set prerelease ...` | `command.grw` | 开关预发布推送 |
| `/grw set bot ...` | `command.grw` + `admin` | 修改发送机器人（敏感） |
| `/grw set token ...` | `command.grw` + `admin` | 修改 GitHub Token（敏感） |

> 控制台默认拥有 root 权限，全部子命令均可执行。

授权建议：
- 普通管理员：仅授予 `command.grw` → 可启停、调参，但不能换 Token / Bot
- 信任运维：授予 `command.grw` + `admin` → 全功能

---

## 推荐：LuckPerms-Mirai

对于需要更细粒度控制的场景（例如：仅允许特定群管理员使用 `/watch add`，或禁止在群里执行 `/grw set token` 等），建议配合 [Karlatemp/LuckPerms-Mirai](https://github.com/Karlatemp/LuckPerms-Mirai) 使用。

LuckPerms-Mirai 支持通过 **context** 定义权限上下文，例如：

- 限制某命令只能在特定群使用
- 限制某用户/群成员才能订阅仓库
- 禁止群聊中暴露 Token 的敏感命令

具体用法请参阅 [LuckPerms-Mirai 官方文档](https://github.com/Karlatemp/LuckPerms-Mirai)。

---

## 安全建议

- `/grw set token <token>` 会将要设置的 Token 明文显示在聊天记录中。建议在**控制台**或**私聊机器人**时执行，避免群聊历史泄露。
- 使用 LuckPerms-Mirai 时，可额外为敏感上下文（如群聊）禁用 `command.grw` 权限。
- 仓库订阅行为与执行者所在会话绑定：私聊订阅推送给个人，群聊订阅推送给整个群。
