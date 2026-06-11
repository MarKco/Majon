package com.ilsecondodasinistra.majon.data.repository

import com.ilsecondodasinistra.majon.data.db.NoteDao
import com.ilsecondodasinistra.majon.data.db.PartDao
import com.ilsecondodasinistra.majon.data.db.ProjectDao
import com.ilsecondodasinistra.majon.data.db.toDomain
import com.ilsecondodasinistra.majon.data.db.toEntity
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectWithParts
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MajonRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val partDao: PartDao,
    private val noteDao: NoteDao,
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

    override fun observeNotes(partId: Long): Flow<List<RowNote>> =
        noteDao.observeByPartId(partId).map { notes -> notes.map { it.toDomain() } }

    override suspend fun upsertNote(note: RowNote): Long =
        noteDao.upsert(note.toEntity())

    override suspend fun deleteNote(id: Long) = noteDao.deleteById(id)

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
        projects.forEach { full ->
            val projectId = upsertProject(full.project.copy(id = 0L))
            full.parts.forEach { fp ->
                val partId = upsertPart(fp.part.copy(id = 0L, projectId = projectId))
                fp.notes.forEach { note ->
                    upsertNote(note.copy(id = 0L, partId = partId))
                }
            }
        }
        return projects.size
    }
}
