package com.ilsecondodasinistra.majon.data.export

import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.RowNote
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class BackupFile(
    val app: String = "majon",
    val version: Int = 1,
    val exportedAt: Long,
    val projects: List<BackupProject>,
)

@Serializable
private data class BackupProject(
    val name: String,
    val icon: String = ProjectIcon.YARN.name,
    val color: String = ProjectColor.TERRACOTTA.name,
    val yarnType: String? = null,
    val needleSize: String? = null,
    val parts: List<BackupPart> = emptyList(),
)

@Serializable
private data class BackupPart(
    val name: String,
    val totalRows: Int,
    val completedRows: Int = 0,
    val notes: List<BackupNote> = emptyList(),
)

@Serializable
private data class BackupNote(
    val rowStart: Int,
    val rowEnd: Int,
    val frequency: String = NoteFrequency.EVERY_ROW.name,
    val text: String,
)

class BackupCodec @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(projects: List<FullProject>, exportedAt: Long): String {
        val file = BackupFile(
            exportedAt = exportedAt,
            projects = projects.map { full ->
                BackupProject(
                    name = full.project.name,
                    icon = full.project.icon.name,
                    color = full.project.color.name,
                    yarnType = full.project.yarnType,
                    needleSize = full.project.needleSize,
                    parts = full.parts.map { fp ->
                        BackupPart(
                            name = fp.part.name,
                            totalRows = fp.part.totalRows,
                            completedRows = fp.part.completedRows,
                            notes = fp.notes.map { n ->
                                BackupNote(
                                    rowStart = n.rowStart,
                                    rowEnd = n.rowEnd,
                                    frequency = n.frequency.name,
                                    text = n.text,
                                )
                            },
                        )
                    },
                )
            },
        )
        return json.encodeToString(file)
    }

    fun decode(content: String): Result<List<FullProject>> = runCatching {
        val file = json.decodeFromString<BackupFile>(content)
        require(file.app == "majon") { "Not a Majon backup file" }
        file.projects.map { bp ->
            FullProject(
                project = Project(
                    name = bp.name,
                    icon = runCatching { ProjectIcon.valueOf(bp.icon) }.getOrDefault(ProjectIcon.YARN),
                    color = runCatching { ProjectColor.valueOf(bp.color) }.getOrDefault(ProjectColor.TERRACOTTA),
                    yarnType = bp.yarnType,
                    needleSize = bp.needleSize,
                ),
                parts = bp.parts.map { part ->
                    FullPart(
                        part = Part(
                            projectId = 0L,
                            name = part.name,
                            totalRows = part.totalRows,
                            completedRows = part.completedRows,
                        ),
                        notes = part.notes.map { n ->
                            RowNote(
                                partId = 0L,
                                rowStart = n.rowStart,
                                rowEnd = n.rowEnd,
                                frequency = runCatching { NoteFrequency.valueOf(n.frequency) }
                                    .getOrDefault(NoteFrequency.EVERY_ROW),
                                text = n.text,
                            )
                        },
                    )
                },
            )
        }
    }
}
