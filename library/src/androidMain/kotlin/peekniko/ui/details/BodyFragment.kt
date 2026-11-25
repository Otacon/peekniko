package peekniko.ui.details

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import peekniko.Peekniko
import peekniko.internal.bodyrenderer.platformText
import peekniko.internal.bodyrenderer.spannedText
import peekniko.internal.support.FlowCollectionFragment
import peekniko.internal.support.decodeBitmap
import peekniko.internal.support.viewById
import peekniko.internal.support.viewModels
import peekniko.library.R

internal class BodyFragment : FlowCollectionFragment(
    contentLayoutId = R.layout.fragment_network_call_body,
    resumedAndVisibleOnly = true
), MenuProvider {

    private val viewModel: BodyViewModel by viewModels {
        val arguments = requireArguments()
        val transactionId = arguments.getString(ARG_TRANSACTION_ID) ?: error("Missing transaction ID")
        check(arguments.containsKey(ARG_IS_REQUEST)) { "Missing isRequest" }
        Peekniko.createBodyViewModel(transactionId, isRequest = arguments.getBoolean(ARG_IS_REQUEST))
    }

    private val progress: View by viewById(R.id.bodyProgress)
    private val content: View by viewById(R.id.bodyScrollView)
    private val emptyScreen: View by viewById(R.id.bodyEmptyScreen)
    private val body: TextView by viewById(R.id.bodyText)
    private val image: ImageView by viewById(R.id.bodyImage)

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
        menuInflater.inflate(R.menu.menu_call_body, menu)
        menu.findItem(R.id.menuBodyPrettyPrint).apply {
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
        R.id.menuBodyPrettyPrint -> {
            when (menuPrettyPrintState) {
                PrettyPrintState.Enabled -> viewModel.onPrettyPrintClick(false)
                PrettyPrintState.Disabled -> viewModel.onPrettyPrintClick(true)
                PrettyPrintState.Unavailable -> Unit
            }
            true
        }

        else -> false
    }

    private suspend fun bind(state: BodyViewModel.State) {
        if (state.prettyPrintState != menuPrettyPrintState) {
            menuPrettyPrintState = state.prettyPrintState
            menuHost.invalidateMenu()
        }
        val isEmpty = when (val stateBody = state.body) {
            is BodyViewModel.Body.Image -> {
                val bitmap = stateBody.bytes.decodeBitmap()
                if (bitmap != null) {
                    image.setImageBitmap(bitmap)
                    image.isVisible = true
                    body.isVisible = false
                    body.text = null
                } else {
                    image.setImageDrawable(null)
                    image.isVisible = false
                    body.isVisible = true
                    body.setText(R.string.peekniko_body_image_error)
                }
                false
            }

            is BodyViewModel.Body.Text, null -> {
                val platformText = (stateBody?.text ?: spannedText()).platformText
                image.setImageDrawable(null)
                image.isVisible = false
                body.isVisible = true
                body.text = platformText
                platformText.isBlank()
            }
        }
        when {
            state.isLoading -> {
                progress.isVisible = true
                content.isVisible = false
                emptyScreen.isVisible = false
            }

            isEmpty -> {
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
        private const val ARG_TRANSACTION_ID = "transaction_id"
        private const val ARG_IS_REQUEST = "is_request"

        fun newInstance(transactionId: String, isRequest: Boolean) = BodyFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TRANSACTION_ID, transactionId)
                putBoolean(ARG_IS_REQUEST, isRequest)
            }
        }
    }

}
