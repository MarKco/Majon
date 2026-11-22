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

/** v2 -> v3: the stash. Yarns (with their blend and gauge swatches) and needles. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `yarns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `brand` TEXT, `colorName` TEXT, `colorArgb` INTEGER, `dyeLot` TEXT, `weight` TEXT, `skeinWeightG` INTEGER, `skeinLengthM` INTEGER, `stockSkeins` REAL NOT NULL, `pricePerSkein` REAL, `shop` TEXT, `care` TEXT, `notes` TEXT, `photoPath` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `yarn_materials` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `yarnId` INTEGER NOT NULL, `material` TEXT NOT NULL, `customName` TEXT, `percent` INTEGER, `position` INTEGER NOT NULL, FOREIGN KEY(`yarnId`) REFERENCES `yarns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_yarn_materials_yarnId` ON `yarn_materials` (`yarnId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `swatches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `yarnId` INTEGER NOT NULL, `stitches` INTEGER NOT NULL, `rows` INTEGER NOT NULL, `needleSizeMm` REAL, `pattern` TEXT NOT NULL, `weightG` REAL, `notes` TEXT, FOREIGN KEY(`yarnId`) REFERENCES `yarns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_swatches_yarnId` ON `swatches` (`yarnId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `needles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `material` TEXT, `sizeMm` REAL, `lengthCm` INTEGER, `quantity` INTEGER NOT NULL, `brand` TEXT, `notes` TEXT)")
    }
}

/** Every migration, in order. Register new ones here (see CLAUDE.md). */
val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
