@file:OptIn(ExperimentalTime::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package restarhalf.stellar.schedule.calendar

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import platform.EventKit.EKAlarm
import platform.EventKit.EKAuthorizationStatusAuthorized
import platform.EventKit.EKAuthorizationStatusFullAccess
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKit.EKSpan
import platform.Foundation.NSDate
import platform.Foundation.NSError
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.time.AcademicCalendar
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import kotlin.coroutines.resume
import kotlin.time.ExperimentalTime

/** NSDate 参考日期 2001-01-01 与 Unix 1970-01-01 之间的秒数差。 */
private const val NSTimeIntervalReferenceDateOffset = 978307200.0

/** 将 Unix epoch 毫秒转为 NSDate。 */
private fun epochMillisToNSDate(epochMs: Long): NSDate =
    NSDate(timeIntervalSinceReferenceDate = epochMs / 1000.0 - NSTimeIntervalReferenceDateOffset)

/**
 * iOS 日历事件端口实现
 *
 * 通过 EventKit 将课程/考试事件写入系统日历,
 * 事件 identifier 持久化到 NSUserDefaults,关闭时按 identifier 批量删除。
 */
class CalendarEventPortImpl(
    private val prefs: ObservableSettings,
) : CalendarEventPort {

    private val eventStore = EKEventStore()

    override fun hasCalendarPermission(): Boolean {
        val status = EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent)
        return status == EKAuthorizationStatusAuthorized ||
            status == EKAuthorizationStatusFullAccess
    }

    private suspend fun requestAccessIfNeeded(): Boolean {
        if (hasCalendarPermission()) return true
        return suspendCancellableCoroutine { cont ->
            eventStore.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { granted, _ ->
                cont.resume(granted)
            }
        }
    }

    override suspend fun syncCourseEvents(
        courses: List<Course>,
        termStartMs: Long,
        timetable: List<TimetableSlot>,
        reminderMinutes: Int,
    ): CalendarEventPort.SyncResult {
        if (!requestAccessIfNeeded()) return CalendarEventPort.SyncResult.PermissionDenied
        return runCatching {
            removeAllCourseEventsInternal()
            val calendar = eventStore.defaultCalendarForNewEvents
                ?: return@runCatching CalendarEventPort.SyncResult.Failed("无默认日历")

            val termStartMonday = AcademicCalendar.getTermStartMonday(termStartMs)
            val slotByNum = timetable.associateBy { it.num }
            val tz = TimeZone.currentSystemDefault()

            val ids = mutableListOf<String>()
            for (course in courses) {
                val startSlot = slotByNum[course.startSection] ?: continue
                val endSlot = slotByNum[course.startSection + course.sectionCount - 1] ?: continue
                val startTime = ClockTime.parseToHourMinute(startSlot.start) ?: continue
                val endTime = ClockTime.parseToHourMinute(endSlot.end) ?: continue

                for (week in course.weeks) {
                    if (week <= 0) continue
                    val date = termStartMonday.plus(
                        (week - 1) * 7 + (course.dayOfWeek - 1),
                        DateTimeUnit.DAY,
                    )
                    val startMs = localDateTimeToMillis(date, startTime.first, startTime.second, tz)
                    val endMs = localDateTimeToMillis(date, endTime.first, endTime.second, tz)

                    val event = EKEvent.eventWithEventStore(eventStore).apply {
                        setTitle(course.name)
                        setLocation(course.location)
                        setCalendar(calendar)
                        setStartDate(epochMillisToNSDate(startMs))
                        setEndDate(epochMillisToNSDate(endMs))
                        val desc = buildString {
                            if (course.teacher.isNotBlank()) append("教师: ${course.teacher}")
                            append("[chrnova-course]")
                        }
                        setNotes(desc)
                        addAlarm(EKAlarm.alarmWithRelativeOffset(-reminderMinutes * 60.0))
                    }

                    val savedId = saveEvent(event)
                    if (savedId != null) ids.add(savedId)
                }
            }
            saveCourseIds(ids)
            CalendarEventPort.SyncResult.Success(ids.size)
        }.getOrElse { e ->
            AppLogger.log("Calendar", "iOS 同步课程事件失败", e)
            CalendarEventPort.SyncResult.Failed(e.message ?: "Unknown")
        }
    }

    override suspend fun syncExamEvents(
        exams: List<Examination>,
        reminderMinutes: Int,
    ): CalendarEventPort.SyncResult {
        if (!requestAccessIfNeeded()) return CalendarEventPort.SyncResult.PermissionDenied
        return runCatching {
            removeAllExamEventsInternal()
            val calendar = eventStore.defaultCalendarForNewEvents
                ?: return@runCatching CalendarEventPort.SyncResult.Failed("无默认日历")

            val tz = TimeZone.currentSystemDefault()
            val ids = mutableListOf<String>()

            for (exam in exams) {
                val parsed = parseExamTime(exam.time) ?: continue
                val startMs = parsed.start.toInstant(tz).toEpochMilliseconds()
                val endMs = parsed.end.toInstant(tz).toEpochMilliseconds()

                val event = EKEvent.eventWithEventStore(eventStore).apply {
                    setTitle("${exam.courseName} 考试")
                    setLocation(exam.examinationPlace)
                    setCalendar(calendar)
                    setStartDate(epochMillisToNSDate(startMs))
                    setEndDate(epochMillisToNSDate(endMs))
                    val desc = buildString {
                        if (exam.zwh.isNotBlank()) append("座位号: ${exam.zwh}\n")
                        if (exam.ksbz.isNotBlank()) append("状态: ${exam.ksbz}")
                        append("[chrnova-exam]")
                    }
                    setNotes(desc)
                    addAlarm(EKAlarm.alarmWithRelativeOffset(-reminderMinutes * 60.0))
                }

                val savedId = saveEvent(event)
                if (savedId != null) ids.add(savedId)
            }
            saveExamIds(ids)
            CalendarEventPort.SyncResult.Success(ids.size)
        }.getOrElse { e ->
            AppLogger.log("Calendar", "iOS 同步考试事件失败", e)
            CalendarEventPort.SyncResult.Failed(e.message ?: "Unknown")
        }
    }

    override suspend fun removeAllCourseEvents(): CalendarEventPort.SyncResult = runCatching {
        val count = removeAllCourseEventsInternal()
        CalendarEventPort.SyncResult.Success(count)
    }.getOrElse { e ->
        AppLogger.log("Calendar", "iOS 删除课程事件失败", e)
        CalendarEventPort.SyncResult.Failed(e.message ?: "Unknown")
    }

    override suspend fun removeAllExamEvents(): CalendarEventPort.SyncResult = runCatching {
        val count = removeAllExamEventsInternal()
        CalendarEventPort.SyncResult.Success(count)
    }.getOrElse { e ->
        AppLogger.log("Calendar", "iOS 删除考试事件失败", e)
        CalendarEventPort.SyncResult.Failed(e.message ?: "Unknown")
    }

    override suspend fun removeAllEvents(): CalendarEventPort.SyncResult {
        val a = removeAllCourseEvents()
        val b = removeAllExamEvents()
        return if (a is CalendarEventPort.SyncResult.Success && b is CalendarEventPort.SyncResult.Success) {
            CalendarEventPort.SyncResult.Success(a.affected + b.affected)
        } else if (a is CalendarEventPort.SyncResult.Failed) a
        else if (b is CalendarEventPort.SyncResult.Failed) b
        else CalendarEventPort.SyncResult.Success(0)
    }

    private fun removeAllCourseEventsInternal(): Int {
        val ids = loadCourseIds()
        deleteEventsByIdentifiers(ids)
        clearCourseIds()
        return ids.size
    }

    private fun removeAllExamEventsInternal(): Int {
        val ids = loadExamIds()
        deleteEventsByIdentifiers(ids)
        clearExamIds()
        return ids.size
    }

    private fun deleteEventsByIdentifiers(identifiers: List<String>) {
        for (id in identifiers) {
            val event = eventStore.eventWithIdentifier(id) ?: continue
            memScoped {
                val errorPtr: ObjCObjectVar<NSError?> = alloc()
                eventStore.removeEvent(event, span = EKSpan.EKSpanThisEvent, commit = true, error = errorPtr.ptr)
            }
        }
    }

    private fun saveEvent(event: EKEvent): String? {
        return memScoped {
            val errorPtr: ObjCObjectVar<NSError?> = alloc()
            val success = eventStore.saveEvent(
                event = event,
                span = EKSpan.EKSpanThisEvent,
                commit = true,
                error = errorPtr.ptr,
            )
            if (success) event.eventIdentifier() else null
        }
    }

    private fun localDateTimeToMillis(
        date: LocalDate,
        hour: Int,
        minute: Int,
        tz: TimeZone,
    ): Long {
        val ldt = LocalDateTime(date.year, date.month, date.day, hour, minute)
        return ldt.toInstant(tz).toEpochMilliseconds()
    }

    private data class ParsedExamTime(val start: LocalDateTime, val end: LocalDateTime)

    private fun parseExamTime(raw: String): ParsedExamTime? {
        val regex = Regex("(\\d{4})-(\\d{2})-(\\d{2}).*?(\\d{1,2}):(\\d{2})\\s*[~-]\\s*(\\d{1,2}):(\\d{2})")
        val match = regex.find(raw) ?: return null
        val (y, mo, d, sh, sm, eh, em) = match.destructured
        val startDate = LocalDate(y.toInt(), mo.toInt(), d.toInt())
        val start = LocalDateTime(startDate.year, startDate.month, startDate.day, sh.toInt(), sm.toInt())
        val end = LocalDateTime(startDate.year, startDate.month, startDate.day, eh.toInt(), em.toInt())
        return ParsedExamTime(start, end)
    }

    private fun loadCourseIds(): List<String> = prefs.getString(KEY_COURSE_IDS, "").orEmpty()
        .split("\u0001")
        .filter { it.isNotBlank() }

    private fun saveCourseIds(ids: List<String>) {
        prefs[KEY_COURSE_IDS] = ids.joinToString("\u0001")
    }

    private fun clearCourseIds() {
        prefs.remove(KEY_COURSE_IDS)
    }

    private fun loadExamIds(): List<String> = prefs.getString(KEY_EXAM_IDS, "").orEmpty()
        .split("\u0001")
        .filter { it.isNotBlank() }

    private fun saveExamIds(ids: List<String>) {
        prefs[KEY_EXAM_IDS] = ids.joinToString("\u0001")
    }

    private fun clearExamIds() {
        prefs.remove(KEY_EXAM_IDS)
    }

    companion object {
        private const val KEY_COURSE_IDS = "calendar_course_event_ids"
        private const val KEY_EXAM_IDS = "calendar_exam_event_ids"
    }
}
