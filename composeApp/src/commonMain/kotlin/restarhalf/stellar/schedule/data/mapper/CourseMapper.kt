package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.entity.CourseEntity
import restarhalf.stellar.schedule.domain.model.Course as CourseDomain

fun CourseEntity.toDomain(): CourseDomain {
    return CourseDomain(
        id = id,
        name = name,
        semesterId = semesterId,
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
        targetWeek = targetWeek,
        userNo = userNo,
    )
}

fun CourseDomain.toEntity(): CourseEntity {
    return CourseEntity(
        id = id,
        name = name,
        semesterId = semesterId,
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
        targetWeek = targetWeek,
        userNo = userNo,
    )
}
