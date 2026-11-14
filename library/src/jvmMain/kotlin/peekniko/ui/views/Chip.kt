package peekniko.ui.views

import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.JLabel

/** A reusable rounded label for methods, response codes, and other short network values. */
public class Chip : JLabel() {
    private var chipBackground = Color(179, 157, 219)

    init {
        isOpaque = false
        horizontalAlignment = CENTER
        font = font.deriveFont(Font.BOLD, 12f)
        border = BorderFactory.createEmptyBorder(4, 7, 4, 7)
        setChipColors(Color(179, 157, 219), Color(38, 50, 56))
    }

    public fun setChipColors(backgroundColor: Color, foregroundColor: Color) {
        chipBackground = backgroundColor
        foreground = foregroundColor
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = chipBackground
        g.fillRoundRect(0, 0, width, height, 6, 6)
        g.dispose()
        super.paintComponent(graphics)
    }
}
