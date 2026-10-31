package space.hugster.calendar.database.impl

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import space.hugster.calendar.AdventCalendar
import space.hugster.calendar.config.CalendarConfig
import space.hugster.calendar.data.CalendarProfile
import space.hugster.calendar.database.StorageProvider
import space.hugster.calendar.database.StorageType
import java.io.File
import java.util.UUID

object AdventPlayerTable : UUIDTable("advent_players", "uuid")
{
    val claimedMask = integer("claimed_mask").default(0)
}

class AdventPlayerEntity(id: EntityID<UUID>) : UUIDEntity(id)
{
    companion object : UUIDEntityClass<AdventPlayerEntity>(AdventPlayerTable)

    var claimedMask by AdventPlayerTable.claimedMask
}

class SqlStorageProvider(
    private val plugin: AdventCalendar,
    private val type: StorageType
) : StorageProvider
{
    private lateinit var dataSource: HikariDataSource
    private lateinit var database: Database

    override fun init()
    {
        val hikari = HikariConfig().apply {
            poolName = "AdventCalendar-${type.name}"
            isAutoCommit = false

            when (type) {
                StorageType.SQLITE -> {
                    val dbFile = File(plugin.dataFolder,
                        CalendarConfig.SQLITE_FILE).apply {
                        parentFile?.mkdirs()
                    }
                    driverClassName = "org.sqlite.JDBC"
                    jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
                    maximumPoolSize = 1
                    transactionIsolation = "TRANSACTION_SERIALIZABLE"
                    connectionTestQuery = "SELECT 1"
                }
                else -> {
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                    jdbcUrl = "jdbc:mysql://${CalendarConfig.MYSQL_HOST}:${CalendarConfig.MYSQL_PORT}/${CalendarConfig.MYSQL_DATABASE}?${CalendarConfig.MYSQL_PROPERTIES}"
                    username = CalendarConfig.MYSQL_USERNAME
                    password = CalendarConfig.MYSQL_PASSWORD
                    maximumPoolSize = CalendarConfig.MYSQL_POOL_SIZE
                    minimumIdle = maxOf(1, CalendarConfig.MYSQL_POOL_SIZE / 2)
                    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                    addDataSourceProperty("cachePrepStmts", "true")
                    addDataSourceProperty("prepStmtCacheSize", "250")
                    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                    addDataSourceProperty("useServerPrepStmts", "true")
                }
            }
        }

        dataSource = HikariDataSource(hikari)
        database = Database.connect(datasource = dataSource)

        transaction(database) {
            SchemaUtils.create(AdventPlayerTable)
        }
    }

    override fun loadProfile(uuid: UUID): CalendarProfile = transaction(database)
    {
        val entity = AdventPlayerEntity.findById(uuid)
        CalendarProfile(uuid,
            entity?.claimedMask ?: 0)
    }

    override fun saveProfile(profile: CalendarProfile): Unit = transaction(database)
    {
        val existing = AdventPlayerEntity
            .findById(profile.uuid)
        if (existing != null) {
            existing.claimedMask = profile.claimedMask
        } else {
            AdventPlayerEntity.new(profile.uuid) {
                claimedMask = profile.claimedMask
            }
        }
    }

    override fun resetAll(): Unit = transaction(database)
    {
        AdventPlayerTable.deleteAll()
    }

    override fun shutdown()
    {
        if (::dataSource.isInitialized
            && !dataSource.isClosed) {
            dataSource.close()
        }
    }
}
