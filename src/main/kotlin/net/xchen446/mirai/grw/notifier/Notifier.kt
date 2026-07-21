package net.xchen446.mirai.grw.notifier

import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getContactOrNull
import net.mamoe.mirai.utils.MiraiLogger
import net.xchen446.mirai.grw.github.Release
import net.xchen446.mirai.grw.github.RepoId

data class Notification(
    val release: Release,
    val subscribers: Set<Long>,
)

/**
 * 负责组装 Release 消息并通过指定 Bot 发送给订阅者。
 */
class Notifier(
    private val botId: Long,
    private val logger: MiraiLogger,
) {
    @OptIn(ConsoleExperimentalApi::class)
    suspend fun notify(messages: List<Pair<RepoId, Notification>>) {
        if (messages.isEmpty()) return
        val bot = runCatching { Bot.getInstance(botId) }.getOrNull()
        if (bot == null) {
            logger.warning("Bot $botId is not online, skip ${messages.size} notification(s)")
            return
        }
        messages.forEach { (repo, n) ->
            val text = formatMessage(repo, n.release)
            n.subscribers.forEach { sub ->
                logger.verbose("Sending notification for $repo to $sub")
                bot.getContactOrNull(sub)?.sendMessage(text)
                    ?: logger.warning("Contact $sub is null, cannot send notification for $repo")
            }
        }
    }

    private fun formatMessage(repo: RepoId, r: Release): String = buildString {
        appendLine("【$repo】发现新版本！")
        appendLine("URL: ${r.url}")
        r.name?.let { appendLine("名称: $it") }
        appendLine("Tag: ${r.tagName}")
        if (r.isPrerelease) appendLine("（预发布版本）")
        appendLine("发布时间: ${r.createdAt}")
        appendLine("更新时间: ${r.updatedAt}")
        r.author?.let { appendLine("作者: $it") }
        val assets = r.releaseAssets.nodes
        if (assets.isEmpty()) {
            append("本次发布未附带资源文件")
        } else {
            appendLine("包含 ${assets.size} 个资源文件：")
            assets.forEach { a ->
                appendLine("--------------------")
                appendLine("  文件名: ${a.name}")
                appendLine("  大小: ${a.sizeString}")
                appendLine("  下载链接: ${a.downloadUrl}")
            }
        }
    }.trim()
}