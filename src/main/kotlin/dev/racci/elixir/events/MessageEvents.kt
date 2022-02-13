@file:OptIn(ExperimentalTime::class)
@file:Suppress("PrivatePropertyName", "BlockingMethodInNonBlockingContext")

package dev.racci.elixir.events

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.sentry.BreadcrumbType
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import dev.racci.elixir.utils.ResponseHelper
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.util.cio.toByteArray
import kotlinx.datetime.Clock
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlin.time.ExperimentalTime

class MessageEvents : Extension() {

    override val name = "messageevents"

    private val LOG_FILE_EXTENSIONS = setOf("log", "gz", "txt")

    override suspend fun setup() {

        /**
         * Upload files that have the extensions specified in [LOG_FILE_EXTENSIONS] to hastebin, giving a user confirmation
         *
         * @author maximumpower55
         */
        event<MessageCreateEvent> {
            action {
                val eventMessage = event.message.asMessageOrNull()

                eventMessage.attachments.forEach { attachment ->
                    val attachmentFileName = attachment.filename
                    val attachmentFileExtension = attachmentFileName.substring(attachmentFileName.lastIndexOf(".") + 1)

                    if (attachmentFileExtension in LOG_FILE_EXTENSIONS) {
                        var confirmationMessage: Message? = null

                        confirmationMessage = ResponseHelper.responseEmbedInChannel(
                            eventMessage.channel,
                            "Do you want to upload this file to Hastebin?",
                            "Hastebin is a website that allows users to share plain text through public posts called “pastes.”\nIt's easier for the support team to view the file on Hastebin, do you want it to be uploaded?",
                            DISCORD_BLURPLE,
                            eventMessage.author
                        ).edit {
                            components {
                                ephemeralButton(row = 0) {
                                    label = "Yes"
                                    style = ButtonStyle.Primary

                                    action {
                                        sentry.breadcrumb(BreadcrumbType.Info) {
                                            category = "events.messageevents.loguploading.uploadAccept"
                                            message = "Upload accepted"
                                        }
                                        if (event.interaction.user.id == eventMessage.author?.id) {
                                            confirmationMessage!!.delete()

                                            val uploadMessage = eventMessage.channel.createEmbed {
                                                color = DISCORD_BLURPLE
                                                title = "Uploading `$attachmentFileName` to Hastebin..."
                                                timestamp = Clock.System.now()

                                                footer {
                                                    text = "Uploaded by ${eventMessage.author?.tag}"
                                                    icon = eventMessage.author?.avatar?.url
                                                }
                                            }

                                            try {
                                                val logBytes = attachment.download()

                                                val builder = StringBuilder()

                                                if (attachmentFileExtension != "gz") {
                                                    builder.append(logBytes.decodeToString())
                                                } else {
                                                    val bis = ByteArrayInputStream(logBytes)
                                                    val gis = GZIPInputStream(bis)

                                                    builder.append(String(gis.readAllBytes()))
                                                }

                                                val response = postToHasteBin(builder.toString())

                                                uploadMessage.edit {
                                                    embed {
                                                        color = DISCORD_BLURPLE
                                                        title = "`$attachmentFileName` uploaded to Hastebin"
                                                        timestamp = Clock.System.now()

                                                        footer {
                                                            text = "Uploaded by ${eventMessage.author?.tag}"
                                                            icon = eventMessage.author?.avatar?.url
                                                        }
                                                    }

                                                    actionRow {
                                                        linkButton(response) {
                                                            label = "Click here to view"
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                uploadMessage.edit {
                                                    ResponseHelper.failureEmbed(
                                                        event.interaction.getChannel(),
                                                        "Failed to upload `$attachmentFileName` to Hastebin",
                                                        e.toString()
                                                    )
                                                }
                                                sentry.breadcrumb(BreadcrumbType.Error) {
                                                    category = "events.messageevnets.loguploading.UploadTask"
                                                    message = "Failed to upload a file to hastebin"
                                                }
                                            }
                                        } else {
                                            respond {
                                                content =
                                                    "Only the uploader can use this menu, if you are the uploader and are experiencing issues, contact the Iris team."
                                            }
                                        }
                                    }
                                }

                                ephemeralButton(row = 0) {
                                    label = "No"
                                    style = ButtonStyle.Secondary

                                    action {
                                        if (event.interaction.user.id == eventMessage.author?.id) {
                                            confirmationMessage!!.delete()
                                            sentry.breadcrumb(BreadcrumbType.Info) {
                                                category = "events.messagevents.loguploading.uploadDeny"
                                                message = "Upload of log denied"
                                            }
                                        } else {
                                            respond {
                                                content =
                                                    "Only the uploader can use this menu, if you are the uploader and are experiencing issues, contact the Iris team."
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun postToHasteBin(text: String): String {
        val client = HttpClient()

        var response = client.post<HttpResponse>("https://www.toptal.com/developers/hastebin/documents") {
            body = text
        }.content.toByteArray().decodeToString()

        if (response.contains("\"key\"")) {
            response = "https://www.toptal.com/developers/hastebin/" + response.substring(
                response.indexOf(":") + 2,
                response.length - 2
            )
        }

        client.close()

        return response
    }
}
