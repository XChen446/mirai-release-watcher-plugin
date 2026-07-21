package net.xchen446.mirai.grw.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.xchen446.mirai.grw.GrwPlugin
import net.xchen446.mirai.grw.config.GrwSettings

/**
 * 全局管理命令：/grw enable|disable|set ...
 *
 * set 子命令通过多段路由实现真正的二级子命令（grw set bot/token/...），
 * 不再依赖原项目里 "set bot" 单字符串带空格的 hack。
 *
 * ### 权限粒度
 * 管理命令整体受 `net.xchen446.mirai.grw:command.grw` 控制；
 * [setBot] 与 [setToken] 这类修改敏感配置的子命令另需 `net.xchen446.mirai.grw:admin`。
 * 普通管理员可 enable/disable 与设置 interval/timeout/prerelease，但不能改 Token 与 Bot。
 */
object GrwCommand : CompositeCommand(
    GrwPlugin, "grw",
    description = "GitHub Release Watcher 管理命令",
) {
    // 兼容 classic PAT（40 位 hex）、fine-grained (github_pat_...) 与 scoped (gh[pousr]_...) 三种 Token
    private val TOKEN_REGEX = Regex(
        "^[0-9a-fA-F]{40}\$" +
            "|^github_pat_[A-Za-z0-9_]{82}\$" +
            "|^gh[pousr]_[A-Za-z0-9]{36}\$"
    )

    @SubCommand
    suspend fun CommandSender.enable() {
        GrwSettings.enabled = true
        sendMessage("GitHub Release Watcher 已启用")
    }

    @SubCommand
    suspend fun CommandSender.disable() {
        GrwSettings.enabled = false
        sendMessage("GitHub Release Watcher 已禁用")
    }

    @SubCommand("set", "bot")
    suspend fun CommandSender.setBot(id: Long) {
        if (!requireAdmin()) return
        GrwSettings.botId = id
        sendMessage("已将发送消息的机器人设为 $id")
    }

    @SubCommand("set", "token")
    suspend fun CommandSender.setToken(token: String) {
        if (!requireAdmin()) return
        if (!TOKEN_REGEX.matches(token)) {
            sendMessage("Token 格式不正确，应为 40 位 hex、github_pat_ 或 gh[pousr]_ 前缀的 Token")
            return
        }
        if (GrwPlugin.applyToken(token)) {
            sendMessage("Token 已设置并验证通过")
        } else {
            sendMessage("Token 验证失败，请检查有效性或网络")
        }
    }

    @SubCommand("set", "interval")
    suspend fun CommandSender.setInterval(interval: Long) {
        if (interval <= 0) {
            sendMessage("轮询间隔必须为正数")
            return
        }
        GrwSettings.interval = interval
        GrwPlugin.restartWatcher()
        sendMessage("轮询间隔已设为 ${interval}ms 并已重启轮询")
    }

    @SubCommand("set", "timeout")
    suspend fun CommandSender.setTimeout(timeout: Long) {
        if (timeout <= 0) {
            sendMessage("请求超时必须为正数")
            return
        }
        GrwSettings.timeout = timeout
        GrwPlugin.rebuildClient()
        sendMessage("请求超时已设为 ${timeout}ms")
    }

    @SubCommand("set", "prerelease")
    suspend fun CommandSender.setPrerelease(enabled: Boolean) {
        GrwSettings.includePrerelease = enabled
        sendMessage("预发布版本推送已${if (enabled) "开启" else "关闭"}")
    }

    /**
     * 校验当前发送者是否拥有敏感操作权限 `net.xchen446.mirai.grw:admin`。
     * 未通过则发送提示并返回 false。控制台始终通过（root）。
     */
    private suspend fun CommandSender.requireAdmin(): Boolean {
        val admin = GrwPlugin.adminPermission ?: return true
        if (!hasPermission(admin)) {
            sendMessage("权限不足：该操作需要 net.xchen446.mirai.grw:admin 权限")
            return false
        }
        return true
    }
}