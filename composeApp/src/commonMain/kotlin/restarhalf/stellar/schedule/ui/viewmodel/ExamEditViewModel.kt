package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.core.course.buildCourseNames
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.usecase.DeleteExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAuthProfileUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveExaminationByIdUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveSelectedTermUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveExaminationUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher

/**
 * 考试编辑ViewModel
 * 
 * 管理考试编辑页面的UI状态，包括：
 * - 考试列表和课程名称
 * - 编辑表单状态
 * - 保存和删除操作
 */
class ExamEditViewModel(
    observeAllCoursesUseCase: ObserveAllCoursesUseCase,
    private val observeExaminationByIdUseCase: ObserveExaminationByIdUseCase,
    private val saveExaminationUseCase: SaveExaminationUseCase,
    private val deleteExaminationUseCase: DeleteExaminationUseCase,
    observeSelectedTerm: ObserveSelectedTermUseCase,
    observeAuthProfile: ObserveAuthProfileUseCase,
) : ViewModel() {

    /**
     * 考试编辑UI状态
     * 
     * @param courseNames 课程名称列表（用于下拉选择，已排除有考试的课程）
     * @param courses 所有课程列表（用于根据名称查找课程编号）
     */
    data class ExamEditUiState(
        val courseNames: List<String>,
        val courses: List<Course>,
    )

    /**
     * 编辑表单状态
     * 
     * @param isEdit 是否为编辑模式（false表示新建）
     * @param selectedIndex 选中的课程名称索引
     * @param courseNumber 课程编号
     * @param courseName 课程名称
     * @param datePart 考试日期
     * @param timePart 考试时间
     * @param examinationPlace 考试地点
     * @param zwh 座位号
     * @param ksbz 备注
     */
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
        observeAllCoursesUseCase()
            .combine(MutableStateFlow(Unit)) { courses, _ ->
                ExamEditUiState(
                    courseNames = buildCourseNames(courses),
                    courses = courses,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExamEditUiState(courseNames = emptyList(), courses = emptyList()),
            )

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<ExamEditUiState> = _uiState

    /** 当前选中的学期ID */
    val selectedTerm: StateFlow<String> = observeSelectedTerm()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    /** 当前登录用户的学号 */
    private val userNo: StateFlow<String> = observeAuthProfile()
        .map { it.userNo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    /**
     * 根据课程名称查找课程编号
     * 
     * @param courses 所有课程列表
     * @param courseName 课程名称
     * @return 课程编号，如果没有找到则返回自动生成的编号
     */
    private fun findCourseNumber(courses: List<Course>, courseName: String): String {
        val course = courses.firstOrNull { it.name == courseName && it.type == 0 }
        return course?.remoteKey?.ifBlank { null }
            ?: "EXAM_${courseName.hashCode().toUInt()}"
    }

    /**
     * 观察正在编辑的考试
     * 
     * @param examinationId 考试ID
     * @return 考试Flow
     */
    fun observeEditingExamination(examinationId: Long?): Flow<Examination?> {
        return observeExaminationByIdUseCase(examinationId ?: -1)
    }

    /**
     * 构建编辑表单状态
     * 
     * @param examinationId 考试ID
     * @param editingExamination 正在编辑的考试
     * @param courseNames 课程名称列表
     * @return 编辑表单状态
     */
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

    /**
     * 构建要保存的考试
     * 
     * @param courseName 课程名称
     * @param datePart 考试日期
     * @param timePart 考试时间
     * @param examinationPlace 考试地点
     * @param zwh 座位号
     * @param ksbz 备注
     * @param courses 所有课程列表
     * @param existing 已存在的考试（编辑时）
     * @return 构建的考试对象，如果参数无效返回null
     */
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

    /**
     * 保存考试
     * 
     * @param examination 考试数据
     * @param onSaved 保存完成回调
     */
    fun saveExamination(examination: Examination, onSaved: () -> Unit) {
        viewModelScope.launch {
            val semesterId = selectedTerm.value.ifBlank {
                examination.time.substringBefore(" ").takeIf { it.isNotBlank() }
                    ?.let { "manual" } ?: ""
            }
            runCatching { withContext(AppIoDispatcher) { saveExaminationUseCase(examination, semesterId) } }
                .onSuccess { onSaved() }
                .onFailure { AppLogger.log("ExamEdit", "保存考试失败", it) }
        }
    }

    /**
     * 删除考试
     * 
     * @param id 考试ID
     * @param onDeleted 删除完成回调
     */
    fun deleteExamination(id: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { withContext(AppIoDispatcher) { deleteExaminationUseCase(id) } }
                .onSuccess { onDeleted() }
                .onFailure { AppLogger.log("ExamEdit", "删除考试失败", it) }
        }
    }
}
