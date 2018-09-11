package com.nononsenseapps.feeder.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nononsenseapps.feeder.ui.text.HtmlToPlainTextConverter
import com.nononsenseapps.feeder.util.relativeLinkIntoAbsolute
import com.nononsenseapps.feeder.util.sloppyLinkToStrictURL
import com.nononsenseapps.jsonfeed.Item
import org.joda.time.DateTime
import java.net.URI
import java.net.URL

@Entity(tableName = "feed_item",
        indices = [Index(value = ["guid", "feed_id"], unique = true),
            Index(value = ["feed_id"])],
        foreignKeys = [ForeignKey(entity = Feed::class,
                parentColumns = ["id"],
                childColumns = ["feed_id"],
                onDelete = CASCADE)])
data class FeedItem(
        @PrimaryKey(autoGenerate = true) var id: Long = ID_UNSET,
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
        var unread: Boolean = true,
        var notified: Boolean = false,
        @ColumnInfo(name = "feed_id") var feedId: Long? = null) {

    fun updateFromParsedEntry(entry: Item, feed: com.nononsenseapps.jsonfeed.Feed) {
        // Be careful about nulls.
        val text = entry.content_html ?: entry.content_text ?: this.description
        val summary: String? = entry.summary ?: entry.content_text?.take(200) ?: HtmlToPlainTextConverter.convert(text).take(200)
        val absoluteImage = when {
            feed.feed_url != null && entry.image != null -> relativeLinkIntoAbsolute(sloppyLinkToStrictURL(feed.feed_url!!), entry.image!!)
            else -> entry.image
        }

        entry.id?.let { this.guid = it }
        entry.title?.let { this.title = it }
        text.let { this.description = it }
        entry.title?.let { this.plainTitle = HtmlToPlainTextConverter.convert(it) }
        summary?.let { this.plainSnippet = it }

        this.imageUrl = absoluteImage
        this.enclosureLink = entry.attachments?.firstOrNull()?.url
        this.author = entry.author?.name ?: feed.author?.name
        this.link = entry.url

        var dt: DateTime? = null
        try {
            dt = DateTime.parse(entry.date_published)
        } catch(t: Throwable) {
        }
        this.pubDate = dt
    }

    val pubDateString: String?
        get() = pubDate?.toString()

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
}
