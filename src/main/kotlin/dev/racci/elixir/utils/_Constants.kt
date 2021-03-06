package dev.racci.elixir.utils

import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.envOrNull

// Allows for easier imports of the secret magic needed
// val BOT_TOKEN = env("TOKEN")
// val GUILD_ID = Snowflake(env("GUILD_ID"))
// val ADMIN = Snowflake(env("ADMIN_ROLE"))
// val MODERATORS = Snowflake(env("MODERATOR_ROLE"))
// val FULLMODERATORS = Snowflake(env("MODERATOR_PING_ROLE"))
// val TRIALMODERATORS = Snowflake(env("TRIAL_MODERATOR_ROLE"))
// val SUPPORT_TEAM = Snowflake(env("SUPPORT_ROLE"))
// val MOD_ACTION_LOG = Snowflake(env("MOD_ACTION_LOG"))
// val MESSAGE_LOGS = Snowflake(env("MESSAGE_LOGS"))
// val SUPPORT_CHANNEL = Snowflake(env("SUPPORT_CHANNEL"))
// val JOIN_CHANNEL = Snowflake(env("JOIN_CHANNEL"))
// val GITHUB_OAUTH = env("GITHUB_OAUTH")
// val CONFIG_PATH = env("CONFIG_PATH")
// val SENTRY_DSN = envOrNull("SENTRY_DSN")
// val MEMBER_COUNTER = Snowflake(env("MEMBER_COUNTER"))
// val STATUS_CHANNEL = Snowflake(env("STATUS_CHANNEL"))
// val STATUS_SERVER = env("STATUS_SERVER")
// const val JDBC_URL = "jdbc:sqlite:database.db"

val BOT_TOKEN = env("TOKEN")
val DATA_PATH = env("DATA_PATH")
val MONGO_URI = envOrNull("MONGO_URI") ?: "mongodb://localhost:27017"
val SENTRY_DSN = envOrNull("SENTRY_DSN")
val GITHUB_OAUTH = envOrNull("GITHUB_OAUTH")
