package restarhalf.stellar.schedule.data.local

import androidx.room3.TypeConverter
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun fromList(value: List<Int>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toList(value: String): List<Int> {
        return Json.decodeFromString(value)
    }
}
