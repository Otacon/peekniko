package peekniko.internal.notifications

import peekniko.NetworkCall

internal interface Notifications {
    val callLimit: Int
    fun show(calls: List<NetworkCall>)
    fun requestPermission() {}
}

internal expect fun createNotifications(): Notifications
