package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.GuidanceTeachingCourse
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort

/**
 * 选修课学分统计ViewModel
 *
 * 统计X1-X5类别的选修课学分，支持Z到X的转换映射。
 * Z到X的转换关系：
 * - Z1 文史经典与外国文化 → X4 文化传承与社会发展
 * - Z2 艺术鉴赏与审美体验 → X2 艺术鉴赏与审美体验
 * - Z3 经济与社会科学 → X3 生命关怀与健康素养
 * - Z4 自然科学与科技 → X5 科学探索与技术创新
 * - X1 为独立类别
 */
class ElectiveCreditViewModel(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
) : ViewModel() {

    /**
     * 学分类别数据
     *
     * @param code 类别代码（X1-X5）
     * @param name 类别名称
     * @param credits 已获得学分
     * @param courses 课程列表
     */
    data class CreditCategory(
        val code: String,
        val name: String,
        val credits: Double,
        val courses: List<GradeCourse>,
    )

    /**
     * 选修课学分统计UI状态
     *
     * @param loading 是否正在加载
     * @param error 错误消息
     * @param categories 学分类别列表
     */
    data class ElectiveCreditUiState(
        val loading: Boolean = false,
        val error: String = "",
        val categories: List<CreditCategory> = emptyList(),
    )

    private val _uiState = MutableStateFlow(ElectiveCreditUiState())
    val uiState: StateFlow<ElectiveCreditUiState> = _uiState.asStateFlow()

    companion object {
        /**
         * Z到X的转换映射
         * key: Z代码, value: X代码
         */
        private val Z_TO_X_MAP = mapOf(
            "Z1" to "X4",
            "Z2" to "X2",
            "Z3" to "X3",
            "Z4" to "X5",
        )

        /**
         * X类别的名称映射
         */
        private val X_CATEGORY_NAMES = mapOf(
            "X1" to "X1",
            "X2" to "X2艺术鉴赏与审美体验",
            "X3" to "X3生命关怀与健康素养",
            "X4" to "X4文化传承与社会发展",
            "X5" to "X5科学探索与技术创新",
        )

        /**
         * 指导教学课程类别映射
         * key: 课程性质代码, value: Pair(类别代码, 类别名称)
         */
        private val GUIDANCE_CATEGORY_MAP = mapOf(
            "54" to Pair("创新创业选修", "创新创业教育平台专业选修"),
            "61" to Pair("专业选修", "专业教育平台选修"),
        )

        /**
         * 从课程代码前两位提取X/Z类别代码
         *
         * @param courseCode 课程代码
         * @return 类别代码（如"X1"、"Z2"），如果未找到则返回null
         */
        fun extractCategoryCode(courseCode: String): String? {
            if (courseCode.length < 2) return null
            val prefix = courseCode.take(2)
            // 匹配 X1-X5 或 Z1-Z4
            return if (prefix.matches(Regex("[XZ][1-5]"))) prefix else null
        }

        /**
         * 将Z代码转换为X代码
         *
         * @param code Z代码或X代码
         * @return 转换后的X代码，如果无法转换则返回原代码
         */
        fun convertToXCode(code: String): String {
            return Z_TO_X_MAP[code] ?: code
        }
    }

    /**
     * 加载选修课学分数据
     *
     * 获取当前学期往前3年到往后3年的成绩数据，统计X1-X5类别的学分，
     * 同时获取创新创业专业融合选修和专业选修的课程列表
     */
    fun load() {
        if (_uiState.value.loading) return

        _uiState.value = _uiState.value.copy(loading = true, error = "")

        viewModelScope.launch {
            try {
                authWorkflow.ensureLoggedIn()

                // 获取当前学期ID
                val currentSemester = academic.fetchCurrentTermId()
                if (currentSemester.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "无法获取当前学期"
                    )
                    return@launch
                }

                // 获取所有学期ID
                val allSemesterIds = academic.fetchSemesterIds()
                    .filter { it.isNotBlank() }
                    .distinct()

                if (allSemesterIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "暂无学期数据"
                    )
                    return@launch
                }

                // 筛选当前学期往前3年的学期
                val currentKey = parseSemesterKey(currentSemester)
                val semesterIds = allSemesterIds.filter { id ->
                    val key = parseSemesterKey(id)
                    if (key != null && currentKey != null) {
                        val yearDiff = key.first - currentKey.first
                        yearDiff in -3..0
                    } else {
                        false
                    }
                }.sortedWith(SemesterComparator)

                // 获取所有学期的成绩数据
                val allCourses = mutableListOf<GradeCourse>()
                for (semesterId in semesterIds) {
                    try {
                        val report = academic.fetchGradeReport(semester = semesterId)
                        allCourses.addAll(report.achievements)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        AppLogger.log("ElectiveCredit", "获取学期$semesterId 成绩失败", e)
                    }
                }

                // 统计X1-X5学分
                val xCategories = calculateCredits(allCourses)

                // 获取创新创业专业融合选修课程列表
                val innovationCourses = try {
                    academic.fetchGuidanceTeachingCourses(kcxz = "54")
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    AppLogger.log("ElectiveCredit", "获取创新创业专业融合选修课程失败", e)
                    emptyList()
                }

                // 获取专业选修课程列表
                val professionalCourses = try {
                    academic.fetchGuidanceTeachingCourses(kcxz = "61")
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    AppLogger.log("ElectiveCredit", "获取专业选修课程失败", e)
                    emptyList()
                }

                // 将指导教学课程转换为GradeCourse格式并过滤已通过的课程
                val innovationInfo =
                    GUIDANCE_CATEGORY_MAP["54"] ?: Pair("创新创业选修", "创新创业教育平台专业选修")
                val innovationCategory = buildGuidanceCategory(
                    code = innovationInfo.first,
                    name = innovationInfo.second,
                    guidanceCourses = innovationCourses,
                    gradeCourses = allCourses
                )

                val professionalInfo =
                    GUIDANCE_CATEGORY_MAP["61"] ?: Pair("专业选修", "专业教育平台选修")
                val professionalCategory = buildGuidanceCategory(
                    code = professionalInfo.first,
                    name = professionalInfo.second,
                    guidanceCourses = professionalCourses,
                    gradeCourses = allCourses
                )

                // 合并所有类别
                val allCategories = xCategories + listOf(innovationCategory, professionalCategory)

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    categories = allCategories,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.log("ElectiveCredit", "加载选修课学分数据失败", e)
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.toUserFacingMessage(UserFacingErrorKind.LoadGrades)
                )
            }
        }
    }

    /**
     * 计算各类别的学分
     *
     * @param courses 所有课程列表
     * @return 学分类别列表
     */
    private fun calculateCredits(courses: List<GradeCourse>): List<CreditCategory> {
        // 按类别分组，只统计及格的课程
        val categorizedCourses = mutableMapOf<String, MutableList<GradeCourse>>()

        for (course in courses) {
            if (!isCoursePassed(course)) continue
            val code = extractCategoryCode(course.courseCode)
            if (code != null) {
                val xCode = convertToXCode(code)
                categorizedCourses.getOrPut(xCode) { mutableListOf() }.add(course)
            }
        }

        // 构建类别列表，按X1-X5排序
        return listOf("X1", "X2", "X3", "X4", "X5").map { code ->
            val coursesInCategory = categorizedCourses[code] ?: emptyList()
            val credits = coursesInCategory.sumOf { it.credit }
            CreditCategory(
                code = code,
                name = X_CATEGORY_NAMES[code] ?: code,
                credits = credits,
                courses = coursesInCategory,
            )
        }
    }

    /**
     * 判断课程是否及格
     *
     * 及格条件：成绩分数 >= 60 或 通过状态为"合格"
     */
    private fun isCoursePassed(course: GradeCourse): Boolean {
        val score = course.score.toDoubleOrNull()
        if (score != null && score >= 60.0) return true
        if (course.passStatus == "合格") return true
        return false
    }

    /**
     * 构建指导教学课程类别
     *
     * 将指导教学课程与成绩数据匹配，筛选出已通过的课程
     *
     * @param code 类别代码（54或61）
     * @param name 类别名称
     * @param guidanceCourses 指导教学课程列表（来自API）
     * @param gradeCourses 成绩课程列表（用于匹配已通过的课程）
     * @return 学分类别
     */
    private fun buildGuidanceCategory(
        code: String,
        name: String,
        guidanceCourses: List<GuidanceTeachingCourse>,
        gradeCourses: List<GradeCourse>
    ): CreditCategory {
        // 构建课程代码到成绩的映射
        val gradeMap = gradeCourses.associateBy { it.courseCode }

        // 筛选已通过的指导教学课程
        val passedCourses = guidanceCourses.filter { guidance ->
            val grade = gradeMap[guidance.courseCode]
            grade != null && isCoursePassed(grade)
        }

        // 将指导教学课程转换为GradeCourse格式
        val courses = passedCourses.map { guidance ->
            val grade = gradeMap[guidance.courseCode]
            GradeCourse(
                courseCode = guidance.courseCode,
                courseName = guidance.courseName,
                score = grade?.score ?: "",
                gradePoint = grade?.gradePoint ?: 0.0,
                credit = guidance.credit.toDoubleOrNull() ?: 0.0,
                curriculumAttributes = guidance.courseAttribute,
                courseNature = guidance.kclbmc,
                examName = guidance.evaluationMode,
                examinationNature = "",
                passStatus = "及格",
                gradeLevel = "",
                markFlag = "",
                repeatSemester = "",
                gradeId = "",
                semester = guidance.openSemester
            )
        }

        val credits = courses.sumOf { it.credit }
        return CreditCategory(
            code = code,
            name = name,
            credits = credits,
            courses = courses,
        )
    }

    /**
     * 学期比较器
     *
     * 支持"2023-2024-1"格式的学期ID比较
     */
    private object SemesterComparator : Comparator<String> {
        override fun compare(a: String, b: String): Int {
            val ka = parse(a)
            val kb = parse(b)
            return when {
                ka != null && kb != null -> {
                    if (ka.first != kb.first) ka.first.compareTo(kb.first)
                    else if (ka.second != kb.second) ka.second.compareTo(kb.second)
                    else ka.third.compareTo(kb.third)
                }

                ka != null -> 1
                kb != null -> -1
                else -> a.compareTo(b)
            }
        }

        private fun parse(id: String): Triple<Int, Int, Int>? {
            val parts = id.trim().split("-")
            if (parts.size < 3) return null
            val y1 = parts[0].toIntOrNull() ?: return null
            val y2 = parts[1].toIntOrNull() ?: return null
            val t = parts[2].toIntOrNull() ?: return null
            return Triple(y1, y2, t)
        }
    }

    /**
     * 解析学期ID为三元组
     *
     * @param semesterId 学期ID，如"2025-2026-2"
     * @return 三元组(学年1, 学年2, 学期)，解析失败返回null
     */
    private fun parseSemesterKey(semesterId: String): Triple<Int, Int, Int>? {
        val parts = semesterId.trim().split("-")
        if (parts.size < 3) return null
        val y1 = parts[0].toIntOrNull() ?: return null
        val y2 = parts[1].toIntOrNull() ?: return null
        val t = parts[2].toIntOrNull() ?: return null
        return Triple(y1, y2, t)
    }
}
