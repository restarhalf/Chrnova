# Chrnova Agent Guide

`CLAUDE.md` 是本仓库完整的架构约束和历史踩坑记录。开始改动前先读与任务相关的章节；这里仅保留日常工作中必须遵守的摘要。新增长期有效的架构、并发或平台约束时，同步更新 `CLAUDE.md`。

## 项目边界

- 默认把跨平台业务、ViewModel、数据模型和 Compose UI 放在 `composeApp/src/commonMain`；仅把 Android 或 iOS 的实际实现放进对应 source set。
- `androidApp/` 负责 Android 入口、签名、版本号；`composeApp/` 是 KMP 共享模块，包含全部业务逻辑和 UI。
- 文案使用 Compose Resources，并同时维护英文默认值和 `values-zh-rCN`。Android Service/通知文案使用 `androidApp/src/main/res` 的对应资源。

## 架构与并发

- `domain.port` 定义 14 个平台无关接口，`data.impl` 提供具体实现。ViewModel 依赖端口接口，不直接依赖 `data.impl.*`。新增平台功能时先在 `domain.port` 定义接口，再在 `data.impl` 实现。
- UseCase 层（`domain.usecase`，30+ 个）封装业务逻辑，ViewModel 调用 UseCase 而非直接操作仓库。UseCase 在 `useCaseModule` 中以 `factory { }` 注册。
- ViewModel 通过 `koinViewModel()` 在 Composable 中获取，不通过 App 参数传递。`App.kt` 的 `App(...)` 签名保持全参数化默认值，屏幕仍参数化。
- 数据库迁移定义在 `AppDatabase.kt`，`buildAppDatabase()` 中 `addMigrations()` 注册。新增字段/表时必须同步更新 Entity + DAO + Migration + 注册，遗漏会让升级用户首次启动 crash。

## Compose 约束

- UI state 保持不可变；commonMain 的 Flow 使用 `collectAsStateWithLifecycle()`。
- 遵循现有 miuix 风格：二级页使用 `AppPageTopBar`（基于 miuix `SmallTopAppBar`，定义在 `AppTopBar.kt`），长列表维持 Lazy item 粒度，复杂多行卡片使用 `groupedCardItems`。squircle 形状使用 `squircleSurface`/`squircleClip`/`squircleBackground`。例外：全屏特殊页面（如 `CropScreen`）可直接用 `SmallTopAppBar`。
- 暗色模式由平台根入口统一解析（`AppRoot.android.kt` 内联 + `ThemeController`），屏幕/组件禁止直接 `isSystemInDarkTheme()`。
- 跨平台代码不要直接依赖 Android `R`、Android Context 或 Android-only API；通过 expect/actual 或 platform 接口隔离。
- `compose_compiler_config.conf` 通配声明 `domain.model.*` 为 stable，新增推断 unstable 的三方/平台字段时优先加进该文件，而非散落 `@Stable` 注解。

## 验证

- 每次改动至少运行 `git diff --check`，并执行与变更匹配的 Gradle 任务。
- KMP 模块：`:composeApp`。commonMain 变更优先运行 `./gradlew :composeApp:compileKotlinIosArm64`（快，无 Android 打包）；Android 平台 actual 变更运行 `./gradlew :composeApp:compileDebugKotlin`；涉及打包/签名时运行 `./gradlew :androidApp:assembleDebug`。

## Git 与工作区

- 保留用户已有的未提交改动；不要用破坏性 reset/checkout 清理工作区，也不要修改或输出 `local.properties` 中的敏感内容。
- 完成修改后先报告变更与验证结果。除非用户在当前请求中明确授权，不执行 `git add`、`git commit` 或 `git push`。
