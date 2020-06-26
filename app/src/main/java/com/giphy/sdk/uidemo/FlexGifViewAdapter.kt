package com.giphy.sdk.uidemo

import timber.log.Timber                                                                            //logging
import android.widget.Toast                                                                         //app message print

import com.giphy.sdk.uidemo.feed.*                                                                  //data classes

import android.content.Context                                                                      //activity context
import android.content.res.Resources

import android.view.View                                                                            //view
import android.view.ViewGroup                                                                       //view group

import android.view.LayoutInflater                                                                  //Layout Inflater
import androidx.recyclerview.widget.RecyclerView                                                    //recyclerview

import kotlinx.android.synthetic.main.gif_activity.view.*                                           //gif activity view [current]

import android.widget.ImageView                                                                     //ImageView
import android.graphics.drawable.Drawable                                                           //drawable for animation calls
import android.graphics.drawable.Animatable                                                         //animation
import android.widget.EditText
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy                                   //glide | down-sample
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions                          //drawable
import com.google.android.material.floatingactionbutton.FloatingActionButton                        //fab

//glide-transformations
import com.giphy.sdk.uidemo.GlideOptions.bitmapTransform                                            // glide transforms | will used these to modify gifs
import com.giphy.sdk.uidemo.GlideOptions.diskCacheStrategyOf
import jp.wasabeef.glide.transformations.CropTransformation

import jp.wasabeef.glide.transformations.*
import jp.wasabeef.glide.transformations.CropTransformation.CropType
import jp.wasabeef.glide.transformations.gpu.*
import jp.wasabeef.glide.transformations.internal.Utils

class FlexGifViewAdapter (private var items: MutableList<FeedDataItem>, private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val itemMessage = 100
    private val itemGIF = 101
    private val itemNone = 102
    private val itemInvalidAPI = 103
    var check = true

    val Float.px: Float get() = (this * Resources.getSystem().displayMetrics.density)               // https://github.com/wasabeef/glide-transformations/blob/0b9311813a49cbfdfce89c17b7ae305999576ddc/example/src/main/java/jp/wasabeef/example/glide/Ext.kt
    val Int.px: Int get() = ((this * Resources.getSystem().displayMetrics.density).toInt())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            itemGIF -> GifViewHolder(LayoutInflater.from(context).inflate(R.layout.gif_activity, parent, false))
            else -> throw RuntimeException("unsupported type")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) { //holder was p0 | position was p1
        when (items[position]) {
            is ViewDataItem -> (holder as GifViewHolder).bind(items[position] as ViewDataItem)
            is InvalidKeyItem -> {
                // Nothing to do
            }
            else -> throw RuntimeException("type not allowed")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MessageItem -> itemMessage
            is ViewDataItem -> itemGIF
            is InvalidKeyItem -> itemInvalidAPI
            else -> itemNone
        }
    }

    inner class GifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.gifView)

        fun bind(message: ViewDataItem) {
            imageView.layout(0, 0, 0, 0)                                                // full screen mode fix | https://github.com/bumptech/glide/issues/1591C

            GlideApp
                .with(this.imageView)
                .asGif()
                .load(message.uri)
                .downsample(DownsampleStrategy.DEFAULT)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .fitCenter()
                .transition(DrawableTransitionOptions.withCrossFade())
                .error(R.drawable.gph_logo_button)
                //.apply(bitmapTransform(CropTransformation(240.px, 240.px, CropType.CENTER)))
                .into(imageView.gifView)

            val drawable = imageView.drawable
            val playBtn: FloatingActionButton = itemView.findViewById(R.id.playFab)

            imageView.setOnClickListener {
                check = if (check) {
                    //Toast.makeText(context, "Width: $width | Height: $height" , Toast.LENGTH_SHORT).show()
                    if (drawable is Animatable) (drawable as Animatable).stop()
                    playBtn.show()
                    false
                } else {
                    if (drawable is Animatable) (drawable as Animatable).start()
                    playBtn.hide()
                    true
                }
            }

            playBtn.setOnClickListener {
                if (!check) {
                    if (drawable is Animatable) (drawable as Animatable).start()
                    playBtn.hide()
                    check = true
                }
            }
        }
    }

    inner class InvalidApiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}