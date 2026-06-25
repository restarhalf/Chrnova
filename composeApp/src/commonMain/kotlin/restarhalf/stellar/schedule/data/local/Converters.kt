package restarhalf.stellar.schedule.data.local

import androidx.room3.ColumnTypeConverter
import kotlinx.serialization.json.Json

class Converters {

    @ColumnTypeConverter
    fun fromList(value: List<Int>): String {
        return Json.encodeToString(value)
    }

    @ColumnTypeConverter
    fun toList(value: String): List<Int> {
        return Json.decodeFromString(value)
    }
}
