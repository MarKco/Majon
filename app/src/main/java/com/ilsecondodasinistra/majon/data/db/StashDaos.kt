package com.ilsecondodasinistra.majon.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface YarnDao {
    @Insert
    suspend fun insert(yarn: YarnEntity): Long

    @Update
    suspend fun update(yarn: YarnEntity)

    @Query("DELETE FROM yarns WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Transaction
    @Query("SELECT * FROM yarns ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun observeAllWithMaterials(): Flow<List<YarnWithMaterialsEntity>>

    @Transaction
    @Query("SELECT * FROM yarns WHERE id = :id")
    fun observeWithMaterials(id: Long): Flow<YarnWithMaterialsEntity?>

    @Transaction
    @Query("SELECT * FROM yarns ORDER BY id ASC")
    suspend fun getAllWithMaterials(): List<YarnWithMaterialsEntity>

    @Query("SELECT * FROM yarns WHERE id = :id")
    suspend fun getById(id: Long): YarnEntity?

    @Insert
    suspend fun insertMaterials(materials: List<YarnMaterialEntity>)

    @Query("DELETE FROM yarn_materials WHERE yarnId = :yarnId")
    suspend fun deleteMaterials(yarnId: Long)

    /** Atomic, never below zero: a skein count that went negative would be a lie. */
    @Query("UPDATE yarns SET stockSkeins = MAX(0.0, stockSkeins + :delta), updatedAt = :now WHERE id = :id")
    suspend fun adjustStock(id: Long, delta: Double, now: Long)
}

@Dao
interface SwatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(swatch: SwatchEntity): Long

    @Query("DELETE FROM swatches WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM swatches WHERE yarnId = :yarnId ORDER BY id ASC")
    fun observeByYarn(yarnId: Long): Flow<List<SwatchEntity>>

    @Query("SELECT * FROM swatches WHERE yarnId = :yarnId ORDER BY id ASC")
    suspend fun getByYarn(yarnId: Long): List<SwatchEntity>
}

@Dao
interface NeedleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(needle: NeedleEntity): Long

    @Query("DELETE FROM needles WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Sizes first (those without a size last), then type. */
    @Query("SELECT * FROM needles ORDER BY sizeMm IS NULL, sizeMm ASC, type ASC, id ASC")
    fun observeAll(): Flow<List<NeedleEntity>>

    @Query("SELECT * FROM needles ORDER BY id ASC")
    suspend fun getAll(): List<NeedleEntity>
}

@Dao
interface ProjectLinkDao {
    @Query("SELECT * FROM project_yarns WHERE projectId = :projectId ORDER BY isPrimary DESC, yarnId ASC")
    fun observeYarnLinks(projectId: Long): Flow<List<ProjectYarnEntity>>

    @Query("SELECT * FROM project_needles WHERE projectId = :projectId ORDER BY needleId ASC")
    fun observeNeedleLinks(projectId: Long): Flow<List<ProjectNeedleEntity>>

    @Query("SELECT * FROM project_needles")
    fun observeAllNeedleLinks(): Flow<List<ProjectNeedleEntity>>

    @Query("SELECT * FROM project_yarns WHERE projectId = :projectId ORDER BY isPrimary DESC, yarnId ASC")
    suspend fun getYarnLinks(projectId: Long): List<ProjectYarnEntity>

    @Query("SELECT * FROM project_needles WHERE projectId = :projectId ORDER BY needleId ASC")
    suspend fun getNeedleLinks(projectId: Long): List<ProjectNeedleEntity>

    @Insert
    suspend fun insertYarnLinks(links: List<ProjectYarnEntity>)

    @Insert
    suspend fun insertNeedleLinks(links: List<ProjectNeedleEntity>)

    @Query("DELETE FROM project_yarns WHERE projectId = :projectId")
    suspend fun deleteYarnLinks(projectId: Long)

    @Query("DELETE FROM project_needles WHERE projectId = :projectId")
    suspend fun deleteNeedleLinks(projectId: Long)
}
