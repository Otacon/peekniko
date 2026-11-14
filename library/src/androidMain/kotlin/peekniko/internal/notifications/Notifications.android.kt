package peekniko.internal.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import peekniko.Peekniko
import peekniko.NetworkCall
import peekniko.NetworkResponse
import peekniko.context
import peekniko.library.R
import peekniko.ui.networkcalls.PeeknikoActivity

internal actual fun createNotifications(): Notifications = AndroidNotifications(Peekniko.context)

internal class AndroidNotifications(context: Context) : Notifications {
    private companion object {
        const val CHANNEL_ID = "peekniko_network_calls"
        const val NOTIFICATION_TAG = "AndroidNotifications"
        const val NOTIFICATION_ID = 0
    }

    override val callLimit: Int = 5
    private val context: Context = context.applicationContext
    private var channelRegistered = false

    private val manager: NotificationManagerCompat?
        get() {
            val application = context
            return try {
                (application.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                    .also { if (it == null) Log.e(NOTIFICATION_TAG, "Notification service unavailable") }
                    ?.let { NotificationManagerCompat.from(application) }
            } catch (exception: RuntimeException) {
                Log.e(NOTIFICATION_TAG, "Unable to access notification service", exception)
                null
            }
        }

    @Synchronized
    internal fun registerChannel() {
        if (channelRegistered) return
        val manager = manager ?: return
        try {
            manager.createNotificationChannel(
                NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.peekniko_notification_channel))
                    .setSound(null, null)
                    .setVibrationEnabled(false)
                    .build()
            )
            channelRegistered = true
        } catch (exception: RuntimeException) {
            Log.e(NOTIFICATION_TAG, "Unable to register notification channel", exception)
        }
    }

    @Synchronized
    override fun show(calls: List<NetworkCall>) {
        val manager = manager ?: return
        try {
            if (calls.isEmpty()) {
                manager.cancel(NOTIFICATION_TAG, NOTIFICATION_ID)
                return
            }

            if (Build.VERSION.SDK_INT < 33 || context.applicationInfo.targetSdkVersion >= 33) {
                registerChannel()
            }
            val lines = calls.map(::formatCall)
            when {
                Build.VERSION.SDK_INT >= 33 -> manager.showApi33AndAbove(lines)
                Build.VERSION.SDK_INT >= 26 -> manager.showApi26To32(lines)
                Build.VERSION.SDK_INT >= 24 -> manager.showApi24To25(lines)
                Build.VERSION.SDK_INT >= 23 -> manager.showApi23(lines)
                else -> manager.showApi22(lines)
            }
        } catch (exception: RuntimeException) {
            Log.e(NOTIFICATION_TAG, "Unable to show network activity notification", exception)
        }
    }

    private fun formatCall(call: NetworkCall): String {
        val path = call.request.endpoint.takeIf { it.isNotEmpty() } ?: "/"
        val code = when(val response = call.response) {
            is NetworkResponse.Response -> response.code.toString()
            is NetworkResponse.Error -> context.getString(R.string.peekniko_status_error)
            null -> context.getString(R.string.peekniko_status_pending)
        }
        return "$code ${call.request.method} $path"
    }

    private fun NotificationManagerCompat.showApi22(lines: List<String>) {
        if (!areNotificationsEnabled()) return
        val notification = context.buildNotificationApi22AndAbove(
            lines, context.contentIntentApi22(),
        )
        notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
    }

    @RequiresApi(23)
    private fun NotificationManagerCompat.showApi23(lines: List<String>) {
        if (!areNotificationsEnabled()) return
        val notification = context.buildNotificationApi22AndAbove(
            lines, context.contentIntentApi23AndAbove(),
        )
        notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
    }

    @RequiresApi(24)
    private fun NotificationManagerCompat.showApi24To25(lines: List<String>) {
        if (!areNotificationsEnabled()) return
        val notification = context.buildNotificationApi22AndAbove(
            lines, context.contentIntentApi23AndAbove(),
        )
        notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
    }

    @RequiresApi(26)
    private fun NotificationManagerCompat.showApi26To32(lines: List<String>) {
        if (!areNotificationsEnabled()) return
        val notification = context.buildNotificationApi22AndAbove(
            lines, context.contentIntentApi23AndAbove(),
        )
        notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
    }

    @RequiresApi(33)
    private fun NotificationManagerCompat.showApi33AndAbove(lines: List<String>) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        if (!areNotificationsEnabled()) return
        if (context.applicationInfo.targetSdkVersion <= 32 && getNotificationChannel(CHANNEL_ID) == null) return
        val notification = context.buildNotificationApi22AndAbove(
            lines, context.contentIntentApi23AndAbove(),
        )
        notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification)
    }

    private fun Context.activityIntentApi22AndAbove(): Intent =
        Intent(this, PeeknikoActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private fun Context.contentIntentApi22(): PendingIntent = PendingIntent.getActivity(
        this, 0, activityIntentApi22AndAbove(), PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @RequiresApi(23)
    private fun Context.contentIntentApi23AndAbove(): PendingIntent = PendingIntent.getActivity(
        this, 0, activityIntentApi22AndAbove(),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun Context.buildNotificationApi22AndAbove(
        lines: List<String>,
        contentIntent: PendingIntent,
    ): Notification {
        val title = getString(R.string.peekniko_notification_title)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        lines.forEach { style.addLine(it) }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_peekniko_notification)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

}
