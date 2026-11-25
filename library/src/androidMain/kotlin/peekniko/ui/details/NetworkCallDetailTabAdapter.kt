package peekniko.ui.details

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import peekniko.library.R

internal const val ARG_TRANSACTION_ID: String = "transaction_id"

internal class NetworkCallDetailTabAdapter(
    context: Context,
    private val fragmentManager: FragmentManager,
    private val containerId: Int,
    private val transactionId: String,
) : BaseAdapter() {
    private val tabTitles = listOf(
        context.getString(R.string.peekniko_tab_headers),
        context.getString(R.string.peekniko_tab_request),
        context.getString(R.string.peekniko_tab_response),
    )
    private val fragments = mutableMapOf<Int, Fragment>()
    private var selectedTab: Int? = null

    override fun getCount(): Int = tabTitles.size

    override fun getItem(position: Int): String = tabTitles[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        (convertView as? TextView ?: TextView(parent.context, null, 0, R.style.Widget_Peekniko_DetailTab)).apply {
            text = getItem(position)
        }

    fun selectTab(index: Int) {
        if (index !in tabTitles.indices) return
        val selected = getFragment(index)
        val transaction = fragmentManager.beginTransaction()
        selectedTab?.let { current ->
            if (index != current) {
                val movingForward = index > current
                transaction.setCustomAnimations(
                    if (movingForward) R.anim.slide_in_from_end else R.anim.slide_in_from_start,
                    if (movingForward) R.anim.slide_out_to_start else R.anim.slide_out_to_end,
                )
            }
        }
        fragmentManager.fragments
            .filter { it.id == containerId && it !== selected }
            .forEach { fragment ->
                transaction.hide(fragment)
                transaction.setMaxLifecycle(fragment, Lifecycle.State.STARTED)
            }

        if (selected.isAdded) transaction.show(selected) else transaction.add(containerId, selected, tag(index))
        transaction.setMaxLifecycle(selected, Lifecycle.State.RESUMED)
        transaction.commitNow()
        selectedTab = index
    }

    private fun getFragment(index: Int): Fragment = fragments.getOrPut(index) {
        val fragmentTag = tag(index)
        fragmentManager.findFragmentByTag(fragmentTag) ?: when (index) {
            1 -> BodyFragment.newInstance(transactionId, isRequest = true)
            2 -> BodyFragment.newInstance(transactionId, isRequest = false)
            else -> SummaryFragment.newInstance(transactionId)
        }
    }

    private fun tag(index: Int) = "detail-tab-$index"
}
