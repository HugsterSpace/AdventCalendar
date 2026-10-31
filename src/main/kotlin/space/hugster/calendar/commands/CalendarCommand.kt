package space.hugster.calendar.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission
import space.hugster.calendar.AdventCalendar
import space.hugster.calendar.config.CalendarConfig
import space.hugster.calendar.ui.CalendarMenu
import space.hugster.calendar.utils.CC
import space.hugster.calendar.utils.menu.Menu

@Command("adventcalendar", "calendar", "advent")
class CalendarCommand(private val plugin: AdventCalendar)
{
    @Command("adventcalendar", "calendar", "advent")
    fun onDefault(player: Player)
    {
        CalendarMenu().openMenu(player)
    }

    @Subcommand("open")
    fun onOpen(player: Player)
    {
        CalendarMenu().openMenu(player)
    }

    @Subcommand("claim")
    fun onClaim(player: Player, day: Int)
    {
        if (day !in 1..CalendarConfig.TOTAL_DAYS) return
        plugin.calendarManager.attemptClaim(player, day)
    }

    @Subcommand("reload")
    @CommandPermission("adventcalendar.admin")
    fun onReload(sender: CommandSender)
    {
        plugin.calendarManager.reload()
        for ((uuid, menu) in Menu.currentlyOpenedMenus) {
            val viewer = plugin.server.getPlayer(uuid) ?: continue
            plugin.foliaScheduler.entity(viewer).run { _ ->
                menu.updateMenu(viewer)
            }
        }
        CC.send(sender, CalendarConfig.RELOADED)
    }

    @Subcommand("reset")
    @CommandPermission("adventcalendar.admin")
    fun onReset(sender: CommandSender, target: Player)
    {
        plugin.calendarManager.resetPlayer(target.uniqueId)
        CC.send(sender, CalendarConfig.RESET_PLAYER
            .replace("%player%", target.name))
    }

    @Subcommand("resetall")
    @CommandPermission("adventcalendar.admin")
    fun onResetAll(sender: CommandSender)
    {
        plugin.calendarManager.resetAllAsync {
            CC.send(sender, CalendarConfig.RESET_ALL)
        }
    }

    @Subcommand("setday")
    @CommandPermission("adventcalendar.admin")
    fun onSetDay(sender: CommandSender, target: Player, day: Int, claimed: Boolean)
    {
        if (day !in 1..CalendarConfig.TOTAL_DAYS) return
        val profile = plugin.calendarManager
            .getProfile(target.uniqueId)

        profile.setClaimed(day, claimed)
        plugin.calendarManager.saveProfileAsync(profile)

        val msg = CalendarConfig.FORCE_DAY
            .replace("%player%", target.name)
            .replace("%day%", day.toString())
        CC.send(sender, msg)
    }
}
