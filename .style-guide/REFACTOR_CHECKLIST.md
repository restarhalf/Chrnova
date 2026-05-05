# ViewModel 重构检查清单

## 🎯 重构优先级

### P0 - 必须立即修复（影响架构一致性）
- [ ] **ExaminationViewModel** - 保留 `_loading/_error/_items` 分散状态，应 combine 到单一 `_uiState`
- [ ] **GradeViewModel** - 同上
- [ ] **ScheduleViewModel** - 有 `_transDialogUiState/_transConflictUiState/_detailSheetUiState` 多个独立状态
- [ ] **SettingsViewModel** - 有 `_remoteTermItems/_loginUiState/_pendingNotificationTarget` 多个独立状态

### P1 - 应该修复（影响代码可维护性）
- [ ] **AboutViewModel** - 命名 `ScreenUi` 应改为 `AboutScreenUi`
- [ ] **ChangeBackgroundViewModel** - 命名 `ScreenUi` 应改为 `ChangeBackgroundUiState`
- [ ] **SettingsViewModel** - 命名 `ScreenUi` 应改为 `SettingsScreenUi`
- [ ] **ExaminationViewModel** - 命名 `ScreenState` 应改为 `ExaminationScreenUi`
- [ ] **GradeViewModel** - 命名 `ScreenState` 应改为 `GradeScreenUi`

### P2 - 可选优化（提升代码质量）
- [ ] 所有 ViewModel 的 `fun build*()` 方法评估是否应迁移到 UseCase
- [ ] 移除所有 `fun get*()/set*()` 暴露方法
- [ ] 统一异常处理模式

---

## 📋 详细重构计划

### 1. ExaminationViewModel 重构

#### 当前问题
```kotlin
// ❌ 分散状态
private val _loading = MutableStateFlow(false)
private val _error = MutableStateFlow("")
private val _items = MutableStateFlow<List<Examination>>(emptyList())

// ❌ 命名不规范
data class ScreenState(...)
```

#### 重构方案
```kotlin
// ✅ 统一状态
data class ExaminationUiState(
    val loading: Boolean = false,
    val error: String = "",
    val items: List<Examination> = emptyList(),
)

private val _loading = MutableStateFlow(false)
private val _error = MutableStateFlow("")
private val _items = MutableStateFlow<List<Examination>>(emptyList())

private val _uiState: StateFlow<ExaminationUiState> =
    combine(_loading, _error, _items) { loading, error, items ->
        ExaminationUiState(loading = loading, error = error, items = items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExaminationUiState(),
    )

val uiState: StateFlow<ExaminationUiState> = _uiState

// ✅ 重命名
data class ExaminationScreenUi(
    val cards: List<ExamCardUi>,
    val statusText: String?,
)

fun buildScreenUi(
    items: List<Examination>,
    loading: Boolean,
    error: String,
    nowMs: Long,
): ExaminationScreenUi {
    // ... 保持原逻辑
}
```

---

### 2. GradeViewModel 重构

#### 当前问题
```kotlin
// ❌ 分散状态
private val _loading = MutableStateFlow(false)
private val _error = MutableStateFlow("")
private val _report = MutableStateFlow(TermGradeReport())

// ❌ 命名不规范
data class ScreenState(...)
```

#### 重构方案
```kotlin
// ✅ 统一状态（已完成 combine，只需重命名）
data class GradeUiState(
    val loading: Boolean,
    val error: String,
    val report: TermGradeReport,
)

// ✅ 重命名
data class GradeScreenUi(
    val cards: List<GradeCardUi>,
    val statusText: String?,
    val summary: GradeSummaryUi?,
)

fun buildScreenUi(
    report: TermGradeReport,
    loading: Boolean,
    error: String,
): GradeScreenUi {
    // ... 保持原逻辑
}
```

---

### 3. ScheduleViewModel 重构

