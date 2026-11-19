package peekniko.ui.networkcalls

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import peekniko.library.R
import peekniko.ui.views.Chip

internal class NetworkCallListAdapter(
    private val onClick: (NetworkCallListItem) -> Unit,
) : ListAdapter<NetworkCallListItem, NetworkCallItemViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkCallItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val row = inflater.inflate(R.layout.item_network_call, parent, false)
        return NetworkCallItemViewHolder(root = row)
    }

    override fun onBindViewHolder(holder: NetworkCallItemViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NetworkCallListItem>() {
            override fun areItemsTheSame(oldItem: NetworkCallListItem, newItem: NetworkCallListItem): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NetworkCallListItem, newItem: NetworkCallListItem): Boolean =
                oldItem == newItem
        }
    }
}

internal class NetworkCallItemViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
    private val methodAndUrl: TextView = root.findViewById(R.id.method_and_url)
    private val method: Chip = root.findViewById(R.id.method_chip)
    private val details: TextView = root.findViewById(R.id.call_details)
    private val error: TextView = root.findViewById(R.id.call_error)
    private val status: Chip = root.findViewById(R.id.response_status)
    private val progress: ProgressBar = root.findViewById(R.id.call_progress)

    fun bind(call: NetworkCallListItem, onClick: (NetworkCallListItem) -> Unit) {
        root.setOnClickListener { onClick(call) }
        root.isActivated = call.isSelected

        method.text = call.method
        methodAndUrl.text = call.urlPath
        val detailParts = buildList {
            call.requestTime?.let(::add)
            call.details.takeIf(String::isNotEmpty)?.let(::add)
        }
        details.text = detailParts.joinToString(" · ")
        details.visibility = if (detailParts.isEmpty()) View.GONE else View.VISIBLE

        val exception = call.exceptionMessage
        when {
            exception != null -> {
                status.text = call.statusBadge
                status.setChipColors(
                    backgroundColor = ContextCompat.getColor(root.context, R.color.peekniko_statusError),
                    foregroundColor = ContextCompat.getColor(root.context, R.color.peekniko_onStatus),
                )
                status.visibility = View.VISIBLE
                progress.visibility = View.GONE
                error.text = exception
                error.visibility = View.VISIBLE
            }

            call.responseCode != null -> {
                status.text = call.statusBadge
                val statusColor = if (call.responseCode in 200..299) {
                    R.color.peekniko_statusSuccess
                } else {
                    R.color.peekniko_statusNonSuccess
                }
                status.setChipColors(
                    backgroundColor = ContextCompat.getColor(root.context, statusColor),
                    foregroundColor = ContextCompat.getColor(root.context, R.color.peekniko_onStatus),
                )
                status.visibility = View.VISIBLE
                progress.visibility = View.GONE
                error.visibility = View.GONE
            }

            else -> {
                status.visibility = View.GONE
                progress.visibility = View.VISIBLE
                error.visibility = View.GONE
            }
        }
    }
}
