package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.ReminderSchedulerPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 同步课程用例
 * 
 * 执行教务系统课程同步的完整流程：
 * 1. 确保用户已登录
 * 2. 获取当前学期
 * 3. 匹配校区
 * 4. 执行同步
 * 5. 刷新提醒
 */
class RunSyncUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
    private val timetable: TimetablePort,
    private val settings: SettingsPort,
    private val sync: SyncPort,
    private val reminderScheduler: ReminderSchedulerPort,
) {

    /**
     * 执行同步操作
     *
     * 流程：
     * 1. 确保已登录
     * 2. 同步当前校区的课程
     * 3. 如果当前校区课程数为 0，自动切换到另一个校区重试
     *
     * @return 同步结果
     * @throws IllegalStateException 同步失败时抛出
     */
    suspend operator fun invoke(): SyncResult {
        authWorkflow.ensureLoggedIn()

        val selectedTerm = settings.observeSelectedTerm().first()
        val semesterId =
            selectedTerm.ifBlank { academic.fetchCurrentTermId() }

        val localCampus = timetable.getCampus()

        val campuses = academic.fetchCampuses()
        if (campuses.isEmpty()) {
            AppLogger.log("Sync", "同步失败: 校区列表为空", level = AppLogger.Level.ERROR)
            throw IllegalStateException("校区列表为空")
        }

        val campus =
            campuses.firstOrNull { isCampusNameMatch(local = localCampus, remoteName = it.name) }
                ?: campuses.firstOrNull { it.isDefault }
                ?: campuses.firstOrNull()

        if (campus == null || campus.id.isBlank() || campus.name.isBlank()) {
            AppLogger.log("Sync", "同步失败: 获取校区失败", level = AppLogger.Level.ERROR)
            throw IllegalStateException("获取校区失败")
        }

        val result = syncWithRetry(semesterId, campus, campuses)

        reminderScheduler.scheduleNow()

        return result
    }

    /**
     * 执行同步，当前校区课程数为 0 时自动切换到另一个校区重试
     */
    private suspend fun syncWithRetry(
        semesterId: String,
        campus: restarhalf.stellar.schedule.domain.model.RemoteCampus,
        allCampuses: List<restarhalf.stellar.schedule.domain.model.RemoteCampus>,
    ): SyncResult {
        val result = doSync(semesterId, campus)

        if (result.inserted == 0 && allCampuses.size > 1) {
            val fallback = allCampuses.firstOrNull { it.id != campus.id && it.id.isNotBlank() }
            if (fallback != null) {
                AppLogger.log(
                    "Sync",
                    "当前校区「${campus.name}」课程数为 0，切换到「${fallback.name}」重试"
                )
                val fallbackResult = doSync(semesterId, fallback)
                if (fallbackResult.inserted > 0) {
                    val targetCampus = matchLocalCampus(fallback.name)
                    if (targetCampus != null) {
                        timetable.setCampus(targetCampus)
                        AppLogger.log("Sync", "已自动切换校区至「${fallback.name}」")
                    }
                    return fallbackResult.copy(campusName = fallback.name)
                }
                AppLogger.log("Sync", "备用校区「${fallback.name}」课程数也为 0，保留原校区")
            }
        }

        return result.copy(campusName = campus.name)
    }

    /**
     * 执行单次同步，失败时刷新会话重试
     */
    private suspend fun doSync(
        semesterId: String,
        campus: restarhalf.stellar.schedule.domain.model.RemoteCampus,
    ): SyncResult {
        val firstAttempt =
            runCatching { sync.sync(semesterId = semesterId, campusId = campus.id, week = "all") }
        return if (firstAttempt.isSuccess) {
            firstAttempt.getOrThrow()
        } else {
            val ex = firstAttempt.exceptionOrNull()
            if (ex != null) {
                if (ex.isNetworkError()) {
                    AppLogger.log("Sync", "同步网络错误", ex)
                    throw ex
                }
                AppLogger.log("Sync", "同步失败，刷新会话重试", ex)
            }
            authWorkflow.refreshSession()
            sync.sync(semesterId = semesterId, campusId = campus.id, week = "all")
        }
    }

    /**
     * 根据远程校区名称反推本地 Campus 枚举
     */
    private fun matchLocalCampus(remoteName: String): Campus? {
        val name = remoteName.trim()
        return when {
            name.contains("开发区") -> Campus.Development
            name.contains("金石滩") -> Campus.Jinshitan
            else -> null
        }
    }

    /**
     * 检查校区名称是否匹配
     * 
     * @param local 本地校区
     * @param remoteName 远程校区名称
     * @return 是否匹配
     */
    private fun isCampusNameMatch(local: Campus, remoteName: String): Boolean {
        val name = remoteName.trim()
        return when (local) {
            Campus.Development -> name.contains("开发区")
            Campus.Jinshitan -> name.contains("金石滩")
        }
    }
}
