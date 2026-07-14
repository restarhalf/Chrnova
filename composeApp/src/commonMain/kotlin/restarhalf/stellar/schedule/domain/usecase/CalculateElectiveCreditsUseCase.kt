package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.GuidanceTeachingCourse

class CalculateElectiveCreditsUseCase {

    data class CreditCategory(
        val code: String,
        val name: String,
        val credits: Double,
        val courses: List<GradeCourse>,
    )

    operator fun invoke(
        courses: List<GradeCourse>,
        innovationGuidanceCourses: List<GuidanceTeachingCourse>,
        professionalGuidanceCourses: List<GuidanceTeachingCourse>,
    ): List<CreditCategory> {
        val xCategories = calculateXCategories(courses)
        val innovationCategory = buildGuidanceCategory(
            code = "创新创业选修",
            name = "创新创业教育平台专业选修",
            guidanceCourses = innovationGuidanceCourses,
            gradeCourses = courses,
        )
        val professionalCategory = buildGuidanceCategory(
            code = "专业选修",
            name = "专业教育平台选修",
            guidanceCourses = professionalGuidanceCourses,
            gradeCourses = courses,
        )
        return xCategories + listOf(innovationCategory, professionalCategory)
    }

    private fun calculateXCategories(courses: List<GradeCourse>): List<CreditCategory> {
        val categorized = mutableMapOf<String, MutableList<GradeCourse>>()
        for (course in courses) {
            if (!isCoursePassed(course)) continue
            val code = extractCategoryCode(course.courseCode) ?: continue
            val xCode = convertToXCode(code)
            categorized.getOrPut(xCode) { mutableListOf() }.add(course)
        }
        return listOf("X1", "X2", "X3", "X4", "X5").map { code ->
            val coursesInCategory = categorized[code] ?: emptyList()
            CreditCategory(
                code = code,
                name = X_CATEGORY_NAMES[code] ?: code,
                credits = coursesInCategory.sumOf { it.credit },
                courses = coursesInCategory,
            )
        }
    }

    private fun buildGuidanceCategory(
        code: String,
        name: String,
        guidanceCourses: List<GuidanceTeachingCourse>,
        gradeCourses: List<GradeCourse>,
    ): CreditCategory {
        val guidanceCourseCodes = guidanceCourses.map { it.courseCode }.toSet()
        val courses = gradeCourses.filter { it.courseCode in guidanceCourseCodes && isCoursePassed(it) }
        return CreditCategory(
            code = code,
            name = name,
            credits = courses.sumOf { it.credit },
            courses = courses,
        )
    }

    private fun isCoursePassed(course: GradeCourse): Boolean {
        val score = course.score.toDoubleOrNull()
        if (score != null && score >= 60.0) return true
        return course.passStatus == "合格"
    }

    companion object {
        private val Z_TO_X_MAP = mapOf(
            "Z1" to "X4",
            "Z2" to "X2",
            "Z3" to "X3",
            "Z4" to "X5",
        )

        private val X_CATEGORY_NAMES = mapOf(
            "X1" to "X1",
            "X2" to "X2艺术鉴赏与审美体验",
            "X3" to "X3生命关怀与健康素养",
            "X4" to "X4文化传承与社会发展",
            "X5" to "X5科学探索与技术创新",
        )

        fun extractCategoryCode(courseCode: String): String? {
            if (courseCode.length < 2) return null
            val prefix = courseCode.take(2)
            return if (prefix.matches(Regex("[XZ][1-5]"))) prefix else null
        }

        fun convertToXCode(code: String): String = Z_TO_X_MAP[code] ?: code
    }
}
