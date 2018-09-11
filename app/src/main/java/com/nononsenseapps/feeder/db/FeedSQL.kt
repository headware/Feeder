package com.nononsenseapps.feeder.db

// SQL convention says Table name should be "singular"
const val FEED_TABLE_NAME = "Feed"
// Naming the id column with an underscore is good to be consistent
// with other Android things. This is ALWAYS needed
const val COL_ID = "_id"
// These fields can be anything you want.
const val COL_TITLE = "title"
const val COL_CUSTOM_TITLE = "customtitle"
const val COL_URL = "url"
const val COL_TAG = "tag"
const val COL_NOTIFY = "notify"

val CREATE_FEED_TABLE = """
    CREATE TABLE $FEED_TABLE_NAME (
      $COL_ID INTEGER PRIMARY KEY,
      $COL_TITLE TEXT NOT NULL,
      $COL_CUSTOM_TITLE TEXT NOT NULL,
      $COL_URL TEXT NOT NULL,
      $COL_TAG TEXT NOT NULL DEFAULT '',
      $COL_NOTIFY INTEGER NOT NULL DEFAULT 0,
      $COL_IMAGEURL TEXT,
      UNIQUE($COL_URL) ON CONFLICT REPLACE
    )"""


