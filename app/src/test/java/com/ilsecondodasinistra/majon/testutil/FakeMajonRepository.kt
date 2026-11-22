package com.ilsecondodasinistra.majon.testutil

import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.FullYarn
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectWithParts
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.StashImportCounts
import com.ilsecondodasinistra.majon.domain.model.StashSnapshot
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMajonRepository : MajonRepository {

    private val projects = MutableStateFlow<List<Project>>(emptyList())
    private val parts = MutableStateFlow<List<Part>>(emptyList())
    private val notes = MutableStateFlow<List<RowNote>>(emptyList())
    private var nextId = 1L
    var now = 1_000L

    private fun withParts(project: Project) = ProjectWithParts(
        project = project,
        parts = parts.value.filter { it.projectId == project.id },
    )

    override fun observeProjects(): Flow<List<ProjectWithParts>> =
        combineState { projects.value.map(::withParts) }

    override fun observeProject(id: Long): Flow<ProjectWithParts?> =
        combineState { projects.value.find { it.id == id }?.let(::withParts) }

    private fun <T> combineState(block: () -> T): Flow<T> =
        kotlinx.coroutines.flow.combine(projects, parts, notes) { _, _, _ -> block() }

    override suspend fun upsertProject(project: Project): Long {
        return if (project.id == 0L) {
            val id = nextId++
            projects.value += project.copy(id = id, createdAt = now, updatedAt = now)
            id
        } else {
            projects.value = projects.value.map {
                if (it.id == project.id) project.copy(updatedAt = now) else it
            }
            project.id
        }
    }

    override suspend fun deleteProject(id: Long) {
        projects.value = projects.value.filterNot { it.id == id }
        val orphanParts = parts.value.filter { it.projectId == id }.map { it.id }.toSet()
        parts.value = parts.value.filterNot { it.projectId == id }
        notes.value = notes.value.filterNot { it.partId in orphanParts }
    }

    override fun observePart(id: Long): Flow<Part?> =
        parts.map { list -> list.find { it.id == id } }

    override suspend fun upsertPart(part: Part): Long {
        return if (part.id == 0L) {
            val id = nextId++
            parts.value += part.copy(id = id, createdAt = now)
            id
        } else {
            parts.value = parts.value.map { if (it.id == part.id) part else it }
            part.id
        }
    }

    override suspend fun deletePart(id: Long) {
        parts.value = parts.value.filterNot { it.id == id }
        notes.value = notes.value.filterNot { it.partId == id }
    }

    override suspend fun setCompletedRows(partId: Long, completedRows: Int) {
        parts.value = parts.value.map {
            if (it.id == partId) it.copy(completedRows = completedRows) else it
        }
    }

    override suspend fun adjustCompletedRows(partId: Long, delta: Int) {
        parts.value = parts.value.map {
            if (it.id == partId) it.copy(completedRows = (it.completedRows + delta).coerceIn(0, it.totalSteps)) else it
        }
    }

    override fun observeNotes(partId: Long): Flow<List<RowNote>> =
        notes.map { list -> list.filter { it.partId == partId }.sortedBy { it.rowStart } }

    override suspend fun upsertNote(note: RowNote): Long {
        return if (note.id == 0L) {
            val id = nextId++
            notes.value += note.copy(id = id)
            id
        } else {
            notes.value = notes.value.map { if (it.id == note.id) note else it }
            note.id
        }
    }

    override suspend fun deleteNote(id: Long) {
        notes.value = notes.value.filterNot { it.id == id }
    }

    // --- stash ---

    private val yarns = MutableStateFlow<List<Yarn>>(emptyList())
    private val swatches = MutableStateFlow<List<Swatch>>(emptyList())
    private val needles = MutableStateFlow<List<Needle>>(emptyList())

    override fun observeYarns(): Flow<List<Yarn>> =
        yarns.map { list -> list.sortedWith(compareBy({ it.name.lowercase() }, { it.id })) }

    override fun observeYarn(id: Long): Flow<Yarn?> = yarns.map { list -> list.find { it.id == id } }

    override suspend fun upsertYarn(yarn: Yarn): Long =
        if (yarn.id == 0L) {
            val id = nextId++
            yarns.value += yarn.copy(id = id, createdAt = now, updatedAt = now)
            id
        } else {
            yarns.value = yarns.value.map { if (it.id == yarn.id) yarn.copy(updatedAt = now) else it }
            yarn.id
        }

    override suspend fun deleteYarn(id: Long) {
        yarns.value = yarns.value.filterNot { it.id == id }
        swatches.value = swatches.value.filterNot { it.yarnId == id }
    }

    override suspend fun adjustYarnStock(id: Long, deltaSkeins: Double) {
        yarns.value = yarns.value.map {
            if (it.id == id) it.copy(stockSkeins = (it.stockSkeins + deltaSkeins).coerceAtLeast(0.0), updatedAt = now) else it
        }
    }

    override fun observeSwatches(yarnId: Long): Flow<List<Swatch>> =
        swatches.map { list -> list.filter { it.yarnId == yarnId }.sortedBy { it.id } }

    override suspend fun upsertSwatch(swatch: Swatch): Long =
        if (swatch.id == 0L) {
            val id = nextId++
            swatches.value += swatch.copy(id = id)
            id
        } else {
            swatches.value = swatches.value.map { if (it.id == swatch.id) swatch else it }
            swatch.id
        }

    override suspend fun deleteSwatch(id: Long) {
        swatches.value = swatches.value.filterNot { it.id == id }
    }

    override fun observeNeedles(): Flow<List<Needle>> =
        needles.map { list -> list.sortedWith(compareBy({ it.sizeMm == null }, { it.sizeMm }, { it.type }, { it.id })) }

    override suspend fun upsertNeedle(needle: Needle): Long =
        if (needle.id == 0L) {
            val id = nextId++
            needles.value += needle.copy(id = id)
            id
        } else {
            needles.value = needles.value.map { if (it.id == needle.id) needle else it }
            needle.id
        }

    override suspend fun deleteNeedle(id: Long) {
        needles.value = needles.value.filterNot { it.id == id }
    }

    override suspend fun getStash(): StashSnapshot = StashSnapshot(
        yarns = yarns.value.map { y -> FullYarn(y, swatches.value.filter { it.yarnId == y.id }) },
        needles = needles.value,
    )

    override suspend fun importStash(stash: StashSnapshot): StashImportCounts {
        stash.yarns.forEach { full ->
            val id = upsertYarn(full.yarn.copy(id = 0L))
            full.swatches.forEach { upsertSwatch(it.copy(id = 0L, yarnId = id)) }
        }
        stash.needles.forEach { upsertNeedle(it.copy(id = 0L)) }
        return StashImportCounts(stash.yarns.size, stash.needles.size)
    }

    override suspend fun getFullProjects(): List<FullProject> =
        projects.value.map { project ->
            FullProject(
                project = project,
                parts = parts.value.filter { it.projectId == project.id }.map { part ->
                    FullPart(part, notes.value.filter { it.partId == part.id })
                },
            )
        }

    override suspend fun importProjects(projects: List<FullProject>): Int {
        projects.forEach { full ->
            val projectId = upsertProject(full.project.copy(id = 0L))
            full.parts.forEach { fp ->
                val partId = upsertPart(fp.part.copy(id = 0L, projectId = projectId))
                fp.notes.forEach { upsertNote(it.copy(id = 0L, partId = partId)) }
            }
        }
        return projects.size
    }
}
