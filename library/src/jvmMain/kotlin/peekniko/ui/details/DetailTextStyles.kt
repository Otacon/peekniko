package peekniko.ui.details

import java.awt.Font
import java.awt.geom.AffineTransform

internal fun condensedMonospaceFont(size: Float = 13f): Font =
    Font(Font.MONOSPACED, Font.PLAIN, size.toInt()).deriveFont(
        AffineTransform.getScaleInstance(0.9, 1.0),
    )
