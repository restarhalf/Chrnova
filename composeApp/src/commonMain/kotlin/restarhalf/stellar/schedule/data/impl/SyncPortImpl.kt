package restarhalf.stellar.schedule.data.impl

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.JwxtSync
import restarhalf.stellar.schedule.data.remote.JwxtTimeParser
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository


class SyncPortImpl(
    private val jwxtSync: JwxtSync,
    private val courseRepository: CourseRepository,
    private val auth: JwxtAuthPort,
) : SyncPort {

    
    private fun JwxtTimeParser.ParsedCourse.toDomain(): Course {
        return Course(
            name = name,
            semesterId = "",
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
        val userNo = try { auth.observeProfile().first().userNo } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AppLogger.log("Sync", "获取用户学号失败，使用空值", e)
            ""
        }
        val courses =
            jwxtSync.fetchCourses(semesterId = semesterId, campusId = campusId, week = week).map {
                it.toDomain().copy(semesterId = semesterId, userNo = userNo)
            }
        courseRepository.replaceSyncedCourses(courses = courses, semesterId = semesterId)
        return SyncResult(
            inserted = courses.size,
            semesterId = semesterId,
            campusId = campusId,
            campusName = "",
            week = week,
        )
    }

    override suspend fun fetchTermStartDate(semesterId: String, campusId: String): Long? {
        return jwxtSync.fetchTermStartDate(semesterId = semesterId, campusId = campusId)
    }
}
