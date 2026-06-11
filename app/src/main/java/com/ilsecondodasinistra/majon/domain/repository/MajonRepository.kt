package com.ilsecondodasinistra.majon.domain.repository

import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectWithParts
import com.ilsecondodasinistra.majon.domain.model.RowNote
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

    fun observeNotes(partId: Long): Flow<List<RowNote>>
    suspend fun upsertNote(note: RowNote): Long
    suspend fun deleteNote(id: Long)

    suspend fun getFullProjects(): List<FullProject>
    suspend fun importProjects(projects: List<FullProject>): Int
}
