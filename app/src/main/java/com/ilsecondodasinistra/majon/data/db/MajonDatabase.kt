package com.ilsecondodasinistra.majon.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, PartEntity::class, NoteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MajonDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun partDao(): PartDao
    abstract fun noteDao(): NoteDao
}
