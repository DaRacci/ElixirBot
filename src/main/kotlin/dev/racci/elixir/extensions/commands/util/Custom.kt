package dev.racci.elixir.extensions.commands.util

import com.github.jezza.Toml
import com.github.jezza.TomlArray
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import dev.racci.elixir.configPath
import kotlinx.datetime.Clock
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This class reads in the config TOML file and converts each array of info into a usable discord slash command
 * @author IMS212
 */
class Custom : Extension() {

    override var name = "custom"

    private val commands: TomlTable = Toml.from(Files.newInputStream(Paths.get("$configPath/commands.toml")))

    override suspend fun setup() {
        val commands: TomlArray = commands.get("command") as TomlArray
        for (cmds in commands) {
            val cmd = cmds as TomlTable
            addCommand(
                cmd.get("name") as String,
                cmd.getOrDefault("help", "An Elixir bot command.") as String,
                cmd.getOrDefault("title", "No title") as String,
                cmd.getOrDefault("description", "") as String,
                cmd.get("subcommand") as TomlArray?
            )
        }
    }

    private suspend fun addCommand(
        names: String,
        desc: String,
        cmdTitle: String,
        cmdValue: String,
        subCmds: TomlArray?,
    ) {
        publicSlashCommand {

            name = names
            description = desc
            if (subCmds == null) {
                action {
                    respond {
                        embed {
                            color = DISCORD_BLURPLE
                            title = cmdTitle
                            description = cmdValue
                            timestamp = Clock.System.now()
                        }
                    }
                }
            } else {
                for (subs in subCmds) {
                    val sub = subs as TomlTable
                    publicSubCommand {
                        name = sub.get("name") as String
                        description = sub.getOrDefault("help", "An Elixir bot command.") as String
                        action {
                            respond {
                                embed {
                                    color = DISCORD_BLURPLE
                                    timestamp = Clock.System.now()
                                    title = sub.getOrDefault("title", null) as String
                                    description = sub.getOrDefault("description", "") as String
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
