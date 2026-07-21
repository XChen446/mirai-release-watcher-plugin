package net.xchen446.mirai.grw.github

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RepoId.Serializer::class)
data class RepoId(
    val owner: String,
    val name: String,
) {
    override fun toString(): String = "$owner/$name"

    fun toLegalId(): String = toString()
        .replaceFirst(Regex("^(\\d)"), "_$1")
        .replace(Regex("[-/.]"), "_")

    companion object {
        private const val IDENTIFIER = "[A-Za-z0-9._-]+"

        private val ID = Regex("^($IDENTIFIER)/($IDENTIFIER)$")
        private val SSH = Regex("^git@github\\.com:($IDENTIFIER)/($IDENTIFIER)(?:\\.git)?$")
        private val HTTPS = Regex("^https?://github\\.com/($IDENTIFIER)/($IDENTIFIER)(?:\\.git)?$")

        fun parse(url: String): RepoId {
            val match = ID.find(url)
                ?: SSH.find(url)
                ?: HTTPS.find(url)
                ?: throw IllegalArgumentException("For input string: \"$url\"")
            return RepoId(match.groupValues[1], match.groupValues[2])
        }
    }

    object Serializer : KSerializer<RepoId> {
        override val descriptor =
            PrimitiveSerialDescriptor("net.xchen446.mirai.grw.github.RepoId", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: RepoId) = encoder.encodeString(value.toString())
        override fun deserialize(decoder: Decoder): RepoId = parse(decoder.decodeString())
    }
}