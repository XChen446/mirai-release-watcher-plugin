# 命令参考

## /grw —— 全局管理命令

| 子命令 | 权限 | 说明 |
|--------|------|------|
| `enable` | 控制台 | 启用轮询循环 |
| `disable` | 控制台 | 停用轮询循环 |
| `set bot <id>` | 控制台 | 设置发送通知消息的机器人 QQ 号 |
| `set token <token>` | 控制台 | 设置并验证 GitHub Token（支持 Classic/Fine-grained/Scope 三类） |
| `set interval <ms>` | 控制台 | 设置轮询间隔，需 > 0 |
| `set timeout <ms>` | 控制台 | 设置 HTTP 请求超时，需 > 0 |
| `set prerelease <true\|false>` | 控制台 | 是否推送预发布版本（默认 true） |

### 用法示例

```
/grw enable
/grw disable
/grw set bot 123456789
/grw set token ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
/grw set interval 60000
/grw set timeout 10000
/grw set prerelease false
```

> `set` 系列子命令使用多段路由实现，如 `/grw set token` 在帮助中显示为 `grw set token` 而非 `grw set token <...>`。

---

## /watch —— 监听管理命令

| 子命令 | 权限 | 说明 |
|--------|------|------|
| `add <repo>...` | 用户 | 添加仓库到监听列表（支持批量，空格分隔） |
| `remove <repo>...` | 用户 | 从监听列表移除仓库（支持批量） |
| `list` | 用户/控制台 | 查看当前监听列表 |

### 仓库格式

支持三种输入格式，`add/remove` 命令自动识别：

| 格式 | 示例 |
|------|------|
| `owner/name` | `neovim/neovim` |
| SSH Clone URL | `git@github.com:neovim/neovim.git` |
| HTTPS URL | `https://github.com/neovim/neovim` |

### 用途示例

```
/watch add neovim/neovim
/watch add neovim/neovim torvalds/linux
/watch remove neovim/neovim
/watch list
```

### 订阅行为

- **私聊**：订阅者为自己的 QQ 号，消息推送到个人
- **群聊**：订阅者为群号，消息推送到整个群
- **取消订阅**：仅移除当前发送者，其他订阅者不受影响；全部退订后自动删除仓库条目

---

## 通知消息示例

```
【neovim/neovim】发现新版本！
URL: https://github.com/neovim/neovim/releases/tag/v0.10.0
名称: NVIM v0.10.0
Tag: v0.10.0
发布时间: 2024-05-15T18:00:00Z
更新时间: 2024-05-15T19:30:00Z
作者: justinmk (Justin M. Keyes)
包含 6 个资源文件：
--------------------
  文件名: nvim-linux64.tar.gz
  大小: 48.52MB
  下载链接: https://github.com/neovim/neovim/releases/download/v0.10.0/nvim-linux64.tar.gz
--------------------
  文件名: nvim-win64.msi
  大小: 52.10MB
  下载链接: https://github.com/neovim/neovim/releases/download/v0.10.0/nvim-win64.msi
……
```
