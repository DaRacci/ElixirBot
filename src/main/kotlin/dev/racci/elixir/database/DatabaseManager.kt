package dev.racci.elixir.database

/**
 * The Database system within the bot
 * @author chalkyjeans
 */
/* object DatabaseManager {

    private val logger = KotlinLogging.logger { }
    private val config = HikariConfig()
    private var dataSource: HikariDataSource

    init {
        config.jdbcUrl = JDBC_URL
        config.connectionTestQuery = "SELECT 1"
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        logger.info("Connected to database.")
    }

    object Warn : Table("warn") {

        val id = text("id")
        val points = text("points").nullable()

        override val primaryKey = PrimaryKey(id)
    }

    object RoleSelector : Table("role_selector") {

        val name = text("name")
        val messageId = long("messageId")
        val channelId = long("channelId")

        override val primaryKey = PrimaryKey(name)
    }

    fun startDatabase() {
        try {
            val database = Path.of("database.db")

            if (Files.notExists(database)) {
                Files.createFile(database)

                logger.info("Created database file.")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        transaction {
            SchemaUtils.createMissingTablesAndColumns(Warn)
            SchemaUtils.createMissingTablesAndColumns(RoleSelector)
        }
    }
} */
