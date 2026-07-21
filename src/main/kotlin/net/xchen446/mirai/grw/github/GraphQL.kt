package net.xchen446.mirai.grw.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GraphQLRequest(
    val query: String,
)

@Serializable
data class GraphQLError(
    val message: String,
    val type: String? = null,
    @SerialName("path") val path: List<String> = emptyList(),
) {
    override fun toString(): String = buildString {
        append(message)
        if (path.isNotEmpty()) append(" [path=").append(path).append(']')
        type?.let { append(" (").append(it).append(')') }
    }
}

@Serializable
data class GraphQLResponse(
    @SerialName("data") val data: Map<String, RepositoryNode?>? = null,
    @SerialName("errors") val errors: List<GraphQLError> = emptyList(),
)

@Serializable
data class RepositoryNode(
    @SerialName("releases") val releases: ReleaseNodes = ReleaseNodes(),
) {
    val latestRelease: Release?
        get() = releases.nodes.firstOrNull()
}

@Serializable
data class ReleaseNodes(
    @SerialName("nodes") val nodes: List<Release> = emptyList(),
)

/**
 * GraphQL 批量查询构建器。
 * 单次请求合并多个仓库的最新 Release，降低 GitHub API 调用次数。
 */
object GraphQLQuery {
    private const val FRAGMENT = """
        fragment latestRelease on Repository {
            releases(first: 1, orderBy: {field: CREATED_AT, direction: DESC}) {
                nodes {
                    name url tagName createdAt updatedAt isPrerelease
                    author { name login }
                    releaseAssets(first: 100) { nodes { name size downloadUrl } }
                }
            }
        }
    """.trimIndent()

    fun build(repos: Collection<RepoId>): String {
        require(repos.isNotEmpty()) { "Can't build query from an empty set" }
        val aliases = repos.joinToString(" ") {
            "${it.toLegalId()}: repository(owner: \"${it.owner}\", name: \"${it.name}\") { ...latestRelease }"
        }
        return "{ $aliases } $FRAGMENT"
    }
}