package com.ilsecondodasinistra.majon.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectEntity::class, PartEntity::class, NoteEntity::class,
        YarnEntity::class, YarnMaterialEntity::class, SwatchEntity::class, NeedleEntity::class,
        ProjectYarnEntity::class, ProjectNeedleEntity::class, FinishingTaskEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class MajonDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun partDao(): PartDao
    abstract fun noteDao(): NoteDao
    abstract fun yarnDao(): YarnDao
    abstract fun swatchDao(): SwatchDao
    abstract fun needleDao(): NeedleDao
    abstract fun projectLinkDao(): ProjectLinkDao
}
