package net.xchen446.mirai.grw.watcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.utils.MiraiLogger
import net.xchen446.mirai.grw.config.GrwSettings
import net.xchen446.mirai.grw.config.GrwWatches
import net.xchen446.mirai.grw.config.WatchEntry
import net.xchen446.mirai.grw.github.GitHubClient
import net.xchen446.mirai.grw.github.RepoId
import net.xchen446.mirai.grw.github.Release
import net.xchen446.mirai.grw.notifier.Notification
import net.xchen446.mirai.grw.notifier.Notifier

/**
 * Release 轮询调度器。
 *
 * 按 [GrwSettings.interval] 周期性向 GitHub 发起批量 GraphQL 查询，
 * 对比每个仓库的 [WatchEntry.lastTag] 基线判定新版本，再交由 [Notifier] 推送。
 *
 * - 单次请求按 [BATCH_SIZE] 分批，规避 GitHub GraphQL 单查询复杂度限制
 * - 不存在的仓库自动从监听列表移除并告警
 * - 预发布按 [GrwSettings.includePrerelease] 过滤，但无论是否推送都会更新基线
 * - 首次记录到基线时不推送（避免历史 Release 轰炸），由 lastTag 初始为 null 保证
 */
class ReleaseWatcher(
    private val client: GitHubClient,
    private val watchesConfig: GrwWatches,
    private val settings: GrwSettings,
    private val notifier: Notifier,
    private val logger: MiraiLogger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                if (settings.enabled) {
                    runCatching { tick() }.onFailure { logger.error("Watcher tick failed", it) }
                }
                delay(settings.interval)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private suspend fun tick() {
        val watches = watchesConfig.watches
        if (watches.isEmpty()) return
        logger.verbose("Starting a new request with ${watches.size} repo(s)")

        val toNotify = mutableListOf<Pair<RepoId, Notification>>()
        val nonexistent = mutableListOf<RepoId>()

        for (batch in watches.keys.toList().chunked(BATCH_SIZE)) {
            val response = runCatching { client.query(batch.toSet()) }
                .onFailure { logger.error("GraphQL request failed", it) }
                .getOrNull() ?: continue
            if (response.errors.isNotEmpty()) {
                logger.error("GitHub returned errors: ${response.errors}")
                continue
            }
            val data = response.data ?: continue

            for (repo in batch) {
                val node = data[repo.toLegalId()]
                val entry = watches[repo]
                if (node == null || entry == null) {
                    nonexistent += repo
                    continue
                }
                val release = node.latestRelease ?: continue
                handleRelease(repo, release, entry, toNotify)
            }
        }

        nonexistent.forEach { repo ->
            logger.warning("Repository $repo does not exist or is inaccessible, removing from watches")
            watches.remove(repo)
        }

        notifier.notify(toNotify)
    }

    private fun handleRelease(
        repo: RepoId,
        release: Release,
        entry: WatchEntry,
        toNotify: MutableList<Pair<RepoId, Notification>>,
    ) {
        val isNewTag = release.tagName != entry.lastTag && entry.lastTag != null
        val shouldPush = isNewTag && (!release.isPrerelease || settings.includePrerelease)
        // 始终更新基线，避免下次重复处理；预发布被过滤时也不应再次触发
        entry.lastTag = release.tagName
        if (shouldPush) {
            toNotify += repo to Notification(release, entry.userSubscribers + entry.groupSubscribers)
        }
    }

    private companion object {
        const val BATCH_SIZE = 50
    }
}