package com.ilsecondodasinistra.majon.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.RowNote

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val icon: String,
    val color: String,
    val yarnType: String?,
    val needleSize: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "parts",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class PartEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long,
    val name: String,
    val totalRows: Int,
    val completedRows: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = PartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("partId")],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val partId: Long,
    val rowStart: Int,
    val rowEnd: Int,
    val frequency: String,
    val text: String,
)

data class ProjectWithPartsEntity(
    @Embedded val project: ProjectEntity,
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val parts: List<PartEntity>,
)

// --- mappers ---

fun ProjectEntity.toDomain() = Project(
    id = id,
    name = name,
    icon = runCatching { ProjectIcon.valueOf(icon) }.getOrDefault(ProjectIcon.YARN),
    color = runCatching { ProjectColor.valueOf(color) }.getOrDefault(ProjectColor.TERRACOTTA),
    yarnType = yarnType,
    needleSize = needleSize,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Project.toEntity() = ProjectEntity(
    id = id,
    name = name,
    icon = icon.name,
    color = color.name,
    yarnType = yarnType,
    needleSize = needleSize,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun PartEntity.toDomain() = Part(
    id = id,
    projectId = projectId,
    name = name,
    totalRows = totalRows,
    completedRows = completedRows,
    createdAt = createdAt,
)

fun Part.toEntity() = PartEntity(
    id = id,
    projectId = projectId,
    name = name,
    totalRows = totalRows,
    completedRows = completedRows,
    createdAt = createdAt,
)

fun NoteEntity.toDomain() = RowNote(
    id = id,
    partId = partId,
    rowStart = rowStart,
    rowEnd = rowEnd,
    frequency = runCatching { NoteFrequency.valueOf(frequency) }.getOrDefault(NoteFrequency.EVERY_ROW),
    text = text,
)

fun RowNote.toEntity() = NoteEntity(
    id = id,
    partId = partId,
    rowStart = rowStart,
    rowEnd = rowEnd,
    frequency = frequency.name,
    text = text,
)
