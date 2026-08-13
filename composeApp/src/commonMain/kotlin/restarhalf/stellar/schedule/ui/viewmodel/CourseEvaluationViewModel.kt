package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.CourseEvaluationSummary
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.domain.model.EvaluationCreateRequest
import restarhalf.stellar.schedule.domain.model.EvaluationUpdateRequest
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.port.CourseEvaluationPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.port.SettingsPort

/**
 * 课程评价 ViewModel
 *
 * 负责评价列表/详情的加载、提交、删除与点赞（交互操作）。
 * 提交评价时由客户端限制只能选择“已选课程”（来自 CourseRepository），后端不做此限制。
 */
class CourseEvaluationViewModel(
    private val port: CourseEvaluationPort,
    private val courseRepository: CourseRepository,
    private val auth: AuthPort,
    private val settings: SettingsPort,
) : ViewModel() {

    @Stable
    data class EvaluationUiState(
        val loading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val evaluations: ImmutableList<Evaluation> = persistentListOf(),
        val total: Int = 0,
        val searchQuery: String = "",
        /** 列表页课程筛选（null 表示全部） */
        val selectedCourse: String? = null,
        /** 列表页教师筛选（null 表示全部；"教师未知"匹配空教师） */
        val selectedTeacher: String? = null,
        /** 列表页"仅看我的"筛选（true 只显示当前 user_hash 提交的评价） */
        val onlyMine: Boolean = false,
        /** 课程聚合列表（评价列表页顶层视图，按课程分组） */
        val courseSummaries: ImmutableList<CourseEvaluationSummary> = persistentListOf(),
        val selectedEvaluation: Evaluation? = null,
        /** 当前用户已选课程（用于提交时限制可选课程） */
        val myCourses: ImmutableList<Course> = persistentListOf(),
        val submitting: Boolean = false,
        val userNo: String = "",
        val profileName: String = "",
        /** 用户自定义昵称（优先于 profileName 用作评价署名） */
        val userNickname: String? = null,
        /** 当前登录用户的 user_hash（学号 SHA-256），用于判断评价是否为本机提交 */
        val userHash: String = "",
    ) {
        /** 课程聚合列表按搜索词过滤（仅匹配课程名/教师） */
        val filteredCourseSummaries: List<CourseEvaluationSummary>
            get() = if (searchQuery.isEmpty()) courseSummaries else courseSummaries.filter {
                it.courseName.contains(searchQuery, ignoreCase = true) ||
                    it.teacher.contains(searchQuery, ignoreCase = true)
            }

        val filteredEvaluations: List<Evaluation>
            get() = evaluations
                .let { list ->
                    if (onlyMine && userHash.isNotBlank()) {
                        list.filter { it.userHash == userHash }
                    } else list
                }
                .let { list ->
                    if (searchQuery.isEmpty()) list else list.filter {
                        it.courseName.contains(searchQuery, ignoreCase = true) ||
                            it.content.contains(searchQuery, ignoreCase = true) ||
                            it.author.contains(searchQuery, ignoreCase = true)
                    }
                }

        /** 当前评价是否为本人提交（可删除 / 可编辑） */
        val canDeleteSelected: Boolean
            get() = selectedEvaluation != null &&
                userHash.isNotBlank() &&
                selectedEvaluation.userHash == userHash
    }

    private val _uiState = MutableStateFlow(EvaluationUiState())
    val uiState: StateFlow<EvaluationUiState> = _uiState

    init {
        _uiState.update {
            it.copy(
                userNickname = settings.getUserNickname(),
            )
        }
        viewModelScope.launch {
            auth.observeProfile().collect { profile ->
                _uiState.update {
                    it.copy(
                        userNo = profile.userNo,
                        profileName = profile.name,
                        userHash = if (profile.userNo.isNotBlank()) {
                            CourseEvaluationPort.hashUserNo(profile.userNo)
                        } else "",
                    )
                }
            }
        }
        viewModelScope.launch {
            runCatching { courseRepository.getAllCoursesAcrossSemesters() }
                .onSuccess { courses ->
                    _uiState.update { it.copy(myCourses = courses.toPersistentList()) }
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    AppLogger.log("Evaluation", "加载已选课程失败", it)
                }
        }
    }

    fun loadEvaluations() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                port.listEvaluations(
                    course = _uiState.value.selectedCourse,
                    teacher = _uiState.value.selectedTeacher,
                )
            }.onSuccess { page ->
                _uiState.update {
                    it.copy(
                        evaluations = page.items.toPersistentList(),
                        total = page.total,
                        loading = false,
                    )
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Evaluation", "加载评价列表失败", e)
                _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    /** 按课程 + 教师加载评价列表（用于 EvaluationCourseScreen 第二层页面） */
    fun loadEvaluations(course: String, teacher: String?) {
        _uiState.update { it.copy(selectedCourse = course, selectedTeacher = teacher) }
        loadEvaluations()
    }

    /** 加载课程聚合列表（评价列表页顶层视图） */
    fun loadCourseSummaries() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { port.listCourseSummaries() }
                .onSuccess { summaries ->
                    _uiState.update {
                        it.copy(
                            courseSummaries = summaries.toPersistentList(),
                            loading = false,
                        )
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "加载课程聚合列表失败", e)
                    _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
                }
        }
    }

    fun loadDetail(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { port.getEvaluation(id) }
                .onSuccess { evaluation ->
                    _uiState.update { it.copy(selectedEvaluation = evaluation, loading = false) }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "加载评价详情失败", e)
                    _uiState.update { it.copy(loading = false, error = e.message ?: "加载失败") }
                }
        }
    }

    fun submitEvaluation(req: EvaluationCreateRequest) {
        viewModelScope.launch {
            if (_uiState.value.userNo.isBlank()) {
                _uiState.update { it.copy(error = "请先登录教务系统后再提交评价") }
                return@launch
            }
            _uiState.update { it.copy(submitting = true, error = null) }
            runCatching { port.createEvaluation(req) }
                .onSuccess {
                    _uiState.update { it.copy(submitting = false, successMessage = "提交成功") }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "提交评价失败", e)
                    _uiState.update { it.copy(submitting = false, error = e.message ?: "提交失败") }
                }
        }
    }

    fun deleteEvaluation(id: String) {
        viewModelScope.launch {
            if (_uiState.value.userNo.isBlank()) {
                _uiState.update { it.copy(error = "请先登录教务系统后再操作") }
                return@launch
            }
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { port.deleteEvaluation(id) }
                .onSuccess {
                    _uiState.update { it.copy(loading = false, successMessage = "已删除") }
                    loadEvaluations()
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "删除评价失败", e)
                    _uiState.update { it.copy(loading = false, error = e.message ?: "删除失败") }
                }
        }
    }

    fun toggleLike(id: String) {
        viewModelScope.launch {
            if (_uiState.value.userNo.isBlank()) {
                _uiState.update { it.copy(error = "请先登录教务系统后再点赞") }
                return@launch
            }
            runCatching { port.toggleLike(id) }
                .onSuccess { result ->
                    _uiState.update { state ->
                        val evaluations = state.evaluations.map { ev ->
                            if (ev.id == id) ev.copy(likes = result.likes, liked = result.liked) else ev
                        }.toPersistentList()
                        val selected = if (state.selectedEvaluation?.id == id) {
                            state.selectedEvaluation.copy(likes = result.likes, liked = result.liked)
                        } else {
                            state.selectedEvaluation
                        }
                        state.copy(evaluations = evaluations, selectedEvaluation = selected)
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "点赞操作失败", e)
                    _uiState.update { it.copy(error = e.message ?: "操作失败") }
                }
        }
    }

    fun setOnlyMine(onlyMine: Boolean) {
        _uiState.update { it.copy(onlyMine = onlyMine) }
    }

    /**
     * 编辑本人提交的评价（需登录）。course_name 不可改。
     * 编辑成功后会同步刷新列表与详情页中对应条目。
     */
    fun updateEvaluation(id: String, req: EvaluationUpdateRequest) {
        viewModelScope.launch {
            if (_uiState.value.userNo.isBlank()) {
                _uiState.update { it.copy(error = "请先登录教务系统后再编辑评价") }
                return@launch
            }
            _uiState.update { it.copy(submitting = true, error = null) }
            runCatching { port.updateEvaluation(id, req) }
                .onSuccess { updated ->
                    _uiState.update { state ->
                        val evaluations = state.evaluations.map { ev ->
                            if (ev.id == id) updated else ev
                        }.toPersistentList()
                        val selected = if (state.selectedEvaluation?.id == id) updated else state.selectedEvaluation
                        state.copy(
                            evaluations = evaluations,
                            selectedEvaluation = selected,
                            submitting = false,
                            successMessage = "编辑成功",
                        )
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "编辑评价失败", e)
                    _uiState.update { it.copy(submitting = false, error = e.message ?: "编辑失败") }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

}
