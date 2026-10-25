package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.domain.policy.buildCourseNames
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveExaminationByIdUseCase
import restarhalf.stellar.schedule.domain.usecase.DeleteExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveExaminationUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher


class ExamEditViewModel(
    private val observeAllCourses: ObserveAllCoursesUseCase,
    private val observeExaminationById: ObserveExaminationByIdUseCase,
    private val auth: JwxtAuthPort,
    private val settings: TermSettingsPort,
    private val academic: AcademicPort,
    private val saveExaminationUseCase: SaveExaminationUseCase,
    private val deleteExaminationUseCase: DeleteExaminationUseCase,
) : ViewModel() {

    
    private val _saving = MutableStateFlow(false)

    
    val isSaving: StateFlow<Boolean> = _saving.asStateFlow()

    
    @Stable
    data class ExamEditUiState(
        val courseNames: ImmutableList<String>,
        val courses: ImmutableList<Course>,
    )

    
    @Immutable
    data class EditingFormState(
        val isEdit: Boolean,
        val selectedIndex: Int,
        val courseNumber: String,
        val courseName: String,
        val datePart: String,
        val timePart: String,
        val examinationPlace: String,
        val zwh: String,
        val ksbz: String,
    )

    private val _uiState: StateFlow<ExamEditUiState> =
        observeAllCourses()
            .map { courses ->
                ExamEditUiState(
                    courseNames = buildCourseNames(courses).toPersistentList(),
                    courses = courses.toPersistentList(),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExamEditUiState(courseNames = persistentListOf(), courses = persistentListOf()),
            )

    
    val uiState: StateFlow<ExamEditUiState> = _uiState

    
    val selectedTerm: StateFlow<String> = settings.observeSelectedTerm()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "",
        )

    
    val currentTerm: StateFlow<String> = settings.observeCurrentTermId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    
    private val userNo: StateFlow<String> = auth.observeProfile()
        .map { it.userNo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "",
        )

    
    private fun findCourseNumber(courses: List<Course>, courseName: String): String {
        val course = courses.firstOrNull { it.name == courseName && it.type == 0 }
        return course?.remoteKey?.ifBlank { null }
            ?: "EXAM_${courseName.hashCode().toUInt()}"
    }

    
    fun observeEditingExamination(examinationId: Long?): Flow<Examination?> {
        return observeExaminationById(examinationId ?: -1)
    }

    
    fun buildEditingFormState(
        examinationId: Long?,
        editingExamination: Examination?,
        courseNames: List<String>,
    ): EditingFormState {
        if (examinationId == null || editingExamination == null) {
            return EditingFormState(
                isEdit = false,
                selectedIndex = 0,
                courseNumber = "",
                courseName = "",
                datePart = "",
                timePart = "",
                examinationPlace = "",
                zwh = "",
                ksbz = ""
            )
        }

        val datePart = editingExamination.time.substringBefore(" ").ifBlank { "" }
        val timePart = editingExamination.time.substringAfter(" ", "").ifBlank { "" }
        val selectedIndex = courseNames.indexOf(editingExamination.courseName).takeIf { it >= 0 } ?: 0

        return EditingFormState(
            isEdit = true,
            selectedIndex = selectedIndex,
            courseNumber = editingExamination.courseNumber,
            courseName = editingExamination.courseName,
            datePart = datePart,
            timePart = timePart,
            examinationPlace = editingExamination.examinationPlace,
            zwh = editingExamination.zwh,
            ksbz = editingExamination.ksbz
        )
    }

    
    fun buildExaminationToSave(
        courseName: String,
        datePart: String,
        timePart: String,
        examinationPlace: String,
        zwh: String,
        ksbz: String,
        courses: List<Course>,
        existing: Examination?,
    ): Examination? {
        if (courseName.isBlank()) return null

        val time = buildString {
            if (datePart.isNotBlank()) append(datePart)
            if (timePart.isNotBlank()) {
                if (isNotEmpty()) append(" ")
                append(timePart)
            }
        }

        val courseNumber = existing?.courseNumber?.takeIf { it.isNotBlank() }
            ?: findCourseNumber(courses, courseName)

        return Examination(
            id = existing?.id ?: 0,
            courseNumber = courseNumber,
            courseName = courseName,
            time = time,
            examinationPlace = examinationPlace,
            zwh = zwh,
            ksbz = ksbz,
            source = existing?.source ?: "manual",
            userNo = existing?.userNo?.takeIf { it.isNotBlank() } ?: userNo.value,
        )
    }

    
    fun saveExamination(examination: Examination, onResult: (Boolean) -> Unit) {
        if (!_saving.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val semesterId = resolveSemesterId()
                val saved = runCatching { withContext(AppIoDispatcher) { saveExaminationUseCase(examination, semesterId) } }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        AppLogger.log("ExamEdit", "保存考试失败", e)
                    }
                    .isSuccess
                onResult(saved)
            } finally {
                _saving.value = false
            }
        }
    }

    
    private suspend fun resolveSemesterId(): String {
        val selected = selectedTerm.value
        if (selected.isNotBlank()) return selected
        return try {
            val termId = academic.fetchCurrentTermId()
            val currentId = currentTerm.value
            termId.ifBlank { currentId }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            currentTerm.value
        }
    }

    
    fun deleteExamination(id: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val deleted = runCatching { withContext(AppIoDispatcher) { deleteExaminationUseCase(id) } }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("ExamEdit", "删除考试失败", e)
                }
                .isSuccess
            onResult(deleted)
        }
    }
}
