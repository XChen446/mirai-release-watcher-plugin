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
data class RepositoryNode(
    @SerialName("releases") val releases: ReleaseNodes = ReleaseNodes(),
    @SerialName("owner") val owner: RepoOwner? = null,
) {
    val latestRelease: Release?
        get() = releases.nodes.firstOrNull()
}

@Serializable
data class ReleaseNodes(
    @SerialName("nodes") val nodes: List<Release> = emptyList(),
)

/**
 * 两个 Release 之间的贡献者请求。
 */
data class ContributorsRequest(
    val repo: RepoId,
    val headTag: String,
    val baseTag: String,
)

@Serializable
data class ContributorsResponse(
    val data: Map<String, ContributorsRepo?>? = null,
)

@Serializable
data class ContributorsRepo(
    val compare: ContributorsCompare? = null,
)

@Serializable
data class ContributorsCompare(
    val commits: ContributorsCommits = ContributorsCommits(),
)

@Serializable
data class ContributorsCommits(
    val nodes: List<ContributorsCommitEdge> = emptyList(),
)

@Serializable
data class ContributorsCommitEdge(
    val commit: ContributorsCommitData,
)

@Serializable
data class ContributorsCommitData(
    val author: ContributorsAuthor? = null,
)

@Serializable
data class ContributorsAuthor(
    val user: ContributorsUser? = null,
)

@Serializable
data class ContributorsUser(
    val login: String,
    val name: String? = null,
)

/**
 * 从 [ContributorsResponse] 中提取指定别名的贡献者 login 集合。
 */
fun extractContributors(response: ContributorsResponse, alias: String): Set<String> =
    response.data?.get(alias)
        ?.compare
        ?.commits
        ?.nodes
        ?.mapNotNull { it.commit.author?.user?.login }
        ?.toSet()
        ?: emptySet()

/**
 * GraphQL 批量查询构建器。
 * 单次请求合并多个仓库的最新 Release，降低 GitHub API 调用次数。
 */
object GraphQLQuery {
    private val FRAGMENT = """
        fragment latestRelease on Repository {
            owner { avatarUrl }
            releases(first: 1, orderBy: {field: CREATED_AT, direction: DESC}) {
                nodes {
                    name url tagName description createdAt updatedAt isPrerelease
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

    /**
     * 批量构建各仓库两版本间提交贡献者的查询。
     */
    fun buildContributorsQuery(requests: List<ContributorsRequest>): String {
        require(requests.isNotEmpty())
        val aliases = requests.joinToString(" ") { req ->
            val alias = req.repo.toLegalId()
            """$alias: repository(owner: "${req.repo.owner}", name: "${req.repo.name}") { compare(headExpr: "${req.headTag}", baseExpr: "${req.baseTag}") { commits(first: 100) { nodes { commit { author { user { login name } } } } } } }"""
        }
        return "{ $aliases }"
    }
}