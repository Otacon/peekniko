package peekniko.internal.notifications

import peekniko.NetworkCall
import peekniko.NetworkResponse
import peekniko.ui.isPeeknikoWindowActive
import peekniko.ui.openPeeknikoWindow
import java.awt.Color
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.MenuItem
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private var activeJvmNotifications: JvmNotifications? = null

internal actual fun createNotifications(): Notifications = JvmNotifications().also {
    activeJvmNotifications = it
}

internal fun onCallListWindowOpened() {
    activeJvmNotifications?.onWindowOpened()
}

private class JvmNotifications : Notifications {
    private enum class Status { SUCCESS, WARNING, FAILURE }

    private val unreadCalls = LinkedHashMap<String, Status>()
    private val trayIcon: TrayIcon? = createTrayIcon()
    override val callLimit: Int = 1

    @Synchronized
    override fun show(calls: List<NetworkCall>) {
        if (trayIcon == null || isPeeknikoWindowActive() || GraphicsEnvironment.isHeadless()) return
        if (calls.isEmpty()) unreadCalls.clear()
        calls.forEach { call ->
            unreadCalls[call.id] = when (val response = call.response) {
                is NetworkResponse.Error -> Status.FAILURE
                is NetworkResponse.Response -> if (response.code !in 200..299) Status.WARNING else Status.SUCCESS
                null -> Status.SUCCESS
            }
        }
        updateTrayIcon()
    }

    @Synchronized
    fun onWindowOpened() {
        unreadCalls.clear()
        updateTrayIcon()
    }

    private fun createTrayIcon(): TrayIcon? {
        if (!SystemTray.isSupported()) return null
        return runCatching {
            val menu = PopupMenu().apply {
                add(MenuItem("Open Peekniko").apply { addActionListener { openPeeknikoWindow() } })
            }
            TrayIcon(createTrayImage(), "Peekniko", menu).apply {
                isImageAutoSize = true
                toolTip = "Peekniko — no new network calls"
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(event: MouseEvent) {
                        if (event.button == MouseEvent.BUTTON1) openPeeknikoWindow()
                    }
                })
                SystemTray.getSystemTray().add(this)
            }
        }.getOrNull()
    }

    private fun updateTrayIcon() {
        val icon = trayIcon ?: return
        icon.image = createTrayImage()
        icon.toolTip = if (unreadCalls.isEmpty()) {
            "Peekniko — no new network calls"
        } else {
            "Peekniko — ${unreadCalls.size} new network calls"
        }
    }

    private fun createTrayImage(): BufferedImage {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val appIcon = javaClass.getResourceAsStream("/tray-icon.png")?.use(ImageIO::read)
        if (appIcon != null) {
            var minX = appIcon.width
            var minY = appIcon.height
            var maxX = -1
            var maxY = -1
            for (y in 0 until appIcon.height) {
                for (x in 0 until appIcon.width) {
                    if ((appIcon.getRGB(x, y) ushr 24) > 10) {
                        minX = minOf(minX, x)
                        minY = minOf(minY, y)
                        maxX = maxOf(maxX, x)
                        maxY = maxOf(maxY, y)
                    }
                }
            }
            if (maxX >= minX && maxY >= minY) {
                val silhouette = appIcon.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1)
                val scale = 24.0 / maxOf(silhouette.width, silhouette.height)
                val width = (silhouette.width * scale).toInt()
                val height = (silhouette.height * scale).toInt()
                graphics.drawImage(
                    silhouette,
                    (image.width - width) / 2,
                    (image.height - height) / 2,
                    width,
                    height,
                    null
                )
            }
        } else {
            graphics.color = Color(45, 115, 190)
            graphics.fillRoundRect(1, 1, 30, 30, 8, 8)
            graphics.color = Color.WHITE
            graphics.fillOval(8, 8, 16, 16)
        }

        if (unreadCalls.isNotEmpty()) {
            val badgeColor = when {
                unreadCalls.values.any { it == Status.FAILURE } -> Color(205, 45, 45)
                unreadCalls.values.any { it == Status.WARNING } -> Color(
                    225,
                    160,
                    20
                )

                else -> Color(45, 165, 85)
            }
            val count = if (unreadCalls.size > 99) "99+" else unreadCalls.size.toString()
            val badgeWidth = if (count.length > 2) 22 else 18
            val badgeHeight = 18
            val badgeX = image.width - badgeWidth
            val badgeY = image.height - badgeHeight
            graphics.color = badgeColor
            graphics.fillOval(badgeX, badgeY, badgeWidth, badgeHeight)
            graphics.color = Color.WHITE
            graphics.font = Font("Dialog", Font.BOLD, if (count.length > 2) 8 else 9)
            val metrics = graphics.fontMetrics
            graphics.drawString(
                count,
                badgeX + (badgeWidth - metrics.stringWidth(count)) / 2,
                badgeY + (badgeHeight - metrics.height) / 2 + metrics.ascent,
            )
        }

        graphics.dispose()
        return image
    }
}
