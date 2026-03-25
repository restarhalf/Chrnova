package restarhalf.stellar.schedule.data.impl

import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.RemoteCampus
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort

class AcademicPortImpl(
    private val gateway: JwxtGateway,
) : AcademicPort {

    override suspend fun fetchCurrentTermId(): String {
        val term = gateway.getCurrentTerm()
        if (!term.isSuccess()) {
            throw IllegalStateException(term.messageOrEmpty().ifBlank { "获取当前学期失败" })
        }
        val id = term.data?.firstOrNull()?.semesterId.orEmpty()
        if (id.isBlank()) {
            throw IllegalStateException("当前学期响应中无学期 ID")
        }
        return id
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
        return gateway.getSemesterList().map { it.xnxq01id }.filter { it.isNotBlank() }.distinct()
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
}
