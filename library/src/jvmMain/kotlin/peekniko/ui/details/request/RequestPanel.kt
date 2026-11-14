package peekniko.ui.details.request

import peekniko.Peekniko
import peekniko.internal.bodyrenderer.setSpannedText
import peekniko.internal.support.ViewModelStore
import peekniko.internal.support.initializer
import peekniko.internal.support.viewModelFactory
import peekniko.ui.details.RequestViewModel
import peekniko.ui.views.CodePane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JScrollPane


internal class RequestPanel : JPanel(BorderLayout()) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var viewModelStore = ViewModelStore()
    private var transactionId: String? = null
    private var observation: Job? = null

    private val content = CodePane()

    init {
        val container = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
            add(content, BorderLayout.NORTH)
        }

        add(JScrollPane(container), BorderLayout.CENTER)
    }

    internal fun showTransaction(id: String) {
        if (transactionId == id && observation?.isActive == true) return

        deactivate()

        if (transactionId != id) {
            viewModelStore.clear()
            viewModelStore = ViewModelStore()
            transactionId = id
            content.text = ""
        }

        val viewModel = viewModelStore.getOrCreate(
            RequestViewModel::class,
            viewModelFactory {
                    initializer { Peekniko.createRequestViewModel(id) }
            },
        )

        observation = scope.launch {
            viewModel.state.collect { state ->
                content.setSpannedText(state.body)
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
        transactionId = null
    }
}
