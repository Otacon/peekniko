package peekniko.ui.networkcalls

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.BaseAdapter
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import peekniko.library.R
import peekniko.ui.views.Chip

internal class NetworkCallListAdapter(context: Context) : BaseAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var calls: List<NetworkCallListItem> = emptyList()
    private var hasSubmittedList = false
    private var previousIds = HashSet<String>()
    private var scratchIds = HashSet<String>()
    private val pendingFadeIns = HashSet<String>()

    internal fun submitList(networkCalls: List<NetworkCallListItem>, animateNewItems: Boolean) {
        if (!animateNewItems) pendingFadeIns.clear()
        if (hasSubmittedList && networkCalls.size == calls.size) {
            var sameIds = true
            var sameContent = true
            for (index in networkCalls.indices) {
                val next = networkCalls[index]
                val previous = calls[index]
                if (next.id != previous.id) {
                    sameIds = false
                    break
                }
                if (sameContent && next != previous) sameContent = false
            }
            if (sameIds) {
                calls = networkCalls
                if (!sameContent) notifyDataSetChanged()
                return
            }
        }

        val shouldAnimate = animateNewItems && hasSubmittedList
        scratchIds.clear()
        for (call in networkCalls) {
            val id = call.id
            scratchIds.add(id)
            if (shouldAnimate && id !in previousIds) pendingFadeIns.add(id)
        }
        pendingFadeIns.retainAll(scratchIds)
        val oldIds = previousIds
        previousIds = scratchIds
        scratchIds = oldIds
        hasSubmittedList = true
        calls = networkCalls
        notifyDataSetChanged()
    }

    override fun getCount(): Int = calls.size

    override fun getItem(position: Int): NetworkCallListItem = calls[position]

    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    override fun hasStableIds(): Boolean = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder = convertView?.tag as? NetworkCallItemViewHolder ?: createViewHolder(parent)
        val call = getItem(position)
        holder.root.isActivated = call.isSelected
        if (holder.boundId != call.id) {
            holder.root.clearAnimation()
            holder.boundId = call.id
        }
        if (pendingFadeIns.remove(call.id)) {
            holder.root.startAnimation(AnimationUtils.loadAnimation(inflater.context, R.anim.network_call_fade_in))
        }

        holder.method.text = call.method
        holder.methodAndUrl.text = call.urlPath
        val detailParts = buildList {
            call.requestTime?.let(::add)
            call.details.takeIf(String::isNotEmpty)?.let(::add)
        }
        holder.details.text = detailParts.joinToString(" · ")
        holder.details.visibility = if (detailParts.isEmpty()) View.GONE else View.VISIBLE

        val exception = call.exceptionMessage
        when {
            exception != null -> {
                holder.status.setText(R.string.peekniko_status_error)
                holder.status.setChipColors(
                    ContextCompat.getColor(inflater.context, R.color.peekniko_error),
                    ContextCompat.getColor(inflater.context, R.color.peekniko_onError),
                )
                holder.status.visibility = View.VISIBLE
                holder.progress.visibility = View.GONE
                holder.error.text = exception
                holder.error.visibility = View.VISIBLE
            }

            call.responseCode != null -> {
                holder.status.text = call.responseCode.toString()
                holder.status.setChipColors(
                    ContextCompat.getColor(inflater.context, R.color.peekniko_colorControlPrimary),
                    ContextCompat.getColor(inflater.context, R.color.peekniko_surface),
                )
                holder.status.visibility = View.VISIBLE
                holder.progress.visibility = View.GONE
                holder.error.visibility = View.GONE
            }

            else -> {
                holder.status.visibility = View.GONE
                holder.progress.visibility = View.VISIBLE
                holder.error.visibility = View.GONE
            }
        }

        return holder.root
    }

    private fun createViewHolder(parent: ViewGroup): NetworkCallItemViewHolder {
        val row = inflater.inflate(R.layout.item_network_call, parent, false)
        return NetworkCallItemViewHolder(
            root = row,
            methodAndUrl = row.findViewById(R.id.method_and_url),
            method = row.findViewById(R.id.method_chip),
            details = row.findViewById(R.id.call_details),
            error = row.findViewById(R.id.call_error),
            status = row.findViewById(R.id.response_status),
            progress = row.findViewById(R.id.call_progress),
        ).also { row.tag = it }
    }

    private data class NetworkCallItemViewHolder(
        val root: View,
        val methodAndUrl: TextView,
        val method: Chip,
        val details: TextView,
        val error: TextView,
        val status: Chip,
        val progress: ProgressBar,
        var boundId: String? = null,
    )

}
