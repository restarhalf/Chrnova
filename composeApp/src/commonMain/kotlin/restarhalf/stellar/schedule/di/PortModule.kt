package restarhalf.stellar.schedule.di

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import restarhalf.stellar.schedule.data.impl.AcademicPortImpl
import restarhalf.stellar.schedule.data.impl.JwxtAuthPortImpl
import restarhalf.stellar.schedule.data.impl.JwxtAuthWorkflowPortImpl
import restarhalf.stellar.schedule.data.impl.BackgroundSettingsPortImpl
import restarhalf.stellar.schedule.data.impl.PEAuthPortImpl
import restarhalf.stellar.schedule.data.impl.PEAuthWorkflowPortImpl
import restarhalf.stellar.schedule.data.impl.PEPasswordEncryptionPortImpl
import restarhalf.stellar.schedule.data.impl.JwxtPasswordEncryptionPortImpl
import restarhalf.stellar.schedule.data.impl.settings.DeviceSettingsPortImpl
import restarhalf.stellar.schedule.data.impl.settings.DisplaySettingsPortImpl
import restarhalf.stellar.schedule.data.impl.settings.LogSettingsPortImpl
import restarhalf.stellar.schedule.data.impl.settings.ReminderSettingsPortImpl
import restarhalf.stellar.schedule.data.impl.settings.TermSettingsPortImpl
import restarhalf.stellar.schedule.data.impl.settings.UserProfilePortImpl
import restarhalf.stellar.schedule.data.impl.SyncPortImpl
import restarhalf.stellar.schedule.data.impl.TimetablePortImpl
import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.data.local.TimetableSettings
import restarhalf.stellar.schedule.data.remote.AnnouncementApi
import restarhalf.stellar.schedule.data.remote.CourseEvaluationApi
import restarhalf.stellar.schedule.data.remote.JwxtAuthPlugin
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.data.remote.JwxtClient
import restarhalf.stellar.schedule.data.remote.JwxtCourseSelectionAdapter
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtSync
import restarhalf.stellar.schedule.data.remote.PEAuthStore
import restarhalf.stellar.schedule.data.remote.PEClient
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.data.remote.PapersApi
import restarhalf.stellar.schedule.data.repository.RoomCourseRepository
import restarhalf.stellar.schedule.data.repository.RoomExaminationRepository
import restarhalf.stellar.schedule.data.repository.RoomGradeRepository
import restarhalf.stellar.schedule.data.repository.RoomPERepositoryImpl
import restarhalf.stellar.schedule.domain.repository.PERepository
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AnnouncementCachePort
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import restarhalf.stellar.schedule.domain.port.CourseSelectionPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import restarhalf.stellar.schedule.domain.port.CourseEvaluationPort
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PEPasswordEncryptionPort
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.JwxtPasswordEncryptionPort
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.port.LogSettingsPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.port.UserProfilePort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.repository.GradeRepository

private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"


