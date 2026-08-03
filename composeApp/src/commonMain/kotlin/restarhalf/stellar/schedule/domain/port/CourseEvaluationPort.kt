package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.CourseEvaluationSummary
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.domain.model.EvaluationCreateRequest
import restarhalf.stellar.schedule.domain.model.EvaluationPage
import restarhalf.stellar.schedule.domain.model.EvaluationUpdateRequest
import restarhalf.stellar.schedule.domain.model.LikeResult

/**
 * 课程评价端口接口
 *
 * 定义课程评价相关的抽象接口，包括列表查询、详情、新增、删除与点赞（交互）。
 * 后端地址固定为 chrnova.evaluate.restarhalf.dpdns.org。
 */
interface CourseEvaluationPort {
    /**
     * 获取评价列表（已去掉审核，全部可见）
     *
     * @param course 按课程名过滤（可选）
     * @param teacher 按教师名过滤（可选，传"教师未知"匹配空教师）
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return 分页评价列表
     */
    suspend fun listEvaluations(
        course: String? = null,
        teacher: String? = null,
        page: Int = 1,
        size: Int = 20,
    ): EvaluationPage

    /**
     * 获取课程评价聚合列表（按课程名分组）。
     *
     * 返回每门课程的平均分、评价数、教师与最新评价时间，
     * 用于评价列表页的"课程卡片"视图。
     *
     * @return 课程聚合摘要列表（按最新评价时间倒序）
     */
    suspend fun listCourseSummaries(): List<CourseEvaluationSummary>

    /**
     * 获取评价详情
     *
     * @param id 评价唯一标识
     * @return 评价详情
     */
    suspend fun getEvaluation(id: String): Evaluation

    /**
     * 提交一条课程评价（需登录：请求会携带 X-User-Hash 头）
     *
     * @param req 评价内容
     * @return 创建后的评价
     */
    suspend fun createEvaluation(req: EvaluationCreateRequest): Evaluation

    /**
     * 删除自己提交的评价（需登录：请求会携带 X-User-Hash 头）
     *
     * @param id 评价唯一标识
     * @return 是否删除成功
     */
    suspend fun deleteEvaluation(id: String): Boolean

    /**
     * 编辑自己提交的评价（需登录：请求会携带 X-User-Hash 头）。
     * 仅本人可改；course_name 不可改。
     *
     * @param id 评价唯一标识
     * @param req 编辑请求体（字段为 null 表示不修改）
     * @return 更新后的评价
     */
    suspend fun updateEvaluation(id: String, req: EvaluationUpdateRequest): Evaluation

    /**
     * 点赞 / 取消点赞（需登录：请求会携带 X-User-Hash 头）
     *
     * @param id 评价唯一标识
     * @return 最新的点赞数与当前点赞状态
     */
    suspend fun toggleLike(id: String): LikeResult

    companion object {
        /**
         * 把学号明文转成 user_hash（学号 SHA-256 hex，小写）。
         *
         * 后端用相同算法（crypto.subtle.digest('SHA-256')）计算，两边结果一致。
         * 客户端把它放进 `X-User-Hash` 请求头，用于登录态校验、防刷与归属判定，
         * 后端不再存储学号明文。
         */
        @OptIn(dev.whyoleg.cryptography.DelicateCryptographyApi::class)
        fun hashUserNo(userNo: String): String {
            if (userNo.isBlank()) return ""
            val hasher = dev.whyoleg.cryptography.CryptographyProvider.Default
                .get(dev.whyoleg.cryptography.algorithms.SHA256).hasher()
            val digest = hasher.hashBlocking(userNo.encodeToByteArray())
            return digest.toHexString().lowercase()
        }
    }
}
