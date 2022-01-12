package dev.racci.elixir.extensions

import com.github.jezza.Toml
import com.github.jezza.TomlArray
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.components.buttons.PublicInteractionButtonContext
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.publicButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.hasRole
import com.kotlindiscord.kord.extensions.utils.parseBoolean
import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordPreview
import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.OptionalBoolean
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.RoleBehavior
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.followUpEphemeral
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.racci.elixir.configPath
import dev.racci.elixir.database.DatabaseManager
import dev.racci.elixir.utils.GUILD_ID
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.properties.Delegates
import kotlinx.coroutines.flow.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RoleSelector: Extension() {

    override var name = "roles"

    private val roles: TomlTable = Toml.from(Files.newInputStream(Paths.get("$configPath/roles.toml")))
    private var channel by Delegates.notNull<GuildMessageChannelBehavior>()

    @OptIn(KordUnsafe::class, KordExperimental::class)
    override suspend fun setup() {
        val roleSelectors: TomlArray = roles.get("roleselector") as TomlArray
        channel = kord.unsafe.guildMessageChannel(GUILD_ID, (Snowflake(roles["roleChannel"] as ULong)))
        newSuspendedTransaction {
            DatabaseManager.RoleSelector.messageId.ddl.forEach {
                val message = channel.getMessageOrNull(Snowflake(it))
                if(message == null) {
                    DatabaseManager.RoleSelector.deleteWhere { DatabaseManager.RoleSelector.messageId eq it }
                }
            }
        }
        for(rsls in roleSelectors) {
            val rsl = rsls as TomlTable
            addRoleSelector(
                rsl.get("name") as String,
                rsl.getOrDefault("attachment", "") as String,
                rsl.getOrDefault("limit", -1) as Int,
                rsl.getOrDefault("removable", true) as Boolean,
                rsl.get("role") as TomlArray?
            )
        }
    }

    private suspend fun ensureExistingMessage(
        name: String,
        attachment: String
    ): Message {
        return if(!DatabaseManager.RoleSelector.name.ddl.any { it == name }) {
            channel.createMessage {
                addFile(Paths.get(attachment))
            }
        } else {
            channel.getMessage(
                Snowflake(
                    DatabaseManager.RoleSelector.select {
                        DatabaseManager.RoleSelector.name eq name
                    }.first()[DatabaseManager.RoleSelector.messageId]
                )
            )
        }
    }

    private suspend fun removeCurrentRoleCheck(
        removable: Boolean,
        member: MemberBehavior,
        roleBehavior: RoleBehavior,
        context: PublicInteractionButtonContext
    ): String? { // Return a nullable string for easily returning if we need to
        return if(member.asMember().hasRole(roleBehavior)) {
            if(removable) {
                member.removeRole(roleBehavior.id)
            } else {
                context.interactionResponse.followUpEphemeral {
                    ephemeral = true
                    content = "Sorry, You cannot the remove the ${roleBehavior.fetchRole().name} role."
                }
            }
            null
        } else ""
    }

    private suspend fun roleLimitCheck(
        limit: Int,
        member: MemberBehavior,
        selectorRoles: List<RoleBehavior>,
        context: PublicInteractionButtonContext,
    ): String? {
        return if(limit != -1 && member.asMember().roles.count(selectorRoles::contains) > limit) {
            context.interactionResponse.followUpEphemeral {
                ephemeral = true
                content = "Sorry, You already have the maximum amount of roles from this selector."
            }
            null
        } else ""
    }

    private suspend fun roleIncompatibleWithCheck(
        incompatibleWith: Collection<RoleBehavior>,
        roleBehavior: RoleBehavior,
        member: MemberBehavior,
        context: PublicInteractionButtonContext,
    ): String? {
        val incompatible = member.asMember().getAnyRole(incompatibleWith)
        return if(incompatible != null) {
            context.interactionResponse.followUpEphemeral {
                ephemeral = true
                content = "Sorry, You ${roleBehavior.fetchRole().name} is incompatible with ${incompatible.fetchRole().name}"
            }
            null
        } else ""
    }

    @OptIn(KordPreview::class)
    @Suppress("UNCHECKED_CAST")
    private suspend fun addRoleSelector(
        name: String,
        attachment: String,
        limit: Int,
        removable: Boolean,
        roles: TomlArray?,
    ) {
        val message = ensureExistingMessage(name, attachment)
        message.edit {
            components {
                // Remove all so we have the order and everything fully up to date
                removeAll()

                if(roles == null) return@components
                val selectorRoles = roles.map { RoleBehavior(GUILD_ID, Snowflake((it as TomlTable)["roleId"] as ULong), kord) }

                for(role in roles.asList() as List<TomlTable>) {
                    val roleBehavior = selectorRoles.first { it.id.value == role["roleId"] as ULong }

                    publicButton {
                        label = role["name"] as String
                        val emoji = (role["emoji"] as String).split(':', limit = 3)
                        partialEmoji = DiscordPartialEmoji.dsl {
                            id = Snowflake(emoji[0])
                            this.name = emoji[1]
                            animated = OptionalBoolean.Value(emoji[2].parseBoolean(Locale.ENGLISH) ?: false)
                        }
                        style = ButtonStyle.Primary

                        val incompatibleWith = (role["incompatibleWith"] as Array<ULong>).map {
                            RoleBehavior(GUILD_ID, Snowflake(it), kord)
                        }

                        action {
                            val member = member ?: return@action

                            removeCurrentRoleCheck(
                                removable,
                                member,
                                roleBehavior,
                                this
                            ) ?: return@action

                            roleLimitCheck(
                                limit,
                                member,
                                selectorRoles,
                                this,
                            ) ?: return@action

                            roleIncompatibleWithCheck(
                                incompatibleWith,
                                roleBehavior,
                                member,
                                this,
                            ) ?: return@action

                            member.addRole(roleBehavior.id, "Role Selections")
                        }
                    }
                }
            }
        }
    }
}

fun Member.hasAnyRole(roles: Collection<RoleBehavior>): Boolean {
    return if (roles.isEmpty()) {
        true
    } else {
        val ids = roles.map { it.id }
        roleIds.any { ids.contains(id) }
    }
}

fun Member.getAnyRole(roles: Collection<RoleBehavior>): RoleBehavior? {
    return if (roles.isEmpty()) {
        null
    } else {
        val ids = roles.map { it.id }
        roleBehaviors.firstOrNull { ids.contains(it.id) }
    }
}
class DiscordPartialEmojiDSL {

    var id: Snowflake? = null
    var name: String? = null
    var animated: OptionalBoolean = OptionalBoolean.Missing

    fun build(): DiscordPartialEmoji = DiscordPartialEmoji(id, name, animated)
}

inline fun DiscordPartialEmoji.Companion.dsl(block: DiscordPartialEmojiDSL.() -> Unit): DiscordPartialEmoji {
    val dsl = DiscordPartialEmojiDSL()
    dsl.block()
    return dsl.build()
}