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
import net.xchen446.mirai.grw.github.ContributorsRequest
import net.xchen446.mirai.grw.github.GitHubClient
import net.xchen446.mirai.grw.github.Release
import net.xchen446.mirai.grw.github.RepoId
import net.xchen446.mirai.grw.notifier.Notification
import net.xchen446.mirai.grw.notifier.Notifier

/**
 * Release 轮询调度器。
 *
 * 按 [GrwSettings.interval] 周期性向 GitHub 发起批量 GraphQL 查询，
 * 对比每个仓库的 [WatchEntry.lastReleaseTag] 基线判定新版本，再交由 [Notifier] 推送。
 *
 * - 单次请求按 [BATCH_SIZE] 分批，规避 GitHub GraphQL 单查询复杂度限制
 * - 不存在的仓库自动从监听列表移除并告警
 * - 预发布按 [GrwSettings.includePrerelease] 过滤，但无论是否推送都会更新基线
 * - 首次记录到基线时不推送（避免历史 Release 轰炸），由 lastReleaseTag 初始为 null 保证
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

    private data class PendingRelease(
        val repo: RepoId,
        val release: Release,
        val subscribers: Set<Long>,
        val prevTag: String,
        val avatarUrl: String?,
    )

    private suspend fun tick() {
        val watches = watchesConfig.watches
        if (watches.isEmpty()) return
        logger.verbose("Starting a new request with ${watches.size} repo(s)")

        val pending = mutableListOf<PendingRelease>()
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

                val prevTag = entry.lastReleaseTag
                entry.lastReleaseTag = release.tagName

                val isNewRelease = release.tagName != prevTag && prevTag != null
                val shouldPush = isNewRelease && (!release.isPrerelease || settings.includePrerelease)
                if (shouldPush) {
                    pending += PendingRelease(repo, release, entry.userSubscribers + entry.groupSubscribers, prevTag!!, node.owner?.avatarUrl)
                }
            }
        }

        nonexistent.forEach { repo ->
            logger.warning("Repository $repo does not exist or is inaccessible, removing from watches")
            watches.remove(repo)
        }

        val contributorsMap = if (pending.isNotEmpty()) {
            val reqs = pending.map { ContributorsRequest(it.repo, it.release.tagName, it.prevTag) }
            runCatching { client.fetchContributors(reqs) }
                .onFailure { logger.warning("Failed to fetch contributors", it) }
                .getOrDefault(emptyMap())
        } else emptyMap()

        val toNotify = pending.map { (repo, release, subscribers, _, avatarUrl) ->
            repo to Notification(release, subscribers, contributorsMap[repo] ?: emptySet(), avatarUrl)
        }

        notifier.notify(toNotify)
    }

    private companion object {
        const val BATCH_SIZE = 50
    }
}