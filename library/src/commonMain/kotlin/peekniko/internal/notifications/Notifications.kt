package peekniko.internal.notifications

import peekniko.NetworkCall

internal interface Notifications {
    val callLimit: Int
    fun show(calls: List<NetworkCall>)

    /**
     * Called when the Peekniko screen opens. Platforms that need a runtime permission to post
     * notifications ask for it here, unless the host app has already settled it.
     */
    fun requestPermission() {}
}

internal expect fun createNotifications(): Notifications
