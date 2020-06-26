package com.giphy.sdk.uidemo

import java.io.File                                                                                 //  file
import android.net.Uri                                                                              //  uri [files url etc]
import android.widget.*                                                                             //  android widgets
import android.os.Build                                                                             //  build version [sdk os]
import android.os.Bundle                                                                            //  save state
import android.os.Environment                                                                       //  context
import java.io.File.separator                                                                       //  forward slash
import com.giphy.sdk.uidemo.feed.*                                                                  //  data classes
import android.provider.MediaStore                                                                  //  media [images etc]
import android.content.res.Resources                                                                //  int to dp conversion
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity                                                     //  app activity
import androidx.recyclerview.widget.RecyclerView                                                    //  android widgets
import kotlinx.android.synthetic.main.gif_activity.*                                                //  xml view
import androidx.constraintlayout.widget.ConstraintLayout                                            //  constraint layout
import androidx.recyclerview.widget.StaggeredGridLayoutManager                                      //  staggered grid layout
import com.google.android.material.floatingactionbutton.FloatingActionButton                        //  fab
import kotlin.math.roundToInt


class FlexGifViewActivity : AppCompatActivity() {

    companion object {
        val TAG = FlexGifViewActivity::class.java.simpleName
    }

    private var feedAdapter: FlexGifViewAdapter? = null
    private var editorItems = ArrayList<FeedDataItem>()

    var editMode = false                                                                            // activates editMode | linked to edit floating action button
    var cropMode = false                                                                            // activates cropMode | linked to crop floating action button
    private var cropTarg = false                                                                    // cropTarget relates to scale factor

    var crp1X: Int = 0                                                                              //crp1X | crop 1:1 mode [ vertical || horizontal ]
    var crp1Y: Int = 0                                                                              //crp1Y | crop 1:1 mode

    var crp2X: Int = 0                                                                              //crp2X | crop 2:3 mode
    var crp2Y: Int = 0                                                                              //crp2Y | crop 2:3 mode

    var cropLeft = false                                                                            //tracks if left crop button is pressed [horizontal only]
    var cropRight = false                                                                           //tracks if right crop button is pressed [horizontal only]
    var cropMidl = false                                                                            ////tracks if right crop button is pressed [horizontal + vertical]

    private var imageHeight = 0
    private var imageWidth  = 0

    var croppedWidth = 0
    var croppedHeight = 0

    private var lastCrop: Int = 0

    //convert px to dp                                                                             | https://medium.com/@johanneslagos/dp-to-px-and-viceversa-for-kotlin-d797815d852b
    val Int.dp: Int
        get() = (this / Resources.getSystem().displayMetrics.density).toInt()

    //convert dp to px
    val Float.px: Float get() = (this * Resources.getSystem().displayMetrics.density)               // https://github.com/wasabeef/glide-transformations/blob/0b9311813a49cbfdfce89c17b7ae305999576ddc/example/src/main/java/jp/wasabeef/example/glide/Ext.kt
    val Int.px: Int get() = ((this * Resources.getSystem().displayMetrics.density).toInt())

    // create value for Activity context
    val context = this@FlexGifViewActivity

