package com.giphy.sdk.uidemo.feed

import com.giphy.sdk.core.models.Media
import com.giphy.sdk.core.models.Images

import android.net.Uri

open class FeedDataItem(val author: Author)
class MessageItem(val text: String, author: Author) : FeedDataItem(author)
class InvalidKeyItem(author: Author) : FeedDataItem(author)
class PictureItem(val uri: Uri, val name: String, author: Author) : FeedDataItem(author)
class ViewDataItem(val uri: Uri, author: Author) : FeedDataItem(author)

enum class Author {
    Me
}