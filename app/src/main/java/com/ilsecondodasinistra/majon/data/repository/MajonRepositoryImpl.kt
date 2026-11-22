package com.ilsecondodasinistra.majon.data.repository

import androidx.room.withTransaction
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.db.NeedleDao
import com.ilsecondodasinistra.majon.data.db.NoteDao
import com.ilsecondodasinistra.majon.data.db.PartDao
import com.ilsecondodasinistra.majon.data.db.ProjectDao
import com.ilsecondodasinistra.majon.data.db.SwatchDao
import com.ilsecondodasinistra.majon.data.db.YarnDao
import com.ilsecondodasinistra.majon.data.db.materialEntities
import com.ilsecondodasinistra.majon.data.db.toDomain
import com.ilsecondodasinistra.majon.data.db.toEntity
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MajonRepositoryImpl @Inject constructor(
    private val db: MajonDatabase,
    private val projectDao: ProjectDao,
    private val partDao: PartDao,
    private val noteDao: NoteDao,
    private val yarnDao: YarnDao,
    private val swatchDao: SwatchDao,
    private val needleDao: NeedleDao,
    private val clock: () -> Long,
) : MajonRepository {

    override fun observeProjects(): Flow<List<ProjectWithParts>> =
        projectDao.observeAllWithParts().map { list ->
            list.map { ProjectWithParts(it.project.toDomain(), it.parts.map { p -> p.toDomain() }) }
        }

    override fun observeProject(id: Long): Flow<ProjectWithParts?> =
        projectDao.observeWithParts(id).map { entity ->
            entity?.let { ProjectWithParts(it.project.toDomain(), it.parts.map { p -> p.toDomain() }) }
        }

    override suspend fun upsertProject(project: Project): Long {
        val now = clock()
        return if (project.id == 0L) {
            projectDao.insert(project.copy(createdAt = now, updatedAt = now).toEntity())
        } else {
            projectDao.update(project.copy(updatedAt = now).toEntity())
            project.id
        }
    }

    override suspend fun deleteProject(id: Long) = projectDao.deleteById(id)

    override fun observePart(id: Long): Flow<Part?> =
        partDao.observeById(id).map { it?.toDomain() }

    override suspend fun upsertPart(part: Part): Long =
        if (part.id == 0L) {
            partDao.insert(part.copy(createdAt = clock()).toEntity())
        } else {
            partDao.update(part.toEntity())
            part.id
        }

    override suspend fun deletePart(id: Long) = partDao.deleteById(id)

    override suspend fun setCompletedRows(partId: Long, completedRows: Int) =
        partDao.updateCompletedRows(partId, completedRows)

    override suspend fun adjustCompletedRows(partId: Long, delta: Int) =
        partDao.adjustCompletedRows(partId, delta)

    override fun observeNotes(partId: Long): Flow<List<RowNote>> =
        noteDao.observeByPartId(partId).map { notes -> notes.map { it.toDomain() } }

    override suspend fun upsertNote(note: RowNote): Long =
        noteDao.upsert(note.toEntity())

    override suspend fun deleteNote(id: Long) = noteDao.deleteById(id)

    // --- stash ---

    override fun observeYarns(): Flow<List<Yarn>> =
        yarnDao.observeAllWithMaterials().map { list -> list.map { it.toDomain() } }

    override fun observeYarn(id: Long): Flow<Yarn?> =
        yarnDao.observeWithMaterials(id).map { it?.toDomain() }

    override suspend fun upsertYarn(yarn: Yarn): Long = db.withTransaction {
        val now = clock()
        val id = if (yarn.id == 0L) {
            yarnDao.insert(yarn.copy(createdAt = now, updatedAt = now).toEntity())
        } else {
            yarnDao.update(yarn.copy(updatedAt = now).toEntity())
            yarn.id
        }
        yarnDao.deleteMaterials(id)
        yarnDao.insertMaterials(yarn.materialEntities(id))
        id
    }

    override suspend fun deleteYarn(id: Long) = yarnDao.deleteById(id)

    override suspend fun adjustYarnStock(id: Long, deltaSkeins: Double) =
        yarnDao.adjustStock(id, deltaSkeins, clock())

    override fun observeSwatches(yarnId: Long): Flow<List<Swatch>> =
        swatchDao.observeByYarn(yarnId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertSwatch(swatch: Swatch): Long = swatchDao.upsert(swatch.toEntity())

    override suspend fun deleteSwatch(id: Long) = swatchDao.deleteById(id)

    override fun observeNeedles(): Flow<List<Needle>> =
        needleDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertNeedle(needle: Needle): Long = needleDao.upsert(needle.toEntity())

    override suspend fun deleteNeedle(id: Long) = needleDao.deleteById(id)

    override suspend fun getStash(): StashSnapshot = StashSnapshot(
        yarns = yarnDao.getAllWithMaterials().map { entity ->
            FullYarn(
                yarn = entity.toDomain(),
                swatches = swatchDao.getByYarn(entity.yarn.id).map { it.toDomain() },
            )
        },
        needles = needleDao.getAll().map { it.toDomain() },
    )

    override suspend fun importStash(stash: StashSnapshot): StashImportCounts {
        db.withTransaction {
            stash.yarns.forEach { full ->
                val yarnId = upsertYarn(full.yarn.copy(id = 0L))
                full.swatches.forEach { upsertSwatch(it.copy(id = 0L, yarnId = yarnId)) }
            }
            stash.needles.forEach { upsertNeedle(it.copy(id = 0L)) }
        }
        return StashImportCounts(yarns = stash.yarns.size, needles = stash.needles.size)
    }

    override suspend fun getFullProjects(): List<FullProject> =
        projectDao.getAllWithParts().map { entity ->
            FullProject(
                project = entity.project.toDomain(),
                parts = entity.parts.map { partEntity ->
                    FullPart(
                        part = partEntity.toDomain(),
                        notes = noteDao.getByPartId(partEntity.id).map { it.toDomain() },
                    )
                },
            )
        }

    override suspend fun importProjects(projects: List<FullProject>): Int {
        db.withTransaction {
            projects.forEach { full ->
                val projectId = upsertProject(full.project.copy(id = 0L))
                full.parts.forEach { fp ->
                    val partId = upsertPart(fp.part.copy(id = 0L, projectId = projectId))
                    fp.notes.forEach { note ->
                        upsertNote(note.copy(id = 0L, partId = partId))
                    }
                }
            }
        }
        return projects.size
    }
}
