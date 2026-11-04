package restarhalf.stellar.schedule.reminder.impl

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import restarhalf.stellar.schedule.core.log.AppLogger
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal object IosReminderNotifications {

    private const val AUTHORIZATION_TIMEOUT_MS = 60_000L
    private const val ADD_TIMEOUT_MS = 10_000L
    private const val QUERY_TIMEOUT_MS = 5_000L

    @OptIn(ExperimentalTime::class)
    suspend fun schedule(
        identifier: String,
        title: String,
        body: String,
        triggerAtMs: Long,
    ): Boolean {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        if (!isAuthorized(center)) {
            AppLogger.log("Reminder", "通知权限未授予，跳过提醒 $identifier")
            return false
        }

        val nowMs = Clock.System.now().toEpochMilliseconds()
        val intervalSeconds = ((triggerAtMs - nowMs).coerceAtLeast(1_000L)).toDouble() / 1000.0

        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = intervalSeconds,
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = identifier,
            content = content,
            trigger = trigger,
        )

        center.removePendingNotificationRequestsWithIdentifiers(listOf(identifier))

        val added = withTimeoutOrNull(ADD_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                center.addNotificationRequest(request, withCompletionHandler = { error ->
                    if (continuation.isActive) continuation.resume(error == null)
                })
            }
        }

        if (added != true) {
            AppLogger.log("Reminder", "添加通知失败 $identifier")
            return false
        }
        return true
    }

    suspend fun isPending(identifier: String): Boolean? {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        return withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                center.getPendingNotificationRequestsWithCompletionHandler { requests ->
                    if (continuation.isActive) {
                        continuation.resume(
                            requests?.any { (it as? UNNotificationRequest)?.identifier == identifier } == true
                        )
                    }
                }
            }
        }
    }

    fun cancel(identifier: String) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(identifier))
    }

    private suspend fun isAuthorized(center: UNUserNotificationCenter): Boolean {
        val status = withTimeoutOrNull(QUERY_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                center.getNotificationSettingsWithCompletionHandler { settings ->
                    if (continuation.isActive) {
                        continuation.resume(settings?.authorizationStatus)
                    }
                }
            }
        }

        return when (status) {
            UNAuthorizationStatusAuthorized,
            UNAuthorizationStatusProvisional,
            UNAuthorizationStatusEphemeral,
                -> true

            UNAuthorizationStatusNotDetermined -> requestAuthorization(center)

            else -> false
        }
    }

    private suspend fun requestAuthorization(center: UNUserNotificationCenter): Boolean =
        withTimeoutOrNull(AUTHORIZATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                center.requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                    completionHandler = { granted, _ ->
                        if (continuation.isActive) continuation.resume(granted)
                    },
                )
            }
        } ?: false
}
