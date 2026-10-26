package restarhalf.stellar.schedule.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import restarhalf.stellar.schedule.ui.viewmodel.AboutViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseEnrollViewModel
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ElectiveCreditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExamEditViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.HomeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.JwxtLoginViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PELoginViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PEAppointmentViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PEFreeApplyViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PersonalInfoViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel

val viewModelModule = module {
    viewModel {
        AppViewModel(
            auth = get(),
            timetable = get(),
            settings = get(),
            fetchExaminations = get(),
            fetchGrades = get(),
            jwxtLoginUseCase = get(),
            runSyncUseCase = get(),
            bindUnboundData = get(),
        )
    }
    viewModel {
        BackgroundViewModel(
            backgroundSettings = get(),
        )
    }
    viewModel {
        CourseEditViewModel(
            observeAllCourses = get(),
            observeCourseById = get(),
            saveCourse = get(),
            deleteCourseUseCase = get(),
            auth = get(),
            transCourseWithConflicts = get(),
            saveTransCourses = get(),
        )
    }
    viewModel {
        ScheduleViewModel(
            settings = get(),
            termSettings = get(),
            observeAllCourses = get(),
            saveCourse = get(),
            deleteCourseUseCase = get(),
            buildScheduleUiStateUseCase = get(),
            transCourseWithConflicts = get(),
            refreshCourseRemindersIfEnabledUseCase = get(),
        )
    }
    viewModel {
        SettingsViewModel(
            auth = get(),
            authWorkflow = get(),
            displaySettings = get(),
            reminderSettings = get(),
            termSettings = get(),
            logSettings = get(),
            userProfile = get(),
            deviceSettings = get(),
            cancelAllCourseReminders = get(),
            cancelAllExamReminders = get(),
            fetchSemesterIds = get(),
            scheduleNextCourseReminder = get(),
            scheduleNextExamReminder = get(),
            verifyGitHubStar = get(),
        )
    }
    viewModel {
        ExaminationViewModel(
            isExamNotEnded = get(),
            observeAllExaminations = get(),
            auth = get(),
            settings = get(),
            rescheduleNextExamReminderIfEnabled = get(),
        )
    }
    viewModel {
        ExamEditViewModel(
            observeAllCourses = get(),
            observeExaminationById = get(),
            auth = get(),
            settings = get(),
            academic = get(),
            saveExaminationUseCase = get(),
            deleteExaminationUseCase = get(),
        )
    }
    viewModel {
        GradeViewModel(
            observeAllGrades = get(),
            settings = get(),
        )
    }
    viewModel {
        HomeViewModel(
            observeAllCourses = get(),
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
    viewModel { AboutViewModel(checkAppUpdate = get()) }
    viewModel {
        PEViewModel(
            peScoreListUseCase = get(),
            peScoreDetailUseCase = get(),
            peAuthProfileUseCase = get(),
            peAuth = get(),
            peAuthWorkflow = get(),
            peSubjectScoreHistoryUseCase = get(),
        )
    }
    viewModel {
        PEAppointmentViewModel(
            appointmentListUseCase = get(),
            appointmentDetailUseCase = get(),
            cancelAppointmentUseCase = get(),
            enterAppointmentUseCase = get(),
            peAuth = get(),
        )
    }
    viewModel {
        PEFreeApplyViewModel(
            useCase = get(),
            peAuth = get(),
        )
    }
    viewModel {
        JwxtLoginViewModel(
            jwxtLoginUseCase = get(),
        )
    }
    viewModel {
        PELoginViewModel(
            peLoginUseCase = get(),
        )
    }
    viewModel {
        PapersViewModel(
            papersPort = get(),
            settings = get(),
            verifyGitHubStar = get(),
        )
    }
    viewModel {
        CourseEvaluationViewModel(
            port = get(),
            getAllCoursesAcrossSemesters = get(),
            auth = get(),
            settings = get(),
        )
    }
    viewModel {
        ElectiveCreditViewModel(
            authWorkflow = get(),
            academic = get(),
            auth = get(),
            getGradesByUserNo = get(),
            fetchSemesterIds = get(),
            calculateElectiveCredits = get(),
        )
    }
    viewModel {
        PersonalInfoViewModel(
            settingsPort = get(),
        )
    }
    viewModel {
        CourseSelectionViewModel(
            useCase = get(),
            servicePort = get(),
        )
    }
    viewModel {
        CourseEnrollViewModel(
            useCase = get(),
        )
    }
    viewModel {
        AnnouncementViewModel(
            fetchAnnouncements = get(),
            markAnnouncementsRead = get(),
            fetchAdConfig = get(),
            fetchAnnouncement = get(),
        )
    }
}
