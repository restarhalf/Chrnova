package restarhalf.stellar.schedule.di

import org.koin.dsl.module
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.domain.port.CourseEvaluationPort
import restarhalf.stellar.schedule.domain.usecase.BindUnboundDataUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeClockSnapshotUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeGreetingUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeHeaderUiUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomePeriodRenderRowsUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomePeriodRowUiUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomePeriodSectionsUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeSurfaceUiUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeTodayScheduleUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildScheduleUiStateUseCase
import restarhalf.stellar.schedule.domain.usecase.CalculateElectiveCreditsUseCase
import restarhalf.stellar.schedule.domain.usecase.CheckAppUpdateUseCase
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import restarhalf.stellar.schedule.domain.usecase.DeleteCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.DeleteExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.GetAllCoursesAcrossSemestersUseCase
import restarhalf.stellar.schedule.domain.usecase.GetGradesByUserNoUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveCourseByIdUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveExaminationByIdUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveTransCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.CollectReminderExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAdConfigUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.JwxtLoginUseCase
import restarhalf.stellar.schedule.domain.usecase.MarkAnnouncementsReadUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllGradesUseCase
import restarhalf.stellar.schedule.domain.usecase.PELoginUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PESubjectScoreHistoryUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreListUseCase
import restarhalf.stellar.schedule.domain.usecase.PEAuthProfileUseCase
import restarhalf.stellar.schedule.domain.usecase.PEAppointmentListUseCase
import restarhalf.stellar.schedule.domain.usecase.PEAppointmentDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PECancelAppointmentUseCase
import restarhalf.stellar.schedule.domain.usecase.PEEnterAppointmentUseCase
import restarhalf.stellar.schedule.domain.usecase.PEFreeApplyUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllCourseRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllExamRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.IsAnyReminderEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RefreshCourseRemindersIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleAllRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextCourseReminderIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextExamReminderIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.RescheduleRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextCourseReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextExamReminderUseCase
import restarhalf.stellar.schedule.ui.model.home.ResolveCourseStatusUseCase
import restarhalf.stellar.schedule.domain.usecase.RunSyncUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseBatchUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseWithConflictsUseCase
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase

val useCaseModule = module {

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
    factory { ObserveAllCoursesUseCase(repository = get()) }
    factory { ObserveCourseByIdUseCase(repository = get()) }
    factory { GetAllCoursesAcrossSemestersUseCase(repository = get()) }
    factory { SaveCourseUseCase(repository = get()) }
    factory { DeleteCourseUseCase(repository = get()) }
    factory { GetGradesByUserNoUseCase(repository = get()) }
    factory { ObserveExaminationByIdUseCase(repository = get()) }
    factory { ObserveAllExaminationsUseCase(repository = get(), auth = get()) }
    factory { SaveExaminationUseCase(repository = get()) }
    factory { DeleteExaminationUseCase(repository = get()) }
    factory { ObserveAllGradesUseCase(repository = get(), auth = get()) }
    factory { FetchSemesterIdsUseCase(authWorkflow = get(), academic = get(), settings = get()) }
    factory { JwxtLoginUseCase(authWorkflow = get()) }
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
            courseReminder = get(),
        )
    }
    factory { ScheduleNextCourseReminderUseCase(courseRepository = get(), courseReminder = get()) }
    factory {
        CollectReminderExaminationsUseCase(
            fetchExaminations = get(),
            repository = get(),
        )
    }
    factory {
        ScheduleNextExamReminderUseCase(
            collectReminderExaminations = get(),
            examReminder = get(),
        )
    }
    factory {
        RescheduleAllRemindersUseCase(
            settings = get(),
            courseRepository = get(),
            timetable = get(),
            courseReminder = get(),
            examReminder = get(),
            collectReminderExaminations = get(),
            cancelAllCourseReminders = get(),
            cancelAllExamReminders = get(),
        )
    }
    factory { CancelAllCourseRemindersUseCase(courseReminder = get()) }
    factory { CancelAllExamRemindersUseCase(examReminder = get()) }
    factory { IsAnyReminderEnabledUseCase(settings = get()) }
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
            collectReminderExaminations = get(),
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
            collectReminderExaminations = get(),
        )
    }
    factory { IsExamNotEndedUseCase() }

    factory {
        BindUnboundDataUseCase(
            auth = get(),
            courseRepository = get(),
            examinationRepository = get(),
            academic = get()
        )
    }
    factory { TransCourseUseCase() }
    factory { TransCourseBatchUseCase(transCourse = get()) }
    factory { SaveTransCoursesUseCase(repository = get()) }
    factory {
        TransCourseWithConflictsUseCase(
            courseRepository = get(),
            transCourse = get(),
            transCourseBatch = get(),
        )
    }
    factory { PELoginUseCase(peAuthWorkflow = get()) }
    factory { PEScoreListUseCase(gateway = get(), authWorkflow = get(), repository = get()) }
    factory { PEScoreDetailUseCase(gateway = get(), authWorkflow = get(), repository = get()) }
    factory { PESubjectScoreHistoryUseCase(gateway = get(), authWorkflow = get()) }
    factory { PEAuthProfileUseCase(gateway = get(), auth = get(), authWorkflow = get()) }
    factory { PEAppointmentListUseCase(gateway = get(), authWorkflow = get()) }
    factory { PEAppointmentDetailUseCase(gateway = get(), authWorkflow = get()) }
    factory { PECancelAppointmentUseCase(gateway = get(), authWorkflow = get()) }
    factory { PEEnterAppointmentUseCase(gateway = get(), authWorkflow = get()) }
    factory { PEFreeApplyUseCase(gateway = get(), authWorkflow = get()) }
    factory { VerifyGitHubStarUseCase(papersPort = get(), settingsPort = get()) }
    factory {
        CheckAppUpdateUseCase(
            appUpdate = get(),
            getGrayUid = {
                val store = get<JwxtAuthStore>()
                val userNo = store.getUserNo()
                    ?: store.getLastUserNo()
                    ?: store.getCredentials()?.first
                userNo?.takeIf { it.isNotBlank() }
                    ?.let { CourseEvaluationPort.hashUserNo(it) }
                    ?.takeIf { it.isNotBlank() }
            },
        )
    }
    factory {
        CourseSelectionUseCase(
            gateway = get(),
            authWorkflow = get(),
        )
    }
    factory { FetchAnnouncementsUseCase(port = get(), store = get()) }
    factory { FetchAdConfigUseCase(port = get()) }
    factory { FetchAnnouncementUseCase(port = get()) }
    factory { MarkAnnouncementsReadUseCase(store = get()) }
}
