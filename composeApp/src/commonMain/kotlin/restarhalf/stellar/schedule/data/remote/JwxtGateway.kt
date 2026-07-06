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
}
