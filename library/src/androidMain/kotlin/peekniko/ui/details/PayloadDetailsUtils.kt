package peekniko.ui.details

import android.graphics.Typeface
import android.widget.LinearLayout
import android.widget.TextView
import peekniko.NetworkHeader

internal fun LinearLayout.setHeaderRows(headers: List<NetworkHeader>) {
    removeAllViews()
    headers.forEach { header ->
        val key = "${header.name}:"
        val row = TextView(context).apply {
            text = android.text.SpannableStringBuilder("$key ${header.value}").apply {
                setSpan(
                    android.text.style.StyleSpan(Typeface.BOLD),
                    0,
                    key.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            setTextIsSelectable(true)
            gravity = android.view.Gravity.TOP
            val verticalPadding = (4 * resources.displayMetrics.density).toInt()
            setPadding(0, verticalPadding, 0, verticalPadding)
        }
        addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }
}
