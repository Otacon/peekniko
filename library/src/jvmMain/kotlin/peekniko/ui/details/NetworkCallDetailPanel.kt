package peekniko.ui.details

import peekniko.ui.details.request.RequestPanel
import peekniko.ui.details.response.ResponsePanel
import peekniko.ui.details.summary.SummaryPanel
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane

internal class NetworkCallDetailPanel : JPanel(BorderLayout()) {

    private val overview = SummaryPanel()
    private val request = RequestPanel()
    private val response = ResponsePanel()

    private val tabs = JTabbedPane().apply {
        addTab("Headers", overview)
        addTab("Request", request)
        addTab("Response", response)
    }

    private var currentCallId: String? = null
    private var activeTab = 0

    init {
        add(tabs, BorderLayout.CENTER)

        tabs.addChangeListener {
            val selectedTab = tabs.selectedIndex
            if (selectedTab == activeTab) return@addChangeListener

            when (activeTab) {
                0 -> overview.deactivate()
                1 -> request.deactivate()
                2 -> response.deactivate()
            }

            activeTab = selectedTab
            showSelectedTab()
        }
    }

    internal fun showTransaction(id: String) {
        currentCallId = id
        showSelectedTab()
    }

    private fun showSelectedTab() {
        val id = currentCallId ?: return

        when (tabs.selectedIndex) {
            0 -> overview.showTransaction(id)
            1 -> request.showTransaction(id)
            2 -> response.showTransaction(id)
        }
    }

    internal fun dispose() {
        overview.dispose()
        request.dispose()
        response.dispose()
    }
}
