package peekniko

import platform.Foundation.NSURLSessionConfiguration
import platform.UIKit.UIViewController
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationResponse

public fun Peekniko.open(from: UIViewController? = null): Unit = Unit

public fun Peekniko.isPeeknikoNotification(notification: UNNotification): Boolean = false

public fun Peekniko.handleNotificationResponse(response: UNNotificationResponse): Boolean = false

public fun Peekniko.registerUrlProtocol(): Boolean = true

public fun Peekniko.unregisterUrlProtocol(): Unit = Unit

public fun Peekniko.addUrlProtocol(configuration: NSURLSessionConfiguration): Unit = Unit