    private val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        ) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //data
        val gifUri = intent.extras?.get("GIF URI")
        val directory = File(                                                                       // directory check for the above
            Environment.getExternalStorageDirectory().toString()
                    + separator + "GiphyDemoApplication")

        //data passed as file
        val file = File(directory, gifUri.toString())

        //layout
        setContentView(R.layout.gif_activity)

        val recyclerView: RecyclerView = findViewById(R.id.gifEditor)

        val gifLayoutManager = StaggeredGridLayoutManager(1,
            StaggeredGridLayoutManager.VERTICAL)
            gifLayoutManager.gapStrategy =
            StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS

        recyclerView.apply {
            feedAdapter = FlexGifViewAdapter(editorItems, this@FlexGifViewActivity)
            layoutManager = gifLayoutManager
            adapter?.notifyDataSetChanged()
            adapter = feedAdapter

            //Toast.makeText(context, "click! -- ${editorItems.size}" , Toast.LENGTH_SHORT).show()
        }
        loadImage(file)
    }

    private fun loadImage(file: File?) {
        if (file != null) {                                                                         // [4]
            editorItems.add(ViewDataItem(Uri.fromFile(file), Author.Me))
        }
        feedAdapter?.notifyItemInserted(editorItems.size - 1)                              // this is the position I think things are asking for
        //Toast.makeText(context, "editorItems! -- ${editorItems.size}" , Toast.LENGTH_SHORT).show()

        // GIF width x height | pixels                                                              https://stackoverflow.com/a/45058846
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(Uri.fromFile(file)), null, options)
        imageHeight = options.outHeight
        imageWidth  = options.outWidth

        //Toast.makeText(context, "width: $imageWidth | height $imageHeight" , Toast.LENGTH_SHORT).show()

        buttonListeners()
    }

    private fun buttonListeners() {

        // KEY
        // shape          == grey border, crop area bounds | selection: top + bottom                [shapeWide + shapeWide necessary as could not reapply constraints to shape so was forced to duplicate crop bounds for left + right]
        // shapeWide      == grey border, crop area bounds | selection: left
        // shapeWideR     == grey border, crop area bounds | selection: right

        //shape_bg        == crop area: bottom | aspect ratio: portrait or 1:1 | visualiser: Orange fill with alpha transparency
        //shape_bgTop     == crop area: top    | aspect ratio: portrait or 1:1 | visualiser: Orange fill with alpha transparency
        //shape_bgLeft    == crop area: left   | aspect ratio: landscape / 3:2 | visualiser: Orange fill with alpha transparency
        //shape_bgRight   == crop area: right  | aspect ratio: landscape / 3:2 | visualiser: Orange fill with alpha transparency

        //shape_bgLeftL2  == crop area: left   | aspect ratio: landscape / 3:2 | visualiser: Orange fill with alpha transparency [ duplicated as could not apply new constraints to shapeBg_Left -- the fill remained central rather than moving to left side of gif image ]
        //shape_bgRightR2 == crop area: right  | aspect ratio: landscape / 3:2 | visualiser: Orange fill with alpha transparency [ duplicated as could not apply new constraints to shapeBg_Right - the fill remained central rather than moving to right side of gif image ]

        val editBtn: FloatingActionButton = findViewById(R.id.editFab)                              // editBtn fab || has setOnClickListener
        val cropBtn: FloatingActionButton = findViewById(R.id.cropFab)                              // cropBtn fab || has setOnClickListener
        val saveBtn: FloatingActionButton = findViewById(R.id.saveFab)                              // saveBtn fab || has setOnClickListener

        val tio1Btn: Button = findViewById(R.id.scale1)                                             // rio1Btn fab || has setOnClickListener | ratio 1:1
        val tio2Btn: Button = findViewById(R.id.scale2)                                             // rio2Btn fab || has setOnClickListener | reset bounds to GIF
        val tio3Btn: Button = findViewById(R.id.scale3)                                             // rio3Btn fab || has setOnClickListener | ratio 2:3

        val crp1Btn: Button = findViewById(R.id.crop1)                                              // crp1Btn fab || has setOnClickListener | top crop   [vertical]
        val crp2Btn: Button = findViewById(R.id.crop2)                                              // crp2Btn fab || has setOnClickListener | bot crop   [vertical]
        val crp3Btn: Button = findViewById(R.id.crop3)                                              // crp3Btn fab || has setOnClickListener | left crop  [horizontal]
        val crp5Btn: Button = findViewById(R.id.crop5)                                              // crp5Btn fab || has setOnClickListener | mid  crop  [horizontal]
        val crp4Btn: Button = findViewById(R.id.crop4)                                              // crp4Btn fab || has setOnClickListener | right crop [horizontal]


        editBtn.setOnClickListener {

            editMode = if (!editMode) {
                //show buttons if editMode button is clicked
                cropBtn.show()
                saveBtn.show()

                val imageView: ImageView = findViewById(R.id.gifView)                               // holds current gif
                val onShape: ImageView = findViewById(R.id.shape)                                   // border indicating crop boundary
                val shapeBg: ImageView = findViewById(R.id.shape_bg)                                // background fill for non highlighted area [south fill]
                val shapeBgTop: ImageView = findViewById(R.id.shape_bgTop)                          // background fill for non highlighted area [north fill]
                val shapeBgLeft: ImageView = findViewById(R.id.shape_bgLeft)                        // background fill for non highlighted area [east  fill]
                val shapeBgRight: ImageView = findViewById(R.id.shape_bgRight)                      // background fill for non highlighted area [west  fill]

                // get imageView width + height | dp of gif
                val width = imageView.drawable.intrinsicWidth                                       //https://stackoverflow.com/a/49253306
                val height = imageView.drawable.intrinsicHeight                                     //

                //set layout parameters for crop bounds indicator | reset to centre
                val lp = onShape.layoutParams as ConstraintLayout.LayoutParams                      //https://stackoverflow.com/q/54347326
                lp.leftToLeft = R.id.gifView
                lp.rightToRight = R.id.gifView
                lp.topToTop = R.id.gifView
                lp.bottomToBottom = R.id.gifView
                lp.width = width
                lp.height = height
                onShape.requestLayout()

                // all fill colour backgrounds set to invisible as crop bounds same a gif size      | could set fill objects to invisible form XML
                onShape.scaleType = ImageView.ScaleType.CENTER                                      //https://stackoverflow.com/a/26032060
                onShape.visibility = ImageView.VISIBLE
                shapeWide.visibility = ImageView.INVISIBLE
                shapeWideR.visibility = ImageView.INVISIBLE
                shapeBg.visibility = ImageView.INVISIBLE
                shapeBgTop.visibility = ImageView.INVISIBLE
                shapeBgLeft.visibility = ImageView.INVISIBLE
                shapeBgRight.visibility = ImageView.INVISIBLE
                shape_bgLeftL2.visibility = ImageView.INVISIBLE
                shape_bgRightR2.visibility = ImageView.INVISIBLE

                lastCrop = 2 // reset transform                                                     // elsewhere in code when lastCrop == 2 return | no action | reset
                true

            } else {
                //hide buttons if editMode button is clicked again | hides all cropEdit features | resets bounds and fills
                cropBtn.hide()
                saveBtn.hide()

                val onShape: ImageView = findViewById(R.id.shape)                                   //crop bounds highlighter | initially constrained to gif
                val shapeBg: ImageView = findViewById(R.id.shape_bg)                                //background fill for non highlighted area
                val shapeBgTop: ImageView = findViewById(R.id.shape_bgTop)
                val shapeBgLeft: ImageView = findViewById(R.id.shape_bgLeft)
                val shapeBgRight: ImageView = findViewById(R.id.shape_bgRight)

                onShape.visibility = ImageView.INVISIBLE                                            // vertical overlays | crop top / bottom
                shapeBg.visibility = ImageView.INVISIBLE
                shapeBgTop.visibility = ImageView.INVISIBLE

                shapeBgLeft.visibility = ImageView.INVISIBLE                                        // horizontal overlays | crop center
                shapeBgRight.visibility = ImageView.INVISIBLE

                //had to add extra crop bounds + fill blocks for left + right
                //shapeWide + shapeWideR initially constrained to centre guideline but otherwise same as onShape
                shapeWide.visibility = ImageView.INVISIBLE                                          // horizontal overlays | crop left
                shape_bgLeftL2.visibility = ImageView.INVISIBLE
                shapeWideR.visibility = ImageView.INVISIBLE                                         // horizontal overlays | crop RIGHT
                shape_bgRightR2.visibility = ImageView.INVISIBLE

                //buttons made invisible if a mode is activated | set to false                      | returns to true under individual CLickListener
                if (cropMode || cropTarg) {                                                         //cropMode linked to cropBtn | cropTarg linked to saveBtn
                    tio1Btn.visibility = Button.INVISIBLE
                    tio2Btn.visibility = Button.INVISIBLE
                    tio3Btn.visibility = Button.INVISIBLE
                    cropMode = false

                    crp1Btn.visibility = Button.INVISIBLE
                    crp2Btn.visibility = Button.INVISIBLE
                    crp3Btn.visibility = Button.INVISIBLE
                    crp4Btn.visibility = Button.INVISIBLE
                    crp5Btn.visibility = Button.INVISIBLE
                    cropTarg = false
                }

                lastCrop = 2 //reset transform

                false
            }
        }

        cropBtn.setOnClickListener {
            cropMode = if (!cropMode) {
                tio1Btn.visibility = Button.VISIBLE                                                 // 1:1 reset 2:3 are visible | cropMode ON
                tio2Btn.visibility = Button.VISIBLE
                tio3Btn.visibility = Button.VISIBLE

                crp1Btn.visibility = Button.INVISIBLE                                               // up down left right crop buttons made invisible
                crp2Btn.visibility = Button.INVISIBLE
                crp3Btn.visibility = Button.INVISIBLE
                crp4Btn.visibility = Button.INVISIBLE
                crp5Btn.visibility = Button.INVISIBLE
                cropTarg = false
                true
            } else {
                tio1Btn.visibility = Button.INVISIBLE                                               //if cropBtn clicked again ratio buttons hidden | cropMode OFF
                tio2Btn.visibility = Button.INVISIBLE
                tio3Btn.visibility = Button.INVISIBLE
                false
            }
        }

        tio1Btn.setOnClickListener {

            val imageView: ImageView = findViewById(R.id.gifView)
            //val onShape: ImageView = findViewById(R.id.shape)
            val shapeBg: ImageView = findViewById(R.id.shape_bg)
            val shapeBgTop: ImageView = findViewById(R.id.shape_bgTop)
            val shapeBgLeft: ImageView = findViewById(R.id.shape_bgLeft)
            val shapeBgRight: ImageView = findViewById(R.id.shape_bgRight)

            var onShape: ImageView = findViewById(R.id.shape)                                       //crop bounds for cropping top or bottom [vertical gif]
            if (cropLeft) onShape = findViewById(R.id.shapeWide)                                    //crop bounds, left side of gif highlighted  [horizontal gif]
            if (cropRight) onShape = findViewById(R.id.shapeWideR)                                  //crop bounds, right side of gif highlighted [horizontal gif]

            if (!cropLeft) shapeWide.visibility = ImageView.INVISIBLE                               //if cropLeft or cropRight not true they are hidden
            if (!cropRight) shapeWideR.visibility = ImageView.INVISIBLE

            //get width x height of imageView & by proxy dimensions of gif in dp | be aware the returned values are not the actual dimensions of the gif | they are relative to the imageView and how the gif has been scaled
            val width = imageView.drawable.intrinsicWidth                                           //https://stackoverflow.com/a/49253306
            val height = imageView.drawable.intrinsicHeight                                         //

            //layout parameters for fill background || may not be needed
            val lpBg = shapeBg.layoutParams as ConstraintLayout.LayoutParams
            val lp = onShape.layoutParams as ConstraintLayout.LayoutParams                          //https://stackoverflow.com/q/54347326
            lp.rightToRight = imageView.right // centralises gif

            // w X h are compared || for 1:1 ratio, the shorted side dictates the other
            if (width > height) { // landscape
                lp.width = height
                lp.height = height

                shapeBg.visibility = ImageView.INVISIBLE        // if gif is landscape, top + bottom colour fills hidden
                shapeBgTop.visibility = ImageView.INVISIBLE

                if (cropLeft || cropRight) { //cropLeft == true?
                    if (cropLeft) {
                        Toast.makeText(context, "LEFT", Toast.LENGTH_SHORT).show()
                        shapeBgLeft.visibility      =   ImageView.INVISIBLE                         //crop ratio 1:1 centre | LEFT side
                        shapeBgRight.visibility     =   ImageView.INVISIBLE                         //crop ratio 1:1 centre | RIGHT side
                        shape_bgLeftL2.visibility   =   ImageView.INVISIBLE                         //duplicate of shapeBgLeft  | constrained to centre guideline because shapeBgLeft  would not re-constrain to left side of gif
                        shape_bgRightR2.visibility  =   ImageView.VISIBLE                           //duplicate of shapeBgRight | constrained to centre guideline because shapeBgRight would not re-constrain to right side of gif
                    }
                    if (cropRight) {
                        Toast.makeText(context, "RIGHT", Toast.LENGTH_SHORT).show()
                        shapeBgLeft.visibility      =   ImageView.INVISIBLE                         //crop ratio 1:1 centre | LEFT side
                        shapeBgRight.visibility     =   ImageView.INVISIBLE                         //crop ratio 1:1 centre | RIGHT side
                        shape_bgLeftL2.visibility   =   ImageView.VISIBLE                           //duplicate of shapeBgLeft  | constrained to centre guideline because shapeBgLeft  would not re-constrain to left side of gif
                        shape_bgRightR2.visibility  =   ImageView.INVISIBLE                         //duplicate of shapeBgRight | constrained to centre guideline because shapeBgRight would not re-constrain to right side of gif
                    }
                }
                else {          //if cropLeft == false, cropRight must be true so else
                    shapeBgLeft.visibility = ImageView.VISIBLE
                    shapeBgRight.visibility = ImageView.VISIBLE
                    shape_bgLeftL2.visibility = ImageView.INVISIBLE                                 //left background for crop highlighter if crop left selected
                    shape_bgRightR2.visibility = ImageView.INVISIBLE
                }
            }

            if (height > width) { // portrait
                lp.width = width
                lp.height = width

                shapeBg.visibility = ImageView.VISIBLE
                shapeBgTop.visibility = ImageView.VISIBLE
                shapeBgLeft.visibility = ImageView.INVISIBLE
                shapeBgRight.visibility = ImageView.INVISIBLE
            }

            if (width == height) {
                lp.width = width
                lp.height = height

                shapeBg.visibility = ImageView.INVISIBLE
                shapeBgTop.visibility = ImageView.INVISIBLE
                shapeBgLeft.visibility = ImageView.INVISIBLE
                shapeBgRight.visibility = ImageView.INVISIBLE
            }

//            lp.matchConstraintMaxWidth = imageHeight
//            lp.matchConstraintMaxHeight = imageHeight

//            lp.width = 0
//            lp.height = 0

            onShape.requestLayout()

            onShape.scaleType = ImageView.ScaleType.CENTER                                          //https://stackoverflow.com/a/26032060
            onShape.visibility = ImageView.VISIBLE

            lpBg.width = 0
            lpBg.height = 0
            shapeBg.requestLayout()
            shapeBg.scaleType = ImageView.ScaleType.CENTER

            crp1X = lp.width
            crp1Y = lp.height

            lastCrop = 1

            croppedWidth  = lp.width    //lp.matchConstraintMaxWidth
            croppedHeight = lp.height   //lp.matchConstraintMaxHeight
            //Toast.makeText(context,"ogX: ${imageView.x}, ogY: ${imageView.y}, bgX: ${onShape.x}, bgY: ${onShape.y}", Toast.LENGTH_LONG).show()
            Toast.makeText(context,"ogX: ${imageWidth}, ogY: ${imageHeight}, bgX: ${dp2px(lp.width.toFloat())}, bgY: ${dp2px(lp.height.toFloat())}", Toast.LENGTH_LONG).show()
        }

        tio2Btn.setOnClickListener {

            val imageView: ImageView = findViewById(R.id.gifView)
            val onShape: ImageView = findViewById(R.id.shape)

            if (cropLeft || cropRight) {
                shapeWide.visibility = ImageView.INVISIBLE
                shapeWideR.visibility = ImageView.INVISIBLE
                cropLeft = false
                cropRight = false
            }

            val width = imageView.drawable.intrinsicWidth                                           //https://stackoverflow.com/a/49253306
            val height = imageView.drawable.intrinsicHeight                                         //

            val lp = onShape.layoutParams as ConstraintLayout.LayoutParams                          //https://stackoverflow.com/q/54347326
            lp.leftToLeft = R.id.gifView
            lp.rightToRight = R.id.gifView
            lp.topToTop = R.id.gifView
            lp.bottomToBottom = R.id.gifView
            lp.width = width
            lp.height = height
            onShape.requestLayout()

            onShape.scaleType = ImageView.ScaleType.CENTER                                          //https://stackoverflow.com/a/26032060
            onShape.visibility = ImageView.VISIBLE

            shape_bg.visibility = ImageView.INVISIBLE
            shape_bgTop.visibility = ImageView.INVISIBLE
            shape_bgLeft.visibility = ImageView.INVISIBLE
            shape_bgRight.visibility = ImageView.INVISIBLE
            shape_bgLeftL2.visibility = ImageView.INVISIBLE
            shape_bgRightR2.visibility = ImageView.INVISIBLE

            lastCrop = 2 // reset

            croppedWidth = lp.width
            croppedHeight = lp.height

            Toast.makeText(context,"ogX: ${imageWidth}, ogY: ${imageHeight}, bgX: ${dp2px(lp.width.toFloat())}, bgY: ${dp2px(lp.height.toFloat())}", Toast.LENGTH_LONG).show()
        }

        tio3Btn.setOnClickListener {

            val imageView: ImageView = findViewById(R.id.gifView)
            val onShape: ImageView = findViewById(R.id.shape)

            shapeWide.visibility = ImageView.INVISIBLE
            shapeWideR.visibility = ImageView.INVISIBLE

            val shapeBg: ImageView = findViewById(R.id.shape_bg)
            val shapeBgTop: ImageView = findViewById(R.id.shape_bgTop)
            val shapeBgLeft: ImageView = findViewById(R.id.shape_bgLeft)
            val shapeBgRight: ImageView = findViewById(R.id.shape_bgRight)

            val width = imageView.drawable.intrinsicWidth                                           //https://stackoverflow.com/a/49253306
            val height = imageView.drawable.intrinsicHeight                                         //

            val lpBg = shapeBg.layoutParams as ConstraintLayout.LayoutParams
            val lp = onShape.layoutParams as ConstraintLayout.LayoutParams                          //https://stackoverflow.com/q/54347326
            lp.leftToLeft = R.id.gifView
            lp.rightToRight = R.id.gifView
            lp.topToTop = R.id.gifView                                                              // left and top is bottom :: weird | imageView.left / top
            lp.bottomToBottom = R.id.gifView

            if (width > height) {
                lp.width = width
                lp.height = height
                shapeBg.visibility = ImageView.INVISIBLE
                shapeBgTop.visibility = ImageView.INVISIBLE

                if (cropLeft || cropRight) {
                    shapeBgLeft.visibility = ImageView.INVISIBLE
                    shapeBgRight.visibility = ImageView.INVISIBLE
                    shape_bgLeftL2.visibility = ImageView.INVISIBLE                                 // normally visible | paired with shapeWide
                    shape_bgRightR2.visibility = ImageView.INVISIBLE                                // normally visible | paired with shapeWide

                }
                else {
                    shapeBgLeft.visibility = ImageView.INVISIBLE
                    shapeBgRight.visibility = ImageView.INVISIBLE
                    shape_bgLeftL2.visibility = ImageView.INVISIBLE
                    shape_bgRightR2.visibility = ImageView.INVISIBLE
                }
            }

            if (height > width || width == height) {
                lp.width = width
                lp.height = width / 100 * 90
                shapeBg.visibility = ImageView.VISIBLE
                shapeBgTop.visibility = ImageView.VISIBLE

                shapeBgLeft.visibility = ImageView.INVISIBLE
                shapeBgRight.visibility = ImageView.INVISIBLE
            }

            onShape.requestLayout()

            onShape.scaleType = ImageView.ScaleType.CENTER                                          //https://stackoverflow.com/a/26032060
            onShape.visibility = ImageView.VISIBLE

            lpBg.width = 0
            lpBg.height = 0

            crp2X = lp.width
            crp2Y = lp.height

            lastCrop = 3

            croppedWidth = lp.width
            croppedHeight = lp.height

            Toast.makeText(context,"ogX: ${imageWidth}, ogY: ${imageHeight}, bgX: ${dp2px(lp.width.toFloat())}, bgY: ${dp2px(lp.height.toFloat())}", Toast.LENGTH_LONG).show()
        }

        saveFab.setOnClickListener {
            if (lastCrop == 2) return@setOnClickListener
            val imageView: ImageView = findViewById(R.id.gifView)
            val width = imageView.drawable.intrinsicWidth
            val height = imageView.drawable.intrinsicHeight

            if (height > width || height == width) {
                cropTarg = if (!cropTarg) {
                    crp1Btn.visibility = Button.VISIBLE                                             // 1 + 2 + 5 [top middle bottom crop]
                    crp2Btn.visibility = Button.VISIBLE
                    crp5Btn.visibility = Button.VISIBLE

                    crp3Btn.visibility = Button.INVISIBLE
                    crp4Btn.visibility = Button.INVISIBLE

                    tio1Btn.visibility = Button.INVISIBLE
                    tio2Btn.visibility = Button.INVISIBLE
                    tio3Btn.visibility = Button.INVISIBLE
                    cropMode = false
                    true
                } else {
                    crp1Btn.visibility = Button.INVISIBLE
                    crp2Btn.visibility = Button.INVISIBLE
                    crp3Btn.visibility = Button.INVISIBLE
                    crp4Btn.visibility = Button.INVISIBLE
                    crp5Btn.visibility = Button.INVISIBLE
                    false
                }
            }
            if (width > height) {
                cropTarg = if (!cropTarg) {
                    crp1Btn.visibility = Button.INVISIBLE
                    crp2Btn.visibility = Button.INVISIBLE

                    crp3Btn.visibility = Button.VISIBLE                                             //3 + 4 + 5 [left middle right crop]
                    crp4Btn.visibility = Button.VISIBLE
                    crp5Btn.visibility = Button.VISIBLE

                    tio1Btn.visibility = Button.INVISIBLE
                    tio2Btn.visibility = Button.INVISIBLE
                    tio3Btn.visibility = Button.INVISIBLE
                    cropMode = false
                    true
                } else {
                    crp1Btn.visibility = Button.INVISIBLE
                    crp2Btn.visibility = Button.INVISIBLE
                    crp3Btn.visibility = Button.INVISIBLE
                    crp4Btn.visibility = Button.INVISIBLE
                    crp5Btn.visibility = Button.INVISIBLE
                    false
                }
            }
        }

        crp1Btn.setOnClickListener {
            val imageView: ImageView = findViewById(R.id.gifView)
            val onShape: ImageView = findViewById(R.id.shape)

            var width: Int
            var height: Int

            width = imageView.drawable.intrinsicWidth                                               //https://stackoverflow.com/a/49253306
            height = imageView.drawable.intrinsicHeight

            if (width > height && lastCrop == 1) return@setOnClickListener
            if (width > height && lastCrop == 2) return@setOnClickListener
            if (width > height && lastCrop == 3) return@setOnClickListener

            val lp = onShape.layoutParams as ConstraintLayout.LayoutParams                          //https://stackoverflow.com/q/54347326

            lp.leftToLeft = R.id.gifView
            lp.rightToRight = R.id.gifView
            lp.topToTop = R.id.gifView
            lp.bottomToBottom = R.id.gifView
            lp.width = width
            lp.height = height
            onShape.requestLayout()

            if (lastCrop == 1) {
                width = crp1X
                height = crp1Y
            }
            if (lastCrop == 2) {
                return@setOnClickListener
            }
            if (lastCrop == 3) {
                width = crp2X
                height = crp2Y
            }

            lp.leftToLeft = imageView.left                                                          //left + bottom for constraint top
            lp.bottomToBottom = imageView.bottom

            lp.width = width
            lp.height = height

            onShape.requestLayout()

            //onShape.scaleType = ImageView.ScaleType.CENTER                                          //https://stackoverflow.com/a/26032060
            onShape.visibility = ImageView.VISIBLE

            croppedWidth = lp.width
            croppedHeight = lp.height
        }

        crp2Btn.setOnClickListener {
            val imageView: ImageView = findViewById(R.id.gifView)
            val onShape: ImageView = findViewById(R.id.shape)

            var width: Int
            var height: Int

            width = imageView.drawable.intrinsicWidth                                               //https://stackoverflow.com/a/49253306
            height = imageView.drawable.intrinsicHeight

            if (width > height && lastCrop == 1) return@setOnClickListener
            if (width > height && lastCrop == 2) return@setOnClickListener
            if (width > height && lastCrop == 3) return@setOnClickListener

            val lp = onShape.layoutParams as ConstraintLayout.LayoutParams                          //https://stackoverflow.com/q/54347326

            lp.leftToLeft = R.id.gifView
            lp.rightToRight = R.id.gifView
            lp.topToTop = R.id.gifView
            lp.bottomToBottom = R.id.gifView
            lp.width = width
            lp.height = height
            onShape.requestLayout()

            if (lastCrop == 1) {
                width = crp1X
                height = crp1Y
            }
            if (lastCrop == 2) return@setOnClickListener
            if (lastCrop == 3) {
                width = crp2X
                height = crp2Y
            }

            lp.leftToLeft = imageView.left                                                          //left + top for constraint bottom
            lp.topToTop = imageView.top

            lp.width = width
            lp.height = height

            onShape.requestLayout()
            onShape.visibility = ImageView.VISIBLE

            croppedWidth = lp.width
            croppedHeight = lp.height
        }

        crp3Btn.setOnClickListener {
            if (lastCrop == 2 || lastCrop == 3) return@setOnClickListener

            cropLeft = true
            cropMidl = false
            cropRight = false

            val imageView: ImageView = findViewById(R.id.gifView)
            val onShape: ImageView = findViewById(R.id.shapeWide)

            shape.visibility = ImageView.INVISIBLE
            shapeWideR.visibility = ImageView.INVISIBLE
            shape_bgLeft.visibility = ImageView.INVISIBLE                                           //left background hidden when left crop is selected
            shape_bgRight.visibility = ImageView.INVISIBLE
            shape_bgLeftL2.visibility = ImageView.INVISIBLE
            shape_bgRightR2.visibility = ImageView.VISIBLE

            var width: Int
            var height: Int

            width = imageView.drawable.intrinsicWidth                                               //https://stackoverflow.com/a/49253306
            height = imageView.drawable.intrinsicHeight

            if (height > width && lastCrop == 1) return@setOnClickListener
            if (height > width && lastCrop == 2) return@setOnClickListener
            if (height > width && lastCrop == 3) return@setOnClickListener

            val lp = onShape.layoutParams as ConstraintLayout.LayoutParams                          //https://stackoverflow.com/q/54347326
            if (lastCrop == 1) {
                width = crp1X
                height = crp1Y
            }
            if (lastCrop == 2) return@setOnClickListener
            if (lastCrop == 3) {
                width = crp2Y
                height = crp2Y
            }
            lp.matchConstraintMinWidth = width
            lp.matchConstraintMaxHeight = height
            onShape.requestLayout()

            //onShape.scaleType = ImageView.ScaleType.CENTER                                          //https://stackoverflow.com/a/26032060
            onShape.visibility = ImageView.VISIBLE

            croppedWidth = lp.width
            croppedHeight = lp.height
        }

        crp4Btn.setOnClickListener {

            if (lastCrop == 2 || lastCrop == 3) return@setOnClickListener

            cropLeft = false
            cropMidl = false
            cropRight = true

            val imageView: ImageView = findViewById(R.id.gifView)
            val onShape: ImageView = findViewById(R.id.shapeWideR)

            shape.visibility = ImageView.INVISIBLE
            shapeWide.visibility = ImageView.INVISIBLE
            shape_bgLeft.visibility = ImageView.INVISIBLE                                           //right background hidden when right crop is selected
            shape_bgRight.visibility = ImageView.INVISIBLE
            shape_bgLeftL2.visibility = ImageView.VISIBLE
            shape_bgRightR2.visibility = ImageView.INVISIBLE

            var width: Int
            var height: Int

            width = imageView.drawable.intrinsicWidth                                               //https://stackoverflow.com/a/49253306
            height = imageView.drawable.intrinsicHeight

            if (height > width && lastCrop == 1) return@setOnClickListener
            if (height > width && lastCrop == 2) return@setOnClickListener
            if (height > width && lastCrop == 3) return@setOnClickListener

            val lp = onShape.layoutParams as ConstraintLayout.LayoutParams                          //https://stackoverflow.com/q/54347326

            if (lastCrop == 1) {
                width = crp1X
                height = crp1Y
            }
            if (lastCrop == 2) return@setOnClickListener
            if (lastCrop == 3) {
                width = crp2X
                height = crp2Y
            }

            lp.matchConstraintDefaultWidth = width                                                  //prevents width + height straying beyond dimensions of ImageView
            lp.matchConstraintMinWidth = width
            lp.matchConstraintMaxHeight = height
            onShape.requestLayout()

            //onShape.scaleType = ImageView.ScaleType.CENTER                                          //https://stackoverflow.com/a/26032060
            onShape.visibility = ImageView.VISIBLE

            croppedWidth = lp.width
            croppedHeight = lp.height
        }

        crp5Btn.setOnClickListener {                                                                //copied from tio1Btn :: centres crop area
            if (lastCrop == 2) return@setOnClickListener

            cropLeft = false
            cropMidl = true
            cropRight = false

            val imageView: ImageView = findViewById(R.id.gifView)
            val shapeBg: ImageView = findViewById(R.id.shape_bg)
            val shapeBgTop: ImageView = findViewById(R.id.shape_bgTop)
            val shapeBgLeft: ImageView = findViewById(R.id.shape_bgLeft)
            val shapeBgRight: ImageView = findViewById(R.id.shape_bgRight)

            val onShape: ImageView = findViewById(R.id.shape)                                       //crop bounds | centre [vertical gif]

            //cropCentre so left and right crop boundaries hidden
            if (cropMidl) {
                shapeWide.visibility = ImageView.INVISIBLE
                shapeWideR.visibility = ImageView.INVISIBLE
            }

            //get width x height of imageView
            val width = imageView.drawable.intrinsicWidth                                           //https://stackoverflow.com/a/49253306
            val height = imageView.drawable.intrinsicHeight                                         //

            //layout parameters for fill background || may not be needed
            val lpBg = shapeBg.layoutParams as ConstraintLayout.LayoutParams
            val lp = onShape.layoutParams as ConstraintLayout.LayoutParams                          //https://stackoverflow.com/q/54347326
            lp.rightToRight = imageView.right // centralises gif

            // w X h are compared || for 1:1 ratio, the shorted side dictates the other
            if (width > height) { // landscape

                lp.leftToLeft = R.id.gifView
                lp.rightToRight = R.id.gifView
                lp.topToTop = R.id.gifView
                lp.bottomToBottom = R.id.gifView
                lp.width = width
                lp.height = height
                onShape.requestLayout()

                if (lastCrop == 1) {
                    lp.width = crp1X
                    lp.height  = crp1Y
                }
                if (lastCrop == 2) return@setOnClickListener
                if (lastCrop == 3) {
                    lp.width = crp2X
                    lp.height  = crp2Y
                }

                shapeBg.visibility = ImageView.INVISIBLE                                            // if gif is landscape, top + bottom colour fills hidden
                shapeBgTop.visibility = ImageView.INVISIBLE
                shape_bgLeftL2.visibility = ImageView.INVISIBLE
                shape_bgRightR2.visibility = ImageView.INVISIBLE

                shapeBgLeft.visibility = ImageView.VISIBLE
                shapeBgRight.visibility = ImageView.VISIBLE
            }

            if (height > width || width == height) { // portrait
                lp.leftToLeft = R.id.gifView
                lp.rightToRight = R.id.gifView
                lp.topToTop = R.id.gifView
                lp.bottomToBottom = R.id.gifView
                lp.width = width
                lp.height = height
                onShape.requestLayout()

                if (lastCrop == 1) {
                    lp.width = crp1X
                    lp.height = crp1Y
                }
                if (lastCrop == 2) return@setOnClickListener
                if (lastCrop == 3) {
                    lp.width = crp2X
                    lp.height = crp2Y
                }

                shapeBg.visibility = ImageView.VISIBLE
                shapeBgTop.visibility = ImageView.VISIBLE
                shapeBgLeft.visibility = ImageView.INVISIBLE
                shapeBgRight.visibility = ImageView.INVISIBLE
                shape_bgLeftL2.visibility = ImageView.INVISIBLE
                shape_bgRightR2.visibility = ImageView.INVISIBLE
            }

            onShape.requestLayout()

            onShape.scaleType = ImageView.ScaleType.CENTER                                          //https://stackoverflow.com/a/26032060
            onShape.visibility = ImageView.VISIBLE

            lpBg.width = 0
            lpBg.height = 0
            shapeBg.requestLayout()
            shapeBg.scaleType = ImageView.ScaleType.CENTER

            if (lastCrop == 1) {
                crp1X = lp.width
                crp1Y = lp.height
                lastCrop = 1
            }
            if (lastCrop == 3) {
                crp2X = lp.width
                crp2Y = lp.height
                lastCrop = 3
            }

            croppedWidth = lp.width
            croppedHeight = lp.height
        }
    }

    //https://stackoverflow.com/a/33686380 | https://gist.github.com/laaptu/7867851
    private fun convertPixelsToDp(px: Float): Int {
        val metrics: DisplayMetrics = Resources.getSystem().displayMetrics
        val dp: Float = px / (metrics.densityDpi / 160f)
        return dp.roundToInt()
    }//as above
    private fun convertDpToPixel(dp: Float): Int {
        val metrics: DisplayMetrics = Resources.getSystem().displayMetrics
        val px: Float = dp * (metrics.densityDpi / 160f)
        return px.roundToInt()
    }
    //https://stackoverflow.com/a/34604848
    private fun convertToPixels(nDp: Int): Float {
        val conversionScale: Float = context.resources.displayMetrics.density
        return (nDp * conversionScale) + 0.5F
    }

    //https://stackoverflow.com/a/30934538
    private fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).roundToInt() / 3 // Added division by 3
    }
}