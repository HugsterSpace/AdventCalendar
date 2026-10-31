package space.hugster.calendar.utils.menu.task

import space.hugster.calendar.utils.menu.Menu

class MenuUpdateTask : TaskTicker(
    1,
    20,
    false
) {
    override fun onRun()
    {
        if (Menu.currentlyOpenedMenus.isEmpty()) return

        val iterator = Menu.currentlyOpenedMenus.entries.iterator()
        while (iterator.hasNext())
        {
            val entry = iterator.next()
            val player = plugin.server.getPlayer(entry.key)
            if (player == null || !player.isOnline) {
                iterator.remove()
                continue
            }

            val menu = entry.value
            if (menu.autoUpdate) {
                plugin.foliaScheduler.entity(player).run { _ ->
                    if (Menu.currentlyOpenedMenus[player.uniqueId] === menu) {
                        menu.updateMenu(player)
                    }
                }
            }
        }
    }

    override fun getTickType(): TickType = TickType.NONE
    override fun getStartTick(): Int = 0
}
