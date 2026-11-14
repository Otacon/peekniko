package peekniko.ui.views

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import peekniko.library.R

/** A reusable rounded label for methods, response codes, and other short network values. */
public class Chip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {
    init {
        setPadding(8.dp, 4.dp, 8.dp, 4.dp)
        textSize = 12f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setChipColors(
            ContextCompat.getColor(context, R.color.peekniko_colorControlPrimary),
            ContextCompat.getColor(context, R.color.peekniko_surface),
        )
    }

    public fun setChipColors(backgroundColor: Int, foregroundColor: Int) {
        background = GradientDrawable().apply {
            setColor(backgroundColor)
            cornerRadius = 5.dp.toFloat()
        }
        setTextColor(foregroundColor)
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
