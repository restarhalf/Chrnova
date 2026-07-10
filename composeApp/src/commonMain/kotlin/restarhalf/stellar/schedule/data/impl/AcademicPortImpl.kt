package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.GuidanceTeachingCourse
import restarhalf.stellar.schedule.domain.model.RemoteCampus
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.SettingsPort

/**
 * 教务数据端口实现类
 *
 * 实现AcademicPort接口，负责从教务系统获取学术数据。
 * 包括学期信息、校区列表、考试安排、成绩报告等。
 *
 * @param gateway 教务系统网关客户端
 * @param settings 设置端口，用于缓存当前学期ID
 */
class AcademicPortImpl(
    private val gateway: JwxtGateway,
    private val settings: SettingsPort,
) : AcademicPort {

    /**
     * 获取当前学期ID
     *
     * 优先返回缓存值，同时后台更新缓存。
     * 如果缓存为空且网络请求失败，抛出异常。
     *
     * @return 当前学期ID
     * @throws IllegalStateException 获取失败时抛出
     */
    override suspend fun fetchCurrentTermId(): String {
        // 先尝试网络请求
        return try {
            val term = gateway.getCurrentTerm()
            if (!term.isSuccess()) {
                throw IllegalStateException(term.messageOrEmpty().ifBlank { "获取当前学期失败" })
            }
            val id = term.data?.firstOrNull()?.semesterId.orEmpty()
            if (id.isBlank()) {
                throw IllegalStateException("当前学期响应中无学期 ID")
            }
            // 成功获取，更新缓存
            settings.setCurrentTermId(id)
            id
        } catch (e: Exception) {
            // 网络请求失败，尝试从缓存获取
            val cachedId = settings.observeCurrentTermId().first()
            if (cachedId.isNotBlank()) {
                cachedId
            } else {
                // 缓存也为空，抛出异常
                throw e
            }
        }
    }

    /**
     * 获取校区列表
     * 
     * @return 远程校区列表
     * @throws IllegalStateException 获取失败时抛出
     */
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

    /**
     * 获取学期ID列表
     * 
     * @return 学期ID列表（去重）
     */
    override suspend fun fetchSemesterIds(): List<String> {
        return gateway.getSemesterListFromEndpoint().map { it.semesterId }
            .filter { it.isNotBlank() }.distinct()
    }

    /**
     * 获取考试安排
     * 
     * @param semester 学期ID
     * @param nameOrNumber 课程名称或编号筛选
     * @return 考试安排列表
     * @throws IllegalStateException 获取失败时抛出
     */
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

    /**
     * 获取学期成绩报告
     * 
     * @param semester 学期ID
     * @return 学期成绩报告
     * @throws IllegalStateException 获取失败时抛出
     */
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

    /**
     * 获取指导教学课程列表
     *
     * @param kcxz 课程性质（54=创新创业专业融合教育选修，61=专业选修）
     * @param kcsx 课程属性筛选（可选）
     * @param kcmc 课程名称筛选（可选）
     * @return 指导教学课程列表
     * @throws IllegalStateException 获取失败时抛出
     */
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
        }.getOrDefault(0)
    }
}
