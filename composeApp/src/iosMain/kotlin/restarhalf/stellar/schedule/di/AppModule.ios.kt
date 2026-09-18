package restarhalf.stellar.schedule.di

import com.russhwolf.settings.ObservableSettings
import org.koin.core.qualifier.named
import org.koin.dsl.module
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
import restarhalf.stellar.schedule.data.local.mmkv.MmkvSettingsFactory
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.CourseSelectionServicePort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderSchedulerPort
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPort
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPortImpl
import restarhalf.stellar.schedule.reminder.impl.CourseReminderPortImpl
import restarhalf.stellar.schedule.reminder.impl.ExamReminderPortImpl
import restarhalf.stellar.schedule.reminder.impl.ReminderSchedulerPortImpl
import restarhalf.stellar.schedule.ui.impl.AppInfoPortImpl
import restarhalf.stellar.schedule.ui.impl.ScreenTunerPortImpl
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.port.ScreenTunerPort

private val iosPlatformModule = module {
    single { MmkvSettingsFactory() }

    single<ObservableSettings>(named(SettingsKeys.PREFS_NAME)) {
        get<MmkvSettingsFactory>().create(SettingsKeys.PREFS_NAME)
    }
    single<ObservableSettings>(named("jwxt_auth")) { get<MmkvSettingsFactory>().create("jwxt_auth") }
    single<ObservableSettings>(named("pe_auth")) { get<MmkvSettingsFactory>().create("pe_auth") }
    single<ObservableSettings>(named("reminder_codes")) { get<MmkvSettingsFactory>().create("reminder_codes") }
    single<ObservableSettings>(named("timetable_prefs")) { get<MmkvSettingsFactory>().create("timetable_prefs") }

    single<AppDatabase> { buildPlatformAppDatabase() }
    single<CourseDao> { get<AppDatabase>().courseDao() }
    single<ExaminationDao> { get<AppDatabase>().examinationDao() }
    single<GradeDao> { get<AppDatabase>().gradeDao() }
    single<PEYearScoreDao> { get<AppDatabase>().peYearScoreDao() }
    single<PEDetailDao> { get<AppDatabase>().peDetailDao() }

    single<PictureSelectorPort> { PictureSelectorPortImpl() }
    single<AppInfoPort> { AppInfoPortImpl() }
    single<ScreenTunerPort> { ScreenTunerPortImpl() }
    single<CourseReminderPort> { CourseReminderPortImpl(settings = get(named("reminder_codes"))) }
    single<ExamReminderPort> { ExamReminderPortImpl(settings = get(named("reminder_codes"))) }
    single<ReminderSchedulerPort> { ReminderSchedulerPortImpl(rescheduleReminders = get()) }
    single<AppUpdatePort> { AppUpdatePortImpl() }
    single<CourseSelectionServicePort> { CourseSelectionServicePortImpl() }
}

val appModule = module {
    includes(commonAppModule, iosPlatformModule)
}
