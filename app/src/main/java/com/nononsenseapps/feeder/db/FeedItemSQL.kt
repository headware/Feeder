package com.nononsenseapps.feeder.db


// SQL convention says Table name should be "singular"
const val FEED_ITEM_TABLE_NAME = "FeedItem"


// These fields can be anything you want.
const val COL_GUID = "guid"
const val COL_DESCRIPTION = "description"
const val COL_PLAINTITLE = "plainTitle"
const val COL_PLAINSNIPPET = "plainSnippet"
const val COL_IMAGEURL = "imageUrl"
const val COL_ENCLOSURELINK = "enclosureLink"
const val COL_LINK = "link"
const val COL_AUTHOR = "author"
const val COL_PUBDATE = "pubdate"
const val COL_UNREAD = "unread"
const val COL_NOTIFIED = "notified"
// These fields corresponds to columns in Feed table
const val COL_FEED = "feed"
const val COL_FEEDTITLE = "feedtitle"
const val COL_FEEDURL = "feedurl"

val CREATE_FEED_ITEM_TABLE = """
    CREATE TABLE $FEED_ITEM_TABLE_NAME (
      $COL_ID INTEGER PRIMARY KEY,
      $COL_GUID TEXT NOT NULL,
      $COL_TITLE TEXT NOT NULL,
      $COL_DESCRIPTION TEXT NOT NULL,
      $COL_PLAINTITLE TEXT NOT NULL,
      $COL_PLAINSNIPPET TEXT NOT NULL,
      $COL_IMAGEURL TEXT,
      $COL_LINK TEXT,
      $COL_ENCLOSURELINK TEXT,
      $COL_AUTHOR TEXT,
      $COL_PUBDATE TEXT,
      $COL_UNREAD INTEGER NOT NULL DEFAULT 1,
      $COL_NOTIFIED INTEGER NOT NULL DEFAULT 0,
      $COL_FEED INTEGER NOT NULL,
      $COL_FEEDTITLE TEXT NOT NULL,
      $COL_FEEDURL TEXT NOT NULL,
      $COL_TAG TEXT NOT NULL,
      FOREIGN KEY($COL_FEED)
        REFERENCES $FEED_TABLE_NAME($COL_ID)
        ON DELETE CASCADE,
      UNIQUE($COL_GUID,$COL_FEED)
        ON CONFLICT IGNORE
    )"""

