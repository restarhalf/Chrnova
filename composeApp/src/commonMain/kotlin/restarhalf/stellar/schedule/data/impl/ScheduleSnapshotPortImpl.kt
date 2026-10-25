package restarhalf.stellar.schedule.data.impl

import restarhalf.stellar.schedule.domain.model.ScheduleSnapshot
import restarhalf.stellar.schedule.domain.port.ScheduleSnapshotPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository


class ScheduleSnapshotPortImpl(
    private val courseRepository: CourseRepository,
    private val timetable: TimetablePort,
) : ScheduleSnapshotPort {

    // getAllCoursesOnce 内部已按 activeScheduleTerm 过滤，这里不再重复判断。
    override suspend fun load(): ScheduleSnapshot {
        return ScheduleSnapshot(
            courses = courseRepository.getAllCoursesOnce(),
            timetable = timetable.getCampusTimetable(),
            totalWeeks = timetable.getTotalWeeks(),
            termStartMs = timetable.getTermStartMs(),
        )
    }
}
