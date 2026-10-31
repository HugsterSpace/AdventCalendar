package space.hugster.calendar.utils.menu

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerQuitEvent
import space.hugster.calendar.AdventCalendar

class MenuListener(
    private val plugin: AdventCalendar
) : Listener
{
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    fun onButtonPress(event: InventoryClickEvent)
    {
        val player = event.whoClicked as? Player ?: return
        val openMenu = Menu.currentlyOpenedMenus[player.uniqueId] ?: return

        val topInventory = event.view.topInventory
        val clickedInventory = event.clickedInventory ?: return

        if (clickedInventory == topInventory) {
            event.isCancelled = true
        }

        val click = event.click
        if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT || click == ClickType.DOUBLE_CLICK) {
            if (clickedInventory != topInventory) {
                event.isCancelled = true
                return
            }
        }

        if (click == ClickType.NUMBER_KEY || click == ClickType.SWAP_OFFHAND) {
            event.isCancelled = true
            return
        }

        if (clickedInventory != topInventory) return

        val slot = event.slot
        val button = openMenu.buttons.get(slot) ?: return
        val cancel = button.shouldCancel(player, click)

        if (!cancel && (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT)) {
            event.isCancelled = true
            event.currentItem?.let { player.inventory.addItem(it) }
        } else {
            event.isCancelled = cancel
        }

        button.clicked(player, click)
        button.clicked(player, slot, click, event.hotbarButton)

        if (button.shouldUpdate(player, click)) {
            val currentMenu = Menu.currentlyOpenedMenus[player.uniqueId]
            if (currentMenu != null) {
                openMenu.closedByMenu = true
                if (currentMenu === openMenu) {
                    currentMenu.updateMenu(player)
                } else {
                    currentMenu.openMenu(player)
                }
            }
        }

        if (event.isCancelled) {
            plugin.foliaScheduler.entity(player).run { _ ->
                player.updateInventory()
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    fun onInventoryDrag(event: InventoryDragEvent)
    {
        val player = event.whoClicked as? Player ?: return
        Menu.currentlyOpenedMenus[player.uniqueId] ?: return

        val topSize = event.view.topInventory.size
        for (rawSlot in event.rawSlots) {
            if (rawSlot < topSize) {
                event.isCancelled = true
                return
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClose(event: InventoryCloseEvent)
    {
        val player = event.player as? Player ?: return
        val openMenu = Menu.currentlyOpenedMenus
            .remove(player.uniqueId) ?: return
        if (!openMenu.closedByMenu) {
            openMenu.onClose(player)
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent)
    {
        val player = event.player
        val openMenu = Menu.currentlyOpenedMenus
            .remove(player.uniqueId) ?: return
        openMenu.onClose(player)
    }
}
