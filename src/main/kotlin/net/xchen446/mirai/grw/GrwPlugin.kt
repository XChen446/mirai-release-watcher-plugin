package net.xchen446.mirai.grw

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregisterAllCommands
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionId
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.xchen446.mirai.grw.command.GrwCommand
import net.xchen446.mirai.grw.command.WatchCommand
import net.xchen446.mirai.grw.config.GrwSettings
import net.xchen446.mirai.grw.config.GrwWatches
import net.xchen446.mirai.grw.github.GitHubClient
import net.xchen446.mirai.grw.notifier.Notifier
import net.xchen446.mirai.grw.watcher.ReleaseWatcher

/**
 * 敏感操作权限节点，用于区分 `/grw set token`、`/grw set bot` 等管理类操作
 * 与普通操作。需与 `command.grw` 同时拥有方可执行敏感子命令。
 */
const val ADMIN_PERMISSION_NAME = "admin"

/**
 * GitHub Release Watcher 插件入口。
 *
 * 持有 [GitHubClient] 与 [ReleaseWatcher] 的运行时实例，并对外暴露
 * [applyToken] / [rebuildClient] / [restartWatcher] 供命令层在运行时调整配置。
 */
object GrwPlugin : KotlinPlugin(
    JvmPluginDescription(
        "net.xchen446.mirai.grw",
        "2.0.0",
        "GitHub Release Watcher",
    ) {
        author("XChen446")
        info("监控 GitHub 仓库 Release 更新并通过 Mirai 推送")
    }
) {
    @Volatile private var client: GitHubClient? = null
    @Volatile private var watcher: ReleaseWatcher? = null

    private val notifier by lazy { Notifier(GrwSettings, logger) }

    /** 敏感操作权限节点（`net.xchen446.mirai.grw:admin`），在 [onEnable] 中初始化。 */
    @Volatile var adminPermission: Permission? = null
        private set

    override fun onEnable() {
        super.onEnable()

        GrwSettings.reload()
        GrwWatches.reload()

        adminPermission = ensurePermission(ADMIN_PERMISSION_NAME, "管理 GitHub Token 与机器人 ID 等敏感配置")

        val token = GrwSettings.token
        if (isTokenSet(token)) {
            runCatching { applyToken(token) }
                .onFailure { logger.warning("启动时 Token 验证失败：${it.message}") }
        } else {
            logger.info("Token 未配置，请使用 /grw set token <token> 设置后启用")
        }

        GrwCommand.register()
        WatchCommand.register()

        restartWatcher()
    }

    override fun onDisable() {
        super.onDisable()
        watcher?.stop()
        client?.close()
        unregisterAllCommands(this)
    }

    /**
     * 用新 Token 重建 [GitHubClient] 并验证有效性。
     * 验证通过后写入 [GrwSettings.token] 持久化，并重启 watcher。
     */
    fun applyToken(token: String): Boolean {
        val verified = runBlocking {
            buildClient(token).use { runCatching { it.verifyToken() }.getOrDefault(false) }
        }
        if (!verified) return false
        client?.close()
        client = buildClient(token)
        GrwSettings.token = token
        restartWatcher()
        return true
    }

    /**
     * 仅按当前 [GrwSettings.token] 重建客户端，不做网络验证。
     * 供 set timeout 等不涉及 Token 变更的场景使用。
     */
    fun rebuildClient() {
        val token = GrwSettings.token
        if (!isTokenSet(token)) return
        client?.close()
        client = buildClient(token)
        restartWatcher()
    }

    /** 重启轮询，使 interval/timeout/client 等变更即时生效。 */
    fun restartWatcher() {
        watcher?.stop()
        val c = client ?: return
        watcher = ReleaseWatcher(c, GrwWatches, GrwSettings, notifier, logger).also { it.start() }
    }

    private fun buildClient(token: String): GitHubClient =
        GitHubClient(token, GrwSettings.timeout)

    private fun isTokenSet(token: String): Boolean =
        token.length == 40 || token.startsWith("github_pat_") || token.startsWith("gh")

    /**
     * 获取或注册一个权限节点。名称沿用插件 id 作为 namespace。
     * 重复注册（插件重载）时直接复用已存在的权限，避免 [PermissionRegistryConflictException]。
     */
    private fun ensurePermission(name: String, desc: String): Permission {
        val service = PermissionService.INSTANCE
        val pid = PermissionId(description.id.lowercase(), name)
        service[pid]?.let { return it }
        return service.register(pid, desc)
    }
}