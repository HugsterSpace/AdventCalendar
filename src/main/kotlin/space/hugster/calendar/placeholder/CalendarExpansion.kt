package space.hugster.calendar.placeholder

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import space.hugster.calendar.AdventCalendar
import space.hugster.calendar.config.CalendarConfig

class CalendarExpansion(private val plugin: AdventCalendar) : PlaceholderExpansion()
{
    override fun getIdentifier(): String = "adventcalendar"
    override fun getAuthor(): String = "Hugster"
    override fun getVersion(): String = plugin.pluginMeta.version
    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String?
    {
        val manager = plugin.calendarManager
        val key = params.lowercase()

        return when
        {
            key == "current_day" -> {
                val day = CalendarConfig.getCurrentAdventDay()
                if (day == -1) "0" else day.toString()
            }
            key == "total_days" -> CalendarConfig.TOTAL_DAYS.toString()
            key == "claimed_count" -> {
                if (player == null) return "0"
                manager.getProfile(player.uniqueId)
                    .claimedCount().toString()
            }
            key == "unclaimed_count" -> {
                if (player == null) return "0"
                val currentDay = CalendarConfig.getCurrentAdventDay()
                if (currentDay == -1) return "0"
                val profile = manager.getProfile(player.uniqueId)

                if (CalendarConfig.ALLOW_PAST_DAYS_CLAIM) {
                    var count = 0
                    for (d in 1..currentDay) {
                        if (!profile.hasClaimed(d)) count++
                    }
                    count.toString()
                } else {
                    if (profile.hasClaimed(currentDay)) "0" else "1"
                }
            }
            key.startsWith("status_") -> {
                if (player == null) return "locked"
                val day = key.removePrefix("status_")
                    .toIntOrNull() ?: return null
                manager.getDayStatus(manager
                    .getProfile(player.uniqueId), day).name.lowercase()
            }
            key.startsWith("countdown_") -> {
                val day = key.removePrefix("countdown_")
                    .toIntOrNull() ?: return null
                manager.formatCountdownUntilDay(day)
            }
            else -> null
        }
    }
}