package com.nononsenseapps.feeder.db.room

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nononsenseapps.feeder.coroutines.BackgroundUI
import com.nononsenseapps.feeder.db.COL_AUTHOR
import com.nononsenseapps.feeder.db.COL_CUSTOM_TITLE
import com.nononsenseapps.feeder.db.COL_DESCRIPTION
import com.nononsenseapps.feeder.db.COL_ENCLOSURELINK
import com.nononsenseapps.feeder.db.COL_FEED
import com.nononsenseapps.feeder.db.COL_GUID
import com.nononsenseapps.feeder.db.COL_ID
import com.nononsenseapps.feeder.db.COL_IMAGEURL
import com.nononsenseapps.feeder.db.COL_LINK
import com.nononsenseapps.feeder.db.COL_NOTIFIED
import com.nononsenseapps.feeder.db.COL_NOTIFY
import com.nononsenseapps.feeder.db.COL_PLAINSNIPPET
import com.nononsenseapps.feeder.db.COL_PLAINTITLE
import com.nononsenseapps.feeder.db.COL_PUBDATE
import com.nononsenseapps.feeder.db.COL_TAG
import com.nononsenseapps.feeder.db.COL_TITLE
import com.nononsenseapps.feeder.db.COL_UNREAD
import com.nononsenseapps.feeder.db.COL_URL
import com.nononsenseapps.feeder.db.FEED_ITEM_TABLE_NAME
import com.nononsenseapps.feeder.db.FEED_TABLE_NAME
import com.nononsenseapps.feeder.db.LEGACY_DATABASE_NAME
import com.nononsenseapps.feeder.db.LegacyDatabaseHandler
import com.nononsenseapps.feeder.util.PrefUtils
import com.nononsenseapps.feeder.util.forEach
import com.nononsenseapps.feeder.util.sloppyLinkToStrictURLNoThrows
import kotlinx.coroutines.experimental.launch
import org.joda.time.DateTime

const val DATABASE_NAME = "feeder-db"
const val ID_UNSET: Long = 0
const val ID_ALL_FEEDS: Long = -10

private val LOG_TAG = "FEEDERMIGRATION"

@Database(entities = [Feed::class, FeedItem::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun feedItemDao(): FeedItemDao

    companion object {
        // For Singleton instantiation
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            if (hasLegacyDb(context)) {
                                Log.d(LOG_TAG, "Has legacy database, migrating...")
                                PrefUtils.clearLastOpenFeed(context)
                                migrateLegacyDatabase(context.applicationContext)
                            } else {
                                Log.d(LOG_TAG, "No legacy database detected.")
                            }
                        }
                    })
                    .build()
        }
    }
}

fun hasLegacyDb(context: Context): Boolean {
    val dbFile = context.getDatabasePath(LEGACY_DATABASE_NAME)
    return dbFile.exists()
}

fun migrateLegacyDatabase(context: Context) = launch(BackgroundUI) {
    try {
        val appDb = AppDatabase.getInstance(context)
        val feedDao = appDb.feedDao()
        val feedItemDao = appDb.feedItemDao()
        LegacyDatabaseHandler(context = context).readableDatabase.use { db ->
            db.query(FEED_TABLE_NAME,
                    arrayOf(COL_ID, COL_TITLE, COL_URL, COL_TAG, COL_CUSTOM_TITLE, COL_NOTIFY, COL_IMAGEURL),
                    null, null, null, null, null)?.use { cursor ->
                cursor.forEach { cursor ->

                    val oldFeedId = cursor.getLong(0)

                    val feed = Feed(
                            title = cursor.getString(1)!!,
                            customTitle = cursor.getString(4)!!,
                            url = sloppyLinkToStrictURLNoThrows(cursor.getString(2)!!),
                            tag = cursor.getString(3)!!,
                            notify = cursor.getInt(5) == 1,
                            imageUrl = cursor.getString(6)?.let { sloppyLinkToStrictURLNoThrows(it) }
                    )

                    Log.d(LOG_TAG, "Migrating ${feed.title}: ${feed.url}")

                    val newFeedId = feedDao.insertFeed(feed)

                    appDb.runInTransaction {
                        db.query(FEED_ITEM_TABLE_NAME,
                                arrayOf(COL_TITLE, COL_DESCRIPTION, COL_PLAINTITLE, COL_PLAINSNIPPET,
                                        COL_IMAGEURL, COL_LINK, COL_AUTHOR, COL_PUBDATE, COL_UNREAD, COL_FEED,
                                        COL_ENCLOSURELINK, COL_NOTIFIED, COL_GUID),
                                "$COL_FEED IS ?",
                                arrayOf(oldFeedId.toString()),
                                null, null, null)?.use { cursor ->
                            cursor.forEach { cursor ->

                                val feedItem = FeedItem(
                                        guid = cursor.getString(12)!!,
                                        title = cursor.getString(0)!!,
                                        description = cursor.getString(1)!!,
                                        plainTitle = cursor.getString(2)!!,
                                        plainSnippet = cursor.getString(3)!!,
                                        imageUrl = cursor.getString(4),
                                        enclosureLink = cursor.getString(10),
                                        author = cursor.getString(6),
                                        pubDate = when (cursor.getString(7)) {
                                            null -> null
                                            else -> {
                                                var dt: DateTime? = null
                                                try {
                                                    dt = DateTime.parse(cursor.getString(7))
                                                } catch (t: Throwable) {
                                                }
                                                dt
                                            }
                                        },
                                        link = cursor.getString(5),
                                        unread = cursor.getInt(8) == 1,
                                        notified = cursor.getInt(11) == 1,
                                        feedId = newFeedId
                                )

                                Log.d(LOG_TAG, "Migrating ${feedItem.title}: ${feedItem.plainSnippet}")

                                feedItemDao.insertFeedItem(feedItem)
                            }
                        }
                    }
                }
            }
        }
    } catch (error: Throwable) {
        Log.e(LOG_TAG, "Migration failed: $error")
    }
}
