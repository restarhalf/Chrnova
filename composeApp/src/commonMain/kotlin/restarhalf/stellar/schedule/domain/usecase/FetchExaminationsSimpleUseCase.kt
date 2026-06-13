package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Examination

/**
 * 简单获取考试安排用例
 * 
 * FetchExaminationsUseCase的简化包装，用于不需要保存到数据库的场景。
 */
class FetchExaminationsSimpleUseCase(
    private val fetchExaminations: FetchExaminationsUseCase,
) {
    /**
     * 获取考试安排
     * 
     * @param semester 学期
     * @param nameOrNumber 课程名称或编号筛选
     * @return 考试安排列表
     */
    suspend operator fun invoke(
        semester: String = "",
        nameOrNumber: String = ""
    ): List<Examination> {
        return fetchExaminations(semester = semester, nameOrNumber = nameOrNumber)
    }
}
