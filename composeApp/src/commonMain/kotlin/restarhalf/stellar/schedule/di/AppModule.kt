package restarhalf.stellar.schedule.di

import restarhalf.stellar.schedule.data.impl.PasswordEncryptionPortImpl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import restarhalf.stellar.schedule.config.LocalSecrets
import restarhalf.stellar.schedule.data.impl.AcademicPortImpl
import restarhalf.stellar.schedule.data.impl.AuthPortImpl
import restarhalf.stellar.schedule.data.impl.AuthWorkflowPortImpl
import restarhalf.stellar.schedule.data.impl.BackgroundSettingsPortImpl
import restarhalf.stellar.schedule.data.impl.PEPasswordEncryptionPortImpl
import restarhalf.stellar.schedule.data.impl.SettingsPortImpl
import restarhalf.stellar.schedule.data.impl.SyncPortImpl
import restarhalf.stellar.schedule.data.impl.TimetablePortImpl
import restarhalf.stellar.schedule.data.local.TimetableSettings
import restarhalf.stellar.schedule.data.remote.JwxtAuthPlugin
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.data.remote.JwxtClient
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtSync
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.repository.RoomCourseRepository
import restarhalf.stellar.schedule.data.repository.RoomExaminationRepository
import restarhalf.stellar.schedule.data.repository.RoomGradeRepository
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEPasswordEncryptionPort
import restarhalf.stellar.schedule.domain.port.PasswordEncryptionPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import restarhalf.stellar.schedule.domain.usecase.BindUnboundDataUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeClockSnapshotUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeGreetingUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeHeaderUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodRenderRowsUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodRowUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodSectionsUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeSurfaceUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeTodayScheduleUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildScheduleUiStateUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllCourseRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllExamRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.ClearAuthUseCase
import restarhalf.stellar.schedule.domain.usecase.DeleteCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.EnsureLoggedInUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.domain.usecase.GetAllCoursesOnceUseCase
import restarhalf.stellar.schedule.domain.usecase.GetCampusTimetableUseCase
import restarhalf.stellar.schedule.domain.usecase.GetCampusUseCase
import restarhalf.stellar.schedule.domain.usecase.GetTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.GetTotalWeeksUseCase
import restarhalf.stellar.schedule.domain.usecase.InsertCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.IsAnyReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.LoginUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAuthProfileUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAuthTokenUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveBackgroundAlphaUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveBackgroundBlurUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveBackgroundImageUriUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveCampusUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveComponentsAlphaUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveCourseByIdUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveCourseReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveExamReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveExaminationByIdUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.DeleteExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveFloatingBarUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveLogEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllGradesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveSelectedTermUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveShowNonCurrentWeekUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveThemeModeUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveTotalWeeksUseCase
import restarhalf.stellar.schedule.domain.usecase.RefreshCourseRemindersIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextCourseReminderIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextExamReminderIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.ResolveCourseStatusUseCase
import restarhalf.stellar.schedule.domain.usecase.RunSyncUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveLabCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextCourseReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextExamReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.SetBackgroundAlphaUseCase
import restarhalf.stellar.schedule.domain.usecase.SetBackgroundBlurUseCase
import restarhalf.stellar.schedule.domain.usecase.SetBackgroundImageUriUseCase
import restarhalf.stellar.schedule.domain.usecase.SetCampusUseCase
import restarhalf.stellar.schedule.domain.usecase.SetComponentsAlphaUseCase
import restarhalf.stellar.schedule.domain.usecase.SetCourseReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.SetExamReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.SetFloatingBarUseCase
import restarhalf.stellar.schedule.domain.usecase.SetLogEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.SetSelectedTermUseCase
import restarhalf.stellar.schedule.domain.usecase.SetShowNonCurrentWeekUseCase
import restarhalf.stellar.schedule.domain.usecase.SetTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.SetThemeModeUseCase
import restarhalf.stellar.schedule.domain.usecase.SetTotalWeeksUseCase
import restarhalf.stellar.schedule.domain.usecase.ShouldAutoSyncAndMarkUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseWithConflictsUseCase
import restarhalf.stellar.schedule.ui.viewmodel.AboutViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ChangeBackgroundViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExamEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.HomeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import restarhalf.stellar.schedule.data.remote.PapersApi
import restarhalf.stellar.schedule.domain.port.PapersPort

