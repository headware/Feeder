package com.nononsenseapps.feeder.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nononsenseapps.feeder.util.sloppyLinkToStrictURL
import com.nononsenseapps.feeder.util.sloppyLinkToStrictURLNoThrows
import com.nononsenseapps.jsonfeed.Feed
import java.net.URL

@Entity(indices = [Index(value = ["url"], unique = true),
    Index(value = ["id", "url", "title"], unique = true)])
data class Feed(
        @PrimaryKey(autoGenerate = true) var id: Long = ID_UNSET,
        var title: String = "",
        @ColumnInfo(name = "custom_title") var customTitle: String = "",
        @ColumnInfo(name = "url") var url: URL = sloppyLinkToStrictURL(""),
        var tag: String = "",
        var notify: Boolean = false,
        @ColumnInfo(name = "image_url") var imageUrl: URL? = null
) {
    val displayTitle: String
        get() = (if (customTitle.isBlank()) title else customTitle)

    fun updateFromParsedFeed(feed: Feed) {
        title = feed.title ?: title
        url = feed.feed_url?.let { sloppyLinkToStrictURLNoThrows(it) } ?: url
        imageUrl = feed.icon?.let { sloppyLinkToStrictURLNoThrows(it) } ?: imageUrl
    }
}
