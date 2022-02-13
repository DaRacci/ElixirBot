@file:OptIn(ExperimentalTime::class)
package dev.racci.elixir.extensions.commands.util

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.rest.builder.message.create.embed
import dev.racci.elixir.utils.GUILD_ID
import dev.racci.elixir.utils.MOD_ACTION_LOG
import dev.racci.elixir.utils.ResponseHelper
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

@Suppress("PrivatePropertyName")
class Ping : Extension() {

    override val name = "ping"

    override suspend fun setup() {
        val actionLog = kord.getGuild(GUILD_ID)?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

//        ResponseHelper.responseEmbedInChannel(actionLog, "Elixir is now online!", null, DISCORD_GREEN, null)

        publicSlashCommand {
            name = "ping"
            description = "Am I alive?"

            action {
                val averagePing = this@Ping.kord.gateway.averagePing

                respond {
                    embed {
                        color = DISCORD_YELLOW
                        title = "Pong!"

                        timestamp = Clock.System.now()

                        field {
                            name = "Your Ping to Elixir is:"
                            value = "**$averagePing**"
                            inline = true
                        }
                    }
                }
            }
        }
    }

    override suspend fun unload() {
        val actionLog = kord.getGuild(GUILD_ID)?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

        ResponseHelper.responseEmbedInChannel(actionLog, "Elixir is now offline!", null, DISCORD_RED, null)
    }
}
