# Chrnova

校园课程表应用。**双模块**：`:composeApp`（KMP 共享代码，`commonMain`/`androidMain`/`iosMain`）+ `:androidApp`（Android 入口、签名、版本号）。UI 用 Compose Multiplatform（`org.jetbrains.compose` 插件）+ miuix（组件库 + 导航 + squircle + blur）+ Navigation3（类型安全路由）。数据：Room 3.0（KSP）+ Ktor（HTTP）+ kotlinx-serialization/coroutines/datetime/collections-immutable + Koin（DI）。Android 平台：WorkManager（后台同步）+ Glance（桌面小组件）+ Coil（图片）。

## 技术栈

Kotlin 2.4.10 + AGP 9.3.0 + KSP 2.3.10。UI：Compose Multiplatform 1.11.1（`org.jetbrains.compose` 插件）+ miuix 0.9.3（组件库 + 导航 + squircle + blur）+ Navigation3 1.1.4（类型安全路由）+ qrose 1.1.2（二维码）。数据：Room 3.0.0（KSP，`@ConstructedBy` 反射 builder）+ Ktor 3.5.1（HTTP，OkHttp/Darwin）+ kotlinx-serialization 1.11.0/coroutines 1.11.0/datetime 0.8.0/collections-immutable 0.5.1 + Koin 4.2.2（DI）。其他：multiplatform-settings 1.3.0（偏好存储）、cryptography 0.6.0（AES 加密）、coil-compose 3.5.0（图片加载）。Android 平台：WorkManager 2.11.2 + Glance 1.1.1 + ExifInterface 1.4.2。iOS 平台：Ktor Darwin 引擎。

**版本与坐标唯一真源 = `gradle/libs.versions.toml`**（含 `[bundles]` 分组）。应用坐标在 `androidApp/build.gradle.kts`（`applicationId = "restarhalf.stellar.schedule"`），SDK 在 `composeApp/build.gradle.kts`。版本号走 `iosApp/Configuration/AppVersion.xcconfig`（`APP_VERSION_NAME`/`APP_VERSION_CODE`），Android 构建时读取。**文档不复述版本号**（避免漂移）。

Compose 稳定性配置：[composeApp/compose_compiler_config.conf](composeApp/compose_compiler_config.conf) 列出 kotlinx.coroutines Flow/Mutex/Semaphore、kotlinx.serialization.Json、kotlinx.datetime Instant/LocalDateTime/TimeZone、Ktor HttpClient、AndroidX ViewModel/SavedStateHandle/RoomDatabase、`restarhalf.stellar.schedule.domain.model.*`（数据模型靠通配声明 stable）；由 [composeApp/build.gradle.kts](composeApp/build.gradle.kts) 的 `composeCompiler.stabilityConfigurationFiles` 加载。新增推断 unstable 的三方/平台字段时优先加进该文件，而非散落 `@Stable` 注解。

密钥管理：`local.properties` 中的 `SIGN_KEY`/`AES_KEY`/`PAPERS_BASE_URL` 通过 `GenerateLocalSecretsTask` 生成 `build/generated/.../LocalSecrets.kt`（字符数组混淆），所有 `compile`/`ksp` 任务依赖该生成任务。

## 项目结构

**双模块**：`:composeApp`（KMP 共享代码）+ `:androidApp`（Android 入口）。分层靠**包名**（`domain.model`/`domain.repository`/`domain.port`/`data.*`/`ui`/`di`/`core`/`platform`），跨层即普通包引用。约定：`domain.model` 只放 `@Serializable` 数据模型 + 枚举，`domain.repository` 只放仓库接口，`domain.port` 只放端口接口（平台抽象），三者不引 compose/ktor/room（约定非 Gradle 强制）。

