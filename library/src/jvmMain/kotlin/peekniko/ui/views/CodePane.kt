package peekniko.ui.views

import peekniko.ui.details.condensedMonospaceFont
import javax.swing.BorderFactory
import javax.swing.JTextPane
import javax.swing.UIManager

internal class CodePane : JTextPane() {

    init {
        isEditable = false
        font = condensedMonospaceFont()

        background = UIManager.getColor("TextPane.inactiveBackground")
            ?: UIManager.getColor("TextPane.background")

        val outsideBorder = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
        val insideBorder = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        border = BorderFactory.createCompoundBorder(outsideBorder, insideBorder)
    }
}