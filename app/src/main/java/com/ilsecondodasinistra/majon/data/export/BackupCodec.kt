package com.ilsecondodasinistra.majon.data.export

import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.FullYarn
import com.ilsecondodasinistra.majon.domain.model.MaterialShare
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.StashSnapshot
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnMaterial
import com.ilsecondodasinistra.majon.domain.model.YarnWeight
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
    val version: Int = 2,
    val exportedAt: Long,
    val projects: List<BackupProject> = emptyList(),
    // Added in version 2; older files simply have none
    val yarns: List<BackupYarn> = emptyList(),
    val needles: List<BackupNeedle> = emptyList(),
)

@Serializable
private data class BackupProject(
    val name: String,
    val icon: String = ProjectIcon.YARN.name,
    val color: String = ProjectColor.TERRACOTTA.name,
    val yarnType: String? = null,
    val needleSize: String? = null,
    val parts: List<BackupPart> = emptyList(),
    // What the project is made with: positions in this file's `yarns` / `needles` lists
    val yarns: List<BackupYarnLink> = emptyList(),
    val needles: List<Int> = emptyList(),
)

@Serializable
private data class BackupYarnLink(
    val yarn: Int,
    val primary: Boolean = false,
    /** Position of the swatch in the flattened list of every swatch of every yarn, in file order. */
    val swatch: Int? = null,
    val plannedSkeins: Double? = null,
)

@Serializable
private data class BackupPart(
    val name: String,
    val totalRows: Int,
    val completedRows: Int = 0,
    val hasPrepRow: Boolean = false,
    val hasFinalRow: Boolean = false,
    val prepNote: String? = null,
    val finalNote: String? = null,
    val stitchMeasure: String? = null,
    val stitchCm: Double? = null,
    val rowMeasure: String? = null,
    val rowCm: Double? = null,
    val swatch: Int? = null,
    val notes: List<BackupNote> = emptyList(),
)

@Serializable
private data class BackupNote(
    val rowStart: Int,
    val rowEnd: Int,
    val frequency: String = NoteFrequency.EVERY_ROW.name,
    val text: String,
)

@Serializable
private data class BackupYarn(
    val name: String,
    val brand: String? = null,
    val colorName: String? = null,
    val colorArgb: Int? = null,
    val dyeLot: String? = null,
    val weight: String? = null,
    val materials: List<BackupMaterial> = emptyList(),
    val skeinWeightG: Int? = null,
    val skeinLengthM: Int? = null,
    val stockSkeins: Double = 0.0,
    val pricePerSkein: Double? = null,
    val shop: String? = null,
    val care: String? = null,
    val notes: String? = null,
    val swatches: List<BackupSwatch> = emptyList(),
)

@Serializable
private data class BackupMaterial(
    val material: String = YarnMaterial.OTHER.name,
    val customName: String? = null,
    val percent: Int? = null,
)

@Serializable
private data class BackupSwatch(
    val stitches: Int = 1,
    val rows: Int = 1,
    val needleSizeMm: Double? = null,
    val pattern: String = StitchPattern.STOCKINETTE.name,
    val weightG: Double? = null,
    val notes: String? = null,
)

@Serializable
private data class BackupNeedle(
    val type: String = NeedleType.CIRCULAR.name,
    val material: String? = null,
    val sizeMm: Double? = null,
    val lengthCm: Int? = null,
    val quantity: Int = 1,
    val brand: String? = null,
    val notes: String? = null,
)

/** Everything a backup file holds. */
data class BackupData(val projects: List<FullProject>, val stash: StashSnapshot)

