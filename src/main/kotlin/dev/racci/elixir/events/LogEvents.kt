package dev.racci.elixir.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.sentry.BreadcrumbType
import com.kotlindiscord.kord.extensions.utils.createdAt
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.InviteCreateEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.message.MessageBulkDeleteEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.racci.elixir.utils.GUILD_ID
import dev.racci.elixir.utils.MESSAGE_LOGS
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toSet
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil

class LogEvents : Extension() {

    override val name = "LogEvents"

    override suspend fun setup() {
        val logChannel = kord.getGuild(GUILD_ID)?.getChannel(MESSAGE_LOGS) as GuildMessageChannelBehavior

        event<MessageDeleteEvent> {
            action {
                if (event.message?.asMessageOrNull()?.author?.isBot == true) return@action
                val messageContent = event.message?.asMessageOrNull()?.content.toString()
                val eventMessage = event.message
                val messageLocation = event.channel.id.value

                logChannel.createEmbed {
                    color = DISCORD_PINK
                    title = "Message Deleted"
                    description = "Location: <#$messageLocation>"
                    timestamp = Clock.System.now()

                    field {
                        name = "Message Contents:"
                        value =
                            messageContent.ifEmpty {
                                "Failed to get content of message\nMessage was likely from a Bot"
                            }
                        inline = false
                    }
                    field {
                        name = "Message Author:"
                        value = eventMessage?.author?.tag.toString()
                        inline = true
                    }
                    field {
                        name = "Author ID:"
                        value = eventMessage?.author?.id.toString()
                        inline = true
                    }
                }

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "events.messageevents.MessageDeleted"
                    message = "A message was deleted"
                    data["content"] = messageContent.ifEmpty { "Failed to get content of message" }
                }
            }
        }

        event<MessageUpdateEvent> {
            action {
                if (event.message.asMessageOrNull()?.author?.isBot == true) return@action
                val messageContentBefore = event.old?.content.toString()
                val messageContentAfter = event.new.content.toString()
                val eventMessage = event.message.asMessageOrNull()
                val messageLocation = event.channel.id.value
                val eventMember = event.message.asMessageOrNull()?.author ?: return@action

                logChannel.createEmbed {
                    color = DISCORD_PINK
                    title = "Message Updated"
                    description = "Location: <#$messageLocation>"
                    timestamp = Clock.System.now()
                    thumbnail {
                        url = eventMember.avatar?.url
                            ?: eventMember.defaultAvatar.url
                    }

                    field {
                        name = "Old Message Contents:"
                        value =
                            messageContentBefore.ifEmpty {
                                "Failed to get content of message\nMessage was likely from a Bot"
                            }
                        inline = false
                    }
                    field {
                        name = "New Message Contents:"
                        value =
                            messageContentAfter.ifEmpty {
                                "Failed to get content of message\nMessage was likely from a Bot"
                            }
                        inline = false
                    }
                    field {
                        name = "Message Author:"
                        value = "@" + eventMessage?.author?.tag.toString()
                        inline = true
                    }
                    field {
                        name = "Author ID:"
                        value = eventMessage?.author?.id.toString()
                        inline = true
                    }
                }

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "events.LogEvents.MessageUpdated"
                    message = "A message was updated"
                    data["oldContent"] = messageContentBefore.ifEmpty { "Failed to get content of message" }
                    data["newContent"] = messageContentAfter.ifEmpty { "Failed to get content of message" }
                }
            }
        }

        event<MessageBulkDeleteEvent> {
            action {
                val deletedCount = event.messages.count()
                val messageLocation = event.channel.id.value

                logChannel.createEmbed {
                    color = DISCORD_PINK
                    title = "Bulk Message Deletion"
                    description = "Location: <#$messageLocation>"
                    timestamp = Clock.System.now()

                    field {
                        name = "Amount of messages deleted:"
                        value = deletedCount.toString()
                        inline = false
                    }
                }

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "events.LogEvents.BulkMessageDeletion"
                    message = "A bulk of messages was deleted"
                    data["amount"] = deletedCount
                    data["messageIds"] = event.messageIds
                    data["messageContents"] = event.messages
                }
            }
        }

        event<InviteCreateEvent> {
            action {
                val inviter = event.inviterMember?.nicknameMention ?: event.inviterId.toString()
                val maxUses = event.maxUses.toString()
                val maxAge = event.maxAge.toString()

                logChannel.createEmbed {
                    color = DISCORD_PINK
                    title = "Created new invite"
                    timestamp = Clock.System.now()

                    field {
                        name = "Invite Author:"
                        value = inviter
                        inline = true
                    }
                    field {
                        name = "Invite max age:"
                        value = maxAge
                        inline = true
                    }
                    field {
                        name = "Invite max uses:"
                        value = maxUses
                        inline = true
                    }

                    sentry.breadcrumb(BreadcrumbType.Info) {
                        category = "events.LogEvents.InviteCreated"
                        message = "An invite was created"
                        data["author"] = inviter
                        data["age"] = maxAge
                        data["uses"] = maxUses
                    }
                }
            }
        }

        event<MemberJoinEvent> {
            action {
                val eventMember = event.member
                val guildMemberCount = event.getGuild().members.count()
                val now = Clock.System.now()

                logChannel.createEmbed {
                    color = DISCORD_PINK
                    title = "User Joined"
                    timestamp = now
                    description = eventMember.mention + " " + eventMember.username
                    thumbnail {
                        url = eventMember.avatar?.url
                            ?: eventMember.defaultAvatar.url
                    }
                    field {
                        name = "Account Age:"
                        value = eventMember.createdAt.periodUntil(now, TimeZone.UTC).toString()
                        inline = false
                    }
                    footer {
                        text = "ID: ${eventMember.id}\nMember Count: $guildMemberCount"
                    }
                    sentry.breadcrumb(BreadcrumbType.Info) {
                        category = "events.LogEvents.MemberJoin"
                        message = "Member joined"
                        data["user"] = eventMember.tag
                    }
                }
            }
        }

        event<MemberLeaveEvent> {
            action {
                val eventMember = event.old ?: return@action
                val guildMemberCount = event.getGuild().members.count()
                val now = Clock.System.now()
                val roles = eventMember.roles.toSet()

                logChannel.createEmbed {
                    color = DISCORD_PINK
                    title = "User Left"
                    timestamp = now
                    description = eventMember.mention + " " + eventMember.username
                    thumbnail {
                        url = eventMember.avatar?.url
                            ?: eventMember.defaultAvatar.url
                    }
                    field {
                        name = "Join date:"
                        value = eventMember.joinedAt.toString()
                        inline = false
                    }
                    field {
                        name = "Roles:"
                        value = roles.joinToString("\n") { it.mention }
                    }
                    footer {
                        text = "ID: ${eventMember.id}\nMember Count: $guildMemberCount"
                    }
                    sentry.breadcrumb(BreadcrumbType.Info) {
                        category = "events.LogEvents.MemberLeft"
                        message = "Member left"
                        data["user"] = eventMember.tag
                    }
                }
            }
        }
    }
}
