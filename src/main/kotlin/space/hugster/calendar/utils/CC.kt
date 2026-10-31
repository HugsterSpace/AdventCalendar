package space.hugster.calendar.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.util.regex.Pattern

object CC
{
    private val HEX_PATTERN: Pattern = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val LEGACY_SERIALIZER: LegacyComponentSerializer = LegacyComponentSerializer.legacySection()

    @JvmStatic
    fun translate(text: String): String
    {
        if (text.isEmpty()) return text

        val matcher = HEX_PATTERN.matcher(text)
        val buffer = StringBuilder(text.length + 32)
        var lastEnd = 0

        while (matcher.find()) {
            buffer.append(text, lastEnd, matcher.start())
            val hex = matcher.group(1)
            buffer.append("§x")
            for (i in 0 until 6) {
                buffer.append('§').append(hex[i])
            }
            lastEnd = matcher.end()
        }
        buffer.append(text, lastEnd, text.length)

        val chars = buffer.toString().toCharArray()
        for (i in 0 until chars.size - 1) {
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = '§'
                chars[i + 1] = chars[i + 1].lowercaseChar()
            }
        }
        return String(chars)
    }

    @JvmStatic
    fun component(text: String): Component =
        LEGACY_SERIALIZER.deserialize(translate(text))

    @JvmStatic
    fun send(sender: CommandSender, message: String) {
        if (message.isNotBlank())
        {
            sender.sendMessage(component(message))
        }
    }

    @JvmStatic
    fun console(msg: String?)
    {
        if (!msg.isNullOrBlank()) {
            Bukkit.getConsoleSender().sendMessage(component(msg))
        }
    }

    @JvmStatic
    fun broadcast(msg: String?)
    {
        if (!msg.isNullOrBlank()) {
            val comp = component(msg)
            for (player in Bukkit.getOnlinePlayers()) {
                player.sendMessage(comp)
            }
        }
    }
}
