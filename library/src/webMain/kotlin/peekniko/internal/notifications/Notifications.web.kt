package peekniko.internal.notifications

import peekniko.NetworkCall

internal actual fun createNotifications(): Notifications = object : Notifications {
    override val callLimit: Int = 1
    override fun show(calls: List<NetworkCall>) {}
}
