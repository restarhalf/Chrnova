package restarhalf.stellar.schedule.data.impl

import restarhalf.stellar.schedule.data.remote.JwxtSync
import restarhalf.stellar.schedule.data.remote.JwxtTimeParser
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import kotlinx.coroutines.flow.first

/**
 * 同步端口实现类
 * 
 * 实现SyncPort接口，负责从教务系统同步课程数据到本地数据库。
 * 
 * @param jwxtSync 教务系统同步服务
 * @param courseRepository 课程仓库
 * @param auth 认证端口，用于获取当前用户学号
 */
class SyncPortImpl(
    private val jwxtSync: JwxtSync,
    private val courseRepository: CourseRepository,
    private val auth: AuthPort,
) : SyncPort {

    /**
     * 将解析的课程数据转换为领域模型
     * 
     * @return 转换后的Course对象
     */
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

    /**
     * 执行课程同步
     * 
     * 从教务系统获取课程数据，转换为领域模型后保存到本地数据库。
     * 
     * @param semesterId 学期ID
     * @param campusId 校区ID
     * @param week 周次筛选
     * @return 同步结果
     */
    override suspend fun sync(semesterId: String, campusId: String, week: String): SyncResult {
        val userNo = try { auth.observeProfile().first().userNo } catch (_: Exception) { "" }
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
}
