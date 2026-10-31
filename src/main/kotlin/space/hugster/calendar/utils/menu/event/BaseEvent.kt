package space.hugster.calendar.utils.menu.event

import org.bukkit.Bukkit
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

open class BaseEvent(
    async: Boolean = !Bukkit.isPrimaryThread()
) : Event(async)
{
    companion object
    {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS

    fun call(): Boolean
    {
        Bukkit.getPluginManager().callEvent(this)
        return this is Cancellable && isCancelled
    }
}
