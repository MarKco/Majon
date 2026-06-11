package com.ilsecondodasinistra.majon.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllWithParts(): Flow<List<ProjectWithPartsEntity>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeWithParts(id: Long): Flow<ProjectWithPartsEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: Long): ProjectEntity?

    @Transaction
    @Query("SELECT * FROM projects ORDER BY createdAt ASC")
    suspend fun getAllWithParts(): List<ProjectWithPartsEntity>
}

@Dao
interface PartDao {
    @Insert
    suspend fun insert(part: PartEntity): Long

    @Update
    suspend fun update(part: PartEntity)

    @Query("DELETE FROM parts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM parts WHERE id = :id")
    fun observeById(id: Long): Flow<PartEntity?>

    @Query("SELECT * FROM parts WHERE id = :id")
    suspend fun getById(id: Long): PartEntity?

    @Query("UPDATE parts SET completedRows = :completedRows WHERE id = :id")
    suspend fun updateCompletedRows(id: Long, completedRows: Int)
}

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM notes WHERE partId = :partId ORDER BY rowStart ASC, rowEnd ASC")
    fun observeByPartId(partId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE partId = :partId ORDER BY rowStart ASC, rowEnd ASC")
    suspend fun getByPartId(partId: Long): List<NoteEntity>
}
