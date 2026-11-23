package com.ilsecondodasinistra.majon.data.repository

import androidx.room.withTransaction
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.db.NeedleDao
import com.ilsecondodasinistra.majon.data.db.NoteDao
import com.ilsecondodasinistra.majon.data.db.PartDao
import com.ilsecondodasinistra.majon.data.db.ProjectDao
import com.ilsecondodasinistra.majon.data.db.ProjectLinkDao
import com.ilsecondodasinistra.majon.data.db.ProjectNeedleEntity
import com.ilsecondodasinistra.majon.data.db.SwatchDao
import com.ilsecondodasinistra.majon.data.db.YarnDao
import com.ilsecondodasinistra.majon.data.db.materialEntities
import com.ilsecondodasinistra.majon.data.db.toDomain
import com.ilsecondodasinistra.majon.data.db.toEntity
import com.ilsecondodasinistra.majon.domain.model.FinishingTask
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.sameNeedleAs
import com.ilsecondodasinistra.majon.domain.model.sameSwatchAs
import com.ilsecondodasinistra.majon.domain.model.sameYarnAs
import com.ilsecondodasinistra.majon.domain.model.BackupImportCounts
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.FullYarn
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.normalized
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val projectLinkDao: ProjectLinkDao,
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

    // --- finishing ---

    override fun observeFinishingTasks(projectId: Long): Flow<List<FinishingTask>> =
        projectDao.observeFinishingTasks(projectId).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertFinishingTask(task: FinishingTask): Long =
        projectDao.upsertFinishingTask(task.toEntity())

    override suspend fun deleteFinishingTask(id: Long) = projectDao.deleteFinishingTask(id)

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

    // --- what a project is made with ---

    override fun observeProjectLinks(projectId: Long): Flow<ProjectLinks> =
        combine(
            projectLinkDao.observeYarnLinks(projectId),
            projectLinkDao.observeNeedleLinks(projectId),
        ) { yarns, needles ->
            ProjectLinks(yarns = yarns.map { it.toDomain() }, needleIds = needles.map { it.needleId })
        }

    override suspend fun setProjectLinks(projectId: Long, links: ProjectLinks) {
        val clean = links.normalized()
        db.withTransaction {
            projectLinkDao.deleteYarnLinks(projectId)
            projectLinkDao.deleteNeedleLinks(projectId)
            projectLinkDao.insertYarnLinks(clean.yarns.map { it.toEntity(projectId) })
            projectLinkDao.insertNeedleLinks(clean.needleIds.map { ProjectNeedleEntity(projectId, it) })
        }
    }

    override fun observePrimaryYarnNames(): Flow<Map<Long, String>> =
        projectLinkDao.observePrimaryYarns().map { rows ->
            rows.associate { it.projectId to listOfNotNull(it.name, it.colorName).joinToString(" ") }
        }

    override fun observeNeedleUsage(): Flow<Map<Long, List<String>>> =
        combine(projectDao.observeAllWithParts(), projectLinkDao.observeAllNeedleLinks()) { projects, links ->
            val unfinished = projects
                .filter { p -> p.parts.isEmpty() || p.parts.any { !it.toDomain().isComplete } }
                .associate { it.project.id to it.project.name }
            links.mapNotNull { link -> unfinished[link.projectId]?.let { link.needleId to it } }
                .groupBy({ it.first }, { it.second })
        }

    override suspend fun duplicateProject(id: Long, copyName: String): Long? = db.withTransaction {
        val source = projectDao.getAllWithParts().firstOrNull { it.project.id == id } ?: return@withTransaction null
        val newId = upsertProject(source.project.toDomain().copy(id = 0L, name = copyName))
        source.parts.sortedBy { it.id }.forEach { part ->
            val newPartId = upsertPart(part.toDomain().copy(id = 0L, projectId = newId, completedRows = 0, currentCopy = 1))
            noteDao.getByPartId(part.id).forEach { note ->
                upsertNote(note.toDomain().copy(id = 0L, partId = newPartId))
            }
        }
        projectDao.getFinishingTasks(id).forEach { task ->
            projectDao.upsertFinishingTask(task.copy(id = 0L, projectId = newId, done = false))
        }
        setProjectLinks(newId, currentLinks(id))
        newId
    }

    private suspend fun currentLinks(projectId: Long) = ProjectLinks(
        yarns = projectLinkDao.getYarnLinks(projectId).map { it.toDomain() },
        needleIds = projectLinkDao.getNeedleLinks(projectId).map { it.needleId },
    )

    override suspend fun importBackup(projects: List<FullProject>, stash: StashSnapshot): BackupImportCounts {
        var created = Triple(0, 0, 0)
        db.withTransaction {
            // Ids of the records the snapshot's yarns, swatches and needles stand for, in the order of the snapshot
            // (links in the file are positions in it). What the stash already has is reused, not made again.
            val have = getStash()
            val yarnIds = mutableListOf<Long>()
            val swatchIds = mutableListOf<Long>()
            var newYarns = 0
            var newNeedles = 0
            stash.yarns.forEach { full ->
                val known = have.yarns.firstOrNull { it.yarn.sameYarnAs(full.yarn) }
                val yarnId = known?.yarn?.id ?: upsertYarn(full.yarn.copy(id = 0L)).also { newYarns++ }
                yarnIds += yarnId
                val owned = known?.swatches.orEmpty().toMutableList()
                full.swatches.forEach { sw ->
                    val same = owned.firstOrNull { it.sameSwatchAs(sw) }
                    if (same != null) {
                        swatchIds += same.id
                    } else {
                        val id = upsertSwatch(sw.copy(id = 0L, yarnId = yarnId))
                        owned += sw.copy(id = id, yarnId = yarnId)
                        swatchIds += id
                    }
                }
            }
            val owned = have.needles.toMutableList()
            val needleIds = stash.needles.map { needle ->
                val same = owned.firstOrNull { it.sameNeedleAs(needle) }
                if (same != null) {
                    same.id
                } else {
                    newNeedles++
                    upsertNeedle(needle.copy(id = 0L)).also { owned += needle.copy(id = it) }
                }
            }
            created = Triple(0, newYarns, newNeedles)

            fun swatch(position: Long?) = position?.let { swatchIds.getOrNull(it.toInt()) }

            projects.forEach { full ->
                val projectId = upsertProject(full.project.copy(id = 0L))
                full.parts.forEach { fp ->
                    val partId = upsertPart(
                        fp.part.copy(id = 0L, projectId = projectId, swatchId = swatch(fp.part.swatchId)),
                    )
                    fp.notes.forEach { upsertNote(it.copy(id = 0L, partId = partId)) }
                }
                full.finishing.forEach { upsertFinishingTask(it.copy(id = 0L, projectId = projectId)) }
                setProjectLinks(
                    projectId,
                    ProjectLinks(
                        yarns = full.links.yarns.mapNotNull { link ->
                            yarnIds.getOrNull(link.yarnId.toInt())?.let { link.copy(yarnId = it, swatchId = swatch(link.swatchId)) }
                        },
                        needleIds = full.links.needleIds.mapNotNull { needleIds.getOrNull(it.toInt()) },
                    ),
                )
            }
        }
        return BackupImportCounts(projects.size, yarns = created.second, needles = created.third)
    }

    override suspend fun getFullProjects(): List<FullProject> =
        projectDao.getAllWithParts().map { entity ->
            FullProject(
                links = currentLinks(entity.project.id),
                project = entity.project.toDomain(),
                parts = entity.parts.map { partEntity ->
                    FullPart(
                        part = partEntity.toDomain(),
                        notes = noteDao.getByPartId(partEntity.id).map { it.toDomain() },
                    )
                },
                finishing = projectDao.getFinishingTasks(entity.project.id).map { it.toDomain() },
            )
        }

    override suspend fun importProjects(projects: List<FullProject>): Int {
        db.withTransaction {
            projects.forEach { full ->
                val projectId = upsertProject(full.project.copy(id = 0L))
                full.parts.forEach { fp ->
                    val partId = upsertPart(fp.part.copy(id = 0L, projectId = projectId, swatchId = null))
                    fp.notes.forEach { note ->
                        upsertNote(note.copy(id = 0L, partId = partId))
                    }
                }
                full.finishing.forEach { upsertFinishingTask(it.copy(id = 0L, projectId = projectId)) }
            }
        }
        return projects.size
    }
}
