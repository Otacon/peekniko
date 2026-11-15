@file:OptIn(BetaInteropApi::class)
package peekniko

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.BetaInteropApi
import peekniko.internal.notifications.IosNotifications
import peekniko.internal.support.PeeknikoPresenter
import peekniko.internal.urlsession.PeeknikoURLProtocol
import peekniko.ui.Screen
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLSessionConfiguration
import platform.UIKit.UIViewController
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationResponse

public fun Peekniko.viewController(onClose: () -> Unit): UIViewController =
    ComposeUIViewController(
        configure = {
            // Peekniko runs inside host apps whose Info.plist it does not control, so a missing
            // CADisableMinimumFrameDurationOnPhone entry must not crash them.
            enforceStrictPlistSanityCheck = false
        },
    ) {
        Peekniko.Screen(modifier = Modifier.fillMaxSize(), onClose = onClose)
    }

public fun Peekniko.present(from: UIViewController? = null) {
    PeeknikoPresenter.present(from)
}

public fun Peekniko.isPeeknikoNotification(notification: UNNotification): Boolean =
    notification.request.identifier == IosNotifications.NOTIFICATION_ID

public fun Peekniko.handleNotificationResponse(response: UNNotificationResponse): Boolean {
    if (!isPeeknikoNotification(response.notification)) return false
    present()
    return true
}

public fun Peekniko.registerUrlProtocol(): Boolean =
    NSURLProtocol.registerClass(PeeknikoURLProtocol.`class`()!!)

public fun Peekniko.unregisterUrlProtocol() {
    NSURLProtocol.unregisterClass(PeeknikoURLProtocol.`class`()!!)
}

public fun Peekniko.addUrlProtocol(configuration: NSURLSessionConfiguration) {
    configuration.protocolClasses = listOf(PeeknikoURLProtocol.`class`()!!) + configuration.protocolClasses.orEmpty()
}
