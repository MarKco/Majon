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

/**
 * v3 -> v4: what a project is made with (yarns and needles from the stash) and the measures a part is
 * planned by (stitch and row direction, in cm) with the swatch used to turn them into stitches and rows.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE parts ADD COLUMN stitchMeasure TEXT")
        db.execSQL("ALTER TABLE parts ADD COLUMN stitchCm REAL")
        db.execSQL("ALTER TABLE parts ADD COLUMN rowMeasure TEXT")
        db.execSQL("ALTER TABLE parts ADD COLUMN rowCm REAL")
        db.execSQL("ALTER TABLE parts ADD COLUMN swatchId INTEGER")
        db.execSQL("CREATE TABLE IF NOT EXISTS `project_yarns` (`projectId` INTEGER NOT NULL, `yarnId` INTEGER NOT NULL, `isPrimary` INTEGER NOT NULL, `swatchId` INTEGER, `plannedSkeins` REAL, PRIMARY KEY(`projectId`, `yarnId`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`yarnId`) REFERENCES `yarns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_yarns_yarnId` ON `project_yarns` (`yarnId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `project_needles` (`projectId` INTEGER NOT NULL, `needleId` INTEGER NOT NULL, PRIMARY KEY(`projectId`, `needleId`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`needleId`) REFERENCES `needles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_needles_needleId` ON `project_needles` (`needleId`)")
    }
}

/**
 * v4 -> v5: a part can be knitted more than once (copies, with the one being worked), can note the stitches to
 * cast on and the width where it ends up, and a project has a list of finishing tasks.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE parts ADD COLUMN copies INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE parts ADD COLUMN currentCopy INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE parts ADD COLUMN castOnStitches INTEGER")
        db.execSQL("ALTER TABLE parts ADD COLUMN endStitchCm REAL")
        db.execSQL("CREATE TABLE IF NOT EXISTS `finishing_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `done` INTEGER NOT NULL DEFAULT 0, `position` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finishing_tasks_projectId` ON `finishing_tasks` (`projectId`)")
    }
}

/** Every migration, in order. Register new ones here (see CLAUDE.md). */
val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
