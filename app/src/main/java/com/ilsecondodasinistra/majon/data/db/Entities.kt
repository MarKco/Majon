package com.ilsecondodasinistra.majon.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.ilsecondodasinistra.majon.domain.model.FinishingTask
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
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
    @ColumnInfo(defaultValue = "0")
    val hasPrepRow: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val hasFinalRow: Boolean = false,
    val prepNote: String? = null,
    val finalNote: String? = null,
    val stitchMeasure: String? = null,
    val stitchCm: Double? = null,
    val rowMeasure: String? = null,
    val rowCm: Double? = null,
    val swatchId: Long? = null,
    @ColumnInfo(defaultValue = "1")
    val copies: Int = 1,
    @ColumnInfo(defaultValue = "1")
    val currentCopy: Int = 1,
    val castOnStitches: Int? = null,
    val endStitchCm: Double? = null,
)

@Entity(
    tableName = "finishing_tasks",
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
data class FinishingTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long,
    val title: String,
    @ColumnInfo(defaultValue = "0")
    val done: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val position: Int = 0,
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
    @ColumnInfo(defaultValue = "1")
    val step: Int = 1,
    /** The exact rows of a note on specific rows, comma-separated; null for the other kinds. */
    val rowList: String? = null,
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
    hasPrepRow = hasPrepRow,
    hasFinalRow = hasFinalRow,
    prepNote = prepNote,
    finalNote = finalNote,
    stitchMeasure = enumOrNull<MeasureKey>(stitchMeasure),
    stitchCm = stitchCm,
    rowMeasure = enumOrNull<MeasureKey>(rowMeasure),
    rowCm = rowCm,
    swatchId = swatchId,
    copies = copies,
    currentCopy = currentCopy,
    castOnStitches = castOnStitches,
    endStitchCm = endStitchCm,
)

fun Part.toEntity() = PartEntity(
    id = id,
    projectId = projectId,
    name = name,
    totalRows = totalRows,
    completedRows = completedRows,
    createdAt = createdAt,
    hasPrepRow = hasPrepRow,
    hasFinalRow = hasFinalRow,
    prepNote = prepNote,
    finalNote = finalNote,
    stitchMeasure = stitchMeasure?.name,
    stitchCm = stitchCm,
    rowMeasure = rowMeasure?.name,
    rowCm = rowCm,
    swatchId = swatchId,
    copies = copies,
    currentCopy = currentCopy,
    castOnStitches = castOnStitches,
    endStitchCm = endStitchCm,
)

fun NoteEntity.toDomain() = RowNote(
    id = id,
    partId = partId,
    rowStart = rowStart,
    rowEnd = rowEnd,
    frequency = runCatching { NoteFrequency.valueOf(frequency) }.getOrDefault(NoteFrequency.EVERY_ROW),
    text = text,
    step = step.coerceAtLeast(1),
    rows = rowList?.split(',')?.mapNotNull { it.trim().toIntOrNull() }.orEmpty(),
)

fun RowNote.toEntity() = NoteEntity(
    id = id,
    partId = partId,
    rowStart = rowStart,
    rowEnd = rowEnd,
    frequency = frequency.name,
    text = text,
    step = step,
    rowList = rows.takeIf { it.isNotEmpty() }?.joinToString(","),
)

fun FinishingTaskEntity.toDomain() = FinishingTask(id = id, projectId = projectId, title = title, done = done, position = position)

fun FinishingTask.toEntity() = FinishingTaskEntity(id = id, projectId = projectId, title = title, done = done, position = position)
