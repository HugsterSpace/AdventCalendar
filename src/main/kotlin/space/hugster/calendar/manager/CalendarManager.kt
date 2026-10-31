package space.hugster.calendar.manager

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import space.hugster.calendar.AdventCalendar
import space.hugster.calendar.config.CalendarConfig
import space.hugster.calendar.data.CalendarProfile
import space.hugster.calendar.data.DayReward
import space.hugster.calendar.data.DayStateConfig
import space.hugster.calendar.data.DayStatus
import space.hugster.calendar.database.StorageProvider
import space.hugster.calendar.database.StorageType
import space.hugster.calendar.database.impl.MongoStorageProvider
import space.hugster.calendar.database.impl.SqlStorageProvider
import space.hugster.calendar.utils.CC
import space.hugster.calendar.utils.menu.Button
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CalendarManager(private val plugin: AdventCalendar)
{
    val profiles: ConcurrentHashMap<UUID, CalendarProfile> = ConcurrentHashMap()
    private lateinit var storage: StorageProvider

    fun init()
    {
        storage = when (CalendarConfig.STORAGE_TYPE) {
            StorageType.SQLITE, StorageType.MYSQL -> SqlStorageProvider(plugin,
                CalendarConfig.STORAGE_TYPE)
            StorageType.MONGODB -> MongoStorageProvider()
        }
        storage.init()

        for (player in plugin.server.onlinePlayers) {
            loadProfileAsync(player.uniqueId)
        }
    }

    fun reload()
    {
        shutdown()
        CalendarConfig.loadConfig(plugin)
        init()
    }

    fun shutdown()
    {
        if (::storage.isInitialized) {
            for (profile in profiles.values) {
                runCatching { storage.saveProfile(profile) }
            }
            profiles.clear()
            runCatching { storage.shutdown() }
        }
    }

    fun hasProfile(uuid: UUID): Boolean =
        profiles.containsKey(uuid)

    fun getProfile(uuid: UUID): CalendarProfile =
        profiles.computeIfAbsent(uuid) {
            CalendarProfile(it, 0) }

    fun removeProfile(uuid: UUID): CalendarProfile? =
        profiles.remove(uuid)

    fun loadProfileSync(uuid: UUID): CalendarProfile
    {
        val loaded = runCatching { storage.loadProfile(uuid) }
            .getOrElse { CalendarProfile(uuid, 0) }
        profiles[uuid] = loaded
        return loaded
    }

    fun loadProfileAsync(uuid: UUID)
    {
        plugin.foliaScheduler.async().runNow { _ ->
            loadProfileSync(uuid)
        }
    }

    fun saveProfileAsync(profile: CalendarProfile)
    {
        plugin.foliaScheduler.async().runNow { _ ->
            runCatching { storage.saveProfile(profile) }
        }
    }

    fun resetPlayer(uuid: UUID)
    {
        val profile = getProfile(uuid)
        profile.reset()
        saveProfileAsync(profile)
    }

    fun resetAllAsync(onComplete: () -> Unit)
    {
        for (profile in profiles.values) {
            profile.reset()
        }
        plugin.foliaScheduler.async().runNow { _ ->
            runCatching { storage.resetAll() }
            onComplete()
        }
    }

    fun getDayStatus(profile: CalendarProfile, day: Int): DayStatus
    {
        if (profile.hasClaimed(day)) return DayStatus.CLAIMED

        val currentDay = CalendarConfig.getCurrentAdventDay()
        if (currentDay == -1) return DayStatus.LOCKED

        return when {
            day == currentDay -> DayStatus.AVAILABLE
            day < currentDay -> if (CalendarConfig.ALLOW_PAST_DAYS_CLAIM)
                DayStatus.AVAILABLE else DayStatus.EXPIRED
            else -> DayStatus.LOCKED
        }
    }

    fun resolveStateConfig(reward: DayReward, status: DayStatus): DayStateConfig =
        reward.customStates[status] ?: CalendarConfig.DEFAULT_STATES[status]!!

    fun formatCountdownUntilDay(targetDay: Int): String
    {
        val now = ZonedDateTime.now(CalendarConfig.TIMEZONE)
        val month = CalendarConfig.MONTH
        val calendarDay = (CalendarConfig.START_DAY + targetDay - 1).coerceIn(1, 28)

        var unlockTime = now
            .withMonth(month)
            .withDayOfMonth(calendarDay)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        if (!unlockTime.isAfter(now)) {
            if (now.monthValue > month ||
                (now.monthValue == month && now.dayOfMonth > calendarDay)) {
                unlockTime = unlockTime.plusYears(1)
            } else {
                return "00h 00m 00s"
            }
        }

        val duration = Duration.between(now, unlockTime)
        val totalSeconds = duration.seconds
            .coerceAtLeast(0L)
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (days > 0) {
            "${days}d ${hours}h ${minutes}m ${seconds}s"
        } else {
            "${hours}h ${minutes}m ${seconds}s"
        }
    }

    fun attemptClaim(player: Player, day: Int): Boolean
    {
        val reward = CalendarConfig.DAYS.get(day) ?: return false
        val profile = getProfile(player.uniqueId)

        when (getDayStatus(profile, day)) {
            DayStatus.CLAIMED -> {
                Button.playFail(player)
                CC.send(player, CalendarConfig.ALREADY_CLAIMED
                    .replace("%day%", day.toString()))
                return false
            }
            DayStatus.LOCKED -> {
                Button.playFail(player)
                val msg = if (CalendarConfig.getCurrentAdventDay() == -1) {
                    CalendarConfig.NOT_ACTIVE_MONTH
                } else {
                    CalendarConfig.LOCKED
                }
                CC.send(
                    player,
                    msg.replace("%day%", day.toString())
                        .replace("%countdown%", formatCountdownUntilDay(day))
                )
                return false
            }
            DayStatus.EXPIRED -> {
                Button.playFail(player)
                CC.send(player, CalendarConfig.EXPIRED
                    .replace("%day%", day.toString()))
                return false
            }
            DayStatus.AVAILABLE -> {
                profile.setClaimed(day, true)
                saveProfileAsync(profile)

                Button.playSuccess(player)
                CC.send(player, CalendarConfig.CLAIMED
                    .replace("%day%", day.toString()))

                plugin.foliaScheduler.global().run { _ ->
                    val console = plugin.server.consoleSender
                    for (cmd in reward.commands) {
                        val parsed = cmd
                            .replace("%player%", player.name)
                            .replace("%uuid%", player.uniqueId.toString())
                            .replace("%day%", day.toString())
                        plugin.server.dispatchCommand(console, parsed)
                    }
                }
                return true
            }
        }
    }
}
