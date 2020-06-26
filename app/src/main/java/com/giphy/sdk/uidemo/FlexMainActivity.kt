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

package com.giphy.sdk.uidemo

//android
import android.content.Intent

//giphy models
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.core.models.enums.RatingType

//giphy ui
import com.giphy.sdk.ui.Giphy
import com.giphy.sdk.ui.GPHSettings
import com.giphy.sdk.ui.GPHContentType
import com.giphy.sdk.ui.themes.GPHTheme
import com.giphy.sdk.ui.themes.GridType
import com.giphy.sdk.ui.views.GiphyDialogFragment

//giphy feed
import com.giphy.sdk.uidemo.feed.*                                                                  // Giphy Media class GIF items

//activities
import kotlinx.android.synthetic.main.flex_activity_main.*                                          // XML views

//logging
import timber.log.Timber                                                                            // Timber logging
import android.widget.Toast                                                                         // UI Messages

//android
import android.net.Uri                                                                              // saveImage function
import android.os.Build                                                                             // os build version
import android.os.Bundle                                                                            // saveStates
import android.os.Environment                                                                       // needed for creating directories on OS versions <29 [my phone]
import android.provider.MediaStore                                                                  // sdk >29 needs this :: Environment.getExternalStorageDirectory() is depreciated sdk versions >=29 | legacy mode <29
import android.content.ContentValues                                                                // ContentValues necessary for MediaStore [images etc]
import android.media.MediaScannerConnection                                                         // scans media directories [type "gif" etc]
import android.view.*

//androidx
import androidx.appcompat.app.AppCompatActivity                                                     // Super class used to build front UI thread of app

//recyclerview
import androidx.recyclerview.widget.RecyclerView                                                    // handles large amounts of data and displays them a view

//recyclerview item selection tracker
import androidx.recyclerview.selection.*                                                            // handles selection of items within recyclerview

//staggered grid layout
import androidx.recyclerview.widget.StaggeredGridLayoutManager                                      // staggered grid layout :: dynamic item sizing + column spans
import androidx.recyclerview.widget.StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS

//OkHttp network library                                                                            //okHttp used to make network calls to download GIFs
import okhttp3.*
import okio.sink
import okio.buffer

//Java                                                                                              //file manager api
import java.io.File
import java.io.IOException
import java.io.File.separator


/**
 * Launcher Activity for the cat gallery demo app that demonstrates the usage of the
 * {@link FlexboxLayoutManager} that handles various sizes of views aligned nicely regardless of
 * the device width like the Google Photo app without loading all the images on the memory.
 * Thus compared to using the {@link FlexboxLayout}, it's much less likely to abuse the memory,
 * which some times leads to the OutOfMemoryError.
 */

class FlexMainActivity : AppCompatActivity() {

    companion object {
        val TAG = FlexMainActivity::class.java.simpleName
        //val INVALID_KEY = "NOT_A_VALID_KEY"
    }

    private var settings = GPHSettings(gridType = GridType.waterfall, useBlurredBackground = true,
        theme = GPHTheme.Dark, stickerColumnCount = 2, rating = RatingType.pg13,
        mediaTypeConfig = arrayOf(GPHContentType.gif, GPHContentType.sticker,
            GPHContentType.text, GPHContentType.emoji, GPHContentType.recents))

    var feedAdapter: FlexCatAdapter? = null //MessageFeedAdapter? = null
    var messageItems = ArrayList<FeedDataItem>()

    private var tracker: SelectionTracker<Long>? = null

    // create value for Activity context
    val context = this@FlexMainActivity
//--------------------------------------------------------------------------------------------------
    /**********************
     *    Giphy API KEY
     ***********************/
    //TODO: Set a valid API KEY
    private val apiKey = "API KEY CAN BE ATTAINED FROM GIPHY.COM | WILL NOT WORK IF NO API KEY FOUND | Matt"
//--------------------------------------------------------------------------------------------------
    private val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL
        ) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
