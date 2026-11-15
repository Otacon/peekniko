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

/**
 * Posts a single, quiet local notification listing the latest network calls.
 *
 * Like on Android, showing a notification never asks for permission: nothing is posted until the
 * app has been authorized, either by the host app itself or by [requestPermission], which runs
 * when the Peekniko screen opens.
 */
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

    // The notification center needs an app bundle, which command-line processes such as unit
    // tests do not have.
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
            // Passive notifications (iOS 15+) go straight to the notification list without a
            // banner or sound, matching the low-importance channel used on Android.
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

    /**
     * iOS drops notifications posted while the app is in the foreground unless a delegate opts in,
     * and the delegate is also what receives taps. Apps that install their own delegate are left
     * alone and can forward to [Peekniko.isPeeknikoNotification] and
     * [Peekniko.handleNotificationResponse].
     */
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
