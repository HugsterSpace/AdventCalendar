package space.hugster.calendar.data

import java.util.UUID

class CalendarProfile(
    val uuid: UUID,
    @Volatile var claimedMask: Int = 0
) {
    fun hasClaimed(day: Int): Boolean
    {
        if (day !in 1..28) return false
        return (claimedMask and (1 shl (day - 1))) != 0
    }

    fun setClaimed(day: Int, claimed: Boolean = true)
    {
        if (day !in 1..28) return
        val bit = 1 shl (day - 1)
        claimedMask = if (claimed) {
            claimedMask or bit
        } else {
            claimedMask and bit.inv()
        }
    }

    fun reset()
    {
        claimedMask = 0
    }

    fun claimedCount(): Int = Integer.bitCount(claimedMask)
}