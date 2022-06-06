package dev.racci.elixir.services

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.TextChannelBehavior
import dev.kord.core.behavior.channel.VoiceChannelBehavior
import dev.kord.core.behavior.channel.edit
import dev.kord.core.kordLogger
import dev.racci.elixir.utils.DatabaseHelper
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(DelicateCoroutinesApi::class)
class StatusService : Extension() {

    override val name: String = "Minecraft Status Service"

    private val executor = newFixedThreadPoolContext(2, "Status Service")

    override suspend fun setup() {
        coroutineScope { run() }
    }

    private val client = HttpClient { install(JsonFeature) }

    private val statusCache = mutableMapOf<Snowflake, Pair<Query, Int>>()

    private suspend fun checkGuild(guildConfig: DatabaseHelper.GuildConfig) {
        val guild = kord.getGuild(guildConfig.guildId)
            ?: return

        if (guildConfig.mcServer != null &&
            guildConfig.statusChannel?.let { guild.getChannelOrNull(it) } != null ||
            guildConfig.playersChannel?.let { guild.getChannelOrNull(it) } != null
        ) {
            val status: Query? = try {
                withTimeoutOrNull(1500.milliseconds) {
                    client.get("https://mcapi.xdefcon.com/server/${guildConfig.mcServer}/status/json")
                }
            } catch (e: Exception) {
                if (e is TimeoutCancellationException) {
                    kordLogger.warn { "Timeout while querying Minecraft server ${guildConfig.mcServer}" }
                }
                null
            }
            val past = statusCache[guildConfig.guildId]

            if (status != null) {
                if (status.serverStatus != past?.first?.serverStatus && guildConfig.statusChannel != null) {
                    // Why can't these just have one unified type edit and be done?
                    when (val channel = guild.getChannel(guildConfig.statusChannel)) {
                        is VoiceChannelBehavior -> channel.edit { name = "Status: ${status.serverStatus.replaceFirstChar(Char::titlecaseChar)}" }
                        is TextChannelBehavior -> channel.edit { name = "Status: ${status.serverStatus.replaceFirstChar(Char::titlecaseChar)}" }
                    }
                }
                if (status.players != past?.first?.players && guildConfig.playersChannel != null) {
                    when (val channel = guild.getChannel(guildConfig.playersChannel)) {
                        is VoiceChannelBehavior -> channel.edit { name = "Players: ${status.players}" }
                        is TextChannelBehavior -> channel.edit { name = "Players: ${status.players}" }
                    }
                }
            }

            if (guildConfig.membersChannel != null) {
                val curMembers = guild.members.count()
                if (curMembers != past?.second) {
                    when (val channel = guild.getChannel(guildConfig.membersChannel)) {
                        is VoiceChannelBehavior -> channel.edit { name = curMembers.toString() }
                        is TextChannelBehavior -> channel.edit { name = curMembers.toString() }
                    }
                }
            }
        }
    }

    private suspend fun run() {
        while (loaded) {
            DatabaseHelper.database
                .listCollections<DatabaseHelper.GuildConfig>()
                .consumeEach { guild -> CoroutineScope(executor).launch { checkGuild(guild) } }

            delay(15.seconds)
        }
    }

    @Serializable
    internal data class Query(
        val serverStatus: String = "online",
        val players: Int = 0,
    )
}
