package peekniko.ui.details

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import peekniko.Peekniko
import peekniko.internal.bodyrenderer.emptySpannedText
import peekniko.internal.bodyrenderer.platformText
import peekniko.internal.support.FlowCollectionFragment
import peekniko.internal.support.viewById
import peekniko.internal.support.viewModels
import peekniko.library.R

internal class ResponseFragment : FlowCollectionFragment(
    contentLayoutId = R.layout.fragment_network_call_response,
    resumedAndVisibleOnly = true
), MenuProvider {

    private val viewModel: ResponseViewModel by viewModels {
        val transactionId = requireArguments().getString(ARG_TRANSACTION_ID) ?: error("Missing transaction ID")
        Peekniko.createResponseViewModel(transactionId)
    }

    private val progress: View by viewById(R.id.responseProgress)
    private val content: View by viewById(R.id.responseScrollView)
    private val emptyScreen: View by viewById(R.id.responseEmptyScreen)
    private val body: TextView by viewById(R.id.responseBody)

    private val menuHost: MenuHost get() = (requireParentFragment() as NetworkCallDetailFragment).menuHost
    private var menuPrettyPrintState = PrettyPrintState.Unavailable

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        collectInView {
            viewModel.state.collect { state -> bind(state) }
        }
        viewModel.onViewCreated()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_call_response, menu)
        menu.findItem(R.id.menuResponsePrettyPrint).apply {
            when (menuPrettyPrintState) {
                PrettyPrintState.Enabled -> {
                    isVisible = true
                    setIcon(R.drawable.ic_pretty_print)
                    setTitle(R.string.peekniko_menu_pretty_print_enabled)
                }

                PrettyPrintState.Disabled -> {
                    isVisible = true
                    setIcon(R.drawable.ic_pretty_print_off)
                    setTitle(R.string.peekniko_menu_pretty_print_disabled)
                }

                PrettyPrintState.Unavailable -> {
                    isVisible = false
                }
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
        R.id.menuResponsePrettyPrint -> {
            when (menuPrettyPrintState) {
                PrettyPrintState.Enabled -> viewModel.onPrettyPrintClick(false)
                PrettyPrintState.Disabled -> viewModel.onPrettyPrintClick(true)
                PrettyPrintState.Unavailable -> Unit
            }
            true
        }

        else -> false
    }

    private fun bind(state: ResponseViewModel.State) {
        if (state.prettyPrintState != menuPrettyPrintState) {
            menuPrettyPrintState = state.prettyPrintState
            menuHost.invalidateMenu()
        }
        val platformText = (state.body ?: emptySpannedText()).platformText
        body.text = platformText
        when {
            state.isLoading -> {
                progress.isVisible = true
                content.isVisible = false
                emptyScreen.isVisible = false
            }

            platformText.isBlank() -> {
                progress.isVisible = false
                content.isVisible = false
                emptyScreen.isVisible = true
            }

            else -> {
                progress.isVisible = false
                content.isVisible = true
                emptyScreen.isVisible = false
            }
        }
    }

    companion object {
        fun newInstance(transactionId: String) = ResponseFragment().apply {
            arguments = Bundle().apply { putString(ARG_TRANSACTION_ID, transactionId) }
        }
    }

}
