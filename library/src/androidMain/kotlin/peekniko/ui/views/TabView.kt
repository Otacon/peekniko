package peekniko.ui.views

import android.content.Context
import android.database.DataSetObserver
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import peekniko.library.R

internal class TabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    private val tabs = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val indicator = View(context).apply {
        setBackgroundColor(context.getColorCompat(R.color.peekniko_toolbarTitle))
    }
    private val tabFrame = FrameLayout(context)
    private val separator = View(context).apply {
        setBackgroundColor(context.themeColor(androidx.appcompat.R.attr.colorControlHighlight))
    }
    private var tabViews: List<View> = emptyList()
    private var selectedIndex = 0
    private var adapter: BaseAdapter? = null
    private var onTabSelected: ((Int) -> Unit)? = null
    private val adapterObserver = object : DataSetObserver() {
        override fun onChanged() = populateTabs()
        override fun onInvalidated() = populateTabs()
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(context.themeColor(androidx.appcompat.R.attr.colorPrimary))
        tabFrame.addView(tabs, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, 48.dp))
        tabFrame.addView(indicator, FrameLayout.LayoutParams(0, 2.dp, Gravity.BOTTOM))
        addView(tabFrame, LayoutParams(LayoutParams.MATCH_PARENT, 48.dp))
        addView(separator, LayoutParams(LayoutParams.MATCH_PARENT, 1.dp))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        placeIndicator(animate = false)
    }

    internal fun setAdapter(
        adapter: BaseAdapter,
        initialSelectedIndex: Int = 0,
        onTabSelected: (Int) -> Unit,
    ) {
        this.adapter?.unregisterDataSetObserver(adapterObserver)
        this.adapter = adapter
        this.onTabSelected = onTabSelected
        selectedIndex = initialSelectedIndex
        adapter.registerDataSetObserver(adapterObserver)
        populateTabs()
        if (tabViews.isNotEmpty()) onTabSelected(selectedIndex)
    }

    private fun populateTabs() {
        tabs.removeAllViews()
        val currentAdapter = adapter
        if (currentAdapter == null) {
            tabViews = emptyList()
            return
        }
        tabViews = (0 until currentAdapter.count).map { index ->
            currentAdapter.getView(index, null, tabs).apply {
                isFocusable = true
                val selectableBackground = TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, selectableBackground, true)
                setBackgroundResource(selectableBackground.resourceId)
                setOnClickListener { selectTab(index) }
                tabs.addView(this, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            }
        }
        selectedIndex = selectedIndex.coerceIn(0, (tabViews.size - 1).coerceAtLeast(0))
        updateSelectedTab(animate = false)
    }

    private fun selectTab(index: Int) {
        if (index !in tabViews.indices || index == selectedIndex) return
        selectedIndex = index
        updateSelectedTab(true)
        onTabSelected?.invoke(index)
    }

    private fun updateSelectedTab(animate: Boolean) {
        tabViews.forEachIndexed { i, tab ->
            tab.isSelected = i == selectedIndex
        }
        placeIndicator(animate)
    }

    private fun placeIndicator(animate: Boolean) {
        if (tabs.width == 0 || tabViews.isEmpty()) return
        val tabWidth = tabs.width.toFloat() / tabViews.size
        val targetWidth = tabWidth * .55f
        val targetX = tabWidth * selectedIndex + (tabWidth - targetWidth) / 2f
        val params = indicator.layoutParams as FrameLayout.LayoutParams
        val width = targetWidth.toInt()
        val left = targetX.toInt()
        if (params.width != width || params.leftMargin != left) {
            if (animate && indicator.isLaidOut) {
                TransitionManager.beginDelayedTransition(tabFrame, ChangeBounds().setDuration(140))
            }
            params.width = width
            params.leftMargin = left
            params.gravity = Gravity.BOTTOM or Gravity.START
            indicator.layoutParams = params
        }
        indicator.translationX = 0f
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    @Suppress("Deprecation")
    private fun Context.themeColor(attribute: Int): Int {
        val value = TypedValue()
        theme.resolveAttribute(attribute, value, true)
        return if (value.resourceId != 0) resources.getColor(value.resourceId) else value.data
    }

    private fun Context.getColorCompat(color: Int): Int =
        androidx.core.content.ContextCompat.getColor(this, color)
}
