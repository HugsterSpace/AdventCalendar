package space.hugster.calendar.utils.menu

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import space.hugster.calendar.AdventCalendar
import space.hugster.calendar.utils.CC

abstract class Button
{
    protected val plugin: AdventCalendar
        get() = AdventCalendar.INSTANCE

    companion object
    {
        private val LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection()

        @JvmStatic
        fun placeholder(material: Material, vararg title: String): Button
        {
            val cachedItem = ItemStack(material).apply {
                editMeta { meta ->
                    meta.displayName(LEGACY_SERIALIZER.deserialize(
                        CC.translate(title.joinToString(""))))
                }
            }
            return object : Button()
            {
                override fun getButtonItem(player: Player): ItemStack = cachedItem
            }
        }

        @JvmStatic
        fun playFail(player: Player)
        {
            player.playSound(player.location,
                Sound.UI_BUTTON_CLICK, 20F, 0.1F)
        }

        @JvmStatic
        fun playSuccess(player: Player)
        {
            player.playSound(player.location,
                Sound.UI_BUTTON_CLICK, 20F, 15F)
        }

        @JvmStatic
        fun playNeutral(player: Player)
        {
            player.playSound(player.location,
                Sound.UI_BUTTON_CLICK, 20F, 1F)
        }
    }

    abstract fun getButtonItem(player: Player): ItemStack

    open fun clicked(player: Player, clickType: ClickType) {}

    open fun clicked(player: Player, slot: Int, clickType: ClickType, hotbarSlot: Int) {}

    open fun shouldCancel(player: Player, clickType: ClickType): Boolean = true

    open fun shouldUpdate(player: Player, clickType: ClickType): Boolean = false
}