private fun HttpClientConfig<*>.installCommonModules(json: Json) {
    install(ContentNegotiation) { json(json) }
    install(HttpCache)
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 10_000
        socketTimeoutMillis = 15_000
    }
}
val portModule = module {

    single { Json { ignoreUnknownKeys = true } }


    single { JwxtAuthStore(get(named("jwxt_auth"))) }

    single { PEAuthStore(get(named("pe_auth"))) }


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


    single(named("pe")) {
        HttpClient {
            installCommonModules(get())
            defaultRequest {
                header(HttpHeaders.UserAgent, USER_AGENT)
            }
        }
    }


    single<JwxtGateway> {
        JwxtClient(
            httpClient = get(named("jwxt")),
            json = get(),
            authStore = get(),
            passwordEncryption = get(),
        )
    }


    single<PEGateway> {
        PEClient(
            httpClient = get(named("pe")),
            json = get(),
            authStore = get(),
            passwordEncryption = get(),
        )
    }


    single<CourseSelectionPort> { JwxtCourseSelectionAdapter(gateway = get()) }


    single { JwxtSync(get()) }


    single { TimetableSettings(get(named("timetable_prefs"))) }


    single<CourseRepository> { RoomCourseRepository(courseDao = get(), settings = get(), auth = get()) }
    single<ExaminationRepository> { RoomExaminationRepository(examinationDao = get()) }
    single<GradeRepository> { RoomGradeRepository(gradeDao = get()) }
    single<PERepository> { RoomPERepositoryImpl(peYearScoreDao = get(), peDetailDao = get()) }


    single(named("papers")) {
        HttpClient {
            installCommonModules(get())
        }
    }


    single(named("evaluate")) {
        HttpClient {
            installCommonModules(get())
        }
    }


    single<DisplaySettingsPort> { DisplaySettingsPortImpl(settings = get(named(SettingsKeys.PREFS_NAME))) }
    single<ReminderSettingsPort> { ReminderSettingsPortImpl(settings = get(named(SettingsKeys.PREFS_NAME))) }
    single<TermSettingsPort> { TermSettingsPortImpl(settings = get(named(SettingsKeys.PREFS_NAME))) }
    single<DeviceSettingsPort> { DeviceSettingsPortImpl(settings = get(named(SettingsKeys.PREFS_NAME))) }
    single<LogSettingsPort> { LogSettingsPortImpl(settings = get(named(SettingsKeys.PREFS_NAME))) }
    single<UserProfilePort> { UserProfilePortImpl(settings = get(named(SettingsKeys.PREFS_NAME))) }
    single<JwxtPasswordEncryptionPort> { JwxtPasswordEncryptionPortImpl() }
    single<PEPasswordEncryptionPort> { PEPasswordEncryptionPortImpl() }
    single<BackgroundSettingsPort> {
        BackgroundSettingsPortImpl(settings = get(named(SettingsKeys.PREFS_NAME)))
    }
    single<JwxtAuthPort> { JwxtAuthPortImpl(authStore = get()) }
    single<AcademicPort> { AcademicPortImpl(gateway = get(), settings = get()) }
    single<JwxtAuthWorkflowPort> {
        JwxtAuthWorkflowPortImpl(
            gateway = get(),
            authStore = get(),
            courseRepository = get()
        )
    }
    single<PEAuthPort> { PEAuthPortImpl(authStore = get()) }
    single<PEAuthWorkflowPort> {
        PEAuthWorkflowPortImpl(
            gateway = get(),
            authStore = get(),
            repository = get()
        )
    }
    single<TimetablePort> { TimetablePortImpl(prefs = get()) }
    single<SyncPort> { SyncPortImpl(jwxtSync = get(), courseRepository = get(), auth = get()) }
    single<PapersPort> {
        PapersApi(
            httpClient = get(named("papers")),
            json = get(),
            baseUrl = "https://chrnova.paper.restarhalf.dpdns.org",
            getDeviceId = { get<DeviceSettingsPort>().getDeviceId() },
        )
    }
    single<CourseEvaluationPort> {
        CourseEvaluationApi(
            httpClient = get(named("evaluate")),
            json = get(),
            baseUrl = "https://chrnova.evaluate.restarhalf.dpdns.org",
            getDeviceId = { get<DeviceSettingsPort>().getDeviceId() },
            getUserHash = {
                val userNo = get<JwxtAuthStore>().getUserNo() ?: ""
                CourseEvaluationPort.hashUserNo(userNo)
            },
        )
    }


    single(named("announcement")) {
        HttpClient {
            installCommonModules(get())
        }
    }


    single<AnnouncementPort> {
        AnnouncementApi(
            httpClient = get(named("announcement")),
            json = get(),
            baseUrl = "https://chrnova.announcement.restarhalf.dpdns.org",
        )
    }
    single<AnnouncementCachePort> { AnnouncementStore(get(named(SettingsKeys.PREFS_NAME))) }
}
