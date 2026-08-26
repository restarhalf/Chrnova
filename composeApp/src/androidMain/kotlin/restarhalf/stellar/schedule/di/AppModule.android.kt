package restarhalf.stellar.schedule.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
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
import restarhalf.stellar.schedule.ui.impl.ScreenTunerPortImpl
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.port.ScreenTunerPort

private val androidPlatformModule = module {
    single<PictureSelectorPort> { PictureSelectorPortImpl(androidContext()) }
    single<AppInfoPort> { AppInfoPortImpl(androidContext()) }
    single<ScreenTunerPort> { ScreenTunerPortImpl(androidContext()) }

    single<AppDatabase> { buildPlatformAppDatabase(androidContext()) }
    single<CourseDao> { get<AppDatabase>().courseDao() }
    single<ExaminationDao> { get<AppDatabase>().examinationDao() }
    single<GradeDao> { get<AppDatabase>().gradeDao() }
    single<PEYearScoreDao> { get<AppDatabase>().peYearScoreDao() }
    single<PEDetailDao> { get<AppDatabase>().peDetailDao() }

    single<ObservableSettings>(named("calendar_codes")) {
        SharedPreferencesSettings.Factory(androidContext()).create("calendar_codes")
    }
    single<ObservableSettings>(named(SettingsKeys.PREFS_NAME)) {
        SharedPreferencesSettings.Factory(androidContext()).create(SettingsKeys.PREFS_NAME)
    }
    single<ObservableSettings>(named("jwxt_auth")) {
        SharedPreferencesSettings.Factory(androidContext()).create("jwxt_auth")
    }
    single<ObservableSettings>(named("pe_auth")) {
        SharedPreferencesSettings.Factory(androidContext()).create("pe_auth")
    }
    single<ObservableSettings>(named("timetable_prefs")) {
        SharedPreferencesSettings.Factory(androidContext()).create("timetable_prefs")
    }

    single<CalendarEventPort> {
        CalendarEventPortImpl(context = androidContext(), prefs = get(named("calendar_codes")))
    }
    single<AppUpdatePort> { AppUpdatePortImpl(context = androidContext()) }
    single<CourseSelectionServicePort> {
        CourseSelectionServicePortImpl(context = androidContext())
    }
}

val appModule = module {
    includes(commonAppModule, androidPlatformModule)
}
