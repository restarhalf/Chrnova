# ViewModel 代码风格规范

## 1. 命名规范

### 1.1 ViewModel 类名
```kotlin
// ✅ 正确
class HomeViewModel(...)
class ScheduleViewModel(...)

// ❌ 错误
class HomeVM(...)
class home_view_model(...)
```

### 1.2 UI 数据类命名
```kotlin
// ✅ 正确 - ViewModel 主状态
data class HomeUiState(
    val courses: List<Course>,
    val loading: Boolean = false,
    val error: String = "",
)

// ✅ 正确 - 辅助 UI 模型
data class CourseCardUi(...)
data class HeaderUi(...)

// ❌ 错误 - 混用
data class ScreenState(...)  // 应改为 *UiState
data class ScreenUi(...)     // 应改为 *UiState（如果是主状态）
```

### 1.3 状态属性命名
```kotlin
// ✅ 正确 - 单一响应式状态
private val _uiState: StateFlow<HomeUiState> = ...
val uiState: StateFlow<HomeUiState> = _uiState

// ❌ 错误 - 多个独立状态
private val _loading = MutableStateFlow(false)
private val _error = MutableStateFlow("")
private val _items = MutableStateFlow<List<*>>(emptyList())
```

---

## 2. ViewModel 结构规范

### 2.1 标准结构模板
```kotlin
class ExampleViewModel(
    private val observeSomethingUseCase: ObserveSomethingUseCase,
    private val doActionUseCase: DoActionUseCase,
) : ViewModel() {

    // 1. UI 数据类定义
    data class ExampleUiState(
        val data: List<Item>,
        val loading: Boolean = false,
        val error: String = "",
    )

    // 2. 私有状态（单一 StateFlow）
    private val _uiState: StateFlow<ExampleUiState> =
        observeSomethingUseCase()
            .map { data -> ExampleUiState(data = data) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExampleUiState(data = emptyList()),
            )

    // 3. 公开状态
    val uiState: StateFlow<ExampleUiState> = _uiState

    // 4. UI 事件处理方法（按字母排序）
    fun onAction() {
        viewModelScope.launch {
            doActionUseCase()
        }
    }

    // 5. 纯 UI 映射方法（private，仅用于 Screen 层）
    fun buildScreenData(uiState: ExampleUiState): ScreenData {
        return ScreenData(...)
    }
}
```

### 2.2 禁止的模式
```kotlin
// ❌ 禁止：暴露 get/set 方法
fun getCampus(): Campus = getCampusUseCase()
fun setCampus(campus: Campus) { setCampusUseCase(campus) }

// ✅ 正确：直接在 UI 事件方法中调用
fun onCampusChanged(campus: Campus) {
    setCampusUseCase(campus)
}

// ❌ 禁止：暴露 observe 方法
fun observeAllCourses(): Flow<List<Course>> = observeAllCoursesUseCase()

// ✅ 正确：整合到 uiState
private val _uiState = observeAllCoursesUseCase()
    .map { courses -> HomeUiState(courses = courses) }
    .stateIn(...)

// ❌ 禁止：多个独立 MutableStateFlow
private val _loading = MutableStateFlow(false)
private val _error = MutableStateFlow("")

// ✅ 正确：combine 到单一 uiState
private val _uiState = combine(_loading, _error) { loading, error ->
    ExampleUiState(loading = loading, error = error)
}.stateIn(...)
```

---

## 3. 状态更新规范

### 3.1 MutableStateFlow 更新
```kotlin
// ✅ 正确
_uiState.value = _uiState.value.copy(loading = true)

// ❌ 错误
_uiState.emit(_uiState.value.copy(loading = true))  // emit 用于 SharedFlow
```

### 3.2 异步操作
```kotlin
// ✅ 正确
fun load() {
    viewModelScope.launch {
        _uiState.value = _uiState.value.copy(loading = true)
        runCatching { fetchDataUseCase() }
            .onSuccess { data ->
                _uiState.value = _uiState.value.copy(
                    data = data,
                    loading = false,
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message ?: "Unknown error",
                    loading = false,
                )
            }
    }
}

// ❌ 错误 - 分散的状态更新
fun load() {
    _loading.value = true
    viewModelScope.launch {
        val data = fetchDataUseCase()
        _items.value = data
        _loading.value = false
    }
}
```

---

## 4. UseCase 调用规范

### 4.1 命名约定
- `Observe*UseCase` → 返回 `Flow<T>`，用于响应式数据
- `Get*UseCase` → 返回 `T`，用于一次性读取
- `Set*UseCase` → 返回 `Unit`，用于写入
- `Build*UseCase` → 返回 UI 模型，用于复杂 UI 构建逻辑
- `Calculate*UseCase` → 返回业务模型，用于业务计算

### 4.2 ViewModel 中的使用
```kotlin
// ✅ 正确 - Observe 整合到 uiState
private val _uiState = observeCoursesUseCase()
    .map { courses -> HomeUiState(courses = courses) }
    .stateIn(...)

// ✅ 正确 - Get 用于一次性读取
fun loadInitialData() {
    val campus = getCampusUseCase()
    // ...
}

// ✅ 正确 - Set 用于写入
fun onCampusChanged(campus: Campus) {
    setCampusUseCase(campus)
}

// ✅ 正确 - Build 用于 UI 映射
fun buildScreenData(uiState: HomeUiState): ScreenData {
    return buildHomeScreenDataUseCase(uiState.courses)
}
```

---

## 5. 方法排序规范

