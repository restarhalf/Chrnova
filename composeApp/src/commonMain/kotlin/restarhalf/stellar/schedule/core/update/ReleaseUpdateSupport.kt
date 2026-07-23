package restarhalf.stellar.schedule.core.update

import kotlinx.serialization.Serializable

internal const val VERSION_WORKER_URL = "https://chrnova.version.restarhalf.dpdns.org"
internal const val QUARK_SHARE_URL = "https://pan.quark.cn/s/2326de687ab1?pwd=E97u"

@Serializable
internal data class VersionJsonResponse(
    val version: String = "",
    val changelog: String = "",
)

internal fun normalizeVersion(version: String): List<Int> {
    val pure = version.trim().removePrefix("v").removePrefix("V")
    return pure
        .split(Regex("[^0-9]+"))
        .filter { it.isNotBlank() }
        .map { it.toIntOrNull() ?: 0 }
}

internal fun isNewerVersion(latest: String, current: String): Boolean {
    val latestParts = normalizeVersion(latest)
    val currentParts = normalizeVersion(current)
    val size = maxOf(latestParts.size, currentParts.size)
    for (index in 0 until size) {
        val latestValue = latestParts.getOrElse(index) { 0 }
        val currentValue = currentParts.getOrElse(index) { 0 }
        if (latestValue != currentValue) return latestValue > currentValue
    }
    return false
}

internal fun buildVersionWorkerUrl(): String = "$VERSION_WORKER_URL/version.json"
