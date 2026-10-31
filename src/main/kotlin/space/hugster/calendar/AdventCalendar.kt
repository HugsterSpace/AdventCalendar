package space.hugster.calendar

import com.cjcrafter.foliascheduler.FoliaCompatibility
import com.cjcrafter.foliascheduler.ServerImplementation
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import revxrsal.commands.Lamp
import revxrsal.commands.bukkit.BukkitLamp
import revxrsal.commands.bukkit.actor.BukkitCommandActor
import space.hugster.calendar.commands.CalendarCommand
import space.hugster.calendar.config.CalendarConfig
import space.hugster.calendar.listener.CalendarListener
import space.hugster.calendar.manager.CalendarManager
import space.hugster.calendar.placeholder.CalendarExpansion
import space.hugster.calendar.utils.menu.Menu
import space.hugster.calendar.utils.menu.MenuListener
import space.hugster.calendar.utils.menu.task.TaskTicker

class AdventCalendar : JavaPlugin()
{
    lateinit var foliaScheduler: ServerImplementation
        private set
    lateinit var calendarManager: CalendarManager
        private set
    private lateinit var lamp: Lamp<BukkitCommandActor>

    companion object
    {
        lateinit var INSTANCE: AdventCalendar
            private set
    }

    override fun onEnable()
    {
        INSTANCE = this

        foliaScheduler = FoliaCompatibility(this)
            .serverImplementation

        CalendarConfig.loadConfig(this)

        calendarManager = CalendarManager(this)
        calendarManager.init()

        Menu.init()

        server.pluginManager.registerEvents(
            MenuListener(this), this)
        server.pluginManager.registerEvents(
            CalendarListener(this), this)

        lamp = BukkitLamp.builder(this).build()
        lamp.register(CalendarCommand(this))

        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            CalendarExpansion(this).register()
        }
    }

    override fun onDisable()
    {
        TaskTicker.cancelAll()
        for (uuid in Menu.currentlyOpenedMenus.keys) {
            server.getPlayer(uuid)?.closeInventory()
        }
        Menu.currentlyOpenedMenus.clear()

        if (::calendarManager.isInitialized) {
            calendarManager.shutdown()
        }
    }
}
