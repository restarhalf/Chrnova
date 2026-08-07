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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.JwxtSelectedCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionClassification
import restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionOperResult
import restarhalf.stellar.schedule.data.remote.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.port.CourseSelectionServicePort
import restarhalf.stellar.schedule.domain.port.ServiceLogEntry
import restarhalf.stellar.schedule.domain.port.SnatchServiceConfig
import restarhalf.stellar.schedule.domain.port.SnatchTarget
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * 自动抢课 ViewModel
 *
 * 管理抢课全流程状态：选轮次 → 选分类 → 浏览课程 → 加入抢课目标 → 启动循环抢课。
 *
 * 抢课循环策略：
 * - 按目标列表顺序依次尝试选课
 * - 选课失败时等待 [SnatchConfig.intervalMs] 后重试
 * - 选课成功后该目标标记为已完成，不再重试
 * - 所有目标完成或被用户停止时结束循环
 * - token 过期会自动刷新会话
 */
class CourseSelectionViewModel(
    private val useCase: CourseSelectionUseCase,
    private val servicePort: CourseSelectionServicePort,
) : ViewModel() {

    /** 抢课目标，按优先级排序（列表前面的优先） */
    @Stable
    data class SelectionTarget(
        val course: JwxtSelectionCourse,
        val classificationCode: String,
        val succeeded: Boolean = false,
        val lastMessage: String = "",
        val attempts: Int = 0,
    ) {
        val key: String get() = "${course.courseId}|${course.noticeId}|${course.kxh}"
    }

    /** 抢课日志条目 */
    @Stable
    data class SelectionLog(
        val time: String,
        val message: String,
        val level: LogLevel = LogLevel.INFO,
    ) {
        enum class LogLevel { INFO, SUCCESS, WARN, ERROR }
    }

    /** 抢课配置 */
    @Stable
    data class SnatchConfig(
        val intervalMs: Long = 800L,
        val maxAttempts: Int = 0,  // 0 = 无限
        val refreshSessionOnAuthError: Boolean = true,
    )

    @Stable
    data class CourseSelectionUiState(
        val loading: Boolean = false,
        val loadingMessage: String = "",
        val error: String = "",
        val rotations: ImmutableList<JwxtSelectionRotation> = persistentListOf(),
        val selectedRotationId: String = "",
        val classifications: ImmutableList<JwxtSelectionClassification> = persistentListOf(),
        val selectedClassificationCode: String = "",
        val courses: ImmutableList<JwxtSelectionCourse> = persistentListOf(),
        val targets: ImmutableList<SelectionTarget> = persistentListOf(),
        val logs: ImmutableList<SelectionLog> = persistentListOf(),
        val snatching: Boolean = false,
        val snatchConfig: SnatchConfig = SnatchConfig(),
        val sessionReady: Boolean = false,
        /** 课程名称搜索关键词（防抖后用于实际请求） */
        val courseSearchQuery: String = "",
        /** 当前平台是否支持后台抢课 */
        val backgroundSupported: Boolean = false,
        /** 后台抢课是否运行中 */
        val backgroundRunning: Boolean = false,
        /** 已选课程列表（用于退课，加载 wxgetYxkcList） */
        val selectedCourses: ImmutableList<JwxtSelectedCourse> = persistentListOf(),
        /** 已选课程是否正在加载 */
        val loadingSelected: Boolean = false,
        /** 点击「加入」时正在试探选课请求 */
        val checkingTarget: Boolean = false,
    )

    private val _uiState = MutableStateFlow(
        CourseSelectionUiState(backgroundSupported = servicePort.isSupported)
    )
    val uiState: StateFlow<CourseSelectionUiState> = _uiState.asStateFlow()

    init {
        // 收集后台服务运行状态
        viewModelScope.launch {
            servicePort.running.collect { running ->
                _uiState.update { it.copy(backgroundRunning = running, snatching = running) }
            }
        }
        // 收集后台服务日志，追加到 UI 日志列表
        viewModelScope.launch {
            servicePort.latestLog.collect { entry ->
                if (entry.message.isBlank()) return@collect
                val level = when (entry.level) {
                    ServiceLogEntry.LEVEL_SUCCESS -> SelectionLog.LogLevel.SUCCESS
                    ServiceLogEntry.LEVEL_WARN -> SelectionLog.LogLevel.WARN
                    ServiceLogEntry.LEVEL_ERROR -> SelectionLog.LogLevel.ERROR
                    else -> SelectionLog.LogLevel.INFO
                }
                appendLog(SelectionLog("", entry.message, level))
            }
        }
    }

    /** 当前会话上下文，不属于 UI 状态（不参与 recomposition） */
    private var session: CourseSelectionUseCase.SessionContext? = null

    /** 抢课循环 Job，可取消 */
    private var snatchJob: Job? = null

    /** 搜索防抖 Job，可取消 */
    private var searchJob: Job? = null

    /** 加载选课轮次列表 */
    fun loadRotations() {
        if (_uiState.value.loading) return
        _uiState.update { it.copy(loading = true, loadingMessage = "正在加载选课轮次...", error = "") }
        viewModelScope.launch {
            try {
                val rotations = useCase.loadRotations()
                _uiState.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                        rotations = rotations.toPersistentList(),
                    )
                }
                if (rotations.isNotEmpty() && _uiState.value.selectedRotationId.isBlank()) {
                    selectRotation(rotations.first().rotationId)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseSelection", "加载选课轮次失败", e)
                _uiState.update {
                    it.copy(loading = false, loadingMessage = "", error = e.message ?: "加载失败")
                }
            }
        }
    }

    /** 选择选课轮次，自动初始化会话 */
    fun selectRotation(rotationId: String) {
        if (_uiState.value.loading) return
        _uiState.update {
            it.copy(
                selectedRotationId = rotationId,
                classifications = persistentListOf(),
                selectedClassificationCode = "",
                courses = persistentListOf(),
                sessionReady = false,
                error = "",
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
                // 会话就绪后自动加载已选课程列表
                loadSelectedCourses()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseSelection", "初始化选课会话失败", e)
                _uiState.update { it.copy(error = e.message ?: "进入选课失败") }
            }
        }
    }

    /** 选择分类，自动加载课程列表（带当前搜索关键词） */
    fun selectClassification(code: String) {
        val ctx = session ?: return
        if (_uiState.value.loading) return
        _uiState.update {
            it.copy(selectedClassificationCode = code, courses = persistentListOf(), error = "")
        }
        launchCourseLoad(ctx, code)
    }

    /**
     * 更新课程名称搜索关键词，防抖后重新加载课程列表。
     * 传空串表示清除筛选。
     */
    fun onCourseSearchQueryChange(query: String) {
        val ctx = session ?: return
        val code = _uiState.value.selectedClassificationCode.ifBlank { return }
        _uiState.update { it.copy(courseSearchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS.milliseconds)
            launchCourseLoad(ctx, code)
        }
    }

    /** 清空搜索条件并重新加载 */
    fun clearSearch() {
        val ctx = session ?: return
        val code = _uiState.value.selectedClassificationCode.ifBlank { return }
        _uiState.update { it.copy(courseSearchQuery = "") }
        launchCourseLoad(ctx, code)
    }

    /** 实际发起课程列表请求，带上当前搜索关键词 */
    private fun launchCourseLoad(ctx: CourseSelectionUseCase.SessionContext, code: String) {
        val courseInfo = _uiState.value.courseSearchQuery.trim()
        _uiState.update { it.copy(loading = true, loadingMessage = "正在加载课程列表...", error = "") }
        viewModelScope.launch {
            try {
                val courses = useCase.loadCourses(
                    ctx = ctx,
                    classificationCode = code,
                    courseInformation = courseInfo,
                )
                _uiState.update {
                    it.copy(loading = false, loadingMessage = "", courses = courses.toPersistentList())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseSelection", "加载课程列表失败", e)
                _uiState.update {
                    it.copy(loading = false, loadingMessage = "", error = e.message ?: "加载课程列表失败")
                }
            }
        }
    }

    /**
     * 点击「加入」时先请求一次选课接口试探：
     * - 成功：直接加入目标并标记为已成功（实际已选上）
     * - 失败但属于「容量已满」类：加入目标继续抢
     * - 失败且为其他原因：不加入，error 字段返回原因
     */
    fun addTargetWithCheck(course: JwxtSelectionCourse) {
        val classificationCode = _uiState.value.selectedClassificationCode
        if (classificationCode.isBlank()) {
            _uiState.update { it.copy(error = "请先选择选课分类") }
            return
        }
        val ctx = session ?: run {
            _uiState.update { it.copy(error = "请先进入选课轮次") }
            return
        }
        val key = "${course.courseId}|${course.noticeId}|${course.kxh}"
        if (_uiState.value.targets.any { it.key == key }) return
        if (_uiState.value.checkingTarget) return

        _uiState.update { it.copy(checkingTarget = true, error = "") }
        viewModelScope.launch {
            try {
                when (val result = useCase.submitOnce(ctx, classificationCode, course)) {
                    is JwxtSelectionOperResult.Success -> {
                        appendLog(SelectionLog(now(), "加入时已选上：${course.courseName} - ${result.message}", SelectionLog.LogLevel.SUCCESS))
                        _uiState.update { state ->
                            state.copy(
                                checkingTarget = false,
                                targets = (state.targets + SelectionTarget(
                                    course = course,
                                    classificationCode = classificationCode,
                                    succeeded = true,
                                    lastMessage = result.message,
                                )).toPersistentList(),
                            )
                        }
                    }

                    is JwxtSelectionOperResult.NeedConfirm -> {
                        // 关联班未自动选上，按失败处理
                        handleAddCheckFailure(course, classificationCode, result.message.ifBlank { "需要确认关联教学班" })
                    }

                    is JwxtSelectionOperResult.Fail -> {
                        handleAddCheckFailure(course, classificationCode, result.message)
                    }

                    is JwxtSelectionOperResult.Unknown -> {
                        handleAddCheckFailure(course, classificationCode, result.message.ifBlank { "未知响应：${result.errorCode}" })
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseSelection", "加入目标试探失败", e)
                handleAddCheckFailure(course, classificationCode, e.message ?: "请求异常")
            }
        }
    }

    /** 试探失败后判断是否可加入目标：容量满则加入，其他原因不加入 */
    private fun handleAddCheckFailure(
        course: JwxtSelectionCourse,
        classificationCode: String,
        message: String,
    ) {
        if (isCapacityFullMessage(message)) {
            appendLog(SelectionLog(now(), "加入目标（容量已满）：${course.courseName} - $message", SelectionLog.LogLevel.WARN))
            _uiState.update { state ->
                val key = "${course.courseId}|${course.noticeId}|${course.kxh}"
                if (state.targets.any { it.key == key }) {
                    state.copy(checkingTarget = false)
                } else {
                    state.copy(
                        checkingTarget = false,
                        targets = (state.targets + SelectionTarget(
                            course = course,
                            classificationCode = classificationCode,
                            lastMessage = message,
                        )).toPersistentList(),
                    )
                }
            }
        } else {
            _uiState.update { it.copy(checkingTarget = false, error = message) }
        }
    }

    /** 判断是否为「课程容量已满」类错误，此类错误允许加入抢课目标 */
    private fun isCapacityFullMessage(message: String): Boolean {
        val lower = message.lowercase()
        return message.contains("满") ||
            message.contains("容量") ||
            message.contains("人数") ||
            lower.contains("full") ||
            lower.contains("capacity")
    }

    /** 移除抢课目标 */
    fun removeTarget(target: SelectionTarget) {
        _uiState.update { state ->
            state.copy(targets = state.targets.filterNot { it.key == target.key }.toPersistentList())
        }
    }

    /** 上移目标（提升优先级） */
    fun moveTargetUp(index: Int) {
        if (index <= 0) return
        _uiState.update { state ->
            val list = state.targets.toMutableList()
            if (index >= list.size) return@update state
            val tmp = list[index - 1]
            list[index - 1] = list[index]
            list[index] = tmp
            state.copy(targets = list.toPersistentList())
        }
    }

    /** 下移目标（降低优先级） */
    fun moveTargetDown(index: Int) {
        _uiState.update { state ->
            val list = state.targets.toMutableList()
            if (index < 0 || index >= list.size - 1) return@update state
            val tmp = list[index + 1]
            list[index + 1] = list[index]
            list[index] = tmp
            state.copy(targets = list.toPersistentList())
        }
    }

    /** 清空日志 */
    fun clearLogs() {
        _uiState.update { it.copy(logs = persistentListOf()) }
    }

    /** 更新抢课配置 */
    fun updateSnatchConfig(config: SnatchConfig) {
        _uiState.update { it.copy(snatchConfig = config) }
    }

    /** 启动抢课循环 */
    fun startSnatch() {
        if (_uiState.value.snatching) return
        val targets = _uiState.value.targets
        if (targets.isEmpty()) {
            _uiState.update { it.copy(error = "请先添加抢课目标") }
            return
        }
        val ctx = session ?: run {
            _uiState.update { it.copy(error = "请先进入选课轮次") }
            return
        }
        _uiState.update { it.copy(snatching = true, error = "") }
        appendLog(SelectionLog(now(), "开始抢课，目标数：${targets.size}", SelectionLog.LogLevel.INFO))
        snatchJob = viewModelScope.launch {
            runSnatchLoop(ctx)
        }
    }

    /**
     * 启动后台抢课（仅 Android 支持）。
     * @return true 启动成功；false 平台不支持或参数无效
     */
    fun startBackgroundSnatch(): Boolean {
        if (!servicePort.isSupported) {
            _uiState.update { it.copy(error = "当前平台不支持后台抢课") }
            return false
        }
        if (_uiState.value.backgroundRunning) return true
        val targets = _uiState.value.targets
        if (targets.isEmpty()) {
            _uiState.update { it.copy(error = "请先添加抢课目标") }
            return false
        }
        val ctx = session ?: run {
            _uiState.update { it.copy(error = "请先进入选课轮次") }
            return false
        }
        val config = SnatchServiceConfig(
            rotationId = ctx.rotationId,
            sessionTime = ctx.sessionTime,
            extraRules = ctx.extraRules,
            targets = targets.map { SnatchTarget.from(it.course, it.classificationCode) },
            intervalMs = _uiState.value.snatchConfig.intervalMs,
            maxAttempts = _uiState.value.snatchConfig.maxAttempts,
            refreshSessionOnAuthError = _uiState.value.snatchConfig.refreshSessionOnAuthError,
        )
        val ok = servicePort.start(config)
        if (!ok) {
            _uiState.update { it.copy(error = "启动后台服务失败，请检查通知权限或后台运行限制") }
        }
        return ok
    }

    /** 停止后台抢课 */
    fun stopBackgroundSnatch() {
        if (!servicePort.isSupported) return
        servicePort.stop()
    }

    /** 停止抢课循环 */
    fun stopSnatch() {
        // 后台模式下走后台停止
        if (_uiState.value.backgroundRunning) {
            stopBackgroundSnatch()
            return
        }
        snatchJob?.cancel()
        snatchJob = null
        _uiState.update { it.copy(snatching = false) }
        appendLog(SelectionLog(now(), "已停止抢课", SelectionLog.LogLevel.WARN))
    }

    /** 加载已选课程列表（用于退课区域展示） */
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
                AppLogger.log("CourseSelection", "加载已选课程失败", e)
                _uiState.update { it.copy(loadingSelected = false) }
            }
        }
    }

    /** 退已选课程列表中的某门课，退课后刷新列表 */
    fun dropSelectedCourse(course: JwxtSelectedCourse) {
        val ctx = session ?: return
        viewModelScope.launch {
            try {
                val resp = useCase.drop(ctx, course.noticeId)
                if (resp.isSuccess()) {
                    appendLog(SelectionLog(now(), "退课成功：${course.courseName}", SelectionLog.LogLevel.SUCCESS))
                    // 从已选列表移除
                    _uiState.update {
                        it.copy(
                            selectedCourses = it.selectedCourses
                                .filterNot { c -> c.noticeId == course.noticeId }
                                .toPersistentList(),
                        )
                    }
                } else {
                    appendLog(SelectionLog(now(), "退课失败：${course.courseName} - ${resp.resolvedMessage()}", SelectionLog.LogLevel.ERROR))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                AppLogger.log("CourseSelection", "退课失败", e)
                appendLog(SelectionLog(now(), "退课异常：${course.courseName} - ${e.message}", SelectionLog.LogLevel.ERROR))
            }
        }
    }

    /** 抢课主循环 */
    private suspend fun runSnatchLoop(initialCtx: CourseSelectionUseCase.SessionContext) {
        var ctx = initialCtx
        val config = _uiState.value.snatchConfig
        var attempt = 0

        while (viewModelScope.isActive) {
            attempt++
            if (config.maxAttempts in 1..<attempt) {
                appendLog(SelectionLog(now(), "已达最大尝试次数 ${config.maxAttempts}，停止", SelectionLog.LogLevel.WARN))
                break
            }

            // 检查是否所有目标都已成功
            val pending = _uiState.value.targets.filterNot { it.succeeded }
            if (pending.isEmpty()) {
                appendLog(SelectionLog(now(), "所有目标已选课成功", SelectionLog.LogLevel.SUCCESS))
                break
            }

            for (target in pending) {
                if (!viewModelScope.isActive) return
                try {
                    val result = useCase.submitOnce(ctx, target.classificationCode, target.course)
                    handleSnatchResult(target, result, attempt)
                    // 成功后更新目标状态
                    if (result is JwxtSelectionOperResult.Success) {
                        markTargetSucceeded(target.key)
                    } else if (result is JwxtSelectionOperResult.NeedConfirm) {
                        // needcf 后续若仍不是 success，也标记一次消息
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    AppLogger.log("CourseSelection", "抢课请求异常", e)
                    val msg = e.message ?: "请求异常"
                    updateTargetMessage(target.key, msg)
                    appendLog(SelectionLog(now(), "${target.course.courseName}：$msg", SelectionLog.LogLevel.ERROR))

                    // 认证失败时刷新会话
                    if (config.refreshSessionOnAuthError && isAuthError(e)) {
                        appendLog(SelectionLog(now(), "检测到认证失效，刷新会话...", SelectionLog.LogLevel.WARN))
                        ctx = try {
                            useCase.refreshSession(ctx.rotationId)
                        } catch (refreshErr: Exception) {
                            if (refreshErr is CancellationException) throw refreshErr
                            AppLogger.log("CourseSelection", "刷新会话失败", refreshErr)
                            appendLog(
                                SelectionLog(now(), "刷新会话失败：${refreshErr.message}", SelectionLog.LogLevel.ERROR),
                            )
                            break
                        }
                        session = ctx
                    }
                }
            }
            // 等待间隔（可被取消）
            try {
                delay(config.intervalMs.milliseconds)
            } catch (e: CancellationException) {
                throw e
            }
        }

        _uiState.update { it.copy(snatching = false) }
        appendLog(SelectionLog(now(), "抢课循环结束（共 $attempt 次）", SelectionLog.LogLevel.INFO))
    }

    /** 处理单次选课结果，记录日志 */
    private fun handleSnatchResult(
        target: SelectionTarget,
        result: JwxtSelectionOperResult,
        attempt: Int,
    ) {
        val name = target.course.courseName
        val teacher = target.course.classTeacher
        val kxh = target.course.kxh
        val prefix = "[$attempt] $name（$teacher 班$kxh）"
        when (result) {
            is JwxtSelectionOperResult.Success -> {
                val extra = if (result.relatedCourses.isNotEmpty()) {
                    "，关联：${result.relatedCourses.joinToString { it.courseName }}"
                } else ""
                appendLog(
                    SelectionLog(now(), "$prefix ${result.message}$extra", SelectionLog.LogLevel.SUCCESS),
                )
                updateTargetMessage(target.key, result.message)
            }

            is JwxtSelectionOperResult.NeedConfirm -> {
                appendLog(
                    SelectionLog(now(), "$prefix ${result.message}（已尝试自动选关联班）", SelectionLog.LogLevel.WARN),
                )
                updateTargetMessage(target.key, result.message)
            }

            is JwxtSelectionOperResult.Fail -> {
                appendLog(SelectionLog(now(), "$prefix ${result.message}", SelectionLog.LogLevel.ERROR))
                updateTargetMessage(target.key, result.message)
            }

            is JwxtSelectionOperResult.Unknown -> {
                appendLog(
                    SelectionLog(now(), "$prefix 未知响应：${result.errorCode} - ${result.message}", SelectionLog.LogLevel.WARN),
                )
                updateTargetMessage(target.key, "未知响应：${result.message}")
            }
        }
        incrementAttempts(target.key)
    }

    private fun markTargetSucceeded(key: String) {
        _uiState.update { state ->
            state.copy(
                targets = state.targets.map {
                    if (it.key == key) it.copy(succeeded = true) else it
                }.toPersistentList(),
            )
        }
    }

    private fun updateTargetMessage(key: String, message: String) {
        _uiState.update { state ->
            state.copy(
                targets = state.targets.map {
                    if (it.key == key) it.copy(lastMessage = message) else it
                }.toPersistentList(),
            )
        }
    }

    private fun incrementAttempts(key: String) {
        _uiState.update { state ->
            state.copy(
                targets = state.targets.map {
                    if (it.key == key) it.copy(attempts = it.attempts + 1) else it
                }.toPersistentList(),
            )
        }
    }

    private fun appendLog(log: SelectionLog) {
        _uiState.update { state ->
            val newLogs = (state.logs + log).takeLast(MAX_LOG_SIZE)
            state.copy(logs = newLogs.toPersistentList())
        }
    }

    /** 简单判断是否为认证类错误（token 过期等） */
    private fun isAuthError(e: Throwable): Boolean {
        val msg = e.message.orEmpty()
        return msg.contains("token", ignoreCase = true) ||
            msg.contains("登录", ignoreCase = true) ||
            msg.contains("认证", ignoreCase = true) ||
            msg.contains("未登录", ignoreCase = true) ||
            msg.contains("HTTP 401", ignoreCase = true)
    }

    /** 当前时间字符串（HH:mm:ss） */
    private fun now(): String {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val instant = Instant.fromEpochMilliseconds(nowMs)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.hour.toString().padStart(2, '0')}:" +
            "${dt.minute.toString().padStart(2, '0')}:" +
                dt.second.toString().padStart(2, '0')
    }

    override fun onCleared() {
        super.onCleared()
        snatchJob?.cancel()
        searchJob?.cancel()
    }

    private companion object {
        const val MAX_LOG_SIZE = 200
        /** 搜索关键词输入防抖时长（毫秒） */
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
