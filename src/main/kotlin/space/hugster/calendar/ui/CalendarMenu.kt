package space.hugster.calendar.ui

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import space.hugster.calendar.config.CalendarConfig
import space.hugster.calendar.data.DayReward
import space.hugster.calendar.utils.ItemBuilder
import space.hugster.calendar.utils.menu.Button
import space.hugster.calendar.utils.menu.Menu

class CalendarMenu : Menu()
{
    init
    {
        autoUpdate = CalendarConfig.MENU_AUTO_UPDATE
        placeholder = false
    }

    override fun getTitle(player: Player): String =
        CalendarConfig.MENU_TITLE

    override fun getSize(): Int =
        CalendarConfig.MENU_SIZE

    override fun getButtons(player: Player): Map<Int, Button>
    {
        val menuSize = CalendarConfig.MENU_SIZE
        val map = Int2ObjectOpenHashMap<Button>(menuSize)

        val filler = CalendarConfig.FILLER
        if (filler.enabled) {
            val fillerItem = ItemBuilder(filler.material)
                .apply {
                    if (filler.texture.isNotEmpty()) texture(filler.texture)
                    name(filler.name)
                    if (filler.lore.isNotEmpty()) lore(filler.lore)
                }
                .flags()
                .build()

            val fillerBtn = object : Button() {
                override fun getButtonItem(player: Player): ItemStack = fillerItem
            }

            if (filler.slots.isEmpty()) {
                for (slot in 0 until menuSize) {
                    map.put(slot, fillerBtn)
                }
            } else {
                for (slot in filler.slots) {
                    if (slot in 0 until menuSize) {
                        map.put(slot, fillerBtn)
                    }
                }
            }
        }

        val iter = CalendarConfig.DAYS.int2ObjectEntrySet()
            .fastIterator()
        while (iter.hasNext()) {
            val reward = iter.next().value
            if (reward.slot in 0 until menuSize) {
                map.put(reward.slot, DayButton(reward))
            }
        }

        return map
    }

    private class DayButton(
        private val reward: DayReward
    ) : Button()
    {
        override fun getButtonItem(player: Player): ItemStack
        {
            val manager = plugin.calendarManager
            val profile = manager.getProfile(player.uniqueId)
            val status = manager.getDayStatus(profile, reward.day)
            val state = manager.resolveStateConfig(reward, status)
            val countdown = manager.formatCountdownUntilDay(reward.day)
            val day = reward.day.toString()

            val displayLore = ArrayList<String>(state.lore.size + reward.rewardDescription.size)
            for (line in state.lore) {
                if (line.contains("%rewards%")) {
                    for (desc in reward.rewardDescription) {
                        displayLore.add(
                            desc.replace("%day%", day)
                                .replace("%countdown%", countdown)
                                .replace("%player%", player.name)
                        )
                    }
                } else {
                    displayLore.add(
                        line.replace("%day%", day)
                            .replace("%countdown%", countdown)
                            .replace("%status%", status.name)
                            .replace("%player%", player.name)
                    )
                }
            }

            return ItemBuilder(state.material)
                .apply {
                    if (state.texture.isNotEmpty()) {
                        texture(state.texture)
                    }
                }
                .name(
                    state.name
                        .replace("%day%", day)
                        .replace("%countdown%", countdown)
                        .replace("%player%", player.name)
                )
                .lore(displayLore)
                .glow(state.glow)
                .flags()
                .build()
        }

        override fun clicked(player: Player, clickType: ClickType)
        {
            plugin.calendarManager.attemptClaim(player, reward.day)
        }

        override fun shouldUpdate(player: Player, clickType: ClickType): Boolean = true
    }
}
