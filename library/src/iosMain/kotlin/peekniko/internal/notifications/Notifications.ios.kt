package peekniko.internal.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import peekniko.NetworkCall
import peekniko.NetworkResponse
import peekniko.Peekniko
import peekniko.handleNotificationResponse
import peekniko.isPeeknikoNotification
import platform.Foundation.NSBundle
import platform.Foundation.NSSelectorFromString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationInterruptionLevel
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionNone
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume

internal actual fun createNotifications(): Notifications = IosNotifications()

internal class IosNotifications : Notifications {
    internal companion object {
        const val NOTIFICATION_ID = "peekniko_network_calls"
        private const val TITLE = "Peekniko Network Monitor"
        private const val STATUS_ERROR = "!!!"
        private const val STATUS_PENDING = "..."
    }

    override val callLimit: Int = 5

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val updates = Channel<List<NetworkCall>>(Channel.CONFLATED)
    private val postLock = Mutex()
    private val delegate = PeeknikoNotificationCenterDelegate()
    private var latestCalls: List<NetworkCall> = emptyList()

    private val center: UNUserNotificationCenter?
        get() = NSBundle.mainBundle.bundleIdentifier?.let { UNUserNotificationCenter.currentNotificationCenter() }

    init {
        scope.launch {
            center?.let(::installDelegateIfAbsent)
            for (calls in updates) {
                latestCalls = calls
                postLatestCalls()
            }
        }
    }

    override fun show(calls: List<NetworkCall>) {
        updates.trySend(calls)
    }

    override fun requestPermission() {
        scope.launch {
            val center = center ?: return@launch
            if (center.authorizationStatus() != UNAuthorizationStatusNotDetermined) return@launch
            if (center.requestAuthorization()) postLatestCalls()
        }
    }

    private suspend fun postLatestCalls() {
        val center = center ?: return
        postLock.withLock {
            val calls = latestCalls
            if (calls.isEmpty()) {
                center.removeDeliveredNotificationsWithIdentifiers(listOf(NOTIFICATION_ID))
                return
            }
            val status = center.authorizationStatus()
            if (status == UNAuthorizationStatusNotDetermined || status == UNAuthorizationStatusDenied) return
            installDelegateIfAbsent(center)
            // Reusing the identifier replaces the notification that is already delivered.
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = NOTIFICATION_ID,
                content = buildContent(calls),
                trigger = null,
            )
            center.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("Peekniko: unable to show network activity notification: ${error.localizedDescription}")
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun buildContent(calls: List<NetworkCall>): UNMutableNotificationContent =
        UNMutableNotificationContent().apply {
            setTitle(TITLE)
            setBody(calls.joinToString(separator = "\n", transform = ::formatCall))
            if (respondsToSelector(NSSelectorFromString("setInterruptionLevel:"))) {
                setInterruptionLevel(UNNotificationInterruptionLevel.UNNotificationInterruptionLevelPassive)
            }
        }

    private fun formatCall(call: NetworkCall): String {
        val path = call.request.endpoint.takeIf { it.isNotEmpty() } ?: "/"
        val code = when (val response = call.response) {
            is NetworkResponse.Response -> response.code.toString()
            is NetworkResponse.Error -> STATUS_ERROR
            null -> STATUS_PENDING
        }
        return "$code ${call.request.method} $path"
    }

    private fun installDelegateIfAbsent(center: UNUserNotificationCenter) {
        if (center.delegate == null) center.delegate = delegate
    }
}

private suspend fun UNUserNotificationCenter.authorizationStatus(): UNAuthorizationStatus =
    suspendCancellableCoroutine { continuation ->
        getNotificationSettingsWithCompletionHandler { settings ->
            continuation.resume(settings?.authorizationStatus ?: UNAuthorizationStatusDenied)
        }
    }

private suspend fun UNUserNotificationCenter.requestAuthorization(): Boolean =
    suspendCancellableCoroutine { continuation ->
        requestAuthorizationWithOptions(UNAuthorizationOptionAlert) { granted, _ ->
            continuation.resume(granted)
        }
    }

private class PeeknikoNotificationCenterDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            if (Peekniko.isPeeknikoNotification(willPresentNotification)) {
                UNNotificationPresentationOptionList
            } else {
                UNNotificationPresentationOptionNone
            }
        )
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        Peekniko.handleNotificationResponse(didReceiveNotificationResponse)
        withCompletionHandler()
    }
}
