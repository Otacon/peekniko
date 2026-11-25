package peekniko.ui.details

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import peekniko.Peekniko
import peekniko.internal.bodyrenderer.setSpannedText
import peekniko.internal.support.FlowCollectionFragment
import peekniko.internal.support.viewById
import peekniko.internal.support.viewModels
import peekniko.library.R

internal class SummaryFragment : FlowCollectionFragment(R.layout.fragment_network_call_summary, resumedAndVisibleOnly = true) {

    private val viewModel: SummaryViewModel by viewModels {
        val transactionId = requireArguments().getString(ARG_TRANSACTION_ID) ?: error("Missing transaction ID")
        Peekniko.createSummaryViewModel(transactionId)
    }

    private val progress: View by viewById(R.id.summaryProgress)
    private val scrollView: View by viewById(R.id.summaryScrollView)
    private val generalContent: TextView by viewById(R.id.summaryGeneralContent)
    private val requestHeadersContent: TextView by viewById(R.id.summaryRequestHeadersContent)
    private val responseHeadersContent: TextView by viewById(R.id.summaryResponseHeadersContent)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collectInView {
            viewModel.state.collect { state ->
                progress.isVisible = state.isLoading
                scrollView.isVisible = !state.isLoading
                generalContent.setSpannedText(state.general)
                requestHeadersContent.setSpannedText(state.requestHeaders)
                responseHeadersContent.setSpannedText(state.responseHeaders)
            }
        }
    }

    companion object {
        private const val ARG_TRANSACTION_ID = "transaction_id"

        fun newInstance(transactionId: String) = SummaryFragment().apply {
            arguments = Bundle().apply { putString(ARG_TRANSACTION_ID, transactionId) }
        }
    }

}
