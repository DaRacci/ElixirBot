package dev.racci.elixir.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.core.behavior.channel.edit
import dev.racci.elixir.utils.GUILD_ID
import dev.racci.elixir.utils.MEMBER_COUNTER
import dev.racci.elixir.utils.STATUS_CHANNEL
import io.ktor.client.HttpClient
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count

class StatChannels: Extension() {

    override val name: String = "statchannels"
    private var loaded2: Boolean = false
    private var members: Int = 0
    private var status: Boolean = false

    override suspend fun setup() {
        loaded2 = true
        checker()
    }

    override suspend fun unload() {
        loaded2 = false
    }

    @OptIn(KordUnsafe::class, KordExperimental::class)
    private suspend fun checker() {
        while(loaded2) {
            try {
                HttpClient().use {client->
                    val response: HttpResponse = client.request {
                        method = HttpMethod.Get
                        url("https://eu.mc-api.net/v3/server/status-http/mc.racci.dev")
                    }
                    status = response.status.value == 200
                    if(status != (response.status.value == 200)) {
                        status = !status
                        kord.unsafe.voiceChannel(GUILD_ID, STATUS_CHANNEL).edit {
                            name = "Status: $status"
                        }
                    }
                }
            } catch(ignored: ServerResponseException) {}
            val tmem = kord.getGuild(GUILD_ID)?.members?.count() ?: -1
            if(members != tmem
                || kord.unsafe.guildChannel(GUILD_ID, MEMBER_COUNTER).asChannel().data.name.value?.matches(Regex("Members: [0-9]*")) != true
            ) {
                members = tmem
                kord.unsafe.voiceChannel(GUILD_ID, MEMBER_COUNTER).edit {
                    name = "Members: $members"
                }
            }

            delay(15.seconds)
        }
        return
    }

}