@file:OptIn(PrivilegedIntent::class)
package dev.racci.elixir

import com.github.jezza.Toml
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import dev.kord.common.entity.PresenceStatus
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.racci.elixir.database.DatabaseManager
import dev.racci.elixir.events.JoinLeaveEvent
import dev.racci.elixir.events.LogEvents
import dev.racci.elixir.events.MessageEvents
import dev.racci.elixir.extensions.RoleSelector
import dev.racci.elixir.extensions.StatChannels
import dev.racci.elixir.extensions.commands.moderation.Moderation
import dev.racci.elixir.extensions.commands.moderation.Report
import dev.racci.elixir.extensions.commands.util.Custom
import dev.racci.elixir.extensions.commands.util.Github
import dev.racci.elixir.extensions.commands.util.Ping
import dev.racci.elixir.support.ThreadInviter
import dev.racci.elixir.utils.BOT_TOKEN
import dev.racci.elixir.utils.DATA_PATH
import dev.racci.elixir.utils.GITHUB_OAUTH
import dev.racci.elixir.utils.MONGO_URI
import dev.racci.elixir.utils.SENTRY_DSN
import mu.KotlinLogging
import org.bson.UuidRepresentation
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.io.IOException
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.properties.Delegates

val config: TomlTable = Toml.from(Path("$DATA_PATH/config.toml").inputStream())
val github: GitHub? = run {
    try {
        val inst = GitHubBuilder().withOAuthToken(GITHUB_OAUTH).build()
        gitHubLogger.info("Logged into GitHub!")
        inst
    } catch (e: IOException) {
        gitHubLogger.error(e) { "Failed to log into GitHub!" }
        null
    }
}

var bot by Delegates.notNull<ExtensibleBot>()

private val gitHubLogger = KotlinLogging.logger {}

suspend fun main() {
    bot = ExtensibleBot(BOT_TOKEN) {
        applicationCommands {
            enabled = true
//            defaultGuild(GUILD_ID) // No more specific guilds
        }
        members {
            lockMemberRequests = true
            all()
//            fill(GUILD_ID)
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
            if (github != null) add(::Github)
            add(::Custom)
            add(::StatChannels)
            add(::LogEvents)
            add(::RoleSelector)

            extPhishing {
                appName = "Elixir Bot"
                detectionAction = DetectionAction.Kick
                logChannelName = "☲⋯logs"
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
            watching("you.")
        }
    }
    bot.start()
}
