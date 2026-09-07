package restarhalf.stellar.schedule.data.mapper

import restarhalf.stellar.schedule.data.local.entity.CourseEntity
import restarhalf.stellar.schedule.data.local.entity.ExaminationEntity
import restarhalf.stellar.schedule.data.local.entity.GradeEntity
import restarhalf.stellar.schedule.data.local.entity.PEYearScoreEntity
import restarhalf.stellar.schedule.data.local.Campus as DataCampus
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PESubjectScore
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.domain.model.Campus as DomainCampus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MappersTest {

    // ---------- Campus ----------

    @Test
    fun campusRoundTrip() {
        for (campus in DomainCampus.entries) {
            assertEquals(campus, campus.toData().toDomain())
        }
        assertEquals(DataCampus.Development, DomainCampus.Development.toData())
        assertEquals(DataCampus.Jinshitan, DomainCampus.Jinshitan.toData())
    }

    // ---------- Course ----------

    private val courseEntity = CourseEntity(
        id = 7L,
        name = "数据结构",
        semesterId = "2025-2026-1",
        location = "教学楼A301",
        teacher = "王五",
        dayOfWeek = 3,
        startSection = 2,
        sectionCount = 2,
        weeks = listOf(1, 3, 5),
        color = "#123456",
        type = 1,
        remoteKey = "RK-001",
        originRemoteKey = "RK-000",
        targetWeek = 8,
        userNo = "2024000000",
    )

    @Test
    fun courseEntityToDomainPreservesAllFields() {
        val domain = courseEntity.toDomain()
        assertEquals(7L, domain.id)
        assertEquals("数据结构", domain.name)
        assertEquals("2025-2026-1", domain.semesterId)
        assertEquals("教学楼A301", domain.location)
        assertEquals("王五", domain.teacher)
        assertEquals(3, domain.dayOfWeek)
        assertEquals(2, domain.startSection)
        assertEquals(2, domain.sectionCount)
        assertEquals(listOf(1, 3, 5), domain.weeks)
        assertEquals("#123456", domain.color)
        assertEquals(1, domain.type)
        assertEquals("RK-001", domain.remoteKey)
        assertEquals("RK-000", domain.originRemoteKey)
        assertEquals(8, domain.targetWeek)
        assertEquals("2024000000", domain.userNo)
    }

    @Test
    fun courseRoundTrip() {
        assertEquals(courseEntity, courseEntity.toDomain().toEntity())
    }

    // ---------- Examination ----------

    private val examinationEntity = ExaminationEntity(
        id = 11L,
        courseNumber = "MA101",
        courseName = "高等数学",
        time = "2026-01-15 14:00-16:00",
        examinationPlace = "考场三",
        zwh = "18",
        ksbz = "正常",
        semesterId = "2025-2026-1",
        source = "manual",
        userNo = "2024000000",
    )

    @Test
    fun examinationRoundTrip() {
        assertEquals(examinationEntity, examinationEntity.toDomain().toEntity())
    }

    @Test
    fun examinationToEntityOverridesSemesterId() {
        val entity = examinationEntity.toDomain().toEntity(semesterId = "2026-2027-1")
        assertEquals("2026-2027-1", entity.semesterId)
        assertEquals(examinationEntity.courseName, entity.courseName)
    }

    // ---------- Grade ----------

    private val gradeEntity = GradeEntity(
        courseCode = "CS102",
        courseName = "操作系统",
        score = "92",
        gradePoint = 4.0,
        credit = 3.5,
        curriculumAttributes = "必修",
        courseNature = "专业课",
        examName = "期末考试",
        examinationNature = "正常考试",
        passStatus = "通过",
        gradeLevel = "A",
        markFlag = "正常",
        repeatSemester = "",
        gradeId = "G-2025-001",
        semester = "2025-2026-1",
        userNo = "2024000000",
    )

    @Test
    fun gradeRoundTrip() {
        assertEquals(gradeEntity, gradeEntity.toDomain().toEntity())
    }

    @Test
    fun gradeToEntityOverridesUserNo() {
        val entity = gradeEntity.toDomain().toEntity(userNo = "new-user")
        assertEquals("new-user", entity.userNo)
        assertEquals("操作系统", entity.courseName)
    }

    // ---------- PE YearScore ----------

    @Test
    fun peYearScoreRoundTrip() {
        val entity = PEYearScoreEntity(
            schoolYear = "2025-2026",
            total = 88.5,
            isFree = 1,
            done = 4,
            nums = 6,
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }

    // ---------- PE Detail ----------

    @Test
    fun peSubjectScoreRoundTripKeepsNullableFields() {
        val dto = PESubjectScore(
            subjectId = "s-01",
            subName = "引体向上",
            result = null,
            score = null,
            unit = "次",
            subRatio = "10%",
            grade = null,
            isJoin = 0,
        )
        val entity = dto.toEntity("2025-2026")
        assertEquals("2025-2026", entity.schoolYear)
        assertNull(entity.result)
        assertNull(entity.score)
        assertNull(entity.grade)
        assertEquals(dto, entity.toDomain())
    }

    @Test
    fun peDetailDataToEntitySplitsSummaryAndScores() {
        val detail = PEDetailData(
            totalScore = 76.0,
            totalGrade = "良好",
            dataArr = listOf(
                PESubjectScore(subjectId = "s1", subName = "跑步", result = "3'40\"", score = 90, isJoin = 1),
                PESubjectScore(subjectId = "s2", subName = "跳远", score = 80, isJoin = 1),
            ),
        )
        val (summaries, scores) = detail.toEntity("2025-2026")
        assertEquals(1, summaries.size)
        assertEquals(76.0, summaries[0].totalScore)
        assertEquals("良好", summaries[0].totalGrade)
        assertEquals("2025-2026", summaries[0].schoolYear)
        assertEquals(2, scores.size)
        // 反向映射还原
        val restored = summaries[0].toDomain(scores.map { it.toDomain() })
        assertEquals(detail, restored)
    }
}
