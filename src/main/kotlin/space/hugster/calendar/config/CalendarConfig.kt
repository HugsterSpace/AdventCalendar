package space.hugster.calendar.config

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import space.hugster.calendar.AdventCalendar
import space.hugster.calendar.data.DayReward
import space.hugster.calendar.data.DayStateConfig
import space.hugster.calendar.data.DayStatus
import space.hugster.calendar.data.FillerConfig
import space.hugster.calendar.database.StorageType
import space.hugster.calendar.utils.CC
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.EnumMap

object CalendarConfig
{
    private lateinit var config: FileConfiguration

    val STORAGE_TYPE: StorageType
        get() = StorageType.fromString(str("STORAGE.TYPE", "SQLITE"))
    val SQLITE_FILE: String
        get() = str("STORAGE.SQLITE.FILE", "database.db")

    val MYSQL_HOST: String
        get() = str("STORAGE.MYSQL.HOST", "127.0.0.1")
    val MYSQL_PORT: Int
        get() = int("STORAGE.MYSQL.PORT", 3306)
    val MYSQL_DATABASE: String
        get() = str("STORAGE.MYSQL.DATABASE", "advent_calendar")
    val MYSQL_USERNAME: String
        get() = str("STORAGE.MYSQL.USERNAME", "root")
    val MYSQL_PASSWORD: String
        get() = str("STORAGE.MYSQL.PASSWORD", "password")
    val MYSQL_POOL_SIZE: Int
        get() = int("STORAGE.MYSQL.POOL_SIZE", 10)
    val MYSQL_PROPERTIES: String
        get() = str("STORAGE.MYSQL.PROPERTIES", "useSSL=false&autoReconnect=true&characterEncoding=utf8")

    val MONGO_URI: String
        get() = str("STORAGE.MONGODB.URI", "mongodb://127.0.0.1:27017")
    val MONGO_DATABASE: String
        get() = str("STORAGE.MONGODB.DATABASE", "advent_calendar")
    val MONGO_COLLECTION: String
        get() = str("STORAGE.MONGODB.COLLECTION", "players")

    val TIMEZONE: ZoneId
        get() = runCatching { ZoneId.of(str("SETTINGS.TIMEZONE", "America/New_York")) }
            .getOrDefault(ZoneId.systemDefault())
    val MONTH: Int
        get() = int("SETTINGS.MONTH", 12).coerceIn(1, 12)
    val START_DAY: Int
        get() = int("SETTINGS.START_DAY", 1).coerceIn(1, 31)
    val TOTAL_DAYS: Int
        get() = int("SETTINGS.TOTAL_DAYS", 28).coerceIn(1, 28)
    val ALLOW_PAST_DAYS_CLAIM: Boolean
        get() = bool("SETTINGS.ALLOW_PAST_DAYS_CLAIM", false)
    val JOIN_REMINDER: Boolean
        get() = bool("SETTINGS.JOIN_REMINDER", true)
    val JOIN_REMINDER_DELAY_TICKS: Long
        get() = long("SETTINGS.JOIN_REMINDER_DELAY_TICKS", 40L).coerceAtLeast(1L)

    val MENU_TITLE: String
        get() = msg("MENU.TITLE", "&8Advent Calendar (28 Days)")
    val MENU_SIZE: Int
        get() = int("MENU.SIZE", 54).coerceIn(9, 54)
    val MENU_AUTO_UPDATE: Boolean
        get() = bool("MENU.AUTO_UPDATE", true)