```
Chrnova/
├── composeApp/                           KMP 共享模块（commonMain + androidMain + iosMain）
│   ├── build.gradle.kts                  kotlin.multiplatform + compose + ksp + room3 + 本地密钥生成
│   ├── compose_compiler_config.conf      Compose 稳定性配置
│   ├── schemas/                          Room 导出 schema（v1→v18）
│   └── src/
│       ├── commonMain/kotlin/.../schedule/
│       │   ├── App.kt                    根组件 + 主题配置 + AppRoot（全参数化）
│       │   ├── config/                   LocalSecrets（构建时生成，密钥存储）
│       │   ├── core/                     核心功能（announcement/ course/ error/ log/ net/ ...）
│       │   ├── data/
│       │   │   ├── impl/                 端口实现（AcademicPortImpl/ AuthPortImpl/ SettingsPortImpl/ ...）
│       │   │   ├── local/                Room 数据库 + DAO + Entity + 迁移 + TimetableSettings
│       │   │   ├── mapper/               数据映射（Remote→Domain）
│       │   │   ├── remote/               HTTP 客户端（JwxtClient/ PEClient/ PapersApi/ JwxtSync）
│       │   │   └── repository/           仓库实现（RoomCourseRepository/ RoomExaminationRepository/ ...）
│       │   ├── di/                       AppModule.kt（portModule + useCaseModule + viewModelModule）
│       │   ├── domain/
│       │   │   ├── model/                @Serializable 数据模型 + Campus enum + SettingsKeys object
│       │   │   ├── port/                 端口接口（14 个：AcademicPort/ AuthPort/ SettingsPort/ ...）
│       │   │   ├── repository/           仓库接口（CourseRepository/ ExaminationRepository/ GradeRepository）
│       │   │   └── usecase/              业务用例（30+ 个，每个封装一个具体业务操作）
│       │   ├── papers/                   课件系统（PdfFilePickerHost expect）
│       │   ├── pictureselector/          图片选择器（PictureSelectorHost expect + ImageCropper）
│       │   ├── platform/                 平台抽象（AppCoroutineDispatchers expect）
│       │   └── ui/
│       │       ├── blur/                 模糊效果组件
│       │       ├── components/           共享组件（33+ 个：GroupedCardItems/ AppCard/ AvatarImage/ ...）
│       │       ├── effect/               动画效果
│       │       ├── icons/                自定义图标
│       │       ├── image/                图片处理
│       │       ├── mapper/               UI 映射
│       │       ├── modifier/             Compose Modifier
│       │       ├── navigation/           导航（Screen/ AppNavigator/ AppChromeState/ GlassNavigationBar/ ...）
│       │       ├── port/                 UI 端口（AppInfoPort/ PictureSelectorPort）
│       │       ├── screens/              页面（AppContent 主内容 + 各功能页面）
│       │       ├── sync/                 同步 UI
│       │       ├── theme/                主题
│       │       └── viewmodel/            ViewModel（16 个）
│       ├── androidMain/kotlin/.../schedule/
│       │   ├── AndroidApp.kt             Android Application 入口（Koin 初始化）
│       │   ├── AppRoot.android.kt        Android 组合根（主题 + 系统栏 + 权限处理）
│       │   ├── MainActivity.kt           Android Activity 入口
│       │   ├── core/log/                 Android 日志存储
│       │   ├── di/AppModule.android.kt   Android 平台模块（数据库 + 调度器）
│       │   ├── papers/                   课件系统 Android 实现
│       │   ├── pictureselector/          图片选择器 Android 实现
│       │   ├── platform/                 Android 平台调度器
│       │   ├── reminder/                 提醒系统（Receiver + Worker + NotificationChannels）
│       │   └── widget/                   桌面小组件（Glance + WidgetUpdater + WidgetRefreshController）
│       ├── iosMain/kotlin/.../schedule/
│       │   ├── AppRoot.ios.kt            iOS 组合根
│       │   ├── di/AppModule.ios.kt       iOS 平台模块
│       │   └── papers/                   课件系统 iOS 实现
│       └── commonMain/composeResources/  Compose Resources（strings/ drawables）
├── androidApp/                           Android 入口模块
│   ├── build.gradle.kts                  com.android.application + 签名 + 版本号
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── res/                          Android 资源（drawable/ mipmap/ xml）
├── iosApp/                               iOS 入口（Xcode 项目）
├── worker/                               后台 Worker（如有）
└── docs/                                 文档
```

## 架构

### 依赖层级

```
AndroidApp / iosApp → Koin 容器（commonAppModule = portModule + useCaseModule + viewModelModule）
  AppRoot（平台入口）→ App（根组件，全参数化）
    → AppContent（NavDisplay + 二级页面）
      → Screen Composable
        → ViewModel（构造注入 domain 接口 + 端口实现）
          → UseCase（业务逻辑协调）
            → domain.port.* 接口
              └ 实现 data.impl.* / data.remote.* / data.repository.*
                  ├→ JwxtClient / PEClient / PapersApi（Ktor HTTP）→ 教务系统 API
                  └→ Room Database（CourseDao / ExaminationDao / GradeDao / PEDao）
```

