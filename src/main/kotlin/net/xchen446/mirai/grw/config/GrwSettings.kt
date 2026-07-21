package net.xchen446.mirai.grw.config

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object GrwSettings : AutoSavePluginConfig("settings") {
    var botId: Long by value(0L)
    var enabled: Boolean by value(false)
    var token: String by value("unset")
    var interval: Long by value(30 * 1000L)
    var timeout: Long by value(15 * 1000L)
    var includePrerelease: Boolean by value(true)
}