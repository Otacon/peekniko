package peekniko.internal.support

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import peekniko.Peekniko
import peekniko.internal.notifications.onCallListWindowOpened
import peekniko.ui.Screen
import java.awt.Dimension
import java.awt.EventQueue
import java.awt.Frame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.WindowConstants

internal object PeeknikoWindow {
    // Written only on the AWT event dispatch thread; read from the notification thread.
    @Volatile
    private var window: ComposeWindow? = null

    val isActive: Boolean
        get() = window?.isActive == true

    fun open() {
        onCallListWindowOpened()
        if (EventQueue.isDispatchThread()) showWindow() else EventQueue.invokeLater(::showWindow)
    }

    private fun showWindow() {
        window?.let { existing ->
            existing.extendedState = existing.extendedState and Frame.ICONIFIED.inv()
            existing.isVisible = true
            existing.toFront()
            existing.requestFocus()
            return
        }

        val newWindow = ComposeWindow()
        newWindow.title = "Peekniko"
        newWindow.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        newWindow.minimumSize = Dimension(360, 420)
        newWindow.setSize(1000, 600)
        newWindow.setLocationRelativeTo(null)
        newWindow.setContent {
            Peekniko.Screen(
                modifier = Modifier.fillMaxSize(),
                onClose = { newWindow.dispose() },
            )
        }
        newWindow.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                if (window === newWindow) window = null
            }
        })
        window = newWindow
        newWindow.isVisible = true
    }
}
