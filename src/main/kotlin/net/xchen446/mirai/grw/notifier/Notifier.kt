package net.xchen446.mirai.grw.notifier

import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.console.util.ContactUtils.getContactOrNull
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.MiraiLogger
import net.xchen446.mirai.grw.config.GrwSettings
import net.xchen446.mirai.grw.github.Release
import net.xchen446.mirai.grw.github.RepoId
import java.net.URL

data class Notification(
    val release: Release,
    val subscribers: Set<Long>,
    val contributors: Set<String> = emptySet(),
    val avatarUrl: String? = null,
)

/**
 * 负责组装 Release 消息并通过指定 Bot 发送给订阅者。
 *
 * Bot id 取自 [GrwSettings.botId]，保证 /grw set bot 即时生效。
 */
class Notifier(
    private val settings: GrwSettings,
    private val logger: MiraiLogger,
) {
    @OptIn(ConsoleExperimentalApi::class, MiraiExperimentalApi::class)
    suspend fun notify(messages: List<Pair<RepoId, Notification>>) {
        if (messages.isEmpty()) return
        val botId = settings.botId
        if (botId == 0L) {
            logger.warning("botId 未设置，跳过 ${messages.size} 条通知")
            return
        }
        val bot = runCatching { Bot.getInstance(botId) }.getOrNull()
        if (bot == null) {
            logger.warning("Bot $botId 不在线，跳过 ${messages.size} 条通知")
            return
        }
        messages.forEach { (repo, n) ->
            val text = formatMessage(repo, n)
            val avatarBytes = runCatching {
                n.avatarUrl?.let { URL(it).openStream().readBytes() }
            }.getOrNull()
            n.subscribers.forEach { sub ->
                logger.verbose("Sending notification for $repo to $sub")
                val contact = bot.getContactOrNull(sub)
                if (contact == null) {
                    logger.warning("Contact $sub 为空，无法推送 $repo 的通知")
                    return@forEach
                }
                val msg = if (avatarBytes != null) {
                    runCatching {
                        val image = contact.uploadImage(avatarBytes.inputStream())
                        MessageChainBuilder().apply {
                            add(image)
                            add(PlainText("\n"))
                            add(PlainText(text))
                        }.build()
                    }.getOrNull()
                } else null
                contact.sendMessage(msg ?: text)
            }
        }
    }

    private fun formatMessage(repo: RepoId, n: Notification): String = buildString {
        val r = n.release
        appendLine("【$repo】发现新版本！")
        appendLine("URL: ${r.url}")
        r.name?.let { appendLine("名称: $it") }
        appendLine("版本: ${r.tagName}")
        if (r.isPrerelease) appendLine("（预发布版本）")
        appendLine("发布时间: ${r.createdAt}")
        appendLine("更新时间: ${r.updatedAt}")
        if (n.contributors.isNotEmpty()) {
            appendLine("贡献者: ${n.contributors.joinToString(", ")}（共 ${n.contributors.size} 人）")
        }
        r.description?.let { desc ->
            val truncated = if (desc.length > 200) desc.take(200) + "…" else desc
            appendLine("更新说明: $truncated")
        }
        val assets = r.releaseAssets.nodes
        if (assets.isEmpty()) {
            append("本次发布未附带资源文件")
        } else {
            appendLine("包含 ${assets.size} 个资源文件：")
            assets.forEach { a ->
                appendLine("--------------------")
                appendLine("  文件名: ${a.name}")
                appendLine("  大小: ${a.sizeString}")
                val url = if (settings.urlPrefix.isEmpty()) a.downloadUrl
                          else "${settings.urlPrefix}${a.downloadUrl}"
                appendLine("  下载链接: $url")
            }
        }
    }.trim()
}