/**
 * 端口实现模块
 * 
 * 注册所有端口接口的实现类，包括：
 * - HTTP客户端配置
 * - 教务系统网关
 * - 数据存储仓库
 * - 端口实现（认证、设置、同步等）
 */
val portModule = module {
    // JSON序列化配置，忽略未知键
    single { Json { ignoreUnknownKeys = true } }

    // 教务系统认证存储
    single { JwxtAuthStore(get(named("jwxt_auth"))) }
    // 体育系统认证存储
    single { restarhalf.stellar.schedule.data.remote.PEAuthStore(get(named("pe_auth"))) }
    single<PEAuthPort> { get<restarhalf.stellar.schedule.data.remote.PEAuthStore>() }

    // 教务系统HTTP客户端，配置JSON序列化和认证插件
    single(named("jwxt")) {
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            install(JwxtAuthPlugin) {
                authStore = get()
            }
            defaultRequest {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                header("Referer", "http://jwyd.dlnu.edu.cn/sjd/#/login")
            }
        }
    }

    // 体育系统HTTP客户端
    single(named("pe")) {
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            defaultRequest {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            }
        }
    }

    // 教务系统网关客户端
    single<JwxtGateway> {
        JwxtClient(
            httpClient = get(named("jwxt")),
            json = get(),
            authStore = get(),
            passwordEncryption = get(),
        )
    }

    // 体育系统客户端
    single<PEGateway> {
        restarhalf.stellar.schedule.data.remote.PEClient(
            httpClient = get(named("pe")),
            json = get(),
            authStore = get(),
            passwordEncryption = get(),
        )
    }

    // 教务系统同步服务
    single { JwxtSync(get()) }

    // 课表时间配置
    single { TimetableSettings(get(named("timetable_prefs"))) }

    // 数据仓库
    single<CourseRepository> { RoomCourseRepository(courseDao = get(), settings = get(), auth = get()) }
    single<ExaminationRepository> { RoomExaminationRepository(examinationDao = get()) }
    single<GradeRepository> { RoomGradeRepository(gradeDao = get()) }
    single { restarhalf.stellar.schedule.data.repository.PERepository(peGateway = get()) }
    single { restarhalf.stellar.schedule.data.repository.PERoomRepository(peYearScoreDao = get(), peStudentInfoDao = get(), peDetailDao = get()) }

    // 课件系统HTTP客户端
    single(named("papers")) {
        HttpClient {
            install(ContentNegotiation) {
                json(get<Json>())
            }
        }
    }

    // 端口实现
    single<SettingsPort> { SettingsPortImpl(settings = get(named(SettingsKeys.PREFS_NAME))) }
    single<PasswordEncryptionPort> { PasswordEncryptionPortImpl() }
    single<PEPasswordEncryptionPort> { PEPasswordEncryptionPortImpl() }
    single<BackgroundSettingsPort> {
        BackgroundSettingsPortImpl(settings = get(named(SettingsKeys.PREFS_NAME)))
    }
    single<AuthPort> { AuthPortImpl(authStore = get()) }
    single<AcademicPort> { AcademicPortImpl(gateway = get()) }
    single<AuthWorkflowPort> {
        AuthWorkflowPortImpl(
            gateway = get(),
            authStore = get(),
            courseRepository = get()
        )
    }
    single<TimetablePort> { TimetablePortImpl(prefs = get()) }
    single<SyncPort> { SyncPortImpl(jwxtSync = get(), courseRepository = get(), auth = get()) }
    single<PapersPort> {
        PapersApi(
            httpClient = get(named("papers")),
            json = get(),
            baseUrl = LocalSecrets.PAPERS_BASE_URL,
            getDeviceId = { get<SettingsPort>().getDeviceId() },
        )
    }
}

