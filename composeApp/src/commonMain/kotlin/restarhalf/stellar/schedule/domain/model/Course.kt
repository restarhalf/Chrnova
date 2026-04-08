package restarhalf.stellar.schedule.domain.model

data class Course(
    val id: Long = 0,
    val name: String,
    val semesterId: String = "",
    val location: String,
    val teacher: String,
    val dayOfWeek: Int,
    val startSection: Int,
    val sectionCount: Int,
    val weeks: List<Int>,
    val color: String,
    val type: Int = 0,
    val remoteKey: String = "",
    val originRemoteKey: String? = null,
    val targetWeek: Int = 0,
)
