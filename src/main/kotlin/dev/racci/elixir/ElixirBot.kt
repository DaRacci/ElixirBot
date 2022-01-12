@file:OptIn(PrivilegedIntent::class)
package dev.racci.elixir

import com.github.jezza.Toml
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import dev.kord.common.entity.PresenceStatus
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.racci.elixir.database.DatabaseManager
import dev.racci.elixir.events.JoinLeaveEvent
import dev.racci.elixir.events.MessageEvents
import dev.racci.elixir.extensions.StatChannels
import dev.racci.elixir.extensions.commands.moderation.Moderation
import dev.racci.elixir.extensions.commands.moderation.Report
import dev.racci.elixir.extensions.commands.util.Custom
import dev.racci.elixir.extensions.commands.util.Github
import dev.racci.elixir.extensions.commands.util.Ping
import dev.racci.elixir.extensions.RoleSelector
import dev.racci.elixir.support.ThreadInviter
import dev.racci.elixir.utils.BOT_TOKEN
import dev.racci.elixir.utils.CONFIG_PATH
import dev.racci.elixir.utils.GITHUB_OAUTH
import dev.racci.elixir.utils.GUILD_ID
import dev.racci.elixir.utils.SENTRY_DSN
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.properties.Delegates
import mu.KotlinLogging
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

val configPath: Path = Paths.get(CONFIG_PATH)
val config: TomlTable = Toml.from(Files.newInputStream(Paths.get("$configPath/config.toml")))
var github: GitHub? = null
var bot by Delegates.notNull<ExtensibleBot>()

private val gitHubLogger = KotlinLogging.logger {}

suspend fun main() {
    bot = ExtensibleBot(BOT_TOKEN) {
        applicationCommands {
            defaultGuild(GUILD_ID)
        }
        members {
            fill(GUILD_ID)
        }
        intents {
            +Intent.GuildMembers
            +Intent.GuildMessageReactions
        }

        chatCommands {
            // Enable chat command handling
            enabled = true
        }

        extensions {
            add(::Ping)
            add(::Moderation)
            add(::ThreadInviter)
            add(::Report)
            add(::JoinLeaveEvent)
            add(::MessageEvents)
            add(::Github)
            add(::Custom)
            add(::StatChannels)
            add(::RoleSelector)

            extPhishing {
                appName = "Elixir Bot"
                detectionAction = DetectionAction.Kick
                logChannelName = "anti-phishing-logs"
                requiredCommandPermission = null
            }

            sentry {
                enableIfDSN(SENTRY_DSN)
            }

        }

        hooks {
            afterKoinSetup {
                DatabaseManager.startDatabase()
            }
        }
        presence {
            status = PresenceStatus.Online
            playing(config.get("activity") as String)
        }

        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            github = GitHubBuilder().withOAuthToken(GITHUB_OAUTH).build()
            gitHubLogger.info("Logged into GitHub!")
        } catch(exception: Exception) {
            exception.printStackTrace()
            gitHubLogger.error("Failed to log into GitHub!")
            throw Exception(exception)
        }
    }
    bot.start()
}
