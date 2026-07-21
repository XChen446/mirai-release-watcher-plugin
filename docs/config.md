# 配置参考

## 配置文件位置

Mirai Console 配置自动持久化在 `config/net.xchen446.mirai.grw/` 目录下：

```
config/net.xchen446.mirai.grw/
├── settings.yml    # 全局设置
└── watches.yml     # 监听列表
```

---

## settings.yml

```yaml
botId: 0
enabled: false
token: unset
interval: 30000
timeout: 15000
includePrerelease: true
```

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `botId` | long | `0` | 发送消息的机器人的 QQ 号，为 0 时不发送 |
| `enabled` | bool | `false` | 是否启动轮询循环 |
| `token` | string | `"unset"` | GitHub Personal Access Token |
| `interval` | long | `30000` | 轮询间隔（ms） |
| `timeout` | long | `15000` | HTTP 请求超时（ms） |
| `includePrerelease` | bool | `true` | 是否推送预发布版本 |

> `token` 通过 `/grw set token <token>` 设置并持久化，不可手动写入配置文件（会被自动覆盖）。

---

## watches.yml

```yaml
watches: {}
```

`watches` 是一个映射，键为仓库标识 `owner/name`，值为包含订阅者集合与最后推送 Tag 的对象：

```yaml
watches:
  neovim/neovim:
    lastTag: v0.10.0
    subscribers:
    - 123456789
  torvalds/linux:
    lastTag: v6.6
    subscribers:
    - 123456789
    - 987654321
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `lastTag` | string\|null | 最近一次推送的 Git Tag。`null` 表示尚未建立基线，下次轮询仅记录不推送 |
| `subscribers` | long set | 订阅者的 QQ 号或群号。私聊为个人 QQ，群聊为群号 |