/**
 * 用例模块
 * 
 * 注册所有业务用例，每个用例封装一个具体的业务操作。
 * 用例层位于UI层和数据层之间，负责协调多个端口完成业务逻辑。
 */
val useCaseModule = module {
    // 同步用例
    single {
        RunSyncUseCase(
            authWorkflow = get(),
            academic = get(),
            timetable = get(),
            settings = get(),
            sync = get(),
            reminderScheduler = get(),
        )
    }
    single { FetchExaminationsUseCase(authWorkflow = get(), academic = get(), repository = get(), auth = get()) }
    single { FetchExaminationsSimpleUseCase(fetchExaminations = get()) }
    single {
        FetchGradesUseCase(
            authWorkflow = get(),
            academic = get(),
            settings = get(),
            repository = get()
        )
    }
    single { FetchGradesSimpleUseCase(fetchGrades = get()) }
    single { ObserveAllExaminationsUseCase(repository = get()) }
    single { ObserveExaminationByIdUseCase(repository = get()) }
    single { SaveExaminationUseCase(repository = get()) }
    single { DeleteExaminationUseCase(repository = get()) }
    single { ObserveAllGradesUseCase(repository = get()) }
    single { FetchSemesterIdsUseCase(authWorkflow = get(), academic = get()) }
    single { GetCampusUseCase(timetable = get()) }
    single { SetCampusUseCase(timetable = get()) }
    single { GetTermStartMsUseCase(timetable = get()) }
    single { SetTermStartMsUseCase(timetable = get()) }
    single { GetTotalWeeksUseCase(timetable = get()) }
    single { SetTotalWeeksUseCase(timetable = get()) }
    single { LoginUseCase(authWorkflow = get()) }
    single { ClearAuthUseCase(auth = get()) }
    single { EnsureLoggedInUseCase(authWorkflow = get()) }
    single { ObserveAuthTokenUseCase(auth = get()) }
    single { ObserveAuthProfileUseCase(auth = get()) }
    single { ObserveCampusUseCase(timetable = get()) }
    single { ObserveTermStartMsUseCase(timetable = get()) }
    single { ObserveTotalWeeksUseCase(timetable = get()) }
    single { ObserveShowNonCurrentWeekUseCase(settings = get()) }
    single { SetShowNonCurrentWeekUseCase(settings = get()) }
    single { ObserveThemeModeUseCase(settings = get()) }
    single { SetThemeModeUseCase(settings = get()) }
    single { ObserveFloatingBarUseCase(settings = get()) }
    single { SetFloatingBarUseCase(settings = get()) }
    single { ObserveLogEnabledUseCase(settings = get()) }
    single { SetLogEnabledUseCase(settings = get()) }
    single { ObserveSelectedTermUseCase(settings = get()) }
    single { SetSelectedTermUseCase(settings = get()) }
    single { ObserveCourseReminderEnabledUseCase(settings = get()) }
    single { SetCourseReminderEnabledUseCase(settings = get()) }
    single { ObserveExamReminderEnabledUseCase(settings = get()) }
    single { SetExamReminderEnabledUseCase(settings = get()) }
    single { ShouldAutoSyncAndMarkUseCase(settings = get()) }
    single { GetCampusTimetableUseCase(timetable = get()) }
    single { BuildScheduleUiStateUseCase(getCampusTimetable = get()) }
    single { BuildHomeTodayScheduleUseCase() }
    single { BuildHomeClockSnapshotUseCase() }
    single { BuildHomeHeaderUiUseCase(buildHomeGreetingUseCase = get()) }
    single { BuildHomeGreetingUseCase() }
    single { BuildHomePeriodRowUiUseCase() }
    single {
        BuildHomePeriodRenderRowsUseCase(
            buildHomeTodayScheduleUseCase = get(),
            resolveCourseStatusUseCase = get(),
            buildHomePeriodRowUiUseCase = get(),
        )
    }
    single { BuildHomePeriodSectionsUseCase() }
    single { BuildHomeSurfaceUiUseCase() }
    single { ResolveCourseStatusUseCase() }
    single {
        RefreshCourseRemindersIfEnabledUseCase(
            settings = get(),
            getAllCoursesOnce = get(),
            courseReminder = get()
        )
    }
    single { ScheduleNextCourseReminderUseCase(getAllCoursesOnce = get(), courseReminder = get()) }
    single { ScheduleNextExamReminderUseCase(fetchExaminations = get(), examReminder = get()) }
    single { CancelAllCourseRemindersUseCase(courseReminder = get()) }
    single { CancelAllExamRemindersUseCase(examReminder = get()) }
    single { IsAnyReminderEnabledUseCase(settings = get()) }
    single { IsExamNotEndedUseCase() }
    single {
        RescheduleNextCourseReminderIfEnabledUseCase(
            settings = get(),
            timetable = get(),
            getAllCoursesOnce = get(),
            courseReminder = get(),
        )
    }
    single {
        RescheduleNextExamReminderIfEnabledUseCase(
            settings = get(),
            fetchExaminations = get(),
            examReminder = get(),
        )
    }
    single {
        RescheduleRemindersUseCase(
            settings = get(),
            courseRepository = get(),
            timetable = get(),
            courseReminder = get(),
            examReminder = get(),
            academic = get(),
            authWorkflow = get(),
        )
    }
    single { ObserveBackgroundImageUriUseCase(backgroundSettings = get()) }
    single { SetBackgroundImageUriUseCase(backgroundSettings = get()) }
    single { ObserveBackgroundAlphaUseCase(backgroundSettings = get()) }
    single { SetBackgroundAlphaUseCase(backgroundSettings = get()) }
    single { ObserveBackgroundBlurUseCase(backgroundSettings = get()) }
    single { SetBackgroundBlurUseCase(backgroundSettings = get()) }
    single { ObserveComponentsAlphaUseCase(backgroundSettings = get()) }
    single { SetComponentsAlphaUseCase(backgroundSettings = get()) }

    single { SaveLabCourseUseCase(courseRepository = get()) }
    single { DeleteCourseUseCase(courseRepository = get()) }
    single { InsertCourseUseCase(courseRepository = get()) }
    single { BindUnboundDataUseCase(auth = get(), courseRepository = get(), examinationRepository = get()) }
    single { ObserveAllCoursesUseCase(courseRepository = get()) }
    single { ObserveCourseByIdUseCase(courseRepository = get()) }
    single { GetAllCoursesOnceUseCase(courseRepository = get()) }
    single { TransCourseUseCase() }
    single { TransCourseWithConflictsUseCase(getAllCoursesOnce = get(), transCourse = get()) }
    single { restarhalf.stellar.schedule.domain.usecase.PEUseCase(repository = get(), peAuth = get(), roomRepository = get()) }
}

