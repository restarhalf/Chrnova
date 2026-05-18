package restarhalf.stellar.schedule.mcp

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.data.remote.JwxtAuthStore
import restarhalf.stellar.schedule.domain.usecase.GetCampusUseCase
import restarhalf.stellar.schedule.domain.usecase.GetTermStartMsUseCase
import restarhalf.stellar.schedule.domain.usecase.GetTotalWeeksUseCase

class RuntimeContextProvider(
    private val authStore: JwxtAuthStore,
    private val getCampus: GetCampusUseCase,
    private val getTermStartMs: GetTermStartMsUseCase,
    private val getTotalWeeks: GetTotalWeeksUseCase,
    private val toolsRegistry: ToolsRegistry,
) {
    fun snapshot(): JsonObject {
        val termStartMs = getTermStartMs()
        val totalWeeks = getTotalWeeks()
        val week = WeekCalculator.detect(totalWeeks = totalWeeks, termStartMs = termStartMs)
        return buildJsonObject {
            put("runtimeId", "default")
            put("platform", "kotlin-multiplatform")
            put("loginStatus", if (authStore.getToken() == null) "logged_out" else "logged_in")
            put("userNo", authStore.getUserNo().orEmpty())
            put("campus", getCampus().name)
            put("termStartMs", termStartMs)
            put("totalWeeks", totalWeeks)
            put("currentWeek", week.week)
            put("isHoliday", week.isHoliday)
            put("toolCount", toolsRegistry.listTools().size)
            put("toolSchemaVersion", "2026-05-17")
        }
    }
}
