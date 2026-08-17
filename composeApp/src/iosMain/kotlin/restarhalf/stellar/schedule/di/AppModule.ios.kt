package restarhalf.stellar.schedule.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import org.koin.core.qualifier.named
import org.koin.dsl.module
import restarhalf.stellar.schedule.calendar.CalendarEventPortImpl
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.data.impl.AppUpdatePortImpl
import restarhalf.stellar.schedule.data.impl.CourseSelectionServicePortImpl
import restarhalf.stellar.schedule.data.local.AppDatabase
import restarhalf.stellar.schedule.data.local.dao.CourseDao
import restarhalf.stellar.schedule.data.local.dao.ExaminationDao
import restarhalf.stellar.schedule.data.local.dao.GradeDao
import restarhalf.stellar.schedule.data.local.dao.PEDetailDao
import restarhalf.stellar.schedule.data.local.dao.PEYearScoreDao
import restarhalf.stellar.schedule.data.local.buildPlatformAppDatabase
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import restarhalf.stellar.schedule.domain.port.CourseSelectionServicePort
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPort
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPortImpl
import restarhalf.stellar.schedule.ui.impl.AppInfoPortImpl
import restarhalf.stellar.schedule.ui.port.AppInfoPort

private val iosPlatformModule = module {
    single { NSUserDefaultsSettings.Factory() }

    single<ObservableSettings>(named(SettingsKeys.PREFS_NAME)) {
        get<NSUserDefaultsSettings.Factory>().create(
            SettingsKeys.PREFS_NAME
        )
    }
    single<ObservableSettings>(named("jwxt_auth")) { get<NSUserDefaultsSettings.Factory>().create("jwxt_auth") }
    single<ObservableSettings>(named("pe_auth")) { get<NSUserDefaultsSettings.Factory>().create("pe_auth") }
    single<ObservableSettings>(named("calendar_codes")) {
        get<NSUserDefaultsSettings.Factory>().create("calendar_codes")
    }
    single<ObservableSettings>(named("timetable_prefs")) {
        get<NSUserDefaultsSettings.Factory>().create(
            "timetable_prefs"
        )
    }

    single<AppDatabase> { buildPlatformAppDatabase() }
    single<CourseDao> { get<AppDatabase>().courseDao() }
    single<ExaminationDao> { get<AppDatabase>().examinationDao() }
    single<GradeDao> { get<AppDatabase>().gradeDao() }
    single<PEDetailDao> { get<AppDatabase>().peDetailDao() }
    single<PEYearScoreDao> { get<AppDatabase>().peYearScoreDao() }

    single<PictureSelectorPort> { PictureSelectorPortImpl() }
    single<AppInfoPort> { AppInfoPortImpl() }
    single<CalendarEventPort> { CalendarEventPortImpl(prefs = get(named("calendar_codes"))) }
    single<AppUpdatePort> { AppUpdatePortImpl() }
    single<CourseSelectionServicePort> { CourseSelectionServicePortImpl() }
}

val appModule = module {
    includes(commonAppModule, iosPlatformModule)
}
