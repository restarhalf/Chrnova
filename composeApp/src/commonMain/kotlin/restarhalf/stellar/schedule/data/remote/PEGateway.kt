package restarhalf.stellar.schedule.data.remote

/**
 * 体育系统网关接口
 *
 * 定义与体育系统通信的所有API接口。
 * 包括登录、成绩查询、学生信息获取等。
 */
interface PEGateway {
    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录响应
     */
    suspend fun login(username: String, password: String): PELoginResponse

    /**
     * 获取成绩列表
     *
     * @return 成绩列表响应
     */
    suspend fun getScoreList(): PEScoreListResponse

    /**
     * 获取成绩详情
     *
     * @param schoolYear 学年
     * @return 成绩详情响应
     */
    suspend fun getScoreDetail(schoolYear: String): PEDetailResponse

    /**
     * 获取单科成绩历史记录（分页）
     *
     * @param schoolYear 学年
     * @param subjectId 科目ID
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     * @return 单科成绩历史响应
     */
    suspend fun getSubjectScoreHistory(
        schoolYear: String,
        subjectId: String,
        pageNum: Int = 1,
        pageSize: Int = 50,
    ): PESubjectHistoryResponse

    /**
     * 获取学生信息
     *
     * @return 学生信息响应
     */
    suspend fun getProfile(): PEAuthProfileResponse

    /**
     * 获取学生预约列表
     *
     * @param type "1"=我的预约，"2"=可预约
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     * @return 预约列表响应
     */
    suspend fun getAppointments(
        type: String,
        pageNum: Int = 1,
        pageSize: Int = 20,
    ): PEAppointmentListResponse

    /**
     * 获取预约详情（可选日期 + 时段摘要）
     *
     * @param appointmentId 场次 ID
     * @param appointmentStatus 列表中的状态
     */
    suspend fun getAppointmentDetail(
        appointmentId: String,
        appointmentStatus: String,
    ): PEAppointmentDetailResponse

    /**
     * 获取指定日期的可约时段（含 times_id）
     *
     * @param appointmentId 场次 ID
     * @param appointmentDate 如 "2026-09-29"
     */
    suspend fun getAppointmentTimes(
        appointmentId: String,
        appointmentDate: String,
    ): PEAppointmentTimesResponse

    /**
     * 取消预约
     *
     * @param temporaryId 我的预约记录标识
     * @return 操作响应（含业务 message）
     */
    suspend fun cancelAppointment(temporaryId: String): PEAppointmentActionResponse

    /**
     * 提交体测预约
     *
     * 报文：appointment_id + times_id + enter_date（无 user_id）
     */
    suspend fun enterAppointment(
        appointmentId: String,
        timesId: String,
        enterDate: String,
    ): PEAppointmentActionResponse
}
