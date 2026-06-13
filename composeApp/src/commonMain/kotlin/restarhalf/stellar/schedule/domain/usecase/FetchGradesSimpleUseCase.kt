package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.TermGradeReport

/**
 * 简单获取成绩用例
 * 
 * FetchGradesUseCase的简化包装，用于不需要保存到数据库的场景。
 */
class FetchGradesSimpleUseCase(
    private val fetchGrades: FetchGradesUseCase,
) {
    /**
     * 获取成绩报告
     * 
     * @param semester 学期
     * @return 学期成绩报告
     */
    suspend operator fun invoke(semester: String = ""): TermGradeReport {
        return fetchGrades(semester = semester)
    }
}