    val FILLER_ENABLED: Boolean
        get() = bool("MENU.FILLER.ENABLED", true)
    val FILLER_MATERIAL: Material
        get() = parseMaterial(str("MENU.FILLER.MATERIAL", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE)
    val FILLER_TEXTURE: String
        get() = str("MENU.FILLER.TEXTURE", "")
    val FILLER_NAME: String
        get() = msg("MENU.FILLER.NAME", " ")
    val FILLER_LORE: List<String>
        get() = config.getStringList("MENU.FILLER.LORE")
    val FILLER_SLOTS: IntArray
        get() = config.getIntegerList("MENU.FILLER.SLOTS").toIntArray()

    val FILLER: FillerConfig
        get() = FillerConfig(
            enabled = FILLER_ENABLED,
            material = FILLER_MATERIAL,
            texture = FILLER_TEXTURE,
            name = FILLER_NAME,
            lore = FILLER_LORE,
            slots = FILLER_SLOTS
        )

    val CLAIMED: String
        get() = msg("MESSAGES.CLAIMED", "&7You have claimed the reward for &eDay #%day%&7.")
    val ALREADY_CLAIMED: String
        get() = msg("MESSAGES.ALREADY_CLAIMED", "&cYou have already claimed &eDay #%day%&c!")
    val LOCKED: String
        get() = msg("MESSAGES.LOCKED", "&cDay &e#%day% &cis locked! Unlocks in &e%countdown%&c.")
    val EXPIRED: String
        get() = msg("MESSAGES.EXPIRED", "&cDay &e#%day% &chas expired and can no longer be claimed!")
    val NOT_ACTIVE_MONTH: String
        get() = msg("MESSAGES.NOT_ACTIVE_MONTH", "&cThe Advent Calendar is currently not active!")
    val JOIN_REMINDER_MESSAGE: String
        get() = msg("MESSAGES.JOIN_REMINDER", "&eYou have unclaimed Advent Calendar rewards for &6Day #%day%&e! Use &6/calendar &eto claim!")
    val RELOADED: String
        get() = msg("MESSAGES.RELOADED", "&aAdventCalendar configuration reloaded successfully.")
    val RESET_PLAYER: String
        get() = msg("MESSAGES.RESET_PLAYER", "&aReset Advent Calendar progress for &e%player%&a.")
    val RESET_ALL: String
        get() = msg("MESSAGES.RESET_ALL", "&aReset Advent Calendar progress for &eall players&a.")
    val FORCE_DAY: String
        get() = msg("MESSAGES.FORCE_DAY", "&aUpdated &e%player%&a's Day &e#%day% &astatus.")

    private fun loadStatesAndDays()
    {
        DEFAULT_STATES.clear()
        for (status in DayStatus.entries)
        {
            val sec = config.getConfigurationSection("MENU.STATES.${status.name}")
            DEFAULT_STATES[status] = parseStateConfig(sec, status)
        }

        DAYS.clear()
        val daysSec = config.getConfigurationSection("DAYS")
        val maxDays = TOTAL_DAYS
        for (day in 1..maxDays) {
            val daySec = daysSec?.getConfigurationSection(day.toString())
            val slot = daySec?.getInt("SLOT", defaultSlots[day - 1])
                ?: defaultSlots[day - 1]
            val commands = daySec?.getStringList("COMMANDS")
                ?: listOf("give %player% diamond 1")
            val rewardDesc = daySec?.getStringList("DESCRIPTION")
                ?: listOf("&8• &b1x Diamond")

            val customStates = EnumMap<DayStatus, DayStateConfig>(DayStatus::class.java)
            val customStatesSec = daySec?.getConfigurationSection("STATES")
            if (customStatesSec != null) {
                for (status in DayStatus.entries) {
                    val stSec = customStatesSec.getConfigurationSection(status.name)
                    if (stSec != null) {
                        val fallback = DEFAULT_STATES[status]!!
                        customStates[status] = DayStateConfig(
                            material = parseMaterial(stSec.getString("MATERIAL"), fallback.material),
                            texture = stSec.getString("TEXTURE", fallback.texture) ?: fallback.texture,
                            name = stSec.getString("NAME", fallback.name) ?: fallback.name,
                            lore = if (stSec.contains("LORE")) stSec.getStringList("LORE") else fallback.lore,
                            glow = stSec.getBoolean("GLOW", fallback.glow)
                        )
                    }
                }
            }

            DAYS.put(
                day,
                DayReward(
                    day = day,
                    slot = slot,
                    commands = commands,
                    rewardDescription = rewardDesc,
                    customStates = customStates
                )
            )
        }
    }

    private fun parseStateConfig(sec: ConfigurationSection?, status: DayStatus): DayStateConfig
    {
        val defaultMat = when (status) {
            DayStatus.AVAILABLE -> Material.PLAYER_HEAD
            DayStatus.CLAIMED -> Material.MINECART
            DayStatus.LOCKED -> Material.PLAYER_HEAD
            DayStatus.EXPIRED -> Material.COAL_BLOCK
        }
        val defaultTex = when (status) {
            DayStatus.AVAILABLE -> "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjJiN2I5ZTA3YzM1MGJjZDY2MmJjZjNmMTYyNGE1OGU5NjVhNzZlYTAxOTE2ZGQzM2M0NzViZDFmYzg2OTQ3YiJ9fX0="
            else -> ""
        }
        if (sec == null) {
            return DayStateConfig(
                material = defaultMat,
                texture = defaultTex,
                name = "&eDay #%day%",
                lore = listOf("%rewards%", "", "&7Status: &f${status.name}"),
                glow = status == DayStatus.AVAILABLE
            )
        }
        return DayStateConfig(
            material = parseMaterial(sec.getString("MATERIAL"), defaultMat),
            texture = sec.getString("TEXTURE", defaultTex) ?: defaultTex,
            name = sec.getString("NAME", "&eDay #%day%") ?: "&eDay #%day%",
            lore = sec.getStringList("LORE"),
            glow = sec.getBoolean("GLOW", false)
        )
    }

    private fun parseMaterial(name: String?, fallback: Material): Material
    {
        if (name.isNullOrBlank()) return fallback
        return Material.matchMaterial(
            name.trim().uppercase()) ?: fallback
    }

    fun getCurrentAdventDay(): Int
    {
        val now = ZonedDateTime.now(TIMEZONE)
        if (now.monthValue != MONTH) return -1
        val day = now.dayOfMonth - START_DAY + 1
        return if (day in 1..TOTAL_DAYS) day else -1
    }

    private val defaultSlots = intArrayOf(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    )

    val DEFAULT_STATES: EnumMap<DayStatus, DayStateConfig> =
        EnumMap(DayStatus::class.java)
    val DAYS: Int2ObjectOpenHashMap<DayReward> =
        Int2ObjectOpenHashMap(28)

    fun loadConfig(plugin: AdventCalendar)
    {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config

        loadStatesAndDays()
    }

    private fun msg(path: String, default: String): String
    {
        return CC.translate(config.getString(path, default) ?: default)
    }

    private fun str(path: String, default: String): String
    {
        return config.getString(path, default) ?: default
    }

    private fun int(path: String, default: Int): Int
    {
        return config.getInt(path, default)
    }

    private fun long(path: String, default: Long): Long
    {
        return config.getLong(path, default)
    }

    private fun bool(path: String, default: Boolean): Boolean
    {
        return config.getBoolean(path, default)
    }
}
