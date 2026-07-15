package restarhalf.stellar.schedule.di

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
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
import restarhalf.stellar.schedule.data.impl.PasswordEncryptionPortImpl
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
import restarhalf.stellar.schedule.data.remote.PapersApi
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
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.PasswordEncryptionPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import restarhalf.stellar.schedule.domain.usecase.BindUnboundDataUseCase
import restarhalf.stellar.schedule.domain.usecase.CalculateElectiveCreditsUseCase
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
import restarhalf.stellar.schedule.domain.usecase.CheckAppUpdateUseCase
import restarhalf.stellar.schedule.domain.usecase.DeleteExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.domain.usecase.IsAnyReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.LoginUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllGradesUseCase
import restarhalf.stellar.schedule.domain.usecase.PELoginUseCase
import restarhalf.stellar.schedule.domain.usecase.PELogoutUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreListUseCase
import restarhalf.stellar.schedule.domain.usecase.PEStudentInfoUseCase
import restarhalf.stellar.schedule.domain.usecase.RefreshCourseRemindersIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextCourseReminderIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextExamReminderIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.ResolveCourseStatusUseCase
import restarhalf.stellar.schedule.domain.usecase.RunSyncUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextCourseReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextExamReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseWithConflictsUseCase
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase
import restarhalf.stellar.schedule.ui.viewmodel.AboutViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ElectiveCreditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExamEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.HomeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel

/**
 * 端口实现模块
 * 
 * 注册所有端口接口的实现类，包括：
 * - HTTP客户端配置
 * - 教务系统网关
 * - 数据存储仓库
 * - 端口实现（认证、设置、同步等）
 */
private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

