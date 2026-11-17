@file:OptIn(BetaInteropApi::class)
package peekniko

import kotlinx.cinterop.BetaInteropApi
import peekniko.internal.notifications.IosNotifications
import peekniko.internal.support.PeeknikoPresenter
import peekniko.internal.urlsession.PeeknikoURLProtocol
import platform.Foundation.NSURLProtocol
import platform.Foundation.NSURLSessionConfiguration
import platform.UIKit.UIViewController
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationResponse

/** Presents the Peekniko inspector full screen from [from], or from the top-most view controller. */
public fun Peekniko.open(from: UIViewController? = null) {
    PeeknikoPresenter.present(from)
}

public fun Peekniko.isPeeknikoNotification(notification: UNNotification): Boolean =
    notification.request.identifier == IosNotifications.NOTIFICATION_ID

public fun Peekniko.handleNotificationResponse(response: UNNotificationResponse): Boolean {
    if (!isPeeknikoNotification(response.notification)) return false
    open()
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
