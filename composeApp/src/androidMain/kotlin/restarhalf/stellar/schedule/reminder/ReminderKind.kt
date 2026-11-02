package restarhalf.stellar.schedule.reminder

enum class ReminderKind(
    val requestCode: Int,
    val triggerKey: String,
    val sessionLabel: String,
    val nounLabel: String,
    val endHint: String,
    val business: String,
) {
    Course(
        requestCode = 20001,
        triggerKey = "course_next_trigger",
        sessionLabel = "上课",
        nounLabel = "课程",
        endHint = "下课后",
        business = "course_schedule",
    ),
    Exam(
        requestCode = 21001,
        triggerKey = "exam_next_trigger",
        sessionLabel = "考试",
        nounLabel = "考试",
        endHint = "考试结束后",
        business = "exam_schedule",
    ),
    ;

    companion object {
        fun fromName(raw: String?): ReminderKind =
            entries.firstOrNull { it.name == raw } ?: Course
    }
}