**Koin 依赖注入**（严格遵循 koin inject，3 模块均在 `:composeApp` 的 `di/AppModule.kt`，按职责拆分）：

- `portModule`：Json 单例、JwxtAuthStore/PEAuthStore（认证存储）、HttpClient（named("jwxt")/named("pe")/named("papers")）、JwxtGateway/PEGateway（网关客户端）、JwxtSync、TimetableSettings、数据仓库（CourseRepository/ExaminationRepository/GradeRepository/PERepository/PERoomRepository）、端口实现（14 个：SettingsPort/PasswordEncryptionPort/AuthPort/AcademicPort/AuthWorkflowPort/TimetablePort/SyncPort/PapersPort 等）
- `useCaseModule`：30+ 个业务用例（factory 注册），每个封装一个具体业务操作
- `viewModelModule`：16 个 ViewModel（`viewModel { }` DSL 注册），通过 `koinViewModel()` 在 Composable 中获取

**端口模式**（Hexagonal Architecture）：`domain.port` 定义平台无关接口（14 个），`data.impl` 提供具体实现。ViewModel 依赖端口接口，不直接依赖平台实现。端口实现通过 Koin 注入。

### 核心模式

- **KMP 架构**：`commonMain` 放共享业务逻辑、UI、ViewModel、数据模型；`androidMain`/`iosMain` 放平台实现（数据库构造、HTTP 引擎、图片选择、日志存储、桌面小组件）。expect/actual 用于平台特定声明（`AppIoDispatcher`/`AppDatabaseConstructor`/`LogFileStorage`）
- **导航**：Navigation3 + 自定义 `AppNavigator`（push/pop/popUntil/replace/replaceRoot/navigateForResult）+ `LocalNavigator` CompositionLocal；路由定义在 `sealed interface Screen : NavKey`，所有路由 `@Serializable data object` 或 `@Serializable data class`；`NavDisplay` + `entryProvider<NavKey>` 映射路由到 Composable
- **主页 Tab**：5 Tab 底部导航栏（首页/课程表/考务/体测/设置），支持 3 种模式：固定导航栏（NavigationBar）、悬浮导航栏（FloatingNavigationBar）、液态玻璃导航栏（GlassNavigationBar，基于 AndroidLiquidGlass 适配）
- **UI 状态**：`@Stable`/`@Immutable` 标注 UI 状态类，`ImmutableList`（kotlinx-collections-immutable）用于列表，`MutableStateFlow` 暴露为 `StateFlow`，Flow 一律 `collectAsStateWithLifecycle()`
- **数据持久化**：Room 3.0 KMP（结构化数据，7 Entity + 7 DAO，v1→v18 迁移）+ multiplatform-settings（简单偏好）
- **HTTP 客户端**：Ktor HttpClient（OkHttp/Darwin），通过 Koin named qualifier 区分不同用途（jwxt/pe/papers），公共配置（JSON 序列化 + 超时）抽取为 `installCommonModules`
- **认证流程**：JwxtAuthPlugin（Ktor 插件）自动管理教务系统 Cookie/Token；PEAuthStore 管理体育系统认证；AuthWorkflowPort 协调登录→同步→绑定流程
- **课件系统**：PapersApi + GitHub Star 验证（VerifyGitHubStarUseCase），PDF 文件选择通过 expect/actual 跨平台
- **提醒系统**：CourseReminderPort/ExamReminderPort（接口）→ Android 平台 WorkManager + BroadcastReceiver 实现；BootReceiver 开机恢复提醒
- **桌面小组件**：Glance 实现（WidgetUpdater + WidgetRefreshController + ScreenStateReceiver），显示今日课程摘要
- **国际化**：英文默认 + 中文（zh-rCN），Compose Resources `stringResource()`；Android Service/通知文案使用 `androidApp/src/main/res` 的对应资源
- **密钥安全**：LocalSecrets 字符数组混淆（构建时生成），不硬编码明文

### 数据库架构（Room 3.0 KMP）

#### 七表结构

| 表                | Entity                  | 用途                     |
| ----------------- | ----------------------- | ------------------------ |
| courses           | CourseEntity            | 课程表                   |
| examinations      | ExaminationEntity       | 考试安排                 |
| grades            | GradeEntity             | 成绩                     |
| pe_scores         | PEYearScoreEntity       | 体育年度成绩             |
| pe_student_info   | PEStudentInfoEntity     | 体育学生信息             |
| pe_detail_scores  | PESubjectScoreEntity    | 体育科目详情成绩         |
| pe_detail_summary | PEDetailSummaryEntity   | 体育成绩摘要             |

