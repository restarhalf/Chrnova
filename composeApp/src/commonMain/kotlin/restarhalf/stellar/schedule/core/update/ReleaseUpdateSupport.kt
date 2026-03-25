package restarhalf.stellar.schedule.core.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal const val GITEE_OWNER = "restarhalf"
internal const val GITEE_REPO = "schedule"
internal const val ANDROID_RELEASE_APK_FILE_NAME = "app-release.apk"
internal const val IOS_RELEASE_IPA_FILE_NAME = "app-release.ipa"

@Serializable
internal data class GiteeLatestReleaseResponse(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    @SerialName("html_url") val htmlUrl: String? = null,
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

internal fun resolvedLatestVersion(response: GiteeLatestReleaseResponse): String =
    response.tagName.ifBlank { response.name }.trim()

internal fun buildGiteeLatestReleaseApi(): String =
    "https://gitee.com/api/v5/repos/$GITEE_OWNER/$GITEE_REPO/releases/latest"

internal fun buildGiteeReleasePageUrl(version: String): String =
    "https://gitee.com/$GITEE_OWNER/$GITEE_REPO/releases/tag/$version"

internal fun buildGiteeReleaseAssetUrl(
    version: String,
    fileName: String,
): String = "https://gitee.com/$GITEE_OWNER/$GITEE_REPO/releases/download/$version/$fileName"
