package restarhalf.stellar.schedule.domain.policy

import restarhalf.stellar.schedule.domain.model.Course

fun Course.familyKey(): String =
    originRemoteKey?.takeIf { it.isNotBlank() } ?: remoteKey

fun Course.isTransOverrideOf(familyKey: String): Boolean =
    type == 2 && familyKey.isNotBlank() && originRemoteKey == familyKey

fun Course.belongsToFamily(familyKey: String): Boolean =
    familyKey.isNotBlank() && (remoteKey == familyKey || originRemoteKey == familyKey)
