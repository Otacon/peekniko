package peekniko.ui.details

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import peekniko.Peekniko
import peekniko.internal.bodyrenderer.emptySpannedText
import peekniko.internal.bodyrenderer.platformText
import peekniko.internal.support.FlowCollectionFragment
import peekniko.internal.support.viewById
import peekniko.internal.support.viewModels
import peekniko.library.R

internal class ResponseFragment :
    FlowCollectionFragment(R.layout.fragment_network_call_response, resumedAndVisibleOnly = true) {

    private val viewModel: ResponseViewModel by viewModels {
        val transactionId = requireArguments().getString(ARG_TRANSACTION_ID) ?: error("Missing transaction ID")
        Peekniko.createResponseViewModel(transactionId)
    }

    private val progress: View by viewById(R.id.responseProgress)
    private val content: View by viewById(R.id.responseScrollView)
    private val emptyScreen: View by viewById(R.id.responseEmptyScreen)
    private val body: TextView by viewById(R.id.responseBody)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectInView {
            viewModel.state.collect { state ->
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
        }
        viewModel.onViewCreated()
    }

    companion object {
        fun newInstance(transactionId: String) = ResponseFragment().apply {
            arguments = Bundle().apply { putString(ARG_TRANSACTION_ID, transactionId) }
        }
    }

}
