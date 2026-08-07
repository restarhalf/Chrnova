package restarhalf.stellar.schedule.data.remote

/**
 * 教务系统网关接口
 * 
 * 定义与教务系统通信的所有API接口。
 * 包括登录、获取学期信息、课程表、考试安排、成绩等。
 */
interface JwxtGateway {
    /**
     * 用户登录
     * 
     * @param userNo 学号
     * @param password 密码
     * @param captchaData 验证码数据
     * @param codeVal 验证码
     * @param p 加密参数
     * @return 登录响应
     */
    suspend fun login(
        userNo: String,
        password: String,
        captchaData: String = "",
        codeVal: String = "",
        p: String? = null
    ): JwxtLoginResponse

    /**
     * 获取当前学期
     * 
     * @return 当前学期响应
     */
    suspend fun getCurrentTerm(): JwxtCurrentTermResponse

    /**
     * 获取学期列表
     * 
     * @return 学期列表
     */
    suspend fun getSemesterList(): List<JwxtSemesterItem>

    /**
     * 获取学期列表（semesterList接口）
     * 
     * @return 学期列表
     */
    suspend fun getSemesterListFromEndpoint(): List<JwxtSemesterListItem>

    /**
     * 获取教学周信息
     * 
     * @return 教学周响应
     */
    suspend fun getTeachingWeek(): JwxtTeachingWeekResponse

    /**
     * 获取校区列表
     * 
     * @return 校区响应
     */
    suspend fun getCampusList(): JwxtCampusResponse

    /**
     * 获取课程表
     * 
     * @param fields 查询参数
     * @return 课程表响应
     */
    suspend fun fetchCurriculum(fields: Map<String, String>): JwxtCurriculumResponse

    /**
     * 获取考试安排
     * 
     * @param semester 学期
     * @param nameOrNumber 课程名称或编号
     * @return 考试安排响应
     */
    suspend fun fetchExaminationArrangement(
        semester: String = "",
        nameOrNumber: String = ""
    ): JwxtExaminationResponse

    /**
     * 获取学期成绩报告
     *
     * @param semester 学期
     * @return 成绩报告响应
     */
    suspend fun fetchTermGradeReport(semester: String): JwxtTermGradeResponse

    /**
     * 获取指导教学课程列表
     *
     * @param kcxz 课程性质（54=创新创业专业融合教育选修，61=专业选修）
     * @param kcsx 课程属性筛选（可选）
     * @param kcmc 课程名称筛选（可选）
     * @return 指导教学课程响应
     */
    suspend fun fetchGuidanceTeachingCourses(
        kcxz: String,
        kcsx: String = "",
        kcmc: String = ""
    ): JwxtGuidanceTeachingResponse

    // ==================== 选课系统接口 ====================

    /**
     * 获取选课轮次列表（wxgetXklc）。
     *
     * @param isnew 是否只看当前可进行轮次（1=是）
     * @return 选课轮次列表响应
     */
    suspend fun fetchSelectionRotations(isnew: Int = 1): JwxtSelectionResponse

    /**
     * 进入选课轮次并初始化会话缓存（wxinitXscache）。
     *
     * 调用后服务端会返回 sessionTime，后续 [fetchSelectionCourses] / [submitSelection] /
     * [dropSelection] 都需要带上该 sessionTime。
     *
     * @param rotationId 选课轮次 ID
     * @return 初始化响应（含 sessionTime 与可选分类列表）
     */
    suspend fun initSelectionSession(rotationId: String): JwxtSelectionResponse

    /**
     * 获取指定分类下的可选课程列表（wxgetKcList）。
     *
     * @param rotationId 选课轮次 ID
     * @param classificationCode 分类码（01 必修 / 02 选修 / 03 本学期计划 / 04 专业内跨年级 / 05 跨专业 / 100 体育）
     * @param sessionTime [initSelectionSession] 返回的 sessionTime
     * @param extraRules 初始化时返回的规则位（compulsorySemester/selectionGrades 等），原样回传
     * @param courseInformation 课程名称搜索关键词（对应 wxgetKcList 的 courseInformation 参数，空串表示不筛选）
     * @return 课程列表响应
     */
    suspend fun fetchSelectionCourses(
        rotationId: String,
        classificationCode: String,
        sessionTime: String,
        extraRules: Map<String, String> = emptyMap(),
        courseInformation: String = "",
    ): JwxtSelectionResponse

    /**
     * 提交选课（wxxkOper）。
     *
     * @return 统一选课结果，详见 [JwxtSelectionOperResult]
     */
    suspend fun submitSelection(
        rotationId: String,
        courseId: String,
        noticeId: String,
        sessionTime: String,
        classificationCode: String,
        splitIdentification: String = "",
        selectedNoticeId: String = "",
        selectedSplitIdentification: String = "",
        extraRules: Map<String, String> = emptyMap(),
    ): JwxtSelectionOperResult

    /**
     * 退课（wxxstkOper）。
     *
     * @param rotationId 选课轮次 ID
     * @param noticeId 教学 ID
     * @param sessionTime 会话时间
     * @param courseQualification 是否资质选课，默认 "true"
     * @return 退课响应
     */
    suspend fun dropSelection(
        rotationId: String,
        noticeId: String,
        sessionTime: String,
        courseQualification: String = "true",
    ): JwxtSelectionResponse

    /**
     * 获取已选课程列表（wxgetYxkcList），用于退课。
     *
     * @param rotationId 选课轮次 ID
     * @return 响应，data 字段为已选课程数组，每个元素含 isCanTk 标志
     */
    suspend fun fetchSelectedCourses(rotationId: String): JwxtSelectionResponse
}
