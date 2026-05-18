package restarhalf.stellar.schedule.mcp

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlin.random.Random

class McpRuntimeIdProvider(
    private val settings: Settings,
) {
    fun runtimeId(): String {
        val existing = settings.getStringOrNull(KEY_RUNTIME_ID)
        if (!existing.isNullOrBlank()) return existing
        val created = "chrnova-client-${newIdSuffix()}"
        settings[KEY_RUNTIME_ID] = created
        return created
    }

    private fun newIdSuffix(): String = List(32) { Random.nextInt(16).toString(16) }.joinToString("")

    private companion object {
        const val KEY_RUNTIME_ID = "mcp_runtime_id"
    }
}
