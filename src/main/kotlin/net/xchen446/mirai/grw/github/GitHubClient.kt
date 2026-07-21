package net.xchen446.mirai.grw.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.Closeable

/**
 * GitHub GraphQL API 客户端。基于 Ktor 2.x 原生 Bearer 认证，
 * 用 ContentNegotiation + kotlinx.serialization 替代旧的 Gson 手动解析。
 *
 * Token 变更时调用方重建实例即可，无需手动重置 provider。
 */
class GitHubClient(
    token: String,
    timeoutMs: Long,
) : Closeable {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(Auth) {
            bearer {
                loadTokens { BearerTokens(token, "") }
                sendWithoutRequest { true }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            socketTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
        }
        expectSuccess = false
    }

    suspend fun query(repos: Collection<RepoId>): GraphQLResponse =
        client.post(ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(GraphQLRequest(GraphQLQuery.build(repos)))
        }.body()

    suspend fun verifyToken(): Boolean = runCatching {
        client.post(ENDPOINT) {
            contentType(ContentType.Application.Json)
            setBody(GraphQLRequest("{ viewer { login } }"))
        }.status.value in 200..299
    }.getOrDefault(false)

    override fun close() = client.close()

    private companion object {
        const val ENDPOINT = "https://api.github.com/graphql"
    }
}