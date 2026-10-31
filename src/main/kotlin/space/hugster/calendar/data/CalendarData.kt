package space.hugster.calendar.data

import org.bukkit.Material
import java.util.UUID

enum class DayStatus
{
    CLAIMED,
    AVAILABLE,
    LOCKED,
    EXPIRED
}

data class DayStateConfig(
    val material: Material,
    val texture: String,
    val name: String,
    val lore: List<String>,
    val glow: Boolean
)

data class DayReward(
    val day: Int,
    val slot: Int,
    val commands: List<String>,
    val rewardDescription: List<String>,
    val customStates: Map<DayStatus, DayStateConfig>
)

data class FillerConfig(
    val enabled: Boolean,
    val material: Material,
    val texture: String,
    val name: String,
    val lore: List<String>,
    val slots: IntArray
)
