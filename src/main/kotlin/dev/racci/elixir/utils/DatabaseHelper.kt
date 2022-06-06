package dev.racci.elixir.utils

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.bson.UuidRepresentation
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo
import kotlin.reflect.KProperty

object DatabaseHelper {

    val database = KMongo.createClient(
        MongoClientSettings.builder()
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .applyConnectionString(ConnectionString(MONGO_URI))
            .build()
    ).coroutine.getDatabase("ElixirBot")

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        TODO()
    }

    suspend fun getConfig(guildId: Snowflake?): GuildConfig? {
        if (guildId == null) return null
        return database.getCollection<GuildConfig>()
            .findOne(GuildConfig::guildId eq guildId)
    }

    suspend fun setConfig(config: GuildConfig): GuildConfig? =
        database.getCollection<GuildConfig>()
            .findOneAndReplace(GuildConfig::guildId eq config.guildId, config)

    suspend fun clearConfig(guildId: Snowflake): Boolean {
        return database.getCollection<GuildConfig>()
            .deleteOne(GuildConfig::guildId eq guildId).deletedCount > 0
    }

    suspend fun getUser(userId: Snowflake): UserConfig? =
        database.getCollection<UserConfig>().findOne(UserConfig::userId eq userId)

    suspend fun setUser(user: UserConfig): UserConfig? =
        database.getCollection<UserConfig>()
            .findOneAndReplace(UserConfig::userId eq user.userId, user)

    suspend fun getWarns(
        guildId: Snowflake,
        userId: Snowflake
    ): Array<UserWarns.WarnData> = database.getCollection<UserWarns>().findOne(UserWarns::guildId eq guildId, UserWarns::userId eq userId)?.warns?.toTypedArray() ?: emptyArray()

    suspend fun addWarn(
        userId: Snowflake,
        guildId: Snowflake,
        issuer: Snowflake?,
        reason: String,
        timestamp: Instant,
        expires: Instant?,
    ) {
        val collection = database.getCollection<UserWarns>()
        val user = collection.find(UserWarns::guildId eq guildId)
            .filter(UserWarns::userId eq userId)
            .first() ?: UserWarns(userId, guildId, mutableListOf())

        user.warns += UserWarns.WarnData(issuer, Snowflake(timestamp), reason, expires)
        collection.replaceOne(UserWarns::guildId eq guildId, user)
    }

    /**
     * Remove a users warning.
     *
     * @param userId The user to remove the warning from.
     * @param guildId The guild in which to remove the warning from.
     * @param warnId The ID of the warning to remove.
     * @return True if the warning was removed, false otherwise.
     */
    suspend fun removeWarn(
        userId: Snowflake,
        guildId: Snowflake,
        warnId: Snowflake,
    ): Boolean {
        val collection = database.getCollection<UserWarns>()
        val user = collection.find(UserWarns::guildId eq guildId)
            .filter(UserWarns::userId eq userId)
            .first()

        if (user?.warns?.removeIf { it.warnId == warnId } == true) {
            collection.replaceOne(UserWarns::guildId eq guildId, user)
            return true
        }

        return false
    }

    @Serializable
    data class GuildConfig(
        val guildId: Snowflake,

        // Role IDs
        val moderatorRole: Snowflake,
        val supportRole: Snowflake,

        // Channel IDs
        val staffActionChannel: Snowflake,
        val logChannel: Snowflake,
        val supportChannel: Snowflake?,
        val joinChannel: Snowflake?,
        val membersChannel: Snowflake?,

        // Minecraft Server tracker
        val statusChannel: Snowflake?,
        val playersChannel: Snowflake?,
        val mcServer: String?,
        val mcPort: Int?,
    )

    @Serializable
    data class UserConfig(
        val userId: Snowflake
    )

    @Serializable
    data class UserWarns(
        val userId: Snowflake,
        val guildId: Snowflake,
        val warns: MutableList<WarnData>
    ) {
        @Serializable
        data class WarnData(
            val issuer: Snowflake?,
            val warnId: Snowflake,
            val reason: String,
            val expires: Instant?
        )
    }
}
