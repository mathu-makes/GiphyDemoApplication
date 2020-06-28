# GiphyDemoApplication
Used the Giphy Demo as a starting point and added download feature for storing gifs locally using OkHttp. Glide is used to decode and playback gifs once downloaded.

![GitHub Logo](/images/app-screenshots.jpg)

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
    
The goal of this app was to create a fluid user experience to view and downloand GIFs, scale them to a specified screen size and save the new version.
OkHttp's network capabilities were used to download GIFs from the Giphy database and store them locally on the Android device.
Glide was used to handle decoding GIFs and a recyclerview to diplays them to a view. A new activity is launched if a GIF is clicked and can be further edited there.

ROADMAP:
    Add ability to store cropped area of GIF and scale preferences + ability to save new version.
    Add ability to connect Android device to MCU with TinyUSB so user can drag over GIFs directly to MCU flash storage or SDcard.


  
  
