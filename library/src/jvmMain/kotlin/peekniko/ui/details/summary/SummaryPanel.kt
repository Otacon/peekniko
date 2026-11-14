package peekniko.ui.details.summary

import peekniko.Peekniko
import peekniko.internal.bodyrenderer.setSpannedText
import peekniko.internal.support.ViewModelStore
import peekniko.internal.support.initializer
import peekniko.internal.support.viewModelFactory
import peekniko.ui.details.SummaryViewModel
import peekniko.ui.views.CodePane
import com.formdev.flatlaf.FlatClientProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane


internal class SummaryPanel : JPanel(BorderLayout()) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var viewModelStore = ViewModelStore()
    private var viewModel: SummaryViewModel? = null
    private var transactionId: String? = null
    private var observation: Job? = null

    private val general = SummarySection("General")
    private val requestHeaders = SummarySection("Request Headers")
    private val responseHeaders = SummarySection("Response Headers")

    init {
        val sections = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(general)
            add(requestHeaders)
            add(responseHeaders)
        }

        val content = JPanel(BorderLayout()).apply { add(sections, BorderLayout.NORTH) }

        add(JScrollPane(content), BorderLayout.CENTER)
    }

    internal fun showTransaction(id: String) {
        if (transactionId == id && observation?.isActive == true) return

        deactivate()

        if (transactionId != id) {
            viewModelStore.clear()
            viewModelStore = ViewModelStore()
            viewModel = viewModelStore.getOrCreate(
                SummaryViewModel::class,
                viewModelFactory {
                    initializer { Peekniko.createSummaryViewModel(id) }
                },
            )
            transactionId = id
        }

        val vm = checkNotNull(viewModel)
        observation = scope.launch {
            vm.state.collect { state ->
                general.code.setSpannedText(state.general)
                requestHeaders.code.setSpannedText(state.requestHeaders)
                responseHeaders.code.setSpannedText(state.responseHeaders)
            }
        }
    }

    internal fun deactivate() {
        observation?.cancel()
        observation = null
    }

    internal fun dispose() {
        deactivate()
        scope.cancel()
        viewModelStore.clear()
        viewModel = null
        transactionId = null
    }
}

private class SummarySection(title: String) : JPanel(BorderLayout(0, 8)) {

    val code = CodePane()

    init {
        alignmentX = LEFT_ALIGNMENT
        border = BorderFactory.createEmptyBorder(12, 16, 8, 16)

        add(JLabel(title).apply {
            putClientProperty(FlatClientProperties.STYLE_CLASS, "h3")
        }, BorderLayout.NORTH)

        add(code, BorderLayout.CENTER)
    }
}
