package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.GuidanceTeachingCourse
import restarhalf.stellar.schedule.domain.model.RemoteCampus
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort

class AcademicPortImpl(
    private val gateway: JwxtGateway,
    private val settings: TermSettingsPort,
) : AcademicPort {

    override suspend fun fetchCurrentTermId(): String {

        return try {
            val term = gateway.getCurrentTerm()
            if (!term.isSuccess()) {
                throw IllegalStateException(term.messageOrEmpty().ifBlank { "获取当前学期失败" })
            }
            val id = term.data?.firstOrNull()?.semesterId.orEmpty()
            if (id.isBlank()) {
                throw IllegalStateException("当前学期响应中无学期 ID")
            }

            settings.setCurrentTermId(id)
            id
        } catch (e: Exception) {

            val cachedId = settings.observeCurrentTermId().first()
            if (cachedId.isNotBlank()) {
                cachedId
            } else {

                throw e
            }
        }
    }

    override suspend fun fetchCampuses(): List<RemoteCampus> {
        val campus = gateway.getCampusList()
        if (!campus.isSuccess()) {
            throw IllegalStateException(campus.msg.ifBlank { "获取校区失败" })
        }
        return campus.data.map {
            RemoteCampus(
                id = it.kbjcmsid,
                name = it.kbjcmsmc,
                isDefault = it.isDefaultCampus(),
            )
        }
    }

    override suspend fun fetchSemesterIds(): List<String> {
        return gateway.getSemesterListFromEndpoint().map { it.semesterId }
            .filter { it.isNotBlank() }.distinct()
    }

    override suspend fun fetchExaminations(
        semester: String,
        nameOrNumber: String
    ): List<Examination> {
        val resp =
            gateway.fetchExaminationArrangement(semester = semester, nameOrNumber = nameOrNumber)
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.messageOrEmpty().ifBlank { "获取考试安排失败" })
        }
        val items = resp.data
        return items.map {
            Examination(
                courseNumber = it.courseNumber,
                courseName = it.courseName,
                time = it.time,
                examinationPlace = it.examinationPlace,
                zwh = it.zwh,
                ksbz = it.ksbz
            )
        }
    }

    override suspend fun fetchGradeReport(semester: String): TermGradeReport {
        val resp = gateway.fetchTermGradeReport(semester = semester)
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.messageOrEmpty().ifBlank { "获取成绩失败" })
        }

        val item = resp.data?.firstOrNull() ?: return TermGradeReport()
        return TermGradeReport(
            studentId = item.studentId,
            studentName = item.name,
            enrollmentYear = item.enrollmentYear,
            averageScore = item.averageScore,
            earnedCredits = item.earnedCredits,
            totalGradePoints = item.totalGradePoints,
            averageCreditGradePoint = item.averageCreditGradePoint,
            achievements =
                item.achievement.map {
                    GradeCourse(
                        courseCode = it.courseCode,
                        courseName = it.courseName,
                        score = it.score,
                        gradePoint = it.gradePoint,
                        credit = it.credit,
                        curriculumAttributes = it.curriculumAttributes,
                        courseNature = it.courseNature,
                        examName = it.examName,
                        examinationNature = it.examinationNature,
                        passStatus = it.passStatus,
                        gradeLevel = it.gradeLevel,
                        markFlag = it.markFlag,
                        repeatSemester = it.repeatSemester,
                        gradeId = it.gradeId,
                        semester = it.semester
                    )
                })
    }

    override suspend fun fetchGuidanceTeachingCourses(
        kcxz: String,
        kcsx: String,
        kcmc: String
    ): List<GuidanceTeachingCourse> {
        val resp = gateway.fetchGuidanceTeachingCourses(
            kcxz = kcxz,
            kcsx = kcsx,
            kcmc = kcmc
        )
        if (!resp.isSuccess()) {
            throw IllegalStateException(resp.messageOrEmpty().ifBlank { "获取指导教学课程失败" })
        }
        return resp.data.map {
            GuidanceTeachingCourse(
                courseAttribute = it.courseAttribute,
                openSemester = it.openSemester,
                courseName = it.courseName,
                totalHours = it.totalHours,
                courseCode = it.courseCode,
                kclbmc = it.kclbmc,
                courseUnits = it.courseUnits,
                whetherTest = it.whetherTest,
                credit = it.credit,
                evaluationMode = it.evaluationMode
            )
        }
    }

    override suspend fun fetchTeachingWeekTotal(): Int {
        return runCatching {
            val resp = gateway.getTeachingWeek()
            if (resp.isSuccess()) resp.data.size else 0
        }.getOrElse { e ->
            if (e is CancellationException) throw e
            0
        }
    }
}
