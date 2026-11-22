package com.ilsecondodasinistra.majon.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: optional preparation row (row 0) and final joining row per part, each with its own note. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE parts ADD COLUMN hasPrepRow INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE parts ADD COLUMN hasFinalRow INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE parts ADD COLUMN prepNote TEXT")
        db.execSQL("ALTER TABLE parts ADD COLUMN finalNote TEXT")
    }
}

/** Every migration, in order. Register new ones here (see CLAUDE.md). */
val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
