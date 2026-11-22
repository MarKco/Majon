package com.ilsecondodasinistra.majon.domain.repository

import com.ilsecondodasinistra.majon.domain.model.BackupImportCounts
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
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

    // --- what a project is made with ---

    fun observeProjectLinks(projectId: Long): Flow<ProjectLinks>

    /** Replaces the project's yarns and needles (normalized: one primary yarn, no duplicates). */
    suspend fun setProjectLinks(projectId: Long, links: ProjectLinks)

    /** For each needle in use, the names of the unfinished projects it is linked to. */
    fun observeNeedleUsage(): Flow<Map<Long, List<String>>>

    /**
     * Copies a project (its parts with measures, row notes and extra rows, and its yarns and needles)
     * under [copyName], with every part's progress back at zero. Returns the new id, null if [id] is unknown.
     */
    suspend fun duplicateProject(id: Long, copyName: String): Long?

    /**
     * Imports a whole backup: the stash first, then the projects, whose links (positions in [stash])
     * are turned into the ids of the records just created. Links pointing nowhere are dropped.
     */
    suspend fun importBackup(projects: List<FullProject>, stash: StashSnapshot): BackupImportCounts

    suspend fun getFullProjects(): List<FullProject>
    suspend fun importProjects(projects: List<FullProject>): Int
}
