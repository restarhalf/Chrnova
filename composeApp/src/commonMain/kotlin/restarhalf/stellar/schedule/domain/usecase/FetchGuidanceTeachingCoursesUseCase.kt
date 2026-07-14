package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.GuidanceTeachingCourse
import restarhalf.stellar.schedule.domain.port.AcademicPort

class FetchGuidanceTeachingCoursesUseCase(
    private val academic: AcademicPort,
) {
    suspend operator fun invoke(
        kcxz: String,
        kcsx: String = "",
        kcmc: String = "",
    ): List<GuidanceTeachingCourse> =
        academic.fetchGuidanceTeachingCourses(kcxz = kcxz, kcsx = kcsx, kcmc = kcmc)
}