//--------------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        //Giphy API key
        Giphy.configure(this, apiKey, true)

        //layout
        setContentView(R.layout.flex_activity_main)
        setupToolbar()
        flexBox()


        if(savedInstanceState != null)                                                              // [2]
            tracker?.onRestoreInstanceState(savedInstanceState)

        //icon listeners
        launchGiphyBtn.setOnClickListener {
            if (DemoConfig.contentType == GPHContentType.recents && Giphy.recents.count == 0) {
                Toast.makeText(applicationContext, "No recent GIFs found. Select other media type to click them.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, GridViewDemoActivity::class.java)
            startActivity(intent)
        }
        //PERMISSIONS

        //logging
        Timber.plant(Timber.DebugTree()) // timber debug log
    }
//--------------------------------------------------------------------------------------------------
    override fun onSaveInstanceState(outState: Bundle) {                                            // [2] - - see >> https://developer.android.com/reference/kotlin/android/app/Activity#onsaveinstancestate_1
        super.onSaveInstanceState(outState)
        tracker?.onSaveInstanceState(outState)

    }
//--------------------------------------------------------------------------------------------------
    // Gif selection listener
    private fun getGifSelectionListener() = object : GiphyDialogFragment.GifSelectionListener {
        override fun onGifSelected(media: Media, searchTerm: String?) {
            Timber.d(TAG, "onGifSelected")
            //messageItems.add(GifItem(media, Author.Me))                                           // add GifItem to messageItems
            //feedAdapter?.notifyItemInserted(messageItems.size - 1)                                // notify view feed adapter so Gif is displayed in view
//--------------------------------------------------------------------------------------------------
            //check if gif has already been downloaded                                              https://stackoverflow.com/questions/16237950/android-check-if-file-exists-without-creating-a-new-one/48021811#48021811
            val fileCheck = media.title.toString() + ".gif"
            val fileExist = File(                                                                   // directory check for the above
                Environment.getExternalStorageDirectory().toString()
                        + separator + "GiphyDemoApplication", fileCheck
            )
            if(fileExist.exists()) {
                Timber.d("already downloaded")
                Toast.makeText(
                    this@FlexMainActivity,
                    media.title.toString() + ".gif already downloaded",
                    Toast.LENGTH_SHORT
                ).show()
            }
//--------------------------------------------------------------------------------------------------
            //if gif not present in directory, proceed with download :: need a Q version
            else {
                Timber.d("starting download")
                Toast.makeText(
                    this@FlexMainActivity,
                    media.title.toString() + ".gif downloading",
                    Toast.LENGTH_SHORT
                ).show()


                //high quality gif download | processed version by Giphy to gif from source         // giphyURL has to be initialised here | creates a new instance each time a Gif is selected, passing URL to OkHttp for download | previous version had giphyURL as var, only one URL would be parsed
                val giphyURL = "https://media.giphy.com/media/${media.id}/giphy.gif"                //down sampled preview | giphyURL = "https://media.giphy.com/media/${media.id}/giphy.gif" | source giphyURL = "https://media.giphy.com/media/${media.id}/source.gif" << not always gif
                Timber.d(giphyURL)

                //----------------------------------------------------------------------------------
                val okClient = OkHttpClient()
                val okRequest = Request.Builder().url(giphyURL).build()
                //OkHttp | newCall to Giphy
                okClient.newCall(okRequest).enqueue(object : Callback {                             // https://www.youtube.com/watch?v=HzPmbzIVxDo
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    // OkHttp onResponse call if call to Giphy url successful
                    override fun onResponse(call: Call, response: Response) {                       // [3] https://gitlab.com/commonsguy/cw-android-q/blob/v0.5/ConferenceVideos/src/main/java/com/commonsware/android/conferencevideos/VideoRepository.kt#L69-102
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        else {
                            // operating system check before download
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {                   // untested until I get a faster workstation
                                val fileName = media.title.toString() + ".gif"
                                val values = ContentValues().apply {
                                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                    put(
                                        MediaStore.Images.Media.RELATIVE_PATH,
                                        "Pictures/GiphyDemoApplication"
                                    )
                                    put(MediaStore.Images.Media.MIME_TYPE, "image/gif")
                                    put(MediaStore.Images.Media.IS_PENDING, true)
                                }

                                val resolver = contentResolver
                                val uri = resolver.insert(collection, values)

                                uri?.let {
                                    resolver.openOutputStream(uri)?.use { outputStream ->
                                        val sink = outputStream.sink().buffer()

                                        response.body?.source()?.let { sink.writeAll(it) }
                                        sink.close()
                                    }

                                    values.clear()
                                    values.put(MediaStore.Images.Media.IS_PENDING, false)
                                    resolver.update(uri, values, null, null)
                                } ?: throw RuntimeException("MediaStore failed for some reason")

                            } else {                                                                // Android OS <29 :: response from call + download GIF to folder

                                val fileName = media.title.toString() + ".gif"                      // filename is taken from Giphy Media class
                                val folderName = "GiphyDemoApplication"                             // folder name the file will be stored

                                val directory = File(                                               // directory for the above
                                        Environment.getExternalStorageDirectory()
                                            .toString() + separator + folderName
                                    )
                                if (!directory.exists()) {                                          // create directory if it doesn't exist | needs runtime permission
                                    Timber.d("CREATE FOLDER PARSE CHECK...")
                                    directory.mkdirs()
                                }

                                val file = File(directory, fileName)                                // create new File

                                val sink = file.sink().buffer()                                     // buffer + sink to new file | OkHttp with Okio operation

                                response.body?.source()?.let { sink.writeAll(it) }                  // source body written to sink file | OKHttp Okio operation
                                sink.close()                                                        // sink closed [stream ended] :: needs progress bar
                                Timber.d("Finished DL")                                    // Timber debug message for when this code is reached

                                MediaScannerConnection.scanFile(                                    // Scans for newly created Gif media
                                    context,
                                    arrayOf(file.absolutePath),
                                    arrayOf("image/gif"),
                                    null
                                )
                                //FileProvider.getUriForFile(context, AUTHORITY, file)              // unknown at this time
                                runOnUiThread {
                                    loadImage(file)                                                 // https://www.youtube.com/watch?v=HzPmbzIVxDo + [4]
                                }
                            }
                        }
                    }
                })
            }

            Timber.d("onGifSelected ${media.id}")
        }
//--------------------------------------------------------------------------------------------------
    // Gif selection listener functions continued >>
        override fun onDismissed() {
            Timber.d("onDismissed")
            //GiphyDialogFragment.KeyboardState.CLOSED
        }

        override fun didSearchTerm(term: String) {
            Timber.d(TAG, "didSearchTerm $term")
        }
    }
//--------------------------------------------------------------------------------------------------
    //Giphy menu stuff
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.demo_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> showSettingsDialog()
            R.id.action_grid -> openGridViewDemo()
            R.id.action_grid_view -> openGridViewExtensionsDemo()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun openGridViewDemo(): Boolean {
        val intent = Intent(this, GridViewSetupActivity::class.java)
        startActivity(intent)
        //Timber.d("openGridViewDemo")
        return true
    }

    private fun openGridViewExtensionsDemo(): Boolean {
        val intent = Intent(this, GridViewExtensionsActivity::class.java)
        startActivity(intent)
        //Timber.d("openGridViewExtensionsDemo")
        return true
    }
//--------------------------------------------------------------------------------------------------
    // Layout setup functions
    private fun flexBox() {

        val recyclerView: RecyclerView = findViewById(R.id.flexFeed)

        val myLayoutManager = StaggeredGridLayoutManager(2,
            StaggeredGridLayoutManager.VERTICAL)
        myLayoutManager.gapStrategy = GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS

        val folderName = "GiphyDemoApplication"
        val directory = File(
            Environment.getExternalStorageDirectory().toString() + separator + folderName
        )
        loadSavedImages(directory)                                                                  // [4]

        recyclerView.apply {
            feedAdapter = FlexCatAdapter(messageItems, this@FlexMainActivity)
            layoutManager = myLayoutManager
            adapter?.notifyDataSetChanged()
            adapter = feedAdapter
        }

        //recyclerview selection tracker
        tracker = SelectionTracker.Builder<Long>( // [1]
            "selection-1",
            recyclerView,
            StableIdKeyProvider(recyclerView),
            MyItemDetailsLookup(recyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()

        feedAdapter?.setTracker(tracker) //tracker set to feedAdapter

        tracker?.addObserver(object: SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                //handle the selected according to your logic
                val nItems:Int? = tracker?.selection?.size()
                Timber.d("isSelected# :: ${nItems}!")
            }
        })
    }
//--------------------------------------------------------------------------------------------------
    //load images from directory | [4]                                                              | https://www.learningsomethingnew.com/how-to-use-a-recycler-view-to-show-images-from-storage
    private fun loadSavedImages(dir: File) {                                                        //https://github.com/syonip/android-recycler-example/blob/master/app/src/main/java/com/example/myapplication/PictureContent.java
        messageItems.clear()
        if (dir.exists()) {
            val files = dir.listFiles()
            for (file in files) {
                val absolutePath = file.absolutePath
                val extension = absolutePath.substring(absolutePath.lastIndexOf("."))
                if (extension == ".gif") {
                    loadImage(file)
                }
            }
        }
    }

    private fun loadImage(file: File?) {                                                            // [4]
        if (file != null) {
            messageItems.add(PictureItem(Uri.fromFile(file), file.name.toString(), Author.Me))
        }
        feedAdapter?.notifyItemInserted(messageItems.size - 1)                              // this is the position I think things are asking for
    }
//--------------------------------------------------------------------------------------------------
    //Giphy menu setup continued >>
    private fun showSettingsDialog(): Boolean {
        val dialog = SettingsDialogFragment.newInstance(settings)
        dialog.dismissListener = ::applyNewSettings
        dialog.show(supportFragmentManager, "settings_dialog")
        return true
    }

    private fun applyNewSettings(settings: GPHSettings) {
        this.settings = settings
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
    }
}


