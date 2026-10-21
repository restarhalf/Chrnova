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
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.CourseEvaluationPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.port.UserProfilePort


class CourseEvaluationViewModel(
    private val port: CourseEvaluationPort,
    private val courseRepository: CourseRepository,
    private val auth: JwxtAuthPort,
    private val settings: UserProfilePort,
) : ViewModel() {

    @Stable
    data class EvaluationUiState(
        val loading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null,
        val evaluations: ImmutableList<Evaluation> = persistentListOf(),
        val total: Int = 0,
        val searchQuery: String = "",
        
        val selectedCourse: String? = null,
        
        val selectedTeacher: String? = null,
        
        val onlyMine: Boolean = false,
        
        val courseSummaries: ImmutableList<CourseEvaluationSummary> = persistentListOf(),
        val selectedEvaluation: Evaluation? = null,
        
        val myCourses: ImmutableList<Course> = persistentListOf(),
        val submitting: Boolean = false,
        val userNo: String = "",
        val profileName: String = "",
        
        val userNickname: String? = null,
        
        val userHash: String = "",
        
        val filteredCourseSummaries: ImmutableList<CourseEvaluationSummary> = persistentListOf(),
        
        val filteredEvaluations: ImmutableList<Evaluation> = persistentListOf(),
    ) {
        
        fun recomputed(): EvaluationUiState {
            val summaries =
                if (searchQuery.isEmpty()) {
                    courseSummaries
                } else {
                    courseSummaries.filter {
                        it.courseName.contains(searchQuery, ignoreCase = true) ||
                            it.teacher.contains(searchQuery, ignoreCase = true)
                    }.toPersistentList()
                }
            val evals = evaluations
                .let { list ->
                    if (onlyMine && userHash.isNotBlank()) {
                        list.filter { it.userHash == userHash }
                    } else {
                        list
                    }
                }
                .let { list ->
                    if (searchQuery.isEmpty()) {
                        list
                    } else {
                        list.filter {
                            it.courseName.contains(searchQuery, ignoreCase = true) ||
                                it.content.contains(searchQuery, ignoreCase = true) ||
                                it.author.contains(searchQuery, ignoreCase = true)
                        }
                    }
                }
                .let { it as? ImmutableList<Evaluation> ?: it.toPersistentList() }
            return if (filteredCourseSummaries == summaries && filteredEvaluations == evals) {
                this
            } else {
                copy(filteredCourseSummaries = summaries, filteredEvaluations = evals)
            }
        }

        
        val canDeleteSelected: Boolean
            get() = selectedEvaluation != null &&
                userHash.isNotBlank() &&
                selectedEvaluation.userHash == userHash
    }

    private val _uiState = MutableStateFlow(EvaluationUiState())
    val uiState: StateFlow<EvaluationUiState> = _uiState

    private fun updateState(transform: (EvaluationUiState) -> EvaluationUiState) {
        _uiState.update { transform(it).recomputed() }
    }

    init {
        updateState {
            it.copy(
                userNickname = settings.getUserNickname(),
            )
        }
        viewModelScope.launch {
            auth.observeProfile().collect { profile ->
                updateState {
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
                    updateState { it.copy(myCourses = courses.toPersistentList()) }
                }
                .onFailure {
                    if (it is CancellationException) throw it
                    AppLogger.log("Evaluation", "加载已选课程失败", it)
                }
        }
    }

    fun loadEvaluations() {
        viewModelScope.launch {
            updateState { it.copy(loading = true, error = null) }
            runCatching {
                port.listEvaluations(
                    course = _uiState.value.selectedCourse,
                    teacher = _uiState.value.selectedTeacher,
                )
            }.onSuccess { page ->
                updateState {
                    it.copy(
                        evaluations = page.items.toPersistentList(),
                        total = page.total,
                        loading = false,
                    )
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Evaluation", "加载评价列表失败", e)
                updateState { it.copy(loading = false, error = e.message ?: "加载失败") }
            }
        }
    }

    
    fun loadEvaluations(course: String, teacher: String?) {
        updateState { it.copy(selectedCourse = course, selectedTeacher = teacher) }
        loadEvaluations()
    }

    
    fun loadCourseSummaries() {
        viewModelScope.launch {
            updateState { it.copy(loading = true, error = null) }
            runCatching { port.listCourseSummaries() }
                .onSuccess { summaries ->
                    updateState {
                        it.copy(
                            courseSummaries = summaries.toPersistentList(),
                            loading = false,
                        )
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "加载课程聚合列表失败", e)
                    updateState { it.copy(loading = false, error = e.message ?: "加载失败") }
                }
        }
    }

    fun loadDetail(id: String) {
        viewModelScope.launch {
            updateState { it.copy(loading = true, error = null) }
            runCatching { port.getEvaluation(id) }
                .onSuccess { evaluation ->
                    updateState { it.copy(selectedEvaluation = evaluation, loading = false) }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "加载评价详情失败", e)
                    updateState { it.copy(loading = false, error = e.message ?: "加载失败") }
                }
        }
    }

    fun submitEvaluation(req: EvaluationCreateRequest) {
        viewModelScope.launch {
            if (_uiState.value.userNo.isBlank()) {
                updateState { it.copy(error = "请先登录教务系统后再提交评价") }
                return@launch
            }
            updateState { it.copy(submitting = true, error = null) }
            runCatching { port.createEvaluation(req) }
                .onSuccess {
                    updateState { it.copy(submitting = false, successMessage = "提交成功") }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "提交评价失败", e)
                    updateState { it.copy(submitting = false, error = e.message ?: "提交失败") }
                }
        }
    }

    
    fun deleteEvaluation(id: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            if (_uiState.value.userNo.isBlank()) {
                updateState { it.copy(error = "请先登录教务系统后再操作") }
                onResult(false)
                return@launch
            }
            updateState { it.copy(loading = true, error = null) }
            runCatching { port.deleteEvaluation(id) }
                .onSuccess {
                    updateState { it.copy(loading = false, successMessage = "已删除") }
                    loadEvaluations()
                    onResult(true)
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Evaluation", "删除评价失败", e)
                    updateState { it.copy(loading = false, error = e.message ?: "删除失败") }
                    onResult(false)
                }
        }
    }

    fun toggleLike(id: String) {
        viewModelScope.launch {
            if (_uiState.value.userNo.isBlank()) {
                updateState { it.copy(error = "请先登录教务系统后再点赞") }
                return@launch
            }
            runCatching { port.toggleLike(id) }
                .onSuccess { result ->
                    updateState { state ->
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
                    updateState { it.copy(error = e.message ?: "操作失败") }
                }
        }
    }

    fun setOnlyMine(onlyMine: Boolean) {
        updateState { it.copy(onlyMine = onlyMine) }
    }

    
    fun updateEvaluation(id: String, req: EvaluationUpdateRequest) {
        viewModelScope.launch {
            if (_uiState.value.userNo.isBlank()) {
                updateState { it.copy(error = "请先登录教务系统后再编辑评价") }
                return@launch
            }
            updateState { it.copy(submitting = true, error = null) }
            runCatching { port.updateEvaluation(id, req) }
                .onSuccess { updated ->
                    updateState { state ->
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
                    updateState { it.copy(submitting = false, error = e.message ?: "编辑失败") }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        updateState { it.copy(searchQuery = query) }
    }

}
