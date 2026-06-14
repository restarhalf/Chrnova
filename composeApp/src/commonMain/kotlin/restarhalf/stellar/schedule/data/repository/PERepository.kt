package restarhalf.stellar.schedule.data.repository

import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.remote.PEDetailResponse
import restarhalf.stellar.schedule.data.remote.PELoginResponse
import restarhalf.stellar.schedule.data.remote.PEScoreListResponse
import restarhalf.stellar.schedule.data.remote.PEStudentInfoResponse

/**
 * 体育数据仓库
 *
 * 封装体育系统的数据访问逻辑。
 *
 * @param peGateway 体育系统网关
 */
class PERepository(private val peGateway: PEGateway) {
    suspend fun login(username: String, password: String): PELoginResponse =
        peGateway.login(username, password)

    suspend fun getScoreList(): PEScoreListResponse = peGateway.getScoreList()

    suspend fun getScoreDetail(schoolYear: String): PEDetailResponse =
        peGateway.getScoreDetail(schoolYear)

    suspend fun getStudentInfo(): PEStudentInfoResponse = peGateway.getStudentInfo()
}
