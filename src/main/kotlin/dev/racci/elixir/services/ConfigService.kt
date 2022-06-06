package dev.racci.elixir.services

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.racci.elixir.utils.DatabaseHelper
import dev.racci.elixir.utils.DatabaseHelper.GuildConfig
import dev.racci.elixir.utils.ResponseHelper

class ConfigService : Extension() {

    override val name = "Config Service"

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = "config"
            description = "Configure the bot"

            ephemeralSubCommand(::Config) {
                name = "set"
                description = "Set the config"

                check { anyGuild() }
                check { hasPermission(Permission.Administrator) }

                action {
                    var config = DatabaseHelper.getConfig(guild!!.id)

                    config.let {
                        config = GuildConfig(
                            guildId = guild!!.id,
                            arguments.moderatorRole?.id ?: it?.moderatorRole ?: return@let GuildConfig::moderatorRole,
                            arguments.supportRole?.id ?: it?.supportRole ?: arguments.moderatorRole?.id ?: it?.moderatorRole!!,
                            arguments.staffActionChannel?.id ?: it?.staffActionChannel ?: return@let GuildConfig::staffActionChannel,
                            arguments.logChannel?.id ?: it?.logChannel ?: return@let GuildConfig::logChannel,
                            arguments.supportChannel?.id ?: it?.supportChannel,
                            arguments.joinChannel?.id ?: it?.joinChannel,
                            arguments.membersChannel?.id ?: it?.membersChannel,
                            arguments.statusChannel?.id ?: it?.statusChannel,
                            arguments.playersChannel?.id ?: it?.playersChannel,
                            arguments.mcServer ?: it?.mcServer,
                            arguments.mcPort ?: it?.mcPort,
                        )

                        DatabaseHelper.setConfig(config!!)

                        respond { content = "Configuration submitted for GuildID: ${guild!!.id}" }
                        null
                    }?.let {
                        respond { content = "Missing required field: ${it.name}" }
                        return@action
                    }

                    val channel = guild!!.getChannel(config!!.staffActionChannel) as GuildMessageChannelBehavior
                    ResponseHelper.responseEmbedInChannel(
                        channel,
                        "Configuration submitted!",
                        "An administrator has updated the configuration for this guild.",
                        null,
                        user.asUser()
                    )
                }
            }
        }
    }

    inner class Config : Arguments() {

        val moderatorRole by optionalRole {
            name = "moderatorRole"
            description = "Your servers moderator role."
        }

        val supportRole by optionalRole {
            name = "supportRole"
            description = "Your servers support role."
        }

        val staffActionChannel by optionalChannel {
            name = "staffActionChannel"
            description = "The channel where staff actions are posted."
        }

        val logChannel by optionalChannel {
            name = "logChannel"
            description = "The channel where logs are posted."
        }

        val membersChannel by optionalChannel {
            name = "membersChannel"
            description = "The channel to display how many members are in the guild."
        }

        val supportChannel by optionalChannel {
            name = "supportChannel"
            description = "The channel where support is posted."
        }

        val joinChannel by optionalChannel {
            name = "joinChannel"
            description = "The channel where new members are posted."
        }

        val statusChannel by optionalChannel {
            name = "statusChannel"
            description = "The channel where the bot status is posted."
        }

        val playersChannel by optionalChannel {
            name = "playersChannel"
            description = "The channel where the players are posted."
        }

        val mcServer by optionalString {
            name = "mcServer"
            description = "The minecraft servers IP address or hostname."
        }

        val mcPort by optionalInt {
            name = "mcPort"
            description = "The minecraft servers query port."
        }
    }
}
