package restarhalf.stellar.schedule.data.impl

import restarhalf.stellar.schedule.data.remote.JwxtSync
import restarhalf.stellar.schedule.data.remote.JwxtTimeParser
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class SyncPortImpl(
    private val jwxtSync: JwxtSync,
    private val courseRepository: CourseRepository,
) : SyncPort {

    private fun JwxtTimeParser.ParsedCourse.toDomain(): Course {
        return Course(
            name = name,
            location = location,
            teacher = teacher,
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            sectionCount = sectionCount,
            weeks = weeks,
            color = color,
            type = type,
            remoteKey = remoteKey,
            originRemoteKey = originRemoteKey,
            targetWeek = targetWeek
        )
    }

    override suspend fun sync(semesterId: String, campusId: String, week: String): SyncResult {
        val courses =
            jwxtSync.fetchCourses(semesterId = semesterId, campusId = campusId, week = week).map {
                it.toDomain()
            }
        courseRepository.replaceSyncedCourses(courses)
        return SyncResult(
            inserted = courses.size,
            semesterId = semesterId,
            campusId = campusId,
            campusName = "",
            week = week,
        )
    }
}
