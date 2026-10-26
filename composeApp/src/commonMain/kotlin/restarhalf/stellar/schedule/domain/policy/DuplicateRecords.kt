package restarhalf.stellar.schedule.domain.policy

import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.PEAppointmentItem
import restarhalf.stellar.schedule.domain.model.PEFreeApplyItem
import restarhalf.stellar.schedule.domain.model.PESubjectHistoryItem
import restarhalf.stellar.schedule.domain.model.PESubjectScore
import restarhalf.stellar.schedule.domain.model.PEYearScore

fun List<Examination>.distinctExaminations(): List<Examination> =
    distinctBy {
        ExaminationIdentity(
            courseNumber = it.courseNumber,
            courseName = it.courseName,
            time = it.time,
            examinationPlace = it.examinationPlace,
            zwh = it.zwh,
        )
    }

fun List<PEAppointmentItem>.distinctAppointments(): List<PEAppointmentItem> =
    distinctBy { AppointmentIdentity.of(it) }

fun List<PEYearScore>.distinctYearScores(): List<PEYearScore> =
    distinctBy { it.schoolYear }

fun List<PESubjectScore>.distinctSubjectScores(): List<PESubjectScore> =
    distinctBy { SubjectScoreIdentity(subjectId = it.subjectId, subName = it.subName) }

fun List<PESubjectHistoryItem>.distinctSubjectHistory(): List<PESubjectHistoryItem> =
    distinctBy { SubjectHistoryIdentity.of(it) }

fun List<PEFreeApplyItem>.distinctFreeApplies(): List<PEFreeApplyItem> =
    distinctBy { FreeApplyIdentity.of(it) }

private data class ExaminationIdentity(
    val courseNumber: String,
    val courseName: String,
    val time: String,
    val examinationPlace: String,
    val zwh: String,
)

private data class AppointmentIdentity(
    val temporaryId: String,
    val appointmentId: String,
    val fallbackName: String,
    val fallbackDate: String,
    val fallbackTimes: String,
) {
    companion object {
        fun of(item: PEAppointmentItem): AppointmentIdentity = when {
            item.temporaryId.isNotBlank() -> AppointmentIdentity(item.temporaryId, "", "", "", "")
            item.appointmentId.isNotBlank() -> AppointmentIdentity("", item.appointmentId, "", "", "")
            else -> AppointmentIdentity(
                temporaryId = "",
                appointmentId = "",
                fallbackName = item.appointmentName,
                fallbackDate = item.appointmentDate,
                fallbackTimes = item.appointmentTimes,
            )
        }
    }
}

private data class SubjectScoreIdentity(
    val subjectId: String,
    val subName: String,
)

private data class SubjectHistoryIdentity(
    val sourceScoreId: String,
    val fallbackName: String,
    val fallbackTime: String,
    val fallbackSession: String,
) {
    companion object {
        fun of(item: PESubjectHistoryItem): SubjectHistoryIdentity =
            if (item.sourceScoreId.isNotBlank()) {
                SubjectHistoryIdentity(item.sourceScoreId, "", "", "")
            } else {
                SubjectHistoryIdentity(
                    sourceScoreId = "",
                    fallbackName = item.subName,
                    fallbackTime = item.scoreTime,
                    fallbackSession = item.sessionName,
                )
            }
    }
}

private data class FreeApplyIdentity(
    val applyId: String,
    val schoolYear: String,
    val freeApplyType: String,
    val applyTime: String,
    val crtTime: String,
) {
    companion object {
        fun of(item: PEFreeApplyItem): FreeApplyIdentity =
            if (item.applyId.isNotBlank()) {
                FreeApplyIdentity(item.applyId, "", "", "", "")
            } else {
                FreeApplyIdentity(
                    applyId = "",
                    schoolYear = item.schoolYear,
                    freeApplyType = item.freeApplyType,
                    applyTime = item.applyTime,
                    crtTime = item.crtTime,
                )
            }
    }
}
