package peekniko.ui

import peekniko.ui.details.NetworkCallDetailPanel
import peekniko.ui.networkcalls.NetworkCallListPanel
import peekniko.internal.support.isSystemDarkMode
import peekniko.internal.notifications.onCallListWindowOpened
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JSplitPane

private var peeknikoFrame: JFrame? = null

internal fun isPeeknikoWindowVisible(): Boolean = peeknikoFrame?.let {
    it.isShowing && (it.extendedState and JFrame.ICONIFIED) == 0
} == true

internal fun isPeeknikoWindowActive(): Boolean = peeknikoFrame?.isActive == true

public fun openPeeknikoWindow() {
    onCallListWindowOpened()
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
        peeknikoFrame?.let {
            it.isVisible = true
            it.toFront()
            return@launch
        }

        if (isSystemDarkMode()) FlatDarkLaf.setup() else FlatLightLaf.setup()
        val frame = JFrame("Peekniko")
        val detailPanel = NetworkCallDetailPanel()
        val listPanel = NetworkCallListPanel(detailPanel::showTransaction)
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.layout = BorderLayout()
        frame.add(JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, detailPanel).apply {
            resizeWeight = 0.4
            dividerLocation = 400
        }, BorderLayout.CENTER)
        frame.minimumSize = Dimension(760, 420)
        frame.setSize(1000, 600)
        frame.setLocationRelativeTo(null)
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                listPanel.dispose()
                detailPanel.dispose()
                peeknikoFrame = null
            }
        })
        peeknikoFrame = frame
        frame.isVisible = true
    }
}
