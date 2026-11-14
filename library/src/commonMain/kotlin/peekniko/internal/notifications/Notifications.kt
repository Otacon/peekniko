package peekniko.internal.notifications

import peekniko.NetworkCall

internal interface Notifications {
    val callLimit: Int
    fun show(calls: List<NetworkCall>)
}

internal expect fun createNotifications(): Notifications
