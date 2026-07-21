# Mirai Release Watcher

[![License](https://img.shields.io/badge/License-AGPLv3-blue.svg)](LICENSE)

Mirai Console 插件，定时监控 GitHub 仓库 Release 更新，发现新版本时通过 QQ 机器人自动推送通知。

> 上游 fork 自 [45gfg9/mirai-release-watcher-plugin](https://github.com/45gfg9/mirai-release-watcher-plugin)，使用 Kotlin 2.x + Ktor 2.x 完整重写。

---

## 功能特性

- **批量 GraphQL 查询** — 单次请求合并监控多个仓库，充分利用 GitHub API 额度
- **协程轮询** — 基于 `kotlinx.coroutines` 的异步调度，非阻塞
- **自动基线** — 首次仅记录版本不推送，避免历史 Release 轰炸；重启后基线持久化
- **预发布过滤** — 可选是否推送 Prerelease 版本
- **不存在仓库自动清理** — 仓库被删除或改名为不可用时自动从监听列表移除
- **Token 格式兼容** — 支持 Classic PAT（40 位 hex）、Fine-grained PAT（`github_pat_`）、Scope Token（`gh_`）

---

## 环境要求

| 组件 | 版本 |
|------|------|
| **Mirai Console** | ≥ 2.16.0 |
| **Mirai Core** | ≥ 2.16.0 |
| **JVM** | ≥ 17 |

---

## 快速开始

### 1. 准备 GitHub Token

前往 [GitHub Settings → Tokens](https://github.com/settings/tokens) 创建 Personal Access Token，勾选 `repo` 及 `public_repo` 权限。

> 无需 GraphQL 相关权限，`repo` 或 `public_repo` 即覆盖所有查询。

### 2. 安装插件

将构建产出的 JAR（`buildPlugin` task 生成）放入 Mirai Console 的 `plugins/` 目录，重启 Console。

### 3. 初始配置

```
/grw set bot <机器人 QQ 号>
/grw set token <GitHub Token>
/grw enable
```

### 4. 监听仓库

```
# 支持 owner/name、SSH URL、HTTPS URL 三种格式
/watch add owner/repo
/watch add git@github.com:owner/repo.git
/watch add https://github.com/owner/repo

# 查看监听列表
/watch list
```

---

## 命令参考

| 命令 | 说明 |
|------|------|
| `/grw enable` | 启用轮询 |
| `/grw disable` | 停用轮询 |
| `/grw set bot <id>` | 设置发送消息的机器人 QQ |
| `/grw set token <token>` | 设置 GitHub Token（验证后持久化） |
| `/grw set interval <ms>` | 轮询间隔（默认 30000ms） |
| `/grw set timeout <ms>` | HTTP 超时（默认 15000ms） |
| `/grw set prerelease <true\|false>` | 是否推送预发布版本（默认 true） |
| `/watch add <repo>...` | 添加监听（支持批量） |
| `/watch remove <repo>...` | 取消监听（支持批量） |
| `/watch list` | 查看当前监听列表 |

完整命令文档见 [docs/commands.md](docs/commands.md)，权限说明见 [docs/permissions.md](docs/permissions.md)。

---

## 从源码构建

```bash
# 生成插件 JAR
./gradlew buildPlugin
```

产物位于 `build/mirai/`（Mirai Console 标准插件目录结构）或 `build/libs/`。

---

## 项目结构

```
src/main/kotlin/net/xchen446/mirai/grw/
├── GrwPlugin.kt              # 插件入口
├── config/
│   ├── GrwSettings.kt        # 全局配置（自动持久化）
│   └── GrwWatches.kt         # 监听列表（自动持久化）
├── github/
│   ├── GitHubClient.kt       # Ktor HTTP 客户端（GraphQL）
│   ├── GraphQL.kt            # GraphQL 查询构建与响应类型
│   ├── Release.kt            # Release / Author / Asset 数据类
│   └── RepoId.kt             # 仓库标识（序列化 + 解析器）
├── watcher/
│   └── ReleaseWatcher.kt     # 协程轮询调度器
├── notifier/
│   └── Notifier.kt           # 消息组装与发送
└── command/
    ├── GrwCommand.kt         # 管理命令（/grw）
    └── WatchCommand.kt       # 监听命令（/watch）
```

---

## 许可证

[GNU Affero General Public License v3.0](LICENSE)

Copyright (C) 2022-2026 上游作者 45gfg9 · fork 作者 XChen446