/**
 * ViewModel模块
 * 
 * 注册所有ViewModel，每个ViewModel对应一个页面或功能模块。
 * ViewModel负责管理UI状态和处理用户交互。
 */
val viewModelModule = module {
    factory {
        AppViewModel(
            clearAuth = get(),
            getCampusUseCase = get(),
            observeCampusUseCase = get(),
            setCampusUseCase = get(),
            getTermStartMsUseCase = get(),
            observeTermStartMsUseCase = get(),
            setTermStartMsUseCase = get(),
            getTotalWeeksUseCase = get(),
            observeTotalWeeksUseCase = get(),
            setTotalWeeksUseCase = get(),
            fetchExaminations = get(),
            fetchGrades = get(),
            loginUseCase = get(),
            runSyncUseCase = get(),
            observeLogEnabled = get(),
            bindUnboundData = get(),
        )
    }
    factory {
        BackgroundViewModel(
            observeBackgroundImageUri = get(),
            observeBackgroundAlpha = get(),
            observeBackgroundBlur = get(),
            observeComponentsAlpha = get(),
            setBackgroundImageUriUseCase = get(),
            setBackgroundAlphaUseCase = get(),
            setBackgroundBlurUseCase = get(),
            setComponentsAlphaUseCase = get(),
        )
    }
    factory {
        CourseEditViewModel(
            observeAllCoursesUseCase = get(),
            observeCourseByIdUseCase = get(),
            saveLabCourseUseCase = get(),
            deleteCourseUseCase = get(),
            observeAuthProfile = get(),
        )
    }
    factory {
        ScheduleViewModel(
            observeShowNonCurrentWeek = get(),
            setShowNonCurrentWeekUseCase = get(),
            observeAllCoursesUseCase = get(),
            buildScheduleUiStateUseCase = get(),
            transCourseWithConflicts = get(),
            insertCourseUseCase = get(),
            deleteCourseUseCase = get(),
            shouldAutoSyncAndMark = get(),
            refreshCourseRemindersIfEnabledUseCase = get(),
        )
    }
    factory {
        SettingsViewModel(
            observeShowNonCurrentWeek = get(),
            setShowNonCurrentWeekUseCase = get(),
            observeCourseReminderEnabled = get(),
            setCourseReminderEnabled = get(),
            cancelAllCourseReminders = get(),
            observeExamReminderEnabled = get(),
            setExamReminderEnabled = get(),
            cancelAllExamReminders = get(),
            observeThemeMode = get(),
            setThemeModeUseCase = get(),
            observeFloatingBar = get(),
            setFloatingBarUseCase = get(),
            observeSelectedTerm = get(),
            setSelectedTermUseCase = get(),
            observeLogEnabled = get(),
            setLogEnabledUseCase = get(),
            observeAuthToken = get(),
            observeAuthProfile = get(),
            ensureLoggedIn = get(),
            fetchSemesterIds = get(),
            scheduleNextCourseReminder = get(),
            scheduleNextExamReminder = get(),
        )
    }
    factory {
        ExaminationViewModel(
            isExamNotEnded = get(),
            observeAllExaminations = get(),
            observeAuthProfile = get(),
        )
    }
    factory {
        ExamEditViewModel(
            observeAllCoursesUseCase = get(),
            observeExaminationByIdUseCase = get(),
            saveExaminationUseCase = get(),
            deleteExaminationUseCase = get(),
            observeSelectedTerm = get(),
            observeAuthProfile = get(),
        )
    }
    factory {
        GradeViewModel(
            observeAllGrades = get(),
            observeSelectedTerm = get()
        )
    }
    factory {
        HomeViewModel(
            observeAllCoursesUseCase = get(),
            getCampusTimetableUseCase = get(),
            buildHomeClockSnapshotUseCase = get(),
            buildHomeTodayScheduleUseCase = get(),
            buildHomeHeaderUiUseCase = get(),
            buildHomePeriodSectionsUseCase = get(),
            buildHomePeriodRenderRowsUseCase = get(),
            buildHomeSurfaceUiUseCase = get(),
        )
    }
    factory { AboutViewModel(appUpdate = get()) }
    factory { ChangeBackgroundViewModel() }
    factory { restarhalf.stellar.schedule.ui.viewmodel.PEViewModel(peUseCase = get()) }
    factory {
        PapersViewModel(
            papersPort = get(),
            settingsPort = get(),
        )
    }
}

/**
 * 通用应用模块
 * 
 * 组合所有子模块，作为应用的主要依赖注入配置。
 */
val commonAppModule = module {
    includes(portModule, useCaseModule, viewModelModule)
}
