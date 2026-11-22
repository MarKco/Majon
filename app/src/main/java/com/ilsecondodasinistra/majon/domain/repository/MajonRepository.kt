package com.ilsecondodasinistra.majon.domain.repository

import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectWithParts
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.StashImportCounts
import com.ilsecondodasinistra.majon.domain.model.StashSnapshot
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import kotlinx.coroutines.flow.Flow

interface MajonRepository {
    fun observeProjects(): Flow<List<ProjectWithParts>>
    fun observeProject(id: Long): Flow<ProjectWithParts?>
    suspend fun upsertProject(project: Project): Long
    suspend fun deleteProject(id: Long)

    fun observePart(id: Long): Flow<Part?>
    suspend fun upsertPart(part: Part): Long
    suspend fun deletePart(id: Long)
    suspend fun setCompletedRows(partId: Long, completedRows: Int)

    /** Atomically adds [delta] to the completed steps, clamped to 0..totalSteps (extra rows included). */
    suspend fun adjustCompletedRows(partId: Long, delta: Int)

    fun observeNotes(partId: Long): Flow<List<RowNote>>
    suspend fun upsertNote(note: RowNote): Long
    suspend fun deleteNote(id: Long)

    // --- stash ---

    /** Yarns sorted by name, ignoring case. */
    fun observeYarns(): Flow<List<Yarn>>
    fun observeYarn(id: Long): Flow<Yarn?>

    /** Inserts or updates a yarn, replacing its blend. */
    suspend fun upsertYarn(yarn: Yarn): Long
    suspend fun deleteYarn(id: Long)

    /** Atomically adds [deltaSkeins] (negative to use some) to the stock, never going below zero. */
    suspend fun adjustYarnStock(id: Long, deltaSkeins: Double)

    fun observeSwatches(yarnId: Long): Flow<List<Swatch>>
    suspend fun upsertSwatch(swatch: Swatch): Long
    suspend fun deleteSwatch(id: Long)

    /** Needles sorted by size (unknown sizes last). */
    fun observeNeedles(): Flow<List<Needle>>
    suspend fun upsertNeedle(needle: Needle): Long
    suspend fun deleteNeedle(id: Long)

    suspend fun getStash(): StashSnapshot

    /** Adds the snapshot's yarns (with swatches) and needles as new records. */
    suspend fun importStash(stash: StashSnapshot): StashImportCounts

    suspend fun getFullProjects(): List<FullProject>
    suspend fun importProjects(projects: List<FullProject>): Int
}
