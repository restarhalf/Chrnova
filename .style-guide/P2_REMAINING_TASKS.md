# P2 优化剩余任务

本文档记录了 P2 可选优化中尚未完成的任务。这些任务不影响架构一致性，可以作为未来的技术债务逐步处理。

## 已完成的 P2 优化

### ✅ AppViewModel
- 移除 `getCampus()` / `setCampus()` → 改为 `onCampusChanged()`
- 移除 `getTermStartMs()` / `setTermStartMs()` → 改为 `onTermStartMsChanged()`
- 移除 `getTotalWeeks()` / `setTotalWeeks()` → 改为 `onTotalWeeksChanged()`
- AppRoot.kt 改用 `vm.uiState` 获取初始值
- 提交：`f8e6c74`

---

## 剩余任务

### 1. 移除 SettingsViewModel 的 set* 方法（高优先级）

**当前问题：**
- `setSelectedTerm()` - 在 SettingsScreen.kt:341 使用
- `setThemeMode()` - 在 SettingsScreen.kt:383 使用
- `setFloatingBar()` - 在 SettingsScreen.kt:391 使用
- `setShowNonCurrentWeek()` - 在 SettingsScreen.kt:369 使用
- `setReminderEnabled()` - 在 SettingsScreen.kt:416, 424 使用
- `setExamReminderEnabled()` - 在 SettingsScreen.kt:434, 440 使用

**建议重构：**
```kotlin
// ViewModel
fun onSelectedTermChanged(value: String) {
    setSelectedTermUseCase.invoke(value)
}

fun onThemeModeChanged(mode: Int) {
    setThemeModeUseCase.invoke(mode)
}

fun onFloatingBarChanged(mode: Int) {
    setFloatingBarUseCase.invoke(mode)
}

fun onShowNonCurrentWeekChanged(show: Boolean) {
    setShowNonCurrentWeekUseCase.invoke(show)
}

fun onReminderEnabledChanged(enabled: Boolean) {
    setCourseReminderEnabled.invoke(enabled)
    if (!enabled) {
        cancelAllCourseReminders()
    }
}

fun onExamReminderEnabledChanged(enabled: Boolean) {
    setExamReminderEnabled.invoke(enabled)
    if (!enabled) {
        cancelAllExamReminders()
    }
}
```

**注意事项：**
- `scheduleCourseReminder()` 和 `scheduleExamReminder()` 方法有重复定义，需要仔细处理
- `handleNotificationPermissionResult()` 方法中也调用了这些 set* 方法，需要同步更新

---

### 2. 移除 BackgroundViewModel 的 set* 方法（高优先级）

**当前问题：**
- `setBackgroundImageUri()` - 在 ChangeBackgroundScreen.kt 使用
- `setBackgroundAlpha()` - 在 ChangeBackgroundScreen.kt 使用
- `setBackgroundBlur()` - 在 ChangeBackgroundScreen.kt 使用
- `setComponentsAlpha()` - 在 ChangeBackgroundScreen.kt 使用

**建议重构：**
```kotlin
// ViewModel
fun onBackgroundImageUriChanged(uri: String?) {
    _backgroundImageUri.value = uri
}

fun onBackgroundAlphaChanged(value: Float) {
    _backgroundAlpha.value = value
}

fun onBackgroundBlurChanged(value: Float) {
    _backgroundBlur.value = value
}

fun onComponentsAlphaChanged(value: Float) {
    _componentsAlpha.value = value
}
```

---

### 3. 移除 HomeViewModel 的 getCampusTimetable() 方法（中优先级）

**当前问题：**
- `getCampusTimetable(campus: Campus)` - 在 HomeScreen.kt 使用

**建议重构：**
- 将 `getCampusTimetable()` 的结果添加到 `HomeUiState`
- 或者保留该方法（因为它是纯函数，不修改状态）

---

### 4. 移除 ScheduleViewModel 的 setShowNonCurrentWeek() 方法（中优先级）

**当前问题：**
- `setShowNonCurrentWeek(show: Boolean)` - 可能在 ScheduleScreen.kt 使用

**建议重构：**
```kotlin
// ViewModel
fun onShowNonCurrentWeekChanged(show: Boolean) {
    setShowNonCurrentWeekUseCase.invoke(show)
}
```

---

### 5. 评估 build*() 方法（中优先级）

**需要评估的方法：**

#### 应该迁移到 UseCase 的方法：
- `HomeViewModel.buildClockSnapshot()` - 已有 `BuildHomeClockSnapshotUseCase`，应该使用它
- `HomeViewModel.buildTodaySchedule()` - 包含业务逻辑，应该迁移到 UseCase
- `HomeViewModel.buildHomeRenderState()` - 包含业务逻辑，应该迁移到 UseCase
- `ScheduleViewModel.buildScheduleUiState()` - 已有 `BuildScheduleUiStateUseCase`，应该使用它

#### 可以保留的方法（纯 UI 映射）：
- 所有 `buildScreenUi()` 方法
- `buildHeaderUi()` / `buildSurfaceUi()` / `buildWeekHeaderUi()` 等 UI 组件构建方法
- `buildTermSelectionUi()` / `buildAccountUi()` 等辅助 UI 构建方法

#### 需要进一步评估的方法：
- `CourseEditViewModel.buildCourseNames()` / `buildDisabledWeeks()` / `buildLabCourseToSave()` / `buildEditingFormState()`
- `GradeViewModel.buildGradeTitle()` / `buildGradeSubtitle()` / `buildGradeDetailsSummary()` / `buildGradeScoreText()`

---

### 6. 统一异常处理模式（低优先级）

**当前问题：**
- 不同 ViewModel 的错误处理方式不一致
- 有些使用 `runCatching { }.fold()`
- 有些使用 `runCatching { }.onSuccess { }.onFailure { }`
- 有些直接 try-catch

**建议统一为：**
```kotlin
viewModelScope.launch {
    runCatching {
        withContext(AppIoDispatcher) {
            // 业务逻辑
        }
    }
        .onSuccess { result ->
            // 处理成功
        }
        .onFailure { error ->
            // 处理失败
            val message = error.toUserFacingMessage(UserFacingErrorKind.XXX)
            _uiState.update { it.copy(error = message) }
        }
}
```

---

## 重构优先级建议

1. **高优先级**：SettingsViewModel 和 BackgroundViewModel 的 set* 方法
   - 影响代码可维护性
   - 调用点相对集中，容易修改

2. **中优先级**：HomeViewModel 和 ScheduleViewModel 的方法
   - 影响较小
   - 可以逐步重构

3. **低优先级**：build*() 方法评估和异常处理统一
   - 不影响功能
   - 可以作为长期优化目标

---

## 重构注意事项

1. **每次只重构一个 ViewModel**
2. **立即验证构建**（`./gradlew assembleDebug`）
3. **提交前运行完整测试**
4. **保持小步快跑，避免大规模改动**
5. **遇到复杂依赖时，考虑保留现状**

---

## 参考文档

- [ViewModel 代码风格规范](.style-guide/VIEWMODEL_STYLE_GUIDE.md)
- [重构检查清单](.style-guide/REFACTOR_CHECKLIST.md)