//credits + sources

//recyclerview tracker [1]
// https://stackoverflow.com/questions/57442809/how-to-use-androidx-recyclerview-selection-in-androidx-recyclerview-widget-or-h
// [2] https://code.tutsplus.com/tutorials/how-to-add-selection-support-to-a-recyclerview--cms-32175
// https://github.com/marcosholgado/multiselection/tree/master/app/src/main/java/com/marcosholgado/multiselection
// https://github.com/bgarcia817/MultipleSelectorExample/tree/master/app/src/main/java/com/example/multipleselectorexample

// [3] | OkHttp call + download | Saving to local storage
//https://www.youtube.com/watch?v=HzPmbzIVxDo | Kotlin on Android development: Image download & display using OkHttp
//https://stackoverflow.com/questions/25893030/download-binary-file-from-okhttp/29012988#29012988
//https://stackoverflow.com/questions/36624756/how-to-save-bitmap-to-android-gallery/57265702#57265702
//https://stackoverflow.com/questions/57726896/mediastore-images-media-insertimage-deprecated/57737524#57737524
//https://gitlab.com/commonsguy/cw-android-q/blob/v0.5/ConferenceVideos/src/main/java/com/commonsware/android/conferencevideos/VideoRepository.kt#L69-102

//storage for post sdk 29 world
//https://stackoverflow.com/questions/58379543/cant-create-directory-in-android-10/58379655#58379655