#### 当前问题
```kotlin
// ❌ 多个独立状态
private val _transDialogUiState = MutableStateFlow(TransDialogUiState())
private val _transConflictUiState = MutableStateFlow(TransConflictUiState())
private val _detailSheetUiState = MutableStateFlow(DetailSheetUiState())

private val _uiState: StateFlow<ScheduleUiState> = ...  // 只包含课程数据
```

#### 重构方案
```kotlin
// ✅ 合并所有状态
data class ScheduleUiState(
    val courses: List<Course>,
    val showNonCurrentWeek: Boolean,
    val transDialog: TransDialogUiState,
    val transConflict: TransConflictUiState,
    val detailSheet: DetailSheetUiState,
)

private val _transDialog = MutableStateFlow(TransDialogUiState())
private val _transConflict = MutableStateFlow(TransConflictUiState())
private val _detailSheet = MutableStateFlow(DetailSheetUiState())

private val _uiState: StateFlow<ScheduleUiState> =
    combine(
        observeAllCoursesUseCase(),
        observeShowNonCurrentWeekUseCase(),
        _transDialog,
        _transConflict,
        _detailSheet,
    ) { courses, showNonCurrentWeek, transDialog, transConflict, detailSheet ->
        ScheduleUiState(
            courses = courses,
            showNonCurrentWeek = showNonCurrentWeek,
            transDialog = transDialog,
            transConflict = transConflict,
            detailSheet = detailSheet,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScheduleUiState(
            courses = emptyList(),
            showNonCurrentWeek = false,
            transDialog = TransDialogUiState(),
            transConflict = TransConflictUiState(),
            detailSheet = DetailSheetUiState(),
        ),
    )

val uiState: StateFlow<ScheduleUiState> = _uiState

// 更新方法改为更新内部状态
fun showTransDialog(course: Course) {
    _transDialog.value = _transDialog.value.copy(show = true, course = course)
}
```

---

### 4. SettingsViewModel 重构

#### 当前问题
```kotlin
// ❌ 多个独立状态
private val _remoteTermItems = MutableStateFlow<List<String>>(emptyList())
private val _loginUiState = MutableStateFlow(LoginUiState())
private val _pendingNotificationTarget = MutableStateFlow<NotificationTarget>(NotificationTarget.None)

private val _uiState: StateFlow<SettingsUiState> = ...  // 只包含部分状态
```

#### 重构方案
```kotlin
// ✅ 合并所有状态
data class SettingsUiState(
    val authToken: String,
    val profile: AuthProfile,
    val campus: Campus,
    val termStartMs: Long,
    val totalWeeks: Int,
    val selectedTerm: String,
    val themeMode: Int,
    val floatingBar: Int,
    val showNonCurrentWeek: Boolean,
    val courseReminderEnabled: Boolean,
    val examReminderEnabled: Boolean,
    val remoteTermItems: List<String>,
    val loginState: LoginUiState,
    val pendingNotificationTarget: NotificationTarget,
)

private val _remoteTermItems = MutableStateFlow<List<String>>(emptyList())
private val _loginState = MutableStateFlow(LoginUiState())
private val _pendingNotificationTarget = MutableStateFlow<NotificationTarget>(NotificationTarget.None)

private val _uiState: StateFlow<SettingsUiState> =
    combine(
        observeAuthTokenUseCase(),
        observeAuthProfileUseCase(),
        observeCampusUseCase(),
        observeTermStartMsUseCase(),
        observeTotalWeeksUseCase(),
        observeSelectedTermUseCase(),
        observeThemeModeUseCase(),
        observeFloatingBarUseCase(),
        observeShowNonCurrentWeekUseCase(),
        observeCourseReminderEnabledUseCase(),
        observeExamReminderEnabledUseCase(),
        _remoteTermItems,
        _loginState,
        _pendingNotificationTarget,
    ) { values ->
        SettingsUiState(
            authToken = values[0] as String,
            profile = values[1] as AuthProfile,
            campus = values[2] as Campus,
            termStartMs = values[3] as Long,
            totalWeeks = values[4] as Int,
            selectedTerm = values[5] as String,
            themeMode = values[6] as Int,
            floatingBar = values[7] as Int,
            showNonCurrentWeek = values[8] as Boolean,
            courseReminderEnabled = values[9] as Boolean,
            examReminderEnabled = values[10] as Boolean,
            remoteTermItems = values[11] as List<String>,
            loginState = values[12] as LoginUiState,
            pendingNotificationTarget = values[13] as NotificationTarget,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(
            authToken = "",
            profile = AuthProfile(),
            campus = Campus.Development,
            termStartMs = 0L,
            totalWeeks = 20,
            selectedTerm = "",
            themeMode = 0,
            floatingBar = 0,
            showNonCurrentWeek = false,
            courseReminderEnabled = false,
            examReminderEnabled = false,
            remoteTermItems = emptyList(),
            loginState = LoginUiState(),
            pendingNotificationTarget = NotificationTarget.None,
        ),
    )

val uiState: StateFlow<SettingsUiState> = _uiState
```

