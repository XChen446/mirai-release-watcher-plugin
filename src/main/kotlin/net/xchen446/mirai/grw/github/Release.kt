package net.xchen446.mirai.grw.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.log10
import kotlin.math.pow

@Serializable
data class Release(
    @SerialName("url") val url: String,
    @SerialName("tagName") val tagName: String,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("isPrerelease") val isPrerelease: Boolean = false,
    @SerialName("author") val author: Author? = null,
    @SerialName("releaseAssets") val releaseAssets: AssetNodes = AssetNodes(),
)

@Serializable
data class Author(
    @SerialName("name") val name: String? = null,
    @SerialName("login") val login: String,
) {
    override fun toString(): String = if (name != null) "$name ($login)" else login
}

@Serializable
data class AssetNodes(
    @SerialName("nodes") val nodes: List<Asset> = emptyList(),
)

@Serializable
data class Asset(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long,
    @SerialName("downloadUrl") val downloadUrl: String,
) {
    private companion object {
        val SUFFIXES = charArrayOf('K', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y')
    }

    val sizeString: String
        get() {
            if (size < 1000) return "${size}B"
            val unit = (log10(size.toDouble()) / 3).toInt().coerceIn(1, SUFFIXES.size)
            return String.format("%.2f%sB", size / 1e3.pow(unit), SUFFIXES[unit - 1])
        }
}