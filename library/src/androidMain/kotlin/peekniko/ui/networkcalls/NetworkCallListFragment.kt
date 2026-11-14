package peekniko.ui.networkcalls

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import peekniko.NetworkMethod
import peekniko.Peekniko
import peekniko.internal.support.FlowCollectionFragment
import peekniko.internal.support.viewById
import peekniko.internal.support.viewModels
import peekniko.library.R

internal class NetworkCallListFragment :
    FlowCollectionFragment(R.layout.fragment_network_call_list),
    SearchView.OnQueryTextListener,
    Toolbar.OnMenuItemClickListener {

    private val viewModel: PeeknikoViewModel by viewModels { PeeknikoViewModel(Peekniko.networkRepository) }

    private val callList by viewById<ListView>(R.id.callList)
    private val progress by viewById<ProgressBar>(R.id.progress)
    private val emptyScreen by viewById<TextView>(R.id.emptyScreen)
    private val toolbar by viewById<Toolbar>(R.id.toolbar)
    private var searchView: SearchView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationIcon(R.drawable.ic_close)
        toolbar.setNavigationOnClickListener { requireActivity().finish() }
        toolbar.inflateMenu(R.menu.menu_call_list)
        toolbar.setOnMenuItemClickListener(this)
        val menu = toolbar.menu
        val filtersMenuItem = menu.findItem(R.id.menuFilters)
        val clearFiltersMenuItem = menu.findItem(R.id.filterClear)
        val clearMenuItem = menu.findItem(R.id.menuClear)
        searchView = menu.findItem(R.id.menuSearch).actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
        searchView?.queryHint = getString(R.string.peekniko_menu_search)
        val networkCallAdapter = NetworkCallListAdapter(requireContext())
        callList.adapter = networkCallAdapter
        callList.setOnItemClickListener { _, _, position, _ ->
            val call = networkCallAdapter.getItem(position)
            viewModel.onCallClick(call)
            (requireActivity() as PeeknikoActivity).onCallSelected(call.id)
        }

        var previousState: State? = null
        collectInView {
            viewModel.state.collect { state ->
                if (state.isLoading) {
                    progress.isVisible = true
                    callList.isVisible = false
                    emptyScreen.isVisible = false
                } else {
                    progress.isVisible = false
                    callList.isVisible = state.calls.isNotEmpty()
                    emptyScreen.isVisible = state.calls.isEmpty()
                    emptyScreen.setText(
                        if (state.showFilteredEmptyState) {
                            R.string.peekniko_empty_filtered_calls
                        } else {
                            R.string.peekniko_empty_calls
                        },
                    )
                }
                val animateNewItems = previousState?.let { !it.isLoading && !state.isLoading } == true
                networkCallAdapter.submitList(state.calls, animateNewItems)
                previousState = state
                clearMenuItem?.isVisible = state.showClearCalls
                filtersMenuItem.setIcon(
                    if (state.hasActiveFilters) R.drawable.ic_filter_applied else R.drawable.ic_filter,
                )
                filtersMenuItem.setTitle(
                    if (state.hasActiveFilters) R.string.peekniko_menu_filters_applied else R.string.peekniko_menu_filters,
                )
                clearFiltersMenuItem.isVisible = state.showResetFilters
                searchView?.let {
                    if (it.query.toString() != state.query) {
                        it.setQuery(state.query, false)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        searchView?.setOnQueryTextListener(null)
        searchView = null
        super.onDestroyView()
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        viewModel.onSearchQueryChanged(query)
        searchView?.clearFocus()
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.onSearchQueryChanged(newText)
        return true
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.filterMethodAll -> selectMethod(null)
        R.id.filterMethodGet -> selectMethod(NetworkMethod.GET)
        R.id.filterMethodHead -> selectMethod(NetworkMethod.HEAD)
        R.id.filterMethodPost -> selectMethod(NetworkMethod.POST)
        R.id.filterMethodPut -> selectMethod(NetworkMethod.PUT)
        R.id.filterMethodPatch -> selectMethod(NetworkMethod.PATCH)
        R.id.filterMethodDelete -> selectMethod(NetworkMethod.DELETE)
        R.id.filterMethodOptions -> selectMethod(NetworkMethod.OPTIONS)
        R.id.filterMethodTrace -> selectMethod(NetworkMethod.TRACE)
        R.id.filterMethodConnect -> selectMethod(NetworkMethod.CONNECT)

        R.id.filterStateAll -> selectStatus(null)
        R.id.filterState2xx -> selectStatus(CallStatusFilter.SUCCESS_2XX)
        R.id.filterStateNonSuccess -> selectStatus(CallStatusFilter.NON_SUCCESS)
        R.id.filterStateException -> selectStatus(CallStatusFilter.EXCEPTION)
        R.id.filterClear -> {
            viewModel.onClearFiltersClick()
            true
        }

        R.id.menuClear -> {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.peekniko_clear_calls_title)
                .setMessage(R.string.peekniko_clear_calls_message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok) { diag, _ ->
                    viewModel.onClearClick()
                    diag.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { diag, _ -> diag.dismiss() }
                .show()
            true
        }

        else -> false
    }

    private fun selectMethod(method: NetworkMethod?): Boolean {
        viewModel.onMethodFilterSelected(method)
        return true
    }

    private fun selectStatus(status: CallStatusFilter?): Boolean {
        viewModel.onStatusFilterSelected(status)
        return true
    }

}
