package restarhalf.stellar.schedule.calendar

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.time.AcademicCalendar
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import kotlin.time.ExperimentalTime

/**
 * Android 日历事件端口实现
 *
 * 通过 CalendarContract 将课程/考试事件写入系统日历,
 * 事件 ID 列表持久化到 SharedPreferences,关闭时按 ID 批量删除。
 */
class CalendarEventPortImpl(
    private val context: Context,
    private val prefs: ObservableSettings,
) : CalendarEventPort {

    override fun hasCalendarPermission(): Boolean {
        val read = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun syncCourseEvents(
        courses: List<Course>,
        termStartMs: Long,
        timetable: List<TimetableSlot>,
        reminderMinutes: Int,
    ): CalendarEventPort.SyncResult {
        if (!hasCalendarPermission()) return CalendarEventPort.SyncResult.PermissionDenied
        return runCatching {
            removeAllCourseEventsInternal()
            val calendarId = resolvePrimaryCalendarId()
                ?: return@runCatching CalendarEventPort.SyncResult.Failed("无可用日历账户")

            val termStartMonday = AcademicCalendar.getTermStartMonday(termStartMs)
            val slotByNum = timetable.associateBy { it.num }
            val tz = TimeZone.currentSystemDefault()

            val ops = ArrayList<ContentProviderOperation>()
            val insertedIds = mutableListOf<Long>()

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
                    val uid = "chrnova-course-${course.id}-w$week@chrnova.local"

                    val eventIdx = ops.size
                    ops.add(
                        ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                            .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                            .withValue(CalendarContract.Events.TITLE, course.name)
                            .withValue(
                                CalendarContract.Events.EVENT_LOCATION,
                                course.location.ifBlank { null },
                            )
                            .withValue(
                                CalendarContract.Events.DESCRIPTION,
                                buildString {
                                    if (course.teacher.isNotBlank()) append("教师: ${course.teacher}")
                                    append("[chrnova-course]")
                                },
                            )
                            .withValue(CalendarContract.Events.DTSTART, startMs)
                            .withValue(CalendarContract.Events.DTEND, endMs)
                            .withValue(CalendarContract.Events.EVENT_TIMEZONE, tz.id)
                            .withValue(CalendarContract.Events.CUSTOM_APP_URI, uid)
                            .withValue(CalendarContract.Events.UID_2445, uid)
                            .build()
                    )
                    ops.add(
                        ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
                            .withValueBackReference(CalendarContract.Reminders.EVENT_ID, eventIdx)
                            .withValue(CalendarContract.Reminders.MINUTES, reminderMinutes)
                            .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                            .build()
                    )
                }
            }

            if (ops.isEmpty()) {
                return@runCatching CalendarEventPort.SyncResult.Success(0)
            }

            val results = context.contentResolver.applyBatch(CalendarContract.AUTHORITY, ops)
            for (result in results) {
                val uri = result.uri ?: continue
                val id = ContentUris.parseId(uri)
                if (id > 0) insertedIds.add(id)
            }
            saveCourseIds(insertedIds)
            CalendarEventPort.SyncResult.Success(insertedIds.size)
        }.getOrElse { e ->
            AppLogger.log("Calendar", "同步课程事件失败", e)
            CalendarEventPort.SyncResult.Failed(e.message ?: "Unknown")
        }
    }

    override suspend fun syncExamEvents(
        exams: List<Examination>,
        reminderMinutes: Int,
    ): CalendarEventPort.SyncResult {
        if (!hasCalendarPermission()) return CalendarEventPort.SyncResult.PermissionDenied
        return runCatching {
            removeAllExamEventsInternal()
            val calendarId = resolvePrimaryCalendarId()
                ?: return@runCatching CalendarEventPort.SyncResult.Failed("无可用日历账户")

            val tz = TimeZone.currentSystemDefault()
            val ops = ArrayList<ContentProviderOperation>()
            val insertedIds = mutableListOf<Long>()

            for (exam in exams) {
                val parsed = parseExamTime(exam.time) ?: continue
                val startMs = parsed.start.toInstant(tz).toEpochMilliseconds()
                val endMs = parsed.end.toInstant(tz).toEpochMilliseconds()
                val uid = "chrnova-exam-${exam.id}@chrnova.local"

                val eventIdx = ops.size
                ops.add(
                    ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                        .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                        .withValue(CalendarContract.Events.TITLE, "${exam.courseName} 考试")
                        .withValue(
                            CalendarContract.Events.EVENT_LOCATION,
                            exam.examinationPlace.ifBlank { null },
                        )
                        .withValue(
                            CalendarContract.Events.DESCRIPTION,
                            buildString {
                                if (exam.zwh.isNotBlank()) append("座位号: ${exam.zwh}\n")
                                if (exam.ksbz.isNotBlank()) append("状态: ${exam.ksbz}")
                                append("[chrnova-exam]")
                            },
                        )
                        .withValue(CalendarContract.Events.DTSTART, startMs)
                        .withValue(CalendarContract.Events.DTEND, endMs)
                        .withValue(CalendarContract.Events.EVENT_TIMEZONE, tz.id)
                        .withValue(CalendarContract.Events.CUSTOM_APP_URI, uid)
                        .withValue(CalendarContract.Events.UID_2445, uid)
                        .build()
                )
                ops.add(
                    ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
                        .withValueBackReference(CalendarContract.Reminders.EVENT_ID, eventIdx)
                        .withValue(CalendarContract.Reminders.MINUTES, reminderMinutes)
                        .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                        .build()
                )
            }

            if (ops.isEmpty()) {
                return@runCatching CalendarEventPort.SyncResult.Success(0)
            }

            val results = context.contentResolver.applyBatch(CalendarContract.AUTHORITY, ops)
            for (result in results) {
                val uri = result.uri ?: continue
                val id = ContentUris.parseId(uri)
                if (id > 0) insertedIds.add(id)
            }
            saveExamIds(insertedIds)
            CalendarEventPort.SyncResult.Success(insertedIds.size)
        }.getOrElse { e ->
            AppLogger.log("Calendar", "同步考试事件失败", e)
            CalendarEventPort.SyncResult.Failed(e.message ?: "Unknown")
        }
    }

    override suspend fun removeAllCourseEvents(): CalendarEventPort.SyncResult =
        runCatching {
            val count = removeAllCourseEventsInternal()
            CalendarEventPort.SyncResult.Success(count)
        }.getOrElse { e ->
            AppLogger.log("Calendar", "删除课程事件失败", e)
            CalendarEventPort.SyncResult.Failed(e.message ?: "Unknown")
        }

    override suspend fun removeAllExamEvents(): CalendarEventPort.SyncResult =
        runCatching {
            val count = removeAllExamEventsInternal()
            CalendarEventPort.SyncResult.Success(count)
        }.getOrElse { e ->
            AppLogger.log("Calendar", "删除考试事件失败", e)
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
        deleteEventsByIds(ids)
        clearCourseIds()
        return ids.size
    }

    private fun removeAllExamEventsInternal(): Int {
        val ids = loadExamIds()
        deleteEventsByIds(ids)
        clearExamIds()
        return ids.size
    }

    private fun deleteEventsByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        val selection = "${CalendarContract.Events._ID} IN (${ids.joinToString(",")})"
        context.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            selection,
            null,
        )
    }

    private fun resolvePrimaryCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}"
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            null,
            "${CalendarContract.Calendars._ID} ASC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(0)
            }
        }
        return null
    }

    @OptIn(ExperimentalTime::class)
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

    private fun loadCourseIds(): List<Long> = prefs.getString(KEY_COURSE_IDS, "").orEmpty()
        .split(",").mapNotNull { it.toLongOrNull() }

    private fun saveCourseIds(ids: List<Long>) {
        prefs[KEY_COURSE_IDS] = ids.joinToString(",")
    }

    private fun clearCourseIds() {
        prefs.remove(KEY_COURSE_IDS)
    }

    private fun loadExamIds(): List<Long> = prefs.getString(KEY_EXAM_IDS, "").orEmpty()
        .split(",").mapNotNull { it.toLongOrNull() }

    private fun saveExamIds(ids: List<Long>) {
        prefs[KEY_EXAM_IDS] = ids.joinToString(",")
    }

    private fun clearExamIds() {
        prefs.remove(KEY_EXAM_IDS)
    }

    companion object {
        private const val KEY_COURSE_IDS = "calendar_course_event_ids"
        private const val KEY_EXAM_IDS = "calendar_exam_event_ids"
    }
}
