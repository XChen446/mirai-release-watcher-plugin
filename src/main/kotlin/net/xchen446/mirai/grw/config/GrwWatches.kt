package net.xchen446.mirai.grw.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.xchen446.mirai.grw.github.RepoId

/**
 * 监听列表配置。每个仓库维护个人订阅者和群聊订阅者集合，以及上一次推送的 tagName。
 * 上一次 tagName 为 null 表示尚未建立基线（首次轮询只记录不推送），
 * 避免重启或新订阅时被历史 Release 轰炸。
 */
object GrwWatches : AutoSavePluginConfig("watches") {
    val watches: MutableMap<RepoId, WatchEntry> by value(mutableMapOf())
}

@kotlinx.serialization.Serializable
data class WatchEntry(
    val userSubscribers: MutableSet<Long> = mutableSetOf(),
    val groupSubscribers: MutableSet<Long> = mutableSetOf(),
    var lastTag: String? = null,
)