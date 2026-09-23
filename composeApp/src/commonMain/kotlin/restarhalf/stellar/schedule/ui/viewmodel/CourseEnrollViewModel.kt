package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.JwxtSelectedCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionClassification
import restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionOperResult
import restarhalf.stellar.schedule.data.remote.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import kotlin.time.Duration.Companion.milliseconds

/**
 * 手动选课 ViewModel
 *
 * 流程：选轮次 → 选分类 → 浏览/搜索课程 → 提交选课或退课。
 * 不包含抢课循环、目标优先级与后台服务。
 */
class CourseEnrollViewModel(
    private val useCase: CourseSelectionUseCase,
) : ViewModel() {

    @Stable
    data class UiState(
        val loading: Boolean = false,
        val loadingSelected: Boolean = false,
        val submitting: Boolean = false,
        /** 非致命提示（成功/失败结果），展示在状态条 */
        val notice: String = "",
        val error: String = "",
        val rotations: ImmutableList<JwxtSelectionRotation> = persistentListOf(),
        val selectedRotationId: String = "",
        val classifications: ImmutableList<JwxtSelectionClassification> = persistentListOf(),
        val selectedClassificationCode: String = "",
        val courses: ImmutableList<JwxtSelectionCourse> = persistentListOf(),
        val searchQuery: String = "",
        val sessionReady: Boolean = false,
        val selectedCourses: ImmutableList<JwxtSelectedCourse> = persistentListOf(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var session: CourseSelectionUseCase.SessionContext? = null
    private var searchJob: Job? = null

    fun loadRotations() {
        if (_uiState.value.loading) return
        _uiState.update { it.copy(loading = true, error = "", notice = "") }
        viewModelScope.launch {
            try {
                val rotations = useCase.loadRotations()
                _uiState.update {
                    it.copy(loading = false, rotations = rotations.toPersistentList())
                }
                if (rotations.isNotEmpty() && _uiState.value.selectedRotationId.isBlank()) {
                    selectRotation(rotations.first().rotationId)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseEnroll", "加载选课轮次失败", e)
                _uiState.update {
                    it.copy(loading = false, error = e.message ?: "加载失败")
                }
            }
        }
    }

    fun selectRotation(rotationId: String) {
        if (_uiState.value.loading) return
        _uiState.update {
            it.copy(
                selectedRotationId = rotationId,
                classifications = persistentListOf(),
                selectedClassificationCode = "",
                courses = persistentListOf(),
                selectedCourses = persistentListOf(),
                sessionReady = false,
                error = "",
                notice = "",
            )
        }
        viewModelScope.launch {
            try {
                val ctx = useCase.initSession(rotationId)
                session = ctx
                _uiState.update {
                    it.copy(
                        classifications = ctx.classifications.toPersistentList(),
                        sessionReady = true,
                    )
                }
                if (ctx.classifications.isNotEmpty() && _uiState.value.selectedClassificationCode.isBlank()) {
                    selectClassification(ctx.classifications.first().classificationCode)
                }
                loadSelectedCourses()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseEnroll", "初始化选课会话失败", e)
                _uiState.update { it.copy(error = e.message ?: "进入选课失败") }
            }
        }
    }

    fun selectClassification(code: String) {
        val ctx = session ?: return
        _uiState.update {
            it.copy(selectedClassificationCode = code, courses = persistentListOf(), error = "", notice = "")
        }
        launchCourseLoad(ctx, code)
    }

    fun onSearchQueryChange(query: String) {
        val ctx = session ?: return
        val code = _uiState.value.selectedClassificationCode.ifBlank { return }
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS.milliseconds)
            launchCourseLoad(ctx, code)
        }
    }

    fun clearSearch() {
        val ctx = session ?: return
        val code = _uiState.value.selectedClassificationCode.ifBlank { return }
        _uiState.update { it.copy(searchQuery = "") }
        launchCourseLoad(ctx, code)
    }

    private fun launchCourseLoad(ctx: CourseSelectionUseCase.SessionContext, code: String) {
        val courseInfo = _uiState.value.searchQuery.trim()
        _uiState.update { it.copy(loading = true, error = "") }
        viewModelScope.launch {
            try {
                val courses = useCase.loadCourses(
                    ctx = ctx,
                    classificationCode = code,
                    courseInformation = courseInfo,
                )
                _uiState.update {
                    it.copy(loading = false, courses = courses.toPersistentList())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseEnroll", "加载课程列表失败", e)
                _uiState.update {
                    it.copy(loading = false, error = e.message ?: "加载课程列表失败")
                }
            }
        }
    }

    /** 提交一次选课（成功后刷新已选列表） */
    fun selectCourse(course: JwxtSelectionCourse) {
        val classificationCode = _uiState.value.selectedClassificationCode
        if (classificationCode.isBlank()) {
            _uiState.update { it.copy(error = "请先选择课程分类") }
            return
        }
        val ctx = session ?: run {
            _uiState.update { it.copy(error = "请先进入选课轮次") }
            return
        }
        if (_uiState.value.submitting) return

        _uiState.update { it.copy(submitting = true, error = "", notice = "") }
        viewModelScope.launch {
            try {
                when (val result = useCase.submitOnce(ctx, classificationCode, course)) {
                    is JwxtSelectionOperResult.Success -> {
                        val extra = if (result.relatedCourses.isNotEmpty()) {
                            "，已连带选上关联课程"
                        } else ""
                        _uiState.update {
                            it.copy(
                                submitting = false,
                                notice = "选课成功：${course.courseName}$extra · ${result.message}",
                            )
                        }
                        loadSelectedCourses()
                    }
                    is JwxtSelectionOperResult.NeedConfirm -> {
                        _uiState.update {
                            it.copy(
                                submitting = false,
                                error = result.message.ifBlank { "需要确认关联教学班，未能自动完成选课" },
                            )
                        }
                    }
                    is JwxtSelectionOperResult.Fail -> {
                        _uiState.update {
                            it.copy(submitting = false, error = result.message.ifBlank { "选课失败" })
                        }
                    }
                    is JwxtSelectionOperResult.Unknown -> {
                        _uiState.update {
                            it.copy(
                                submitting = false,
                                error = result.message.ifBlank { "未知响应：${result.errorCode}" },
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseEnroll", "选课失败", e)
                _uiState.update {
                    it.copy(submitting = false, error = e.message ?: "选课请求失败")
                }
            }
        }
    }

    fun loadSelectedCourses() {
        val ctx = session ?: return
        if (_uiState.value.loadingSelected) return
        _uiState.update { it.copy(loadingSelected = true) }
        viewModelScope.launch {
            try {
                val list = useCase.loadSelectedCourses(ctx)
                _uiState.update {
                    it.copy(selectedCourses = list.toPersistentList(), loadingSelected = false)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseEnroll", "加载已选课程失败", e)
                _uiState.update { it.copy(loadingSelected = false) }
            }
        }
    }

    /** 退课（成功后刷新已选列表） */
    fun dropCourse(course: JwxtSelectedCourse) {
        val ctx = session ?: return
        if (_uiState.value.submitting) return
        _uiState.update { it.copy(submitting = true, error = "", notice = "") }
        viewModelScope.launch {
            try {
                val resp = useCase.drop(ctx, course.noticeId)
                if (resp.isSuccess()) {
                    _uiState.update {
                        it.copy(
                            submitting = false,
                            notice = "退课成功：${course.courseName}",
                            selectedCourses = it.selectedCourses
                                .filterNot { c -> c.noticeId == course.noticeId }
                                .toPersistentList(),
                        )
                    }
                    val code = _uiState.value.selectedClassificationCode
                    if (code.isNotBlank()) {
                        val loaded = session
                        if (loaded != null) launchCourseLoad(loaded, code)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            submitting = false,
                            error = resp.resolvedMessage().ifBlank { "退课失败" },
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseEnroll", "退课失败", e)
                _uiState.update {
                    it.copy(submitting = false, error = e.message ?: "退课请求失败")
                }
            }
        }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = "") }
    }

    fun clearError() {
        _uiState.update { it.copy(error = "") }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