---

### 5. 命名统一重构

#### AboutViewModel
```kotlin
// ❌ 当前
data class ScreenUi(...)

// ✅ 修改为
data class AboutScreenUi(...)
```

#### ChangeBackgroundViewModel
```kotlin
// ❌ 当前
data class ScreenUi(...)

// ✅ 修改为
data class ChangeBackgroundUiState(...)  // 这是主状态
```

#### SettingsViewModel
```kotlin
// ❌ 当前
data class ScreenUi(...)

// ✅ 修改为
data class SettingsScreenUi(...)
```

---

## 🔧 重构步骤

### Step 1: 备份当前代码
```bash
git checkout -b refactor/viewmodel-unification
git add .
git commit -m "chore: backup before ViewModel refactoring"
```

### Step 2: 按优先级重构
1. 先修复 P0（ExaminationViewModel、GradeViewModel、ScheduleViewModel、SettingsViewModel）
2. 每个 ViewModel 重构后立即运行 `./gradlew assembleDebug` 验证
3. 修复 P1（命名统一）
4. 最后处理 P2（可选优化）

### Step 3: 更新 Screen 层调用
每个 ViewModel 重构后，需要同步更新对应的 Screen：
- `ExaminationScreen.kt` → 使用 `vm.uiState` 替代分散的状态
- `GradeScreen.kt` → 同上
- `ScheduleScreen.kt` → 同上
- `SettingsScreen.kt` → 同上

### Step 4: 验证
```bash
./gradlew assembleDebug
./gradlew test  # 如果有测试
```

---

## 📊 重构进度追踪

| ViewModel | P0 状态合并 | P1 命名统一 | P2 方法优化 | Screen 更新 | 验证通过 |
|-----------|------------|------------|------------|------------|---------|
| ExaminationViewModel | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| GradeViewModel | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| ScheduleViewModel | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| SettingsViewModel | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| AboutViewModel | ✅ | ⬜ | ⬜ | ⬜ | ⬜ |
| ChangeBackgroundViewModel | ✅ | ⬜ | ⬜ | ⬜ | ⬜ |
| AppViewModel | ✅ | ✅ | ✅ | ✅ | ✅ |
| BackgroundViewModel | ✅ | ✅ | ✅ | ✅ | ✅ |
| CourseEditViewModel | ✅ | ✅ | ⬜ | ✅ | ✅ |
| HomeViewModel | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 🎓 重构注意事项

1. **不要一次性重构所有 ViewModel**，每次只重构一个，立即验证
2. **保持 git 提交粒度小**，每个 ViewModel 重构完成后提交一次
3. **优先修复 P0 问题**，这些影响架构一致性
4. **Screen 层更新要同步**，避免编译错误
5. **测试覆盖**：如果有单元测试，重构后确保测试通过

---

**最后更新**: 2026-05-05
**维护者**: Sisyphus AI
