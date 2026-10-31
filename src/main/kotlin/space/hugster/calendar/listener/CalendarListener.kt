package space.hugster.calendar.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import space.hugster.calendar.AdventCalendar
import space.hugster.calendar.config.CalendarConfig
import space.hugster.calendar.utils.CC

class CalendarListener(
    private val plugin: AdventCalendar
) : Listener
{
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPreLogin(event: AsyncPlayerPreLoginEvent)
    {
        if (event.loginResult !=
            AsyncPlayerPreLoginEvent.Result.ALLOWED) return
        plugin.calendarManager
            .loadProfileSync(event.uniqueId)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent)
    {
        val player = event.player
        val uuid = player.uniqueId
        val manager = plugin.calendarManager

        if (!manager.hasProfile(uuid)) {
            manager.loadProfileAsync(uuid)
        }

        if (!CalendarConfig.JOIN_REMINDER) return

        plugin.foliaScheduler.entity(player).runDelayed({ _ ->
            if (!player.isOnline) return@runDelayed
            val currentDay = CalendarConfig.getCurrentAdventDay()
            if (currentDay == -1) return@runDelayed

            val profile = manager.getProfile(uuid)
            val unclaimed = if (CalendarConfig.ALLOW_PAST_DAYS_CLAIM) {
                (1..currentDay).any { !profile.hasClaimed(it) }
            } else {
                !profile.hasClaimed(currentDay)
            }

            if (unclaimed) {
                CC.send(player, CalendarConfig.JOIN_REMINDER_MESSAGE
                    .replace("%day%", currentDay.toString()))
            }
        }, CalendarConfig.JOIN_REMINDER_DELAY_TICKS)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent)
    {
        val profile = plugin.calendarManager
            .removeProfile(event.player.uniqueId) ?: return
        plugin.calendarManager.saveProfileAsync(profile)
    }
}
