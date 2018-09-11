package com.nononsenseapps.feeder.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

const val LEGACY_DATABASE_VERSION = 6
const val LEGACY_DATABASE_NAME = "rssDatabase"

class LegacyDatabaseHandler constructor(context: Context): SQLiteOpenHelper(context, LEGACY_DATABASE_NAME, null, LEGACY_DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_FEED_TABLE)
        db.execSQL(CREATE_FEED_ITEM_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        if (!db.isReadOnly) {
            // Enable foreign key constraints
            db.setForeignKeyConstraintsEnabled(true)
        }
    }
}
