package peekniko.ui.networkcalls

import peekniko.Peekniko
import peekniko.NetworkMethod
import peekniko.internal.support.ViewModelStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.EventQueue
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

internal class NetworkCallListPanel(
    private val onTransactionSelected: (String) -> Unit,
) : JPanel(BorderLayout()) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val viewModelStore = ViewModelStore()
    private val viewModel = Peekniko.getPeeknikoViewModel(viewModelStore)
    private val listModel = NetworkCallListModel()
    private val list = JList(listModel).apply {
        cellRenderer = NetworkCallListCellRenderer()
        fixedCellHeight = -1
        addListSelectionListener { event ->
            if (!event.valueIsAdjusting) selectedValue?.let { onTransactionSelected(it.id) }
        }
    }

    private val searchField = JTextField(15)

    private val methods = arrayOf(
        "All" to null,
        "GET" to NetworkMethod.GET,
        "HEAD" to NetworkMethod.HEAD,
        "POST" to NetworkMethod.POST,
        "PUT" to NetworkMethod.PUT,
        "PATCH" to NetworkMethod.PATCH,
        "DELETE" to NetworkMethod.DELETE,
        "OPTIONS" to NetworkMethod.OPTIONS,
        "TRACE" to NetworkMethod.TRACE,
        "CONNECT" to NetworkMethod.CONNECT,
    )
    private val methodCombo = JComboBox(methods.map { it.first }.toTypedArray())

    private val statuses = arrayOf(
        "All" to null,
        "Success (2xx)" to CallStatusFilter.SUCCESS_2XX,
        "Non-success (non-2xx)" to CallStatusFilter.NON_SUCCESS,
        "Request exceptions" to CallStatusFilter.EXCEPTION,
    )
    private val statusCombo = JComboBox(statuses.map { it.first }.toTypedArray())

    private val resetButton = JButton("Reset").apply {
        isVisible = false
        addActionListener { viewModel.onClearFiltersClick() }
    }
    private val clearButton = JButton("Clear All").apply {
        addActionListener {
            val confirmed = JOptionPane.showConfirmDialog(
                this@NetworkCallListPanel,
                "Are you sure you want to clear all recorded calls?",
                "Clear calls",
                JOptionPane.YES_NO_OPTION,
            )
            if (confirmed == JOptionPane.YES_OPTION) {
                viewModel.onClearClick()
            }
        }
    }

    init {
        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT, 6, 6)).apply {
            add(JLabel("Search URL:"))
            add(searchField)
            add(methodCombo)
            add(statusCombo)
            add(resetButton)
            add(clearButton)
        }

        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = update()
            override fun removeUpdate(e: DocumentEvent) = update()
            override fun changedUpdate(e: DocumentEvent) = update()
            private fun update() = viewModel.onSearchQueryChanged(searchField.text)
        })

        methodCombo.addActionListener {
            val index = methodCombo.selectedIndex
            val method = methods[index].second
            viewModel.onMethodFilterSelected(method)
        }

        statusCombo.addActionListener {
            val index = statusCombo.selectedIndex
            val status = statuses[index].second
            viewModel.onStatusFilterSelected(status)
        }

        add(topPanel, BorderLayout.NORTH)
        add(JScrollPane(list), BorderLayout.CENTER)

        scope.launch {
            viewModel.state.collect { state ->
                listModel.submitList(state.calls)
                resetButton.isVisible = state.showResetFilters
                clearButton.isEnabled = state.totalCount > 0
                if (searchField.text != state.query) {
                    EventQueue.invokeLater {
                        if (searchField.text != state.query) searchField.text = state.query
                    }
                }
                methodCombo.selectedIndex = methods.indexOfFirst { it.second == state.method }
                statusCombo.selectedIndex = statuses.indexOfFirst { it.second == state.status }
            }
        }
    }

    internal fun dispose() {
        scope.cancel()
        viewModelStore.clear()
    }
}
