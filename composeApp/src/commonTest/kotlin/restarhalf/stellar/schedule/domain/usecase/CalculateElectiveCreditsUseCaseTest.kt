package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.GuidanceTeachingCourse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalculateElectiveCreditsUseCaseTest {

    private val useCase = CalculateElectiveCreditsUseCase()

    private fun course(
        code: String,
        score: String,
        credit: Double,
        passStatus: String = "",
    ) = GradeCourse(
        courseCode = code,
        courseName = "课程$code",
        score = score,
        credit = credit,
        passStatus = passStatus,
    )

    private fun guidance(code: String) = GuidanceTeachingCourse(courseCode = code)

    @Test
    fun `X类课程按代码分组累计学分`() {
        val result = useCase(
            courses = listOf(
                course("X1A001", "85", 2.0),
                course("X1B002", "90", 1.5),
                course("X3C003", "75", 1.0),
            ),
            innovationGuidanceCourses = emptyList(),
            professionalGuidanceCourses = emptyList(),
        )
        assertEquals(8, result.size) // X1-X5 五类 + 创新创业选修 + 专业选修 + 挂科
        val x1 = result.first { it.code == "X1" }
        assertEquals(3.5, x1.credits)
        assertEquals(2, x1.courses.size)
        val x3 = result.first { it.code == "X3" }
        assertEquals(1.0, x3.credits)
        val x2 = result.first { it.code == "X2" }
        assertEquals(0.0, x2.credits)
        assertTrue(x2.courses.isEmpty())
    }

    @Test
    fun `Z类代码转换为对应X类`() {
        val result = useCase(
            courses = listOf(course("Z1A001", "88", 2.0), course("Z4B001", "80", 1.0)),
            innovationGuidanceCourses = emptyList(),
            professionalGuidanceCourses = emptyList(),
        )
        // Z1→X4，Z4→X5
        assertEquals(2.0, result.first { it.code == "X4" }.credits)
        assertEquals(1.0, result.first { it.code == "X5" }.credits)
    }

    @Test
    fun `挂科课程不计入X类且进入挂科分类`() {
        val result = useCase(
            courses = listOf(
                course("X1A001", "55", 2.0),
                course("X1B002", "90", 1.5),
            ),
            innovationGuidanceCourses = emptyList(),
            professionalGuidanceCourses = emptyList(),
        )
        assertEquals(1.5, result.first { it.code == "X1" }.credits)
        val failed = result.first { it.code == "挂科" }
        assertTrue(failed.isFailed)
        assertEquals(2.0, failed.credits)
        assertEquals(1, failed.courses.size)
    }

    @Test
    fun `非数字分数凭合格状态通过`() {
        val result = useCase(
            courses = listOf(
                course("X2A001", "优秀", 1.0, passStatus = "合格"),
                course("X2B002", "59", 1.0, passStatus = "合格"),
                course("X2C003", "良好", 1.0, passStatus = "不合格"),
            ),
            innovationGuidanceCourses = emptyList(),
            professionalGuidanceCourses = emptyList(),
        )
        // "优秀"+合格 与 "59"+合格 通过；"良好"+不合格 不通过
        assertEquals(2.0, result.first { it.code == "X2" }.credits)
        assertEquals(1.0, result.first { it.code == "挂科" }.credits)
    }

    @Test
    fun `创新创业选修按指导课程代码匹配`() {
        val result = useCase(
            courses = listOf(
                course("X5A001", "85", 2.0),
                course("X5B002", "40", 1.0),
            ),
            innovationGuidanceCourses = listOf(guidance("X5A001"), guidance("X5B002")),
            professionalGuidanceCourses = emptyList(),
        )
        val innovation = result.first { it.code == "创新创业选修" }
        // 匹配代码且通过的才有学分：X5B002 挂科不计
        assertEquals(2.0, innovation.credits)
        assertEquals(1, innovation.courses.size)
    }

    @Test
    fun `专业选修与创新创业选修互不串类`() {
        val result = useCase(
            courses = listOf(course("X4A001", "85", 2.0), course("X5A001", "85", 3.0)),
            innovationGuidanceCourses = listOf(guidance("X5A001")),
            professionalGuidanceCourses = listOf(guidance("X4A001")),
        )
        assertEquals(3.0, result.first { it.code == "创新创业选修" }.credits)
        assertEquals(2.0, result.first { it.code == "专业选修" }.credits)
    }

    @Test
    fun `无匹配指导课程时选修类学分为零`() {
        val result = useCase(
            courses = listOf(course("X1A001", "85", 2.0)),
            innovationGuidanceCourses = listOf(guidance("X9ZZZ")),
            professionalGuidanceCourses = emptyList(),
        )
        assertEquals(0.0, result.first { it.code == "创新创业选修" }.credits)
    }

    @Test
    fun `extractCategoryCode 仅接受XZ加一位数字前缀`() {
        assertEquals("X1", CalculateElectiveCreditsUseCase.extractCategoryCode("X1A001"))
        assertEquals("Z3", CalculateElectiveCreditsUseCase.extractCategoryCode("Z3XYZ"))
        assertNull(CalculateElectiveCreditsUseCase.extractCategoryCode("XXA001"))
        assertNull(CalculateElectiveCreditsUseCase.extractCategoryCode("X"))
        assertNull(CalculateElectiveCreditsUseCase.extractCategoryCode("AB123"))
    }

    @Test
    fun `convertToXCode 未知代码原样返回`() {
        assertEquals("X2", CalculateElectiveCreditsUseCase.convertToXCode("Z2"))
        assertEquals("X1", CalculateElectiveCreditsUseCase.convertToXCode("X1"))
    }
}
