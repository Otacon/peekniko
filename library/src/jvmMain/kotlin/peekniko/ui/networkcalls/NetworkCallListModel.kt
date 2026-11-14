package peekniko.ui.networkcalls

import javax.swing.AbstractListModel

internal class NetworkCallListModel : AbstractListModel<NetworkCallListItem>() {
    private var items: List<NetworkCallListItem> = emptyList()

    override fun getSize(): Int = items.size

    override fun getElementAt(index: Int): NetworkCallListItem = items[index]

    internal fun submitList(updatedItems: List<NetworkCallListItem>) {
        val previousSize = items.size
        items = updatedItems.toList()
        val sharedSize = minOf(previousSize, items.size)
        if (sharedSize > 0) fireContentsChanged(this, 0, sharedSize - 1)
        if (items.size > previousSize) {
            fireIntervalAdded(this, previousSize, items.lastIndex)
        } else if (previousSize > items.size) {
            fireIntervalRemoved(this, items.size, previousSize - 1)
        }
    }
}