### 5.1 ViewModel 内部排序
```kotlin
class ExampleViewModel(...) : ViewModel() {
    // 1. 数据类定义
    data class ExampleUiState(...)
    data class ExampleUi(...)

    // 2. 私有状态
    private val _uiState: StateFlow<ExampleUiState> = ...

    // 3. 公开状态
    val uiState: StateFlow<ExampleUiState> = _uiState

    // 4. 初始化块（如需要）
    init { ... }

    // 5. 公开方法（按字母排序）
    fun buildScreenData(...) { ... }
    fun onAction() { ... }
    fun onLoad() { ... }

    // 6. 私有方法（按字母排序）
    private fun helperMethod() { ... }
}
```

---

## 6. 迁移检查清单

### 6.1 ViewModel 重构检查
- [ ] 所有 ViewModel 都有 `private val _uiState` + `val uiState`
- [ ] 没有 `fun observe*()` 方法暴露
- [ ] 没有 `fun get*()` / `fun set*()` 方法暴露
- [ ] 所有 `fun build*()` 要么迁移到 UseCase，要么标记为纯 UI 映射
- [ ] 没有多个独立的 `MutableStateFlow`（除非有充分理由）
- [ ] 所有异步操作都在 `viewModelScope.launch` 中

### 6.2 命名检查
- [ ] UI 数据类命名：`*UiState`（主状态）、`*Ui`（辅助模型）
- [ ] 没有 `ScreenState`、`ScreenUi` 等混用命名
- [ ] UseCase 命名符合约定（`Observe*`、`Get*`、`Set*`、`Build*`、`Calculate*`）

### 6.3 状态更新检查
- [ ] 使用 `_state.value =` 而非 `_state.emit()`
- [ ] 使用 `copy()` 更新不可变状态
- [ ] 没有直接修改 `MutableStateFlow` 的 `value` 属性后不更新

---

## 7. 示例对比

### 7.1 重构前（❌ 不规范）
```kotlin
class GradeViewModel : ViewModel() {
    data class ScreenState(...)  // ❌ 命名不规范

    private val _loading = MutableStateFlow(false)  // ❌ 分散状态
    private val _error = MutableStateFlow("")
    private val _report = MutableStateFlow(TermGradeReport())

    fun load() {  // ❌ 分散更新
        _loading.value = true
        viewModelScope.launch {
            val report = fetchGradesUseCase()
            _report.value = report
            _loading.value = false
        }
    }

    fun buildScreenState(...): ScreenState { ... }  // ❌ 应在 Screen 层调用
}
```

### 7.2 重构后（✅ 规范）
```kotlin
class GradeViewModel(
    private val calculateGradeSummary: CalculateGradeSummaryUseCase,
) : ViewModel() {
    data class GradeUiState(  // ✅ 规范命名
        val loading: Boolean,
        val error: String,
        val report: TermGradeReport,
    )

    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow("")
    private val _report = MutableStateFlow(TermGradeReport())

    private val _uiState: StateFlow<GradeUiState> =  // ✅ 单一状态
        combine(_loading, _error, _report) { loading, error, report ->
            GradeUiState(loading = loading, error = error, report = report)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GradeUiState(
                loading = false,
                error = "",
                report = TermGradeReport(),
            ),
        )

    val uiState: StateFlow<GradeUiState> = _uiState

    fun load() {  // ✅ 统一更新
        _loading.value = true
        _error.value = ""
        viewModelScope.launch {
            runCatching { fetchGradesUseCase() }
                .onSuccess { _report.value = it }
                .onFailure { _error.value = it.message ?: "Unknown error" }
            _loading.value = false
        }
    }

    // ✅ 纯 UI 映射，在 Screen 层调用
    fun buildScreenState(
        report: TermGradeReport,
        loading: Boolean,
        error: String,
    ): ScreenState {
        val summary = if (report.achievements.isNotEmpty()) {
            calculateGradeSummary(report)
        } else null
        return ScreenState(...)
    }
}
```

---

## 8. 常见问题

### Q1: 什么时候可以保留 `fun build*()`？
**A**: 仅当该方法是**纯 UI 映射**（无业务逻辑），且在 Screen 层调用时可以保留。如果包含业务逻辑，应迁移到 UseCase。

### Q2: 为什么要统一为单一 `uiState`？
**A**: 
1. 避免状态不一致（多个状态更新顺序问题）
2. 简化 Screen 层订阅（只需 `collectAsState()` 一次）
3. 便于测试和调试

### Q3: `MutableStateFlow` vs `MutableSharedFlow`？
**A**:
- `MutableStateFlow`: 用于**状态**（有初始值，新订阅者立即收到当前值）
- `MutableSharedFlow`: 用于**事件**（无初始值，仅发送新事件）

### Q4: 什么时候可以有多个 `MutableStateFlow`？
**A**: 仅当状态来源完全独立且需要分别更新时（如 `ExaminationViewModel` 的 `_loading`、`_error`、`_items`），但最终仍需 `combine` 到单一 `_uiState`。

---

## 9. 自动化检查

### 9.1 Lint 规则（待实现）
```kotlin
// 检测规则：
// 1. ViewModel 必须有 uiState
// 2. 禁止暴露 observe*/get*/set* 方法
// 3. UI 数据类命名必须为 *UiState 或 *Ui
// 4. 禁止使用 .emit() 更新 MutableStateFlow
```

### 9.2 重构脚本
见 `refactor-viewmodels.kts`

---

**最后更新**: 2026-05-05
**维护者**: Sisyphus AI
