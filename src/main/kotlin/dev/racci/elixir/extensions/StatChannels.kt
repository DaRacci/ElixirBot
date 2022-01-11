package dev.racci.elixir.extensions

import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.annotation.KordExperimental
import dev.kord.common.annotation.KordUnsafe
import dev.kord.core.behavior.channel.edit
import dev.racci.elixir.utils.GUILD_ID
import dev.racci.elixir.utils.MEMBER_COUNTER
import dev.racci.elixir.utils.STATUS_CHANNEL
import dev.racci.elixir.utils.STATUS_SERVER
import io.ktor.client.HttpClient
import io.ktor.client.features.ServerResponseException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class StatChannels: Extension() {

    override val name: String = "statchannels"

    // TODO Fix this janky thing
    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun setup() {
        GlobalScope.launch { checker() }
    }

    @Serializable
    data class Query(val online: Boolean = false, val serverStatus: String = "Online")

    @OptIn(KordUnsafe::class, KordExperimental::class)
    private suspend fun checker() {
        var members = 0
        var status = false
        while(loaded) {
            try {
                HttpClient {install(JsonFeature)}.use {client->
                    val response: Query = client.get("https://mcapi.xdefcon.com/server/$STATUS_SERVER/status/json")
                    val tStatus = response.online
                    if(status != tStatus) {
                        status = !status
                        kord.unsafe.voiceChannel(GUILD_ID, STATUS_CHANNEL).edit {
                            name = "Status: ${response.serverStatus.replaceFirstChar {
                                if(it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            }}"
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