/** 为 HttpClient 安装公共模块：JSON 序列化 + 超时配置 */
private fun HttpClientConfig<*>.installCommonModules(json: Json) {
    install(ContentNegotiation) { json(json) }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
}

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
            installCommonModules(get())
            install(JwxtAuthPlugin) {
                authStore = get()
            }
            defaultRequest {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header("Referer", "http://jwyd.dlnu.edu.cn/sjd/#/login")
            }
        }
    }

    // 体育系统HTTP客户端
    single(named("pe")) {
        HttpClient {
            installCommonModules(get())
            defaultRequest {
                header(HttpHeaders.UserAgent, USER_AGENT)
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
            installCommonModules(get())
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
    single<AcademicPort> { AcademicPortImpl(gateway = get(), settings = get()) }
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
    factory {
        RunSyncUseCase(
            authWorkflow = get(),
            academic = get(),
            timetable = get(),
            settings = get(),
            sync = get(),
            reminderScheduler = get(),
        )
    }
    factory {
        FetchExaminationsUseCase(
            authWorkflow = get(),
            academic = get(),
            repository = get(),
            auth = get(),
            settings = get()
        )
    }
    factory { FetchExaminationsSimpleUseCase(fetchExaminations = get()) }
    factory {
        FetchGradesUseCase(
            authWorkflow = get(),
            academic = get(),
            settings = get(),
            repository = get(),
            auth = get()
        )
    }
    factory { FetchGradesSimpleUseCase(fetchGrades = get()) }
    factory { ObserveAllExaminationsUseCase(repository = get(), auth = get()) }
    factory { SaveExaminationUseCase(repository = get()) }
    factory { DeleteExaminationUseCase(repository = get()) }
    factory { ObserveAllGradesUseCase(repository = get(), auth = get()) }
    factory { FetchSemesterIdsUseCase(authWorkflow = get(), academic = get(), settings = get()) }
    factory { LoginUseCase(authWorkflow = get()) }
    factory { CalculateElectiveCreditsUseCase() }
    factory { BuildScheduleUiStateUseCase(timetable = get()) }
    factory { BuildHomeTodayScheduleUseCase() }
    factory { BuildHomeClockSnapshotUseCase() }
    factory { BuildHomeHeaderUiUseCase(buildHomeGreetingUseCase = get()) }
    factory { BuildHomeGreetingUseCase() }
    factory { BuildHomePeriodRowUiUseCase() }
    factory {
        BuildHomePeriodRenderRowsUseCase(
            buildHomeTodayScheduleUseCase = get(),
            resolveCourseStatusUseCase = get(),
            buildHomePeriodRowUiUseCase = get(),
        )
    }
    factory { BuildHomePeriodSectionsUseCase() }
    factory { BuildHomeSurfaceUiUseCase() }
    factory { ResolveCourseStatusUseCase() }
    factory {
        RefreshCourseRemindersIfEnabledUseCase(
            settings = get(),
            courseRepository = get(),
            courseReminder = get()
        )
    }
    factory { ScheduleNextCourseReminderUseCase(courseRepository = get(), courseReminder = get()) }
    factory { ScheduleNextExamReminderUseCase(fetchExaminations = get(), examReminder = get()) }
    factory { CancelAllCourseRemindersUseCase(courseReminder = get()) }
    factory { CancelAllExamRemindersUseCase(examReminder = get()) }
    factory { IsAnyReminderEnabledUseCase(settings = get()) }
    factory { IsExamNotEndedUseCase() }
    factory {
        RescheduleNextCourseReminderIfEnabledUseCase(
            settings = get(),
            timetable = get(),
            courseRepository = get(),
            courseReminder = get(),
        )
    }
    factory {
        RescheduleNextExamReminderIfEnabledUseCase(
            settings = get(),
            fetchExaminations = get(),
            examReminder = get(),
        )
    }
    factory {
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

    factory {
        BindUnboundDataUseCase(
            auth = get(),
            courseRepository = get(),
            examinationRepository = get(),
            academic = get()
        )
    }
    factory { TransCourseUseCase() }
    factory { TransCourseWithConflictsUseCase(courseRepository = get(), transCourse = get()) }
    factory { PELoginUseCase(repository = get(), peAuth = get()) }
    factory { PELogoutUseCase(peAuth = get(), roomRepository = get()) }
    factory { PEScoreListUseCase(repository = get(), peAuth = get(), roomRepository = get()) }
    factory { PEScoreDetailUseCase(repository = get(), peAuth = get(), roomRepository = get()) }
    factory { PEStudentInfoUseCase(repository = get(), peAuth = get(), roomRepository = get()) }
    factory { VerifyGitHubStarUseCase(papersPort = get(), settingsPort = get()) }
    factory { CheckAppUpdateUseCase(appUpdate = get()) }
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
            auth = get(),
            timetable = get(),
            settings = get(),
            fetchExaminations = get(),
            fetchGrades = get(),
            loginUseCase = get(),
            runSyncUseCase = get(),
            bindUnboundData = get(),
        )
    }
    factory {
        BackgroundViewModel(
            backgroundSettings = get(),
        )
    }
    factory {
        CourseEditViewModel(
            courseRepository = get(),
            auth = get(),
        )
    }
    factory {
        ScheduleViewModel(
            settings = get(),
            courseRepository = get(),
            buildScheduleUiStateUseCase = get(),
            transCourseWithConflicts = get(),
            refreshCourseRemindersIfEnabledUseCase = get(),
        )
    }
    factory {
        SettingsViewModel(
            auth = get(),
            authWorkflow = get(),
            settings = get(),
            cancelAllCourseReminders = get(),
            cancelAllExamReminders = get(),
            fetchSemesterIds = get(),
            scheduleNextCourseReminder = get(),
            scheduleNextExamReminder = get(),
        )
    }
    factory {
        ExaminationViewModel(
            isExamNotEnded = get(),
            observeAllExaminations = get(),
            auth = get(),
            settings = get(),
        )
    }
    factory {
        ExamEditViewModel(
            courseRepository = get(),
            examinationRepository = get(),
            auth = get(),
            settings = get(),
            academic = get(),
            saveExaminationUseCase = get(),
            deleteExaminationUseCase = get(),
        )
    }
    factory {
        GradeViewModel(
            observeAllGrades = get(),
            settings = get(),
        )
    }
    factory {
        HomeViewModel(
            courseRepository = get(),
            observeAllExaminations = get(),
            auth = get(),
            isExamNotEnded = get(),
            timetable = get(),
            buildHomeClockSnapshotUseCase = get(),
            buildHomeTodayScheduleUseCase = get(),
            buildHomeHeaderUiUseCase = get(),
            buildHomePeriodSectionsUseCase = get(),
            buildHomePeriodRenderRowsUseCase = get(),
            buildHomeSurfaceUiUseCase = get(),
        )
    }
    factory { AboutViewModel(checkAppUpdate = get()) }
    factory {
        restarhalf.stellar.schedule.ui.viewmodel.PEViewModel(
            peLoginUseCase = get(),
            peLogoutUseCase = get(),
            peScoreListUseCase = get(),
            peScoreDetailUseCase = get(),
            peStudentInfoUseCase = get(),
            peAuth = get(),
        )
    }
    factory {
        restarhalf.stellar.schedule.ui.viewmodel.JWLoginViewModel(
            loginUseCase = get(),
        )
    }
    factory {
        restarhalf.stellar.schedule.ui.viewmodel.PELoginViewModel(
            peLoginUseCase = get(),
        )
    }
    factory {
        PapersViewModel(
            papersPort = get(),
            settings = get(),
            verifyGitHubStar = get(),
        )
    }
    factory {
        ElectiveCreditViewModel(
            authWorkflow = get(),
            academic = get(),
            auth = get(),
            gradeRepository = get(),
            fetchSemesterIds = get(),
            calculateElectiveCredits = get(),
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
