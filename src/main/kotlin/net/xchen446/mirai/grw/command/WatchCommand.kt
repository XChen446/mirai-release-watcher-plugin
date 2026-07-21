package net.xchen446.mirai.grw.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.UserCommandSender
import net.mamoe.mirai.contact.Group
import net.xchen446.mirai.grw.GrwPlugin
import net.xchen446.mirai.grw.config.GrwWatches
import net.xchen446.mirai.grw.config.WatchEntry
import net.xchen446.mirai.grw.github.RepoId

/**
 * 监听列表命令：/watch add|remove|list ...
 *
 * 统一在 watch 命名空间下管理，替代原 watch-release / unwatch-release / watch-list 三个平铺命令。
 * 订阅者 id 取自 [UserCommandSender.subject.id]：私聊订阅则推送给个人，
 * 群内订阅则推送给整个群，与原项目行为一致。
 */
object WatchCommand : CompositeCommand(
    GrwPlugin, "watch",
    description = "管理 GitHub Release 监听列表",
) {
    @SubCommand
    suspend fun UserCommandSender.add(vararg repos: String) {
        if (repos.isEmpty()) {
            sendMessage("用法: /watch add <owner/name | SSH | HTTPS> ...")
            return
        }
        var ok = 0
        var fail = 0
        val isGroup = subject is Group
        for (arg in repos) {
            runCatching { RepoId.parse(arg) }
                .onSuccess { repo ->
                    val entry = GrwWatches.watches.getOrPut(repo) { WatchEntry() }
                    if (isGroup) entry.groupSubscribers += subject.id
                    else entry.userSubscribers += subject.id
                    ok++
                }
                .onFailure { fail++ }
        }
        sendMessage(buildResultMessage("添加", ok, fail))
    }

    @SubCommand
    suspend fun UserCommandSender.remove(vararg repos: String) {
        if (repos.isEmpty()) {
            sendMessage("用法: /watch remove <owner/name | SSH | HTTPS> ...")
            return
        }
        var ok = 0
        var fail = 0
        val isGroup = subject is Group
        for (arg in repos) {
            runCatching { RepoId.parse(arg) }
                .onSuccess { repo ->
                    val entry = GrwWatches.watches[repo] ?: return@onSuccess
                    if (isGroup) entry.groupSubscribers -= subject.id
                    else entry.userSubscribers -= subject.id
                    if (entry.userSubscribers.isEmpty() && entry.groupSubscribers.isEmpty())
                        GrwWatches.watches.remove(repo)
                    ok++
                }
                .onFailure { fail++ }
        }
        sendMessage(buildResultMessage("移除", ok, fail))
    }

    @SubCommand
    suspend fun CommandSender.list() {
        val watches = GrwWatches.watches
        if (watches.isEmpty()) {
            sendMessage("当前没有监听的仓库")
            return
        }
        val text = buildString {
            appendLine("当前监听 ${watches.size} 个仓库：")
            watches.entries.forEachIndexed { i, (repo, entry) ->
                val desc = buildList {
                    if (entry.userSubscribers.isNotEmpty()) add("${entry.userSubscribers.size}人")
                    if (entry.groupSubscribers.isNotEmpty()) add("${entry.groupSubscribers.size}群")
                }.ifEmpty { listOf("无订阅") }.joinToString("/")
                appendLine("${i + 1}. $repo  订阅 $desc  最近 Tag: ${entry.lastTag ?: "（待首次轮询）"}")
            }
        }.trim()
        sendMessage(text)
    }

    private fun buildResultMessage(action: String, ok: Int, fail: Int): String = buildString {
        append("已$action $ok 个仓库")
        if (fail > 0) append("（$fail 个解析失败）")
    }
}