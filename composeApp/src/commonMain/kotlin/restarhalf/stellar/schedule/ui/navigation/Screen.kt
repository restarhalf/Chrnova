package restarhalf.stellar.schedule.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
sealed interface Screen : NavKey {
    
    @Serializable
    data object Main : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object Schedule : Screen

    @Serializable
    data object EMS : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object ChangeBackground : Screen

    @Serializable
    data object About : Screen

    @Serializable
    data object PEScore : Screen

    @Serializable
    data class PEDetail(val schoolYear: String) : Screen

    @Serializable
    data class PESubjectHistory(
        val schoolYear: String,
        val subjectId: String,
        val subjectName: String,
        val unit: String = "",
        val currentResult: String? = null,
    ) : Screen

    @Serializable
    data object Log : Screen

    @Serializable
    data class ClassEdit(val courseId: Long? = null, val dayOfWeek: Int = 1, val startSection: Int = 1, val selectedWeek: Int = 1) : Screen

    @Serializable
    data class TransCourseEdit(val courseId: Long) : Screen

    @Serializable
    data class ExamEdit(val examinationId: Long? = null) : Screen

    @Serializable
    data object Papers : Screen

    @Serializable
    data class PapersDetail(val paperId: String) : Screen

    @Serializable
    data object PapersUpload : Screen

    @Serializable
    data object Evaluation : Screen

    @Serializable
    data class EvaluationCourse(
        val courseName: String,
        val teacher: String = "",
    ) : Screen

    @Serializable
    data class EvaluationDetail(val evaluationId: String) : Screen

    @Serializable
    data class EvaluationSubmit(val evaluationId: String? = null) : Screen

    @Serializable
    data object JwxtLogin : Screen

    @Serializable
    data object PELogin : Screen

    @Serializable
    data object PEQRCode : Screen

    @Serializable
    data object PEAppointment : Screen

    @Serializable
    data object PEAppointmentWeb : Screen

    @Serializable
    data object PEFreeApply : Screen

    @Serializable
    data object PEFreeApplyEdit : Screen

    @Serializable
    data class PEFreeApplyDetail(val applyId: String) : Screen

    @Serializable
    data object Profile : Screen

    @Serializable
    data object ElectiveCredit : Screen

    @Serializable
    data object CourseEnroll : Screen

    @Serializable
    data object CourseSelection : Screen

    @Serializable
    data object AnnouncementList : Screen

    @Serializable
    data class AnnouncementDetail(val announcementId: String) : Screen

    @Serializable
    data class AnnouncementImageViewer(val url: String) : Screen

}
