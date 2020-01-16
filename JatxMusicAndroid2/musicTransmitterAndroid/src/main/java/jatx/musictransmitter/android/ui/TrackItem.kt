package jatx.musictransmitter.android.ui

import androidx.core.content.ContextCompat.getColor
import com.xwray.groupie.kotlinandroidextensions.GroupieViewHolder
import com.xwray.groupie.kotlinandroidextensions.Item
import jatx.musictransmitter.android.R
import jatx.musictransmitter.android.db.entity.Track
import kotlinx.android.synthetic.main.item_track.*

class TrackItem(val track: Track, val position: Int, val isCurrent: Boolean): Item() {
    override fun getLayout() = R.layout.item_track

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.titleTV.text = track.title
        var meta = track.artist
        if (meta.isNotEmpty()) {
            meta += " | ${track.length}"
        } else {
            meta = track.length
        }
        viewHolder.metaTV.text = meta
        val context = viewHolder.containerView.context
        if (isCurrent) {
            viewHolder.wholeLayout.setBackgroundColor(getColor(context, R.color.black))
            viewHolder.titleTV.setTextColor(getColor(context, R.color.white))
            viewHolder.metaTV.setTextColor(getColor(context, R.color.white))
        } else {
            viewHolder.wholeLayout.setBackgroundColor(getColor(context, R.color.white))
            viewHolder.titleTV.setTextColor(getColor(context, R.color.black))
            viewHolder.metaTV.setTextColor(getColor(context, R.color.black))
        }
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean =
        if (other is TrackItem) (track.path == other.track.path)
            .and(position == other.position)
            .and(isCurrent == other.isCurrent)
        else false

    override fun hasSameContentAs(other: com.xwray.groupie.Item<*>?): Boolean =
        if (other is TrackItem) (track == other.track)
            .and(isCurrent == other.isCurrent)
        else false
}