class BackupCodec @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(
        projects: List<FullProject>,
        exportedAt: Long,
        stash: StashSnapshot = StashSnapshot(emptyList(), emptyList()),
    ): String {
        // Links in the file are positions in the stash lists, not database ids
        val yarnPosition = stash.yarns.withIndex().associate { (i, y) -> y.yarn.id to i }
        val swatchPosition = stash.yarns.flatMap { it.swatches }.withIndex().associate { (i, sw) -> sw.id to i }
        val needlePosition = stash.needles.withIndex().associate { (i, n) -> n.id to i }
        val file = BackupFile(
            exportedAt = exportedAt,
            yarns = stash.yarns.map { it.toBackup() },
            needles = stash.needles.map { it.toBackup() },
            projects = projects.map { full ->
                BackupProject(
                    yarns = full.links.yarns.mapNotNull { link ->
                        yarnPosition[link.yarnId]?.let { y ->
                            BackupYarnLink(y, link.isPrimary, link.swatchId?.let(swatchPosition::get), link.plannedSkeins)
                        }
                    },
                    needles = full.links.needleIds.mapNotNull(needlePosition::get),
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
                            hasPrepRow = fp.part.hasPrepRow,
                            hasFinalRow = fp.part.hasFinalRow,
                            prepNote = fp.part.prepNote,
                            finalNote = fp.part.finalNote,
                            stitchMeasure = fp.part.stitchMeasure?.name,
                            stitchCm = fp.part.stitchCm,
                            rowMeasure = fp.part.rowMeasure?.name,
                            rowCm = fp.part.rowCm,
                            swatch = fp.part.swatchId?.let(swatchPosition::get),
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

    /** Only the projects of a backup file (kept for callers that don't care about the stash). */
    fun decode(content: String): Result<List<FullProject>> = decodeAll(content).map { it.projects }

    fun decodeAll(content: String): Result<BackupData> = runCatching {
        val file = json.decodeFromString<BackupFile>(content)
        require(file.app == "majon") { "Not a Majon backup file" }
        BackupData(
            projects = decodeProjects(file),
            stash = StashSnapshot(
                yarns = file.yarns.filter { it.name.isNotBlank() }.map { it.toDomain() },
                needles = file.needles.map { it.toDomain() },
            ),
        )
    }

    private fun decodeProjects(file: BackupFile): List<FullProject> {
        return file.projects.map { bp ->
            require(bp.name.isNotBlank()) { "Project without a name" }
            FullProject(
                project = Project(
                    name = bp.name.trim(),
                    icon = runCatching { ProjectIcon.valueOf(bp.icon) }.getOrDefault(ProjectIcon.YARN),
                    color = runCatching { ProjectColor.valueOf(bp.color) }.getOrDefault(ProjectColor.TERRACOTTA),
                    yarnType = bp.yarnType,
                    needleSize = bp.needleSize,
                ),
                parts = bp.parts.map { part ->
                    val totalRows = part.totalRows.coerceAtLeast(1)
                    val extraRows = (if (part.hasPrepRow) 1 else 0) + (if (part.hasFinalRow) 1 else 0)
                    FullPart(
                        part = Part(
                            projectId = 0L,
                            name = part.name.trim().ifBlank { "-" },
                            totalRows = totalRows,
                            completedRows = part.completedRows.coerceIn(0, totalRows + extraRows),
                            hasPrepRow = part.hasPrepRow,
                            hasFinalRow = part.hasFinalRow,
                            prepNote = part.prepNote?.takeIf { it.isNotBlank() },
                            finalNote = part.finalNote?.takeIf { it.isNotBlank() },
                            stitchMeasure = enumOrNull<MeasureKey>(part.stitchMeasure),
                            stitchCm = part.stitchCm?.takeIf { it > 0.0 },
                            rowMeasure = enumOrNull<MeasureKey>(part.rowMeasure),
                            rowCm = part.rowCm?.takeIf { it > 0.0 },
                            // A position in the stash, turned into an id when the backup is imported
                            swatchId = part.swatch?.takeIf { it >= 0 }?.toLong(),
                        ),
                        notes = part.notes.filter { it.text.isNotBlank() }.map { n ->
                            val rowStart = n.rowStart.coerceAtLeast(1)
                            RowNote(
                                partId = 0L,
                                rowStart = rowStart,
                                rowEnd = n.rowEnd.coerceAtLeast(rowStart),
                                frequency = runCatching { NoteFrequency.valueOf(n.frequency) }
                                    .getOrDefault(NoteFrequency.EVERY_ROW),
                                text = n.text,
                            )
                        },
                    )
                },
                // Positions in the stash of the same file; the repository turns them into ids on import
                links = ProjectLinks(
                    yarns = bp.yarns.filter { it.yarn >= 0 }.map {
                        ProjectYarnLink(
                            yarnId = it.yarn.toLong(),
                            isPrimary = it.primary,
                            swatchId = it.swatch?.takeIf { s -> s >= 0 }?.toLong(),
                            plannedSkeins = it.plannedSkeins?.takeIf { n -> n > 0.0 },
                        )
                    },
                    needleIds = bp.needles.filter { it >= 0 }.map { it.toLong() },
                ),
            )
        }
    }
}

private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
    name?.let { n -> enumValues<E>().firstOrNull { it.name == n } }

private fun FullYarn.toBackup() = BackupYarn(
    name = yarn.name,
    brand = yarn.brand,
    colorName = yarn.colorName,
    colorArgb = yarn.colorArgb,
    dyeLot = yarn.dyeLot,
    weight = yarn.weight?.name,
    materials = yarn.materials.map { BackupMaterial(it.material.name, it.customName, it.percent) },
    skeinWeightG = yarn.skeinWeightG,
    skeinLengthM = yarn.skeinLengthM,
    stockSkeins = yarn.stockSkeins,
    pricePerSkein = yarn.pricePerSkein,
    shop = yarn.shop,
    care = yarn.care,
    notes = yarn.notes,
    swatches = swatches.map {
        BackupSwatch(it.stitches, it.rows, it.needleSizeMm, it.pattern.name, it.weightG, it.notes)
    },
)

private fun BackupYarn.toDomain() = FullYarn(
    yarn = Yarn(
        name = name.trim(),
        brand = brand,
        colorName = colorName,
        colorArgb = colorArgb,
        dyeLot = dyeLot,
        weight = enumOrNull<YarnWeight>(weight),
        materials = materials.map {
            MaterialShare(
                material = enumOrNull<YarnMaterial>(it.material) ?: YarnMaterial.OTHER,
                customName = it.customName,
                percent = it.percent?.coerceIn(0, 100),
            )
        },
        skeinWeightG = skeinWeightG?.takeIf { it > 0 },
        skeinLengthM = skeinLengthM?.takeIf { it > 0 },
        stockSkeins = stockSkeins.coerceAtLeast(0.0),
        pricePerSkein = pricePerSkein?.takeIf { it >= 0 },
        shop = shop,
        care = care,
        notes = notes,
    ),
    swatches = swatches.map {
        Swatch(
            yarnId = 0L,
            stitches = it.stitches.coerceAtLeast(1),
            rows = it.rows.coerceAtLeast(1),
            needleSizeMm = it.needleSizeMm?.takeIf { mm -> mm > 0 },
            pattern = enumOrNull<StitchPattern>(it.pattern) ?: StitchPattern.OTHER,
            weightG = it.weightG?.takeIf { g -> g > 0 },
            notes = it.notes,
        )
    },
)

private fun Needle.toBackup() = BackupNeedle(
    type = type.name,
    material = material?.name,
    sizeMm = sizeMm,
    lengthCm = lengthCm,
    quantity = quantity,
    brand = brand,
    notes = notes,
)

private fun BackupNeedle.toDomain() = Needle(
    type = enumOrNull<NeedleType>(type) ?: NeedleType.CIRCULAR,
    material = enumOrNull<NeedleMaterial>(material),
    sizeMm = sizeMm?.takeIf { it > 0 },
    lengthCm = lengthCm?.takeIf { it > 0 },
    quantity = quantity.coerceAtLeast(1),
    brand = brand,
    notes = notes,
)
