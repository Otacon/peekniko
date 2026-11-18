package peekniko.ui.details

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.isVisible
import peekniko.Peekniko
import peekniko.internal.support.FlowCollectionFragment
import peekniko.internal.support.viewById
import peekniko.internal.support.viewModels
import peekniko.library.R
import peekniko.ui.networkcalls.PeeknikoActivity
import peekniko.ui.views.TabView

internal class NetworkCallDetailFragment : FlowCollectionFragment(R.layout.fragment_network_call_detail) {

    internal val transactionId: String? get() = arguments?.getString(ARG_TRANSACTION_ID)

    private val detailViewModel: NetworkCallDetailViewModel by viewModels {
        Peekniko.createNetworkCallDetailViewModel(transactionId)
    }
    private val toolbar: Toolbar by viewById(R.id.detail_toolbar)
    private val detailTabs: TabView by viewById(R.id.detail_tabs)
    private val emptyState: TextView by viewById(R.id.detail_empty_state)
    private val content: View by viewById(R.id.detail_content)
    internal val menuHost: MenuHost get() = toolbar
    private var selectedTabIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTabIndex = savedInstanceState?.getInt(STATE_SELECTED_TAB) ?: 0
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener { (requireActivity() as PeeknikoActivity).onBackPressedDispatcher.onBackPressed() }
        transactionId?.let {
            val tabAdapter = NetworkCallDetailTabAdapter(
                context = requireContext(),
                fragmentManager = childFragmentManager,
                containerId = R.id.detail_fragment_container,
                transactionId = it,
            )
            detailTabs.setAdapter(tabAdapter, selectedTabIndex) { index ->
                selectedTabIndex = index
                tabAdapter.selectTab(index)
            }
        }
        collectInView {
            detailViewModel.state.collect {
                toolbar.title = it.title
                content.isVisible = it.transaction != null
                emptyState.isVisible = it.transaction == null
                emptyState.text = it.emptyMessage
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAB, selectedTabIndex)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val ARG_TRANSACTION_ID = "transaction_id"
        private const val STATE_SELECTED_TAB = "selected_tab"
        fun newInstance(transactionId: String?) = NetworkCallDetailFragment().apply {
            arguments = Bundle().apply {
                transactionId?.let { putString(ARG_TRANSACTION_ID, it) }
            }
        }
    }
}
