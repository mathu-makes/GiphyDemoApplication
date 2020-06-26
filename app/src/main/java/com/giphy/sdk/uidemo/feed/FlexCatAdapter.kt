/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.giphy.sdk.uidemo.feed

//android
import android.net.Uri
import android.content.Intent
import android.content.Context                                                                      // Application context [awareness for launching new activities]
import android.content.res.Resources
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK                                                // new intent flag activity task

//android view layouts
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.view.LayoutInflater                                                                  //inflate layout with adapter items

//recyclerview selection tracker
import android.view.MotionEvent                                                                     // tracks touch motion events
import androidx.recyclerview.widget.RecyclerView                                                    // recyclerview for item data
import androidx.recyclerview.selection.SelectionTracker                                             // tracks items in recyclerview (gifs)
import androidx.recyclerview.selection.ItemDetailsLookup                                            // class used to get x y items position data

//XML views
import kotlinx.android.synthetic.main.flex_viewholder_cat.view.*

//glide image engine
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions                          // fades in new image when loaded by Glide engine

//logging
import timber.log.Timber
import com.giphy.sdk.uidemo.*

/**
 * Adapter class that handles the data set with the {@link RecyclerView.LayoutManager}
 */

class FlexCatAdapter(private var items: MutableList<FeedDataItem>, private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_MESSAGE = 100
    private val ITEM_GIF = 101
    private val ITEM_NONE = 102
    private val ITEM_INVALID_API = 103

    private var positionID = 0
    private var showPlayBtn = -1

    lateinit var uriGif: Uri

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_GIF -> GifViewHolder(LayoutInflater.from(context).inflate(R.layout.flex_viewholder_cat, parent, false))
            else -> throw RuntimeException("unsupported type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) { //holder was p0 | position was p1
        when (items[position]) {
            is PictureItem -> (holder as GifViewHolder).bind(items[position] as PictureItem)
            is InvalidKeyItem -> {
                // Nothing to do
            }
            else -> throw RuntimeException("type not allowed")
        }

        if(tracker!!.isSelected(position.toLong())) {
            //newPlace = position
            Timber.d("Position# :: ${items[position]}!")
            //showPlayBtn = 1
        }
        if(!tracker!!.isSelected(position.toLong())) {
            //showPlayBtn = 0
        }
        Timber.d("playBtn State:: $showPlayBtn")
    }

    fun getPlayState(): Int {
        return showPlayBtn
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MessageItem -> ITEM_MESSAGE
            is PictureItem -> ITEM_GIF
            is InvalidKeyItem -> ITEM_INVALID_API
            else -> ITEM_NONE
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
/*------------------------------------------------------------------------------------------------*/ // [1] | references: FlexMainActivity sources + sources
    init {
    setHasStableIds(true)
    }

    private var tracker: SelectionTracker<Long>? = null // | item tracker

    override fun getItemId(position: Int): Long {
        positionID = position
        return position.toLong()
    }

    fun positionID(): Long {
        return positionID.toLong()
    }

    fun setTracker(tracker: SelectionTracker<Long>?) {
        this.tracker = tracker
    }

/*------------------------------------------------------------------------------------------------*/

    inner class GifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.imageview)

        //convert int to dp                                                                         | https://medium.com/@johanneslagos/dp-to-px-and-viceversa-for-kotlin-d797815d852b
        val Int.dp: Int
            get() = (this / Resources.getSystem().displayMetrics.density).toInt()

        fun bind(message: PictureItem) {

            imageView.layout(0,0,0,0)                                                   // full screen mode fix | https://github.com/bumptech/glide/issues/1591C

            GlideApp
                    .with(this.imageView)
                    .asGif()
                    .load(message.uri)
                    .downsample(DownsampleStrategy.DEFAULT)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .error(R.drawable.gph_logo_button)
                    .into(imageView.imageview)

            uriGif = message.uri
            var check = false
            imageView.setOnClickListener {
                //Toast.makeText(context, "click! -- ${message.uri}" , Toast.LENGTH_SHORT).show()

                val gifName = message.name
                val gifUri = message.uri.toString()

                val myActivity = Intent(context.applicationContext, FlexGifViewActivity::class.java)   // https://stackoverflow.com/a/43393450
                myActivity.addFlags(FLAG_ACTIVITY_NEW_TASK)
                myActivity.putExtra("GIF URI", gifName)
                context.applicationContext.startActivity(myActivity)

            }

        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =                                 // [1] | references: FlexMainActivity credits + sources
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = adapterPosition
                override fun getSelectionKey(): Long? = itemId
                override fun inSelectionHotspot(e: MotionEvent): Boolean {                          // [scs] | single click selector | uncomment for long press
                    return false
                }
            }
    }

    inner class InvalidApiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

// [scs] single click selector
// https://stackoverflow.com/questions/55118388/select-reyclerview-itemjust-single-tap-with-recycler-view-selection-library