### 路由清单

`Screen.kt` 中定义的路由，均实现 `NavKey`：

| 路由             | 类型        | 页面               | 入口                  |
| ---------------- | ----------- | ------------------ | --------------------- |
| Main             | data object | 主页容器（5 Tab）  | 根路由                |
| Home             | data object | 首页（今日课程）   | 底栏 Tab 1            |
| Schedule         | data object | 课程表（按周查看） | 底栏 Tab 2            |
| EMS              | data object | 考务（考试+成绩）  | 底栏 Tab 3            |
| Settings         | data object | 设置               | 底栏 Tab 5            |
| ChangeBackground | data object | 更换背景           | 设置页                |
| About            | data object | 关于               | 设置页                |
| PEScore          | data object | 体育成绩列表       | 底栏 Tab 4            |
| PEDetail         | data class  | 体育成绩详情       | 体测页点击项          |
| Log              | data object | 日志               | 设置页                |
| ClassEdit        | data class  | 课程编辑           | 课程表长按/新建       |
| ExamEdit         | data class  | 考试编辑           | 考务页新建/编辑       |
| Papers           | data object | 课件列表           | 设置页                |
| PapersDetail     | data class  | 课件详情           | 课件页点击项          |
| PapersUpload     | data object | 课件上传           | 课件页                |
| JWLogin          | data object | 教务系统登录       | 设置页/同步触发       |
| PELogin          | data object | 体育系统登录       | 体测页                |
| PEQRCode         | data object | 体育二维码         | 体测页                |
| Profile          | data object | 个人资料           | 设置页                |
| ElectiveCredit   | data object | 选修课学分统计     | 设置页                |

### 页面与 ViewModel

| Screen           | ViewModel             | 说明                                         |
| ---------------- | --------------------- | -------------------------------------------- |
| HomeScreen       | HomeViewModel         | 今日课程/时间段/问候语/时钟/考试摘要          |
| ScheduleScreen   | ScheduleViewModel     | 课表网格/周次切换/课程冲突/实验课编辑         |
| EMS screens      | ExaminationViewModel  | 考试列表/成绩列表/学期切换                    |
| PEScoreScreen    | PEViewModel           | 体育成绩列表/详情/学生信息                    |
| SettingsScreen   | SettingsViewModel     | 设置入口/账号管理/提醒/学期切换/Star验证      |
| AboutScreen      | AboutViewModel        | 版本信息/更新检查                             |
| JWLoginScreen    | JWLoginViewModel      | 教务系统登录                                  |
| PELoginScreen    | PELoginViewModel      | 体育系统登录                                  |
| PapersScreen     | PapersViewModel       | 课件列表/搜索/上传                            |
| ClassEditScreen  | CourseEditViewModel   | 课程编辑（新建/修改实验课）                   |
| ExamEditScreen   | ExamEditViewModel     | 考试编辑（新建/修改）                         |
| GradeViewModel   | GradeViewModel        | 成绩查询/学期切换                             |
| ElectiveCredits  | ElectiveCreditViewModel| 选修课学分统计                               |
| ProfileScreen    | PersonalInfoViewModel | 个人资料/头像/昵称                            |
| BackgroundScreen | BackgroundViewModel   | 背景图片/透明度/模糊度                        |
| AppViewModel     | AppViewModel          | 全局状态（登录/同步/初始化）                  |

## 构建命令

```bash
# 快速验证 Kotlin（跳过 Android 打包，秒级）：
./gradlew :composeApp:compileKotlinIosArm64    # iOS 编译验证
./gradlew :composeApp:compileDebugKotlin       # Android 编译验证

# 构建 Android APK：
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:assembleRelease

# Room schema 导出（跑一次 assemble 即可）：
./gradlew :androidApp:assembleDebug
```

`:composeApp` 是 KMP 模块，改 commonMain 优先用 `:composeApp:compileKotlinIosArm64` 快速验证（无 Android 打包）；涉及 Android 平台代码时用 `:composeApp:compileDebugKotlin`；只有验证打包/签名才跑 `:androidApp:assembleDebug`。

## 关键架构约束

不读代码看不出来的约束。违反会直接踩坑。

