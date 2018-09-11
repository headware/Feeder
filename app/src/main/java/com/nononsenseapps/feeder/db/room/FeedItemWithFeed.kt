package com.nononsenseapps.feeder.db.room

import android.os.Bundle
import androidx.room.ColumnInfo
import com.nononsenseapps.feeder.ui.ARG_AUTHOR
import com.nononsenseapps.feeder.ui.ARG_DATE
import com.nononsenseapps.feeder.ui.ARG_ENCLOSURE
import com.nononsenseapps.feeder.ui.ARG_FEEDTITLE
import com.nononsenseapps.feeder.ui.ARG_FEED_URL
import com.nononsenseapps.feeder.ui.ARG_ID
import com.nononsenseapps.feeder.ui.ARG_IMAGEURL
import com.nononsenseapps.feeder.ui.ARG_LINK
import com.nononsenseapps.feeder.ui.ARG_TITLE
import com.nononsenseapps.feeder.util.setLong
import com.nononsenseapps.feeder.util.setString
import com.nononsenseapps.feeder.util.sloppyLinkToStrictURLNoThrows
import org.joda.time.DateTime
import java.net.URI
import java.net.URL

const val feedItemColumnsWithFeed = "feed_item.id AS id, guid, feed_item.title AS title, " +
        "description, plain_title, plain_snippet, feed_item.image_url, enclosure_link, " +
        "author, pub_date, link, unread, feed.tag AS tag, feed.id AS feed_id, " +
        "feed.title AS feed_title, feed.url AS feed_url"

data class FeedItemWithFeed(
        var id: Long = ID_UNSET,
        var guid: String = "",
        var title: String = "",
        var description: String = "",
        @ColumnInfo(name = "plain_title") var plainTitle: String = "",
        @ColumnInfo(name = "plain_snippet") var plainSnippet: String = "",
        @ColumnInfo(name = "image_url") var imageUrl: String? = null,
        @ColumnInfo(name = "enclosure_link") var enclosureLink: String? = null,
        var author: String? = null,
        @ColumnInfo(name = "pub_date") var pubDate: DateTime? = null,
        var link: String? = null,
        var tag: String = "",
        var unread: Boolean = true,
        @ColumnInfo(name = "feed_id") var feedId: Long? = null,
        @ColumnInfo(name = "feed_title") var feedTitle: String = "",
        @ColumnInfo(name = "feed_url") var feedUrl: URL = sloppyLinkToStrictURLNoThrows("")
) {
    val enclosureFilename: String?
        get() {
            if (enclosureLink != null) {
                var fname: String? = null
                try {
                    fname = URI(enclosureLink).path.split("/").last()
                } catch (e: Exception) {
                }
                if (fname == null || fname.isEmpty()) {
                    return null
                } else {
                    return fname
                }
            }
            return null
        }

    val pubDateString: String?
        get() = pubDate?.toString()

    val domain: String?
        get() {
            val l: String? = enclosureLink ?: link
            if (l != null) {
                try {
                    return URL(l).host.replace("www.", "")
                } catch (e: Throwable) {
                }
            }
            return null
        }

    fun storeInBundle(bundle: Bundle): Bundle {
        bundle.storeFeedItem()
        return bundle
    }

    private fun Bundle.storeFeedItem() {
        setLong(ARG_ID to id)
        setString(ARG_TITLE to title)
        setString(ARG_LINK to link)
        setString(ARG_ENCLOSURE to enclosureLink)
        setString(ARG_IMAGEURL to imageUrl)
        setString(ARG_FEEDTITLE to feedTitle)
        setString(ARG_AUTHOR to author)
        setString(ARG_DATE to pubDateString)
        setString(ARG_FEED_URL to feedUrl.toString())
    }
}