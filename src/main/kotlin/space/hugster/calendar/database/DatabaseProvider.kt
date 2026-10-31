package space.hugster.calendar.database

import space.hugster.calendar.data.CalendarProfile
import java.util.UUID

enum class StorageType
{
    SQLITE,
    MYSQL,
    MONGODB;

    companion object
    {
        fun fromString(value: String): StorageType =
            entries.firstOrNull {
                it.name.equals(value.trim(), ignoreCase = true)
            } ?: SQLITE
    }
}

interface StorageProvider
{
    fun init()
    fun loadProfile(uuid: UUID): CalendarProfile
    fun saveProfile(profile: CalendarProfile)
    fun resetAll()
    fun shutdown()
}