**组合根注入**：`App.kt` 的 `App(...)` 签名保持全参数化默认值，屏幕仍参数化。Android 由 `AppRoot.android.kt` 从 Koin 取图后透传；iOS 由 `AppRoot.ios.kt` 同理。ViewModel 通过 `koinViewModel()` 获取，不通过 App 参数传递。

**端口模式（Hexagonal Architecture）**：`domain.port` 定义 14 个平台无关接口，`data.impl` 提供具体实现。ViewModel 依赖端口接口（如 `AuthPort`/`SettingsPort`/`AcademicPort`），不直接依赖 `data.impl.*`。新增平台功能时：① 在 `domain.port` 定义接口；② 在 `data.impl` 实现；③ 在 `portModule` 注册 Koin 绑定。

**UseCase 层**：业务逻辑封装在 `domain.usecase`（30+ 个），每个用例一个类，通过构造函数注入端口/仓库。ViewModel 调用 UseCase 而非直接操作仓库。UseCase 在 `useCaseModule` 中以 `factory { }` 注册（非单例，每次注入新建）。

**数据库迁移**：Room 3.0 KMP，当前 v18。迁移定义在 `AppDatabase.kt` 的 private val，`buildAppDatabase()` 中 `addMigrations()` 注册。新增字段/表时：① 更新 Entity + DAO；② 导出 schema（`./gradlew :androidApp:assembleDebug`）；③ 新增 Migration；④ `addMigrations()` 注册。**遗漏注册会让升级用户首次启动 crash**。

**HTTP 客户端所有权**：Ktor HttpClient 通过 Koin named qualifier 管理（`named("jwxt")`/`named("pe")`/`named("papers")`），公共配置（JSON + 超时）在 `installCommonModules`。禁止在 ViewModel 或 UseCase 中直接构造 HttpClient；统一从 Koin 获取。`JwxtAuthPlugin` 自动管理教务系统认证状态。

**暗色模式**：Android 由 `AppRoot.android.kt` 内联解析 `themeMode`（0=系统/1=浅色/2=深色），通过 `ThemeController` + `MiuixTheme` 提供主题。屏幕/组件不直接调 `isSystemInDarkTheme()`（仅根入口一处调用）。iOS 由 `AppRoot.ios.kt` 的 `rememberAppThemeController` 同理处理。

**导航栈生命周期**：`AppNavigator` 的 `backStack` 是 `mutableStateListOf<NavKey>`，随 Compose 状态自动重组。`navigateForResult` 通过 `SharedFlow` 传递结果，`pop()` 后自动清理无订阅者的通道。路由 `@Serializable` 但**无显式进程死亡恢复序列化**（当前 back stack 不持久化到 Bundle/SavedStateHandle）。

**密钥管理**：`LocalSecrets` 由 `GenerateLocalSecretsTask` 构建时生成（字符数组混淆），不硬编码明文。`SIGN_KEY`/`AES_KEY`/`PAPERS_BASE_URL` 来自 `local.properties` 或环境变量。`AES_KEY` 必须恰好 16 字节（AES-128）。`local.properties` 含敏感信息，不入版本控制。

**Compose 稳定性**：`compose_compiler_config.conf` 通配声明 `domain.model.*` 为 stable（避免散落 `@Stable` 注解）。新增推断 unstable 的三方/平台字段时优先加进该文件。`@Immutable` 用于不可变数据类（如 `Examination`/`TermGradeReport`），`@Stable` 用于可变但稳定引用的类（如 `HomeUiState`）。

**expect/actual 约束**：`commonMain` 中 3 个 expect 声明（`AppIoDispatcher`/`AppDatabaseConstructor`/`LogFileStorage`），`androidMain` 和 `iosMain` 各有 actual 实现。新增跨平台功能时优先在 `commonMain` 实现，仅在确实需要平台特定行为时才用 expect/actual。

**提醒系统**：接口在 `domain.port`（`CourseReminderPort`/`ExamReminderPort`/`ReminderSchedulerPort`），Android 实现在 `androidMain/reminder/`（WorkManager + BroadcastReceiver）。BootReceiver 开机恢复提醒。接口设计为平台无关，iOS 实现可独立添加。

**桌面小组件**：Glance 实现（`androidMain/widget/`），`WidgetUpdater` 负责数据更新，`WidgetRefreshController` 管理刷新时机，`ScreenStateReceiver` 监听屏幕开关。小组件显示今日课程摘要，数据来自 `WidgetDataRepository`。
