package space.hugster.calendar.utils

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import space.hugster.calendar.utils.CC.translate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class ItemBuilder(
    var item: ItemStack
) {
    companion object
    {
        private val LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection()
        private val ALL_FLAGS = ItemFlag.entries.toTypedArray()
        private val PROFILE_CACHE = ConcurrentHashMap<String, PlayerProfile>()

        @JvmStatic
        fun copyOf(item: ItemStack): ItemBuilder = ItemBuilder(item.clone())

        @JvmStatic
        fun of(material: Material, block: ItemBuilder.() -> Unit = {}): ItemStack =
            ItemBuilder(material).apply(block).build()

        private fun deserializeLine(line: String): Component =
            LEGACY_SERIALIZER.deserialize(translate(line))
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
    }

    constructor(material: Material) : this(ItemStack(material))

    inline fun edit(block: (ItemStack) -> Unit): ItemBuilder = apply {
        block(item)
    }

    fun edit(function: Consumer<ItemStack>): ItemBuilder = apply {
        function.accept(item)
    }

    inline fun meta(crossinline block: (ItemMeta) -> Unit): ItemBuilder = apply {
        item.editMeta { block(it) }
    }

    fun meta(metaConsumer: Consumer<ItemMeta>): ItemBuilder = apply {
        item.editMeta(metaConsumer)
    }

    @JvmName("metaTyped")
    inline fun <reified T : ItemMeta> meta(crossinline block: (T) -> Unit): ItemBuilder = apply {
        item.editMeta(T::class.java) { block(it) }
    }

    fun <T : ItemMeta> meta(metaClass: Class<T>, metaConsumer: Consumer<T>): ItemBuilder = apply {
        item.editMeta(metaClass, metaConsumer)
    }

    fun type(material: Material): ItemBuilder = apply {
        item = item.withType(material)
    }

    fun amount(amount: Int): ItemBuilder = apply {
        item.amount = amount
    }

    fun enchant(enchantment: Enchantment, level: Int = 1): ItemBuilder = meta {
        it.addEnchant(enchantment, level, true)
    }

    fun name(name: String): ItemBuilder = meta {
        it.displayName(deserializeLine(name))
    }

    fun lore(vararg lore: String): ItemBuilder = meta { meta ->
        val list = ArrayList<Component>(lore.size)
        for (line in lore) {
            list.add(deserializeLine(line))
        }
        meta.lore(list)
    }

    fun lore(lore: List<String>): ItemBuilder = meta { meta ->
        val list = ArrayList<Component>(lore.size)
        for (line in lore) {
            list.add(deserializeLine(line))
        }
        meta.lore(list)
    }

    fun addLore(vararg lines: String): ItemBuilder = meta { meta ->
        val current = meta.lore() ?: ArrayList(lines.size)
        for (line in lines) {
            current.add(deserializeLine(line))
        }
        meta.lore(current)
    }

    fun addLore(lines: List<String>): ItemBuilder = meta { meta ->
        val current = meta.lore() ?: ArrayList(lines.size)
        for (line in lines) {
            current.add(deserializeLine(line))
        }
        meta.lore(current)
    }

    fun owner(owner: String): ItemBuilder = meta<SkullMeta> { meta ->
        meta.owningPlayer = Bukkit.getOfflinePlayer(owner)
    }

    fun texture(base64: String): ItemBuilder = apply {
        if (base64.isEmpty()) return@apply
        if (item.type != Material.PLAYER_HEAD) {
            item = item.withType(Material.PLAYER_HEAD)
        }
        val profile = PROFILE_CACHE.computeIfAbsent(base64) { key ->
            Bukkit.createProfile(UUID.nameUUIDFromBytes(key.toByteArray())).apply {
                setProperty(ProfileProperty("textures", key))
            }
        }
        meta<SkullMeta> { meta ->
            meta.playerProfile = profile
        }
    }

    fun glow(glow: Boolean = true): ItemBuilder = meta {
        it.setEnchantmentGlintOverride(glow)
    }

    fun flags(vararg flags: ItemFlag): ItemBuilder = meta {
        it.addItemFlags(*flags)
    }

    fun flags(): ItemBuilder = meta {
        it.addItemFlags(*ALL_FLAGS)
    }

    fun build(): ItemStack = item
}