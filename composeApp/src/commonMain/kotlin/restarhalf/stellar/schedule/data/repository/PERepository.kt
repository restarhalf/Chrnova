package restarhalf.stellar.schedule.data.repository

import restarhalf.stellar.schedule.data.remote.PEClient
import restarhalf.stellar.schedule.data.remote.PEDetailResponse
import restarhalf.stellar.schedule.data.remote.PELoginResponse
import restarhalf.stellar.schedule.data.remote.PEScoreListResponse
import restarhalf.stellar.schedule.data.remote.PEStudentInfoResponse

class PERepository(private val peClient: PEClient) {
    suspend fun login(username: String, password: String): PELoginResponse =
        peClient.login(username, password)

    suspend fun getScoreList(): PEScoreListResponse = peClient.getScoreList()

    suspend fun getScoreDetail(schoolYear: String): PEDetailResponse =
        peClient.getScoreDetail(schoolYear)

    suspend fun getStudentInfo(): PEStudentInfoResponse = peClient.getStudentInfo()
}
