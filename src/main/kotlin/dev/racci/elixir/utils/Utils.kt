package dev.racci.elixir.utils

import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.types.CheckContext

suspend fun CheckContext<*>.configPresent() {
    if (!passed) {
        return
    }

    if (guildFor(event) == null) fail("Must be in a server")

    val config = DatabaseHelper.getConfig(guildFor(event)!!.id)

    if (config == null) {
        fail("Unable to access config for this guild! Please inform a member of staff")
    } else pass()
}
