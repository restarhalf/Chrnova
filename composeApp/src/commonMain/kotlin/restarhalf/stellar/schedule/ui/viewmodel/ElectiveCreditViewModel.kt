package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import restarhalf.stellar.schedule.domain.usecase.CalculateElectiveCreditsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase

private fun parseSemesterKey(semesterId: String): Triple<Int, Int, Int>? {
    val parts = semesterId.trim().split("-")
    if (parts.size < 3) return null
    val y1 = parts[0].toIntOrNull() ?: return null
    val y2 = parts[1].toIntOrNull() ?: return null
    val t = parts[2].toIntOrNull() ?: return null
    return Triple(y1, y2, t)
}

/**
 * 选修课学分统计ViewModel
 *
 * 统计X1-X5类别的选修课学分，支持Z到X的转换映射。
 */
class ElectiveCreditViewModel(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
    private val auth: AuthPort,
    private val gradeRepository: GradeRepository,
    private val fetchSemesterIds: FetchSemesterIdsUseCase,
    private val calculateElectiveCredits: CalculateElectiveCreditsUseCase,
) : ViewModel() {

    data class ElectiveCreditUiState(
        val loading: Boolean = false,
        val error: String = "",
        val categories: List<CalculateElectiveCreditsUseCase.CreditCategory> = emptyList(),
    )

    private val _uiState = MutableStateFlow(ElectiveCreditUiState())
    val uiState: StateFlow<ElectiveCreditUiState> = _uiState.asStateFlow()

    fun load() {
        if (_uiState.value.loading) return

        _uiState.value = _uiState.value.copy(loading = true, error = "")

        viewModelScope.launch {
            try {
                authWorkflow.ensureLoggedIn()

                val currentSemester = academic.fetchCurrentTermId()
                if (currentSemester.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "无法获取当前学期"
                    )
                    return@launch
                }

                val allSemesterIds = fetchSemesterIds()
                    .filter { it.isNotBlank() }
                    .distinct()

                if (allSemesterIds.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "暂无学期数据"
                    )
                    return@launch
                }

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

                val allCourses = fetchCoursesWithFallback(semesterIds)

                val innovationCourses = runCatching {
                    academic.fetchGuidanceTeachingCourses(kcxz = "54")
                }.getOrElse { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    AppLogger.log("ElectiveCredit", "获取创新创业专业融合选修课程失败", e)
                    emptyList()
                }

                val professionalCourses = runCatching {
                    academic.fetchGuidanceTeachingCourses(kcxz = "61")
                }.getOrElse { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    AppLogger.log("ElectiveCredit", "获取专业选修课程失败", e)
                    emptyList()
                }

                val categories = calculateElectiveCredits(
                    courses = allCourses,
                    innovationGuidanceCourses = innovationCourses,
                    professionalGuidanceCourses = professionalCourses,
                )

                _uiState.value = _uiState.value.copy(
                    loading = false,
                    categories = categories,
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

    private suspend fun fetchCoursesWithFallback(semesterIds: List<String>): List<GradeCourse> {
        val allCourses = mutableListOf<GradeCourse>()
        for (semesterId in semesterIds) {
            try {
                val report = academic.fetchGradeReport(semester = semesterId)
                allCourses.addAll(report.achievements)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.log("ElectiveCredit", "获取学期$semesterId 成绩失败，尝试本地回退", e)
                try {
                    val userNo = auth.observeProfile().first().userNo
                    if (userNo.isNotBlank()) {
                        val localGrades = gradeRepository.getAllGradesByUserNo(userNo)
                            .filter { it.semester == semesterId }
                        allCourses.addAll(localGrades)
                    }
                } catch (fallback: Exception) {
                    if (fallback is kotlinx.coroutines.CancellationException) throw fallback
                    AppLogger.log("ElectiveCredit", "本地回退也失败", fallback)
                }
            }
        }
        return allCourses
    }

    private object SemesterComparator : Comparator<String> {
        override fun compare(a: String, b: String): Int {
            val ka = parseSemesterKey(a)
            val kb = parseSemesterKey(b)
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
    }
}
