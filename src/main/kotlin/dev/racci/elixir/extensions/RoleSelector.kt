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
import dev.kord.core.exception.EntityNotFoundException
import dev.racci.elixir.configPath
import dev.racci.elixir.database.DatabaseManager
import dev.racci.elixir.utils.GUILD_ID
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toSet
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RoleSelector: Extension() {

    override var name = "roles"

    private val roles: TomlTable = Toml.from(Files.newInputStream(Paths.get("$configPath/roles.toml")))

    @OptIn(KordUnsafe::class, KordExperimental::class)
    override suspend fun setup() {
        val roleSelectors: TomlArray = roles.get("roleselector") as TomlArray
        // TODO: create a way to auto delete if the target channel has changed.
        newSuspendedTransaction {
            for(resultRow in DatabaseManager.RoleSelector.selectAll()) {
                val channel: GuildMessageChannelBehavior

                try {
                    channel = kord.unsafe.guildMessageChannel(
                        GUILD_ID,
                        Snowflake(resultRow[DatabaseManager.RoleSelector.channelId])
                    )
                } catch(e: EntityNotFoundException) {
                    DatabaseManager.RoleSelector.deleteWhere {
                        DatabaseManager.RoleSelector.name eq resultRow[DatabaseManager.RoleSelector.name]
                    }
                    continue
                }

                if(channel.getMessageOrNull(
                        Snowflake(resultRow[DatabaseManager.RoleSelector.messageId])
                    ) == null
                ) {
                    DatabaseManager.RoleSelector.deleteWhere {
                        DatabaseManager.RoleSelector.name eq resultRow[DatabaseManager.RoleSelector.name]
                    }
                }
            }
        }
        for(rsls in roleSelectors) {
            val rsl = rsls as TomlTable
            addRoleSelector(
                rsl["name"] as String,
                rsl["channel"] as Long,
                rsl["autoRemoveOnLimit", false] as Boolean,
                rsl["attachment", ""] as String,
                rsl["limit", -1] as Long,
                rsl["removable", true] as Boolean,
                rsl["role"] as TomlArray?
            )
        }
    }

    private suspend fun ensureExistingMessage(
        selectorName: String,
        attachment: String,
        channel: GuildMessageChannelBehavior,
    ): Message {
        var message: Message? = null
        newSuspendedTransaction {
            val localMessageId = DatabaseManager.RoleSelector.select {
                DatabaseManager.RoleSelector.name eq selectorName
            }.singleOrNull()?.get(DatabaseManager.RoleSelector.messageId)

            if(localMessageId == null) {
                message = channel.createMessage {
                    addFile(Paths.get(attachment))
                }
                DatabaseManager.RoleSelector.insertIgnore {
                    it[name] = selectorName
                    it[messageId] = message!!.id.value.toLong()
                    it[channelId] = channel.id.value.toLong()
                }
            } else {
                message = channel.getMessage(
                    Snowflake(
                        DatabaseManager.RoleSelector.select {
                            DatabaseManager.RoleSelector.name eq selectorName
                        }.single()[DatabaseManager.RoleSelector.messageId]
                    )
                )
            }
        }
        return message!!
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
        limit: Long,
        autoRemoveOnLimit: Boolean,
        member: MemberBehavior,
        selectorRoles: List<RoleBehavior>,
        context: PublicInteractionButtonContext,
    ): String? {
        if(limit == -1L) return ""
        val conflictingRoles = member.asMember().roles.filter(selectorRoles::contains)
        if(conflictingRoles.count() < limit) return ""
        return if(autoRemoveOnLimit) {
            for(role in conflictingRoles.toSet()) {
                member.removeRole(role.id, "Maximum Amount of roles for role selector, auto removing to give new role.")
            }
            ""
        } else {
            context.interactionResponse.followUpEphemeral {
                ephemeral = true
                content = "Sorry, You already have the maximum amount of roles from this selector."
            }
            null
        }
    }

    private suspend fun roleIncompatibleWithCheck(
        incompatibleWith: Collection<RoleBehavior>?,
        roleBehavior: RoleBehavior,
        member: MemberBehavior,
        context: PublicInteractionButtonContext,
    ): String? {
        incompatibleWith ?: return ""
        val incompatible = member.asMember().getAnyRole(incompatibleWith)
        return if(incompatible != null) {
            context.interactionResponse.followUpEphemeral {
                ephemeral = true
                content = "Sorry, You ${roleBehavior.fetchRole().name} is incompatible with ${incompatible.fetchRole().name}"
            }
            null
        } else ""
    }

    private suspend fun requiredRoleCheck(
        roleLong: Long?,
        member: MemberBehavior,
        context: PublicInteractionButtonContext,
    ): String? {
        roleLong ?: return ""
        val requiredRole = RoleBehavior(GUILD_ID, Snowflake(roleLong), kord)
        return if(!member.asMember().hasRole(requiredRole)) {
            context.interactionResponse.followUpEphemeral {
                ephemeral = true
                content = "Sorry, You need the ${requiredRole.fetchRole().name} to be able to get this."
            }
            null
        } else ""
    }

    @OptIn(KordPreview::class, KordUnsafe::class, KordExperimental::class)
    @Suppress("UNCHECKED_CAST")
    private suspend fun addRoleSelector(
        name: String,
        channel: Long,
        autoRemoveOnLimit: Boolean,
        attachment: String,
        limit: Long,
        removable: Boolean,
        roles: TomlArray?,
    ) {
        val message = ensureExistingMessage(name, attachment, kord.unsafe.guildMessageChannel(GUILD_ID, Snowflake(channel)))
        message.edit {
            components {
                // Remove all so we have the order and everything fully up to date
                removeAll()

                if(roles == null) return@components
                val selectorRoles = roles.map { RoleBehavior(GUILD_ID, Snowflake((it as TomlTable)["roleId"] as Long), kord) }

                for(role in roles.asList() as List<TomlTable>) {
                    val roleName: String = role["name"] as String
                    val roleEmoji: String? = role["emoji"] as? String
                    val roleRoleId: Long = role["roleId"] as Long
                    val roleRequiredRoleId: Long? = role["requiredRoleId"] as? Long
                    val roleIncompatibleWith: TomlArray? = role["incompatibleWith"] as? TomlArray
                    val roleBehavior = selectorRoles.first { it.id.value.toLong() == roleRoleId }

                    publicButton {
                        label = roleName
                        if(!roleEmoji.isNullOrBlank()) {
                            val emoji = roleEmoji.split(':', limit = 3)

                            partialEmoji = DiscordPartialEmoji.dsl {
                                id = Snowflake(emoji[0])
                                this.name = emoji[1]
                                animated = OptionalBoolean.Value(emoji[2].parseBoolean(Locale.ENGLISH) ?: false)
                            }
                        }
                        style = ButtonStyle.Secondary

                        val incompatibleWith = roleIncompatibleWith?.map {
                            RoleBehavior(GUILD_ID, Snowflake(it.toString()), kord)
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
                                autoRemoveOnLimit,
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

                            requiredRoleCheck(
                                roleRequiredRoleId,
                                member,
                                this
                            ) ?: return@action

                            member.addRole(roleBehavior.id, "Role Selections")
                        }
                    }
                }
            }
        }
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

operator fun TomlTable.get(key: String, defaultValue: Any): Any = getOrDefault(key, defaultValue)