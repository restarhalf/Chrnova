package restarhalf.stellar.schedule.data.impl

import restarhalf.stellar.schedule.data.local.Course
import restarhalf.stellar.schedule.data.local.CourseDao
import restarhalf.stellar.schedule.data.remote.JwxtSync
import restarhalf.stellar.schedule.data.remote.JwxtTimeParser
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.SyncPort

class SyncPortImpl(
    private val jwxtSync: JwxtSync,
    private val courseDao: CourseDao,
) : SyncPort {

    private fun JwxtTimeParser.ParsedCourse.toEntity(): Course {
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
                it.toEntity()
            }
        courseDao.deleteSyncedCourses()
        courseDao.insertCourses(courses)
        return SyncResult(
            inserted = courses.size,
            semesterId = semesterId,
            campusId = campusId,
            campusName = "",
            week = week,
        )
    }
}
