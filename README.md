# GiphyDemoApplication
Used the Giphy Demo as a starting point and added download feature for storing gifs locally using OkHttp. Glide is used to decode and playback gifs once downloaded.

First steps towards building a companion app for my gesture controlled GIF player.
Modified from Giphy demo application - the following files contain the additional code blocks I have added:
    
    KOTLIN
    FlexMainActivity.kt
    FlexCatAdapter.kt
    FlexGifViewActivity.kt
    FlexGifViewAdapter.kt
  
    XML
    flex_activity_main.xml
    flex_viewholder_cat.xml
    gif_activity.xml
    
The goal of this app is to create a fluid experience to downloand GIFs and scale them to a specified screen size.
OkHttp's network capabilities are used to download GIFs from the Giphy database and stored locally on the Android device.
Glide is used to handle decoding GIFs and a recyclerview to diplays them to a view. A new activity is launced if a GIF is clicked and can be further edited there.

ROADMAP:
    Add ability to store cropped area of GIF and scale preferences + ability to save new version
    Add ability to connect MCU with TinyUSB so can drag over GIFs directly to device


  
  
