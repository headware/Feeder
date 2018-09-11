package com.nononsenseapps.feeder.db

import android.content.UriMatcher
import android.net.Uri


const val AUTHORITY = "com.nononsenseapps.feeder.provider"
const val SCHEME = "content://"

// URIs
// Feed
@JvmField
val URI_FEEDS: Uri = Uri.withAppendedPath(Uri.parse(SCHEME + AUTHORITY), FEED_TABLE_NAME)
// Feed item
@JvmField
val URI_FEEDITEMS: Uri = Uri.withAppendedPath(Uri.parse(SCHEME + AUTHORITY), FEED_ITEM_TABLE_NAME)
