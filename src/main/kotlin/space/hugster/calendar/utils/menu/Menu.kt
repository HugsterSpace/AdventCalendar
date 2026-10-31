package space.hugster.calendar.utils.menu

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import space.hugster.calendar.AdventCalendar
import space.hugster.calendar.utils.CC.translate
import space.hugster.calendar.utils.menu.event.MenuOpenEvent
import space.hugster.calendar.utils.menu.event.MenuUpdateEvent
import space.hugster.calendar.utils.menu.task.MenuUpdateTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

abstract class Menu : InventoryHolder
{
    protected val plugin: AdventCalendar
        get() = AdventCalendar.INSTANCE

    companion object
    {
        val currentlyOpenedMenus: ConcurrentHashMap<UUID, Menu> = ConcurrentHashMap()

        private val DEFAULT_PLACEHOLDER: Button = Button.placeholder(Material.GRAY_STAINED_GLASS_PANE, " ")
        private val LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection()

        fun init()
        {
            MenuUpdateTask()
        }
    }

    val buttons: Int2ObjectOpenHashMap<Button> = Int2ObjectOpenHashMap()
    var autoUpdate: Boolean = false
    var closedByMenu: Boolean = false
    var placeholder: Boolean = false
    var placeholderButton: Button = DEFAULT_PLACEHOLDER

    private var internalInventory: Inventory? = null

    override fun getInventory(): Inventory = internalInventory
        ?: throw IllegalStateException("Menu inventory has not been initialized yet.")

    fun openMenu(player: Player)
    {
        try
        {
            val uuid = player.uniqueId
            val previousMenu = currentlyOpenedMenus[uuid]
            if (previousMenu != null)
            {
                previousMenu.onClose(player)
                previousMenu.closedByMenu = true
                currentlyOpenedMenus.remove(uuid)
                MenuUpdateEvent(this).call()
            } else {
                MenuOpenEvent(this).call()
            }

            buttons.clear()
            buttons.putAll(getButtons(player))

            val rawTitle = translate(getTitle(player))
            val title = LEGACY_SERIALIZER.deserialize(
                if (rawTitle.length > 32) rawTitle.substring(0, 32) else rawTitle
            )
            val size = if (getSize() == -1) size(buttons) else getSize()

            val inv = plugin.server.createInventory(this, size, title)
            internalInventory = inv

            val iter = buttons.int2ObjectEntrySet().fastIterator()
            while (iter.hasNext())
            {
                val entry = iter.next()
                val slot = entry.intKey
                if (slot in 0 until size)
                {
                    inv.setItem(slot, entry.value.getButtonItem(player))
                }
            }

            if (placeholder)
            {
                val fillerItem = placeholderButton.getButtonItem(player)
                for (index in 0 until size)
                {
                    if (!buttons.containsKey(index))
                    {
                        buttons.put(index, placeholderButton)
                        inv.setItem(index, fillerItem)
                    }
                }
            }

            player.openInventory(inv)
            currentlyOpenedMenus[uuid] = this
            onOpen(player)
            closedByMenu = false
        } catch (e: Exception)
        {
            e.printStackTrace()
            player.closeInventory()
        }
    }

    fun updateMenu(player: Player)
    {
        val topInventory = player.openInventory.topInventory
        val invSize = topInventory.size

        buttons.clear()
        buttons.putAll(getButtons(player))

        val fillerItem: ItemStack? = if (placeholder)
            placeholderButton.getButtonItem(player) else null

        for (index in 0 until invSize)
        {
            val button = buttons.get(index)
            if (button != null)
            {
                val newItem = button.getButtonItem(player)
                val currentItem = topInventory.getItem(index)

                if (currentItem == null || currentItem.type != newItem.type
                    || currentItem.amount != newItem.amount)
                {
                    topInventory.setItem(index, newItem)
                } else if (currentItem.itemMeta != newItem.itemMeta)
                {
                    currentItem.itemMeta = newItem.itemMeta
                }
            } else if (fillerItem != null)
            {
                buttons.put(index, placeholderButton)
                if (topInventory.getItem(index) == null)
                {
                    topInventory.setItem(index, fillerItem)
                }
            } else if (topInventory.getItem(index) != null)
            {
                topInventory.setItem(index, null)
            }
        }
    }

    fun size(buttons: Map<Int, Button>): Int
    {
        if (buttons.isEmpty()) return 9
        var highest = 0
        if (buttons is Int2ObjectOpenHashMap<Button>)
        {
            val iter = buttons.keys.intIterator()
            while (iter.hasNext())
            {
                val slot = iter.nextInt()
                if (slot > highest) highest = slot
            }
        } else {
            for (slot in buttons.keys)
            {
                if (slot > highest) highest = slot
            }
        }
        return (((highest / 9) + 1) * 9).coerceIn(9, 54)
    }

    open fun getSize(): Int = -1

    fun getSlot(x: Int, y: Int): Int = (9 * y) + x

    abstract fun getTitle(player: Player): String

    fun getID(): String = this::class.simpleName ?: ""

    abstract fun getButtons(player: Player): Map<Int, Button>

    open fun onOpen(player: Player) {}

    open fun onClose(player: Player) {}
}
