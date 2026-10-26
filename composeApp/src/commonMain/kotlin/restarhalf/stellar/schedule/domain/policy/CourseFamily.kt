package restarhalf.stellar.schedule.domain.policy

import restarhalf.stellar.schedule.domain.model.Course

/**
 * 一门课和它的所有调课记录共用的归属键。
 *
 * 调课记录本身再被调课时，要退回它最初那门课，否则会认不出同一组记录。
 */
fun Course.familyKey(): String =
    originRemoteKey?.takeIf { it.isNotBlank() } ?: remoteKey

/** 是否是归属键为 [familyKey] 那门课的调课记录。 */
fun Course.isTransOverrideOf(familyKey: String): Boolean =
    type == 2 && familyKey.isNotBlank() && originRemoteKey == familyKey

/** 是否是归属键为 [familyKey] 那门课本身，或它的调课记录。 */
fun Course.belongsToFamily(familyKey: String): Boolean =
    familyKey.isNotBlank() && (remoteKey == familyKey || originRemoteKey == familyKey)
