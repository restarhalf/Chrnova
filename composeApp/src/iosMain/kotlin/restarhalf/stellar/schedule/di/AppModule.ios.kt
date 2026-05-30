package restarhalf.stellar.schedule.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import org.koin.core.qualifier.named
import org.koin.dsl.module
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.data.impl.AppUpdatePortImpl
import restarhalf.stellar.schedule.data.impl.PasswordEncryptionPortImpl
import restarhalf.stellar.schedule.data.local.AppDatabase
import restarhalf.stellar.schedule.data.local.CourseDao
import restarhalf.stellar.schedule.data.local.ExaminationDao
import restarhalf.stellar.schedule.data.local.GradeDao
import restarhalf.stellar.schedule.data.local.buildPlatformAppDatabase
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.PasswordEncryptionPort
import restarhalf.stellar.schedule.domain.port.ReminderSchedulerPort
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPort
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPortImpl
import restarhalf.stellar.schedule.reminder.impl.CourseReminderPortImpl
import restarhalf.stellar.schedule.reminder.impl.ExamReminderPortImpl
import restarhalf.stellar.schedule.reminder.impl.ReminderSchedulerPortImpl
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
    single<ObservableSettings>(named("reminder_codes")) {
        get<NSUserDefaultsSettings.Factory>().create("reminder_codes")
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

    single<PasswordEncryptionPort> { PasswordEncryptionPortImpl() }
    single<PictureSelectorPort> { PictureSelectorPortImpl() }
    single<AppInfoPort> { AppInfoPortImpl() }
    single<CourseReminderPort> { CourseReminderPortImpl(settings = get(named("reminder_codes"))) }
    single<ExamReminderPort> { ExamReminderPortImpl(settings = get(named("reminder_codes"))) }
    single<ReminderSchedulerPort> { ReminderSchedulerPortImpl(rescheduleReminders = get()) }
    single<AppUpdatePort> { AppUpdatePortImpl() }
}

val appModule = module {
    includes(commonAppModule, iosPlatformModule)
}
