package peekniko.ui.networkcalls

import peekniko.ui.views.Chip
import com.formdev.flatlaf.FlatClientProperties
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.UIManager


internal class NetworkCallListCellRenderer :
    JPanel(BorderLayout(12, 0)),
    ListCellRenderer<NetworkCallListItem> {

    private val method = Chip()
    private val path = JLabel().apply { putClientProperty(FlatClientProperties.STYLE_CLASS, "semibold") }
    private val details = JLabel().apply { putClientProperty(FlatClientProperties.STYLE_CLASS, "small") }
    private val error = JLabel().apply { putClientProperty(FlatClientProperties.STYLE_CLASS, "small") }
    private val status = Chip()

    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(
                0, 0, 1, 0,
                UIManager.getColor("Separator.foreground")
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        )

        val header = JPanel(BorderLayout(8, 0)).apply {
            isOpaque = false
            add(method, BorderLayout.WEST)
            add(path, BorderLayout.CENTER)
        }

        val content = JPanel(BorderLayout(0, 4)).apply {
            isOpaque = false
            add(header, BorderLayout.NORTH)
            add(details, BorderLayout.CENTER)
            add(error, BorderLayout.SOUTH)
        }

        val statusContainer = JPanel(GridBagLayout()).apply {
            isOpaque = false
            add(status)
        }

        add(content, BorderLayout.CENTER)
        add(statusContainer, BorderLayout.EAST)
    }

    override fun getListCellRendererComponent(
        list: JList<out NetworkCallListItem>,
        value: NetworkCallListItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        background = if (isSelected) list.selectionBackground else list.background
        foreground = if (isSelected) list.selectionForeground else list.foreground

        method.text = value.method
        method.setThemeColor(methodColor(value.method))

        path.text = value.urlPath
        path.foreground = foreground

        details.text = listOfNotNull(
            value.requestTime,
            value.details.takeIf { it.isNotEmpty() }
        ).joinToString(" · ")
        details.isVisible = details.text.isNotEmpty()
        details.foreground = if (isSelected) {
            foreground
        } else {
            UIManager.getColor("Label.disabledForeground")
        }

        error.text = value.exceptionMessage.orEmpty()
        error.isVisible = error.text.isNotEmpty()
        error.foreground = if (isSelected) {
            foreground
        } else {
            UIManager.getColor("Actions.Red")
        }

        when {
            value.exceptionMessage != null -> {
                status.text = "ERR"
                status.setThemeColor("Actions.Green")
            }

            value.responseCode != null -> {
                status.text = value.responseCode.toString()
                status.setThemeColor(statusColor(value.responseCode))
            }

            else -> {
                status.text = "Pending"
                status.setChipColors(background, foreground)
            }
        }

        return this
    }

    private fun Chip.setThemeColor(key: String) {
        val background = UIManager.getColor(key) ?: Color.GRAY
        val brightness = (background.red * 299 + background.green * 587 + background.blue * 114) / 1000
        val foreground = if (brightness > 150) Color.BLACK else Color.WHITE
        setChipColors(background, foreground)
    }

    private fun methodColor(method: String) = when (method) {
        "GET" -> "Actions.Green"
        "POST" -> "Actions.Blue"
        "PUT", "PATCH" -> "Actions.Yellow"
        "DELETE" -> "Actions.Red"
        "HEAD" -> "Objects.Purple"
        else -> "Actions.Grey"
    }

    private fun statusColor(code: Int) = when (code / 100) {
        2 -> "Actions.Green"
        3 -> "Actions.Blue"
        4 -> "Actions.Yellow"
        5 -> "Actions.Red"
        else -> "Actions.Grey"
    }
}
