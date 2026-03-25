package restarhalf.stellar.schedule.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.data.impl.AppUpdatePortImpl
import restarhalf.stellar.schedule.data.impl.PasswordEncryptionPortImpl
import restarhalf.stellar.schedule.data.local.AppDatabase
import restarhalf.stellar.schedule.data.local.CourseDao
import restarhalf.stellar.schedule.data.local.buildPlatformAppDatabase
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.PasswordEncryptionPort
import restarhalf.stellar.schedule.domain.port.ReminderSchedulerPort
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPort
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPortImpl
import restarhalf.stellar.schedule.reminder.CourseReminderScheduler
import restarhalf.stellar.schedule.reminder.ExamReminderScheduler
import restarhalf.stellar.schedule.reminder.impl.CourseReminderPortImpl
import restarhalf.stellar.schedule.reminder.impl.ExamReminderPortImpl
import restarhalf.stellar.schedule.reminder.impl.WorkManagerReminderSchedulerPortImpl
import restarhalf.stellar.schedule.ui.impl.AppInfoPortImpl
import restarhalf.stellar.schedule.ui.port.AppInfoPort

private val androidPlatformModule = module {
    single<PasswordEncryptionPort> { PasswordEncryptionPortImpl() }
    single<PictureSelectorPort> { PictureSelectorPortImpl(androidContext()) }
    single<AppInfoPort> { AppInfoPortImpl(androidContext()) }

    single<AppDatabase> { buildPlatformAppDatabase(androidContext()) }
    single<CourseDao> { get<AppDatabase>().courseDao() }

    single { CourseReminderScheduler(androidContext(), get(named("reminder_codes"))) }
    single { ExamReminderScheduler(androidContext()) }

    single<ObservableSettings>(named("reminder_codes")) {
        SharedPreferencesSettings.Factory(androidContext()).create("reminder_codes")
    }
    single<ObservableSettings>(named(SettingsKeys.PREFS_NAME)) {
        SharedPreferencesSettings.Factory(androidContext()).create(SettingsKeys.PREFS_NAME)
    }
    single<ObservableSettings>(named("jwxt_auth")) {
        SharedPreferencesSettings.Factory(androidContext()).create("jwxt_auth")
    }
    single<ObservableSettings>(named("timetable_prefs")) {
        SharedPreferencesSettings.Factory(androidContext()).create("timetable_prefs")
    }

    single<CourseReminderPort> { CourseReminderPortImpl(scheduler = get()) }
    single<ExamReminderPort> { ExamReminderPortImpl(scheduler = get()) }
    single<ReminderSchedulerPort> { WorkManagerReminderSchedulerPortImpl(context = androidContext()) }
    single<AppUpdatePort> { AppUpdatePortImpl(context = androidContext()) }
}

val appModule = module {
    includes(commonAppModule, androidPlatformModule)
}
