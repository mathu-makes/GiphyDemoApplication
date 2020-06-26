package com.giphy.sdk.uidemo

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.giphy.sdk.uidemo.feed.FlexCatAdapter

class MyItemDetailsLookup(private val rv: RecyclerView)                                             // [1] | references: FlexMainActivity sources + sources
    : ItemDetailsLookup<Long>() {
    override fun getItemDetails(event: MotionEvent)
            : ItemDetails<Long>? {

        val view = rv.findChildViewUnder(event.x, event.y)
        if (view != null) {
            return (rv.getChildViewHolder(view) as FlexCatAdapter.GifViewHolder)
                .getItemDetails()
        }
        return null
    }
}