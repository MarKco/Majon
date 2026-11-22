package com.ilsecondodasinistra.majon.data

import com.ilsecondodasinistra.majon.data.export.BackupCodec
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val codec = BackupCodec()

    private fun sample(): List<FullProject> = listOf(
        FullProject(
            project = Project(
                id = 7L,
                name = "Maglione righe",
                icon = ProjectIcon.SWEATER,
                color = ProjectColor.SAGE,
                yarnType = "Lana merino",
                needleSize = "4",
                createdAt = 111L,
                updatedAt = 222L,
            ),
            parts = listOf(
                FullPart(
                    part = Part(id = 3L, projectId = 7L, name = "Davanti", totalRows = 120, completedRows = 42),
                    notes = listOf(
                        RowNote(id = 9L, partId = 3L, rowStart = 20, rowEnd = 30, frequency = NoteFrequency.ODD_ROWS, text = "diminuisci di due"),
                    ),
                ),
            ),
        ),
    )

    @Test
    fun `export then import preserves all content`() {
        val json = codec.encode(sample(), exportedAt = 12345L)
        val decoded = codec.decode(json).getOrThrow()

        assertEquals(1, decoded.size)
        val project = decoded.first()
        assertEquals("Maglione righe", project.project.name)
        assertEquals(ProjectIcon.SWEATER, project.project.icon)
        assertEquals(ProjectColor.SAGE, project.project.color)
        assertEquals("Lana merino", project.project.yarnType)
        assertEquals("4", project.project.needleSize)

        val part = project.parts.first()
        assertEquals("Davanti", part.part.name)
        assertEquals(120, part.part.totalRows)
        assertEquals(42, part.part.completedRows)

        val note = part.notes.first()
        assertEquals(20, note.rowStart)
        assertEquals(30, note.rowEnd)
        assertEquals(NoteFrequency.ODD_ROWS, note.frequency)
        assertEquals("diminuisci di due", note.text)
    }

    @Test
    fun `decoded ids are zero so import inserts as new records`() {
        val json = codec.encode(sample(), exportedAt = 1L)
        val decoded = codec.decode(json).getOrThrow()
        assertEquals(0L, decoded.first().project.id)
        assertEquals(0L, decoded.first().parts.first().part.id)
        assertEquals(0L, decoded.first().parts.first().notes.first().id)
    }

    @Test
    fun `export contains version and timestamp`() {
        val json = codec.encode(sample(), exportedAt = 98765L)
        assertTrue(json.contains("\"version\""))
        assertTrue(json.contains("98765"))
    }

    @Test
    fun `malformed json returns failure`() {
        assertTrue(codec.decode("not json at all").isFailure)
    }

    @Test
    fun `json without majon marker returns failure`() {
        assertTrue(codec.decode("""{"foo": 1}""").isFailure)
    }

    @Test
    fun `unknown icon or frequency falls back to defaults`() {
        val json = """
            {
              "app": "majon",
              "version": 1,
              "exportedAt": 1,
              "projects": [
                {
                  "name": "X",
                  "icon": "DRAGON",
                  "color": "NEON",
                  "parts": [
                    {
                      "name": "P",
                      "totalRows": 10,
                      "completedRows": 0,
                      "notes": [
                        {"rowStart": 1, "rowEnd": 2, "frequency": "WEIRD", "text": "t"}
                      ]
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val decoded = codec.decode(json).getOrThrow()
        assertEquals(ProjectIcon.YARN, decoded.first().project.icon)
        assertEquals(ProjectColor.TERRACOTTA, decoded.first().project.color)
        assertEquals(NoteFrequency.EVERY_ROW, decoded.first().parts.first().notes.first().frequency)
    }

    @Test
    fun `preparation and final rows round-trip`() {
        val source = listOf(
            FullProject(
                project = Project(name = "Sciarpa"),
                parts = listOf(
                    FullPart(
                        part = Part(
                            projectId = 0L, name = "Unica", totalRows = 50, completedRows = 52,
                            hasPrepRow = true, hasFinalRow = true, prepNote = "Avvia", finalNote = "Chiudi",
                        ),
                        notes = emptyList(),
                    ),
                ),
            ),
        )
        val part = codec.decode(codec.encode(source, 1L)).getOrThrow().single().parts.single().part
        assertTrue(part.hasPrepRow)
        assertTrue(part.hasFinalRow)
        assertEquals("Avvia", part.prepNote)
        assertEquals("Chiudi", part.finalNote)
        assertEquals(52, part.completedRows)
    }

    @Test
    fun `old backup without the extra rows decodes with them off`() {
        val json = """{"app":"majon","version":1,"exportedAt":1,"projects":[{"name":"X","parts":[{"name":"P","totalRows":10,"completedRows":4}]}]}"""
        val part = codec.decode(json).getOrThrow().single().parts.single().part
        assertEquals(false, part.hasPrepRow)
        assertEquals(false, part.hasFinalRow)
        assertEquals(null, part.prepNote)
        assertEquals(4, part.completedRows)
    }

    private fun stash() = StashSnapshot(
        yarns = listOf(
            FullYarn(
                yarn = Yarn(
                    id = 5L, name = "Bamboo", brand = "WAK", colorName = "Mustard", colorArgb = 0xFFE0A800.toInt(),
                    dyeLot = "L42", weight = YarnWeight.DK,
                    materials = listOf(MaterialShare(YarnMaterial.BAMBOO, percent = 100)),
                    skeinWeightG = 100, skeinLengthM = 350, stockSkeins = 2.5, pricePerSkein = 6.9,
                    shop = "Negozio", care = "30°", notes = "morbida", photoPath = "/data/foto.jpg",
                ),
                swatches = listOf(
                    Swatch(id = 8L, yarnId = 5L, stitches = 26, rows = 34, needleSizeMm = 3.0,
                        pattern = StitchPattern.GARTER, weightG = 4.2, notes = "lavato"),
                ),
            ),
        ),
        needles = listOf(
            Needle(id = 3L, type = NeedleType.CIRCULAR, material = NeedleMaterial.BAMBOO, sizeMm = 4.5,
                lengthCm = 80, quantity = 2, brand = "KnitPro", notes = "n"),
        ),
    )

    @Test
    fun `stash round-trips with yarns, blend, swatches and needles`() {
        val json = codec.encode(emptyList(), exportedAt = 1L, stash = stash())
        val data = codec.decodeAll(json).getOrThrow()
        val yarn = data.stash.yarns.single()
        assertEquals("Bamboo", yarn.yarn.name)
        assertEquals("WAK", yarn.yarn.brand)
        assertEquals(YarnWeight.DK, yarn.yarn.weight)
        assertEquals(listOf(MaterialShare(YarnMaterial.BAMBOO, percent = 100)), yarn.yarn.materials)
        assertEquals(2.5, yarn.yarn.stockSkeins, 0.0)
        assertEquals(6.9, yarn.yarn.pricePerSkein!!, 0.0)
        val swatch = yarn.swatches.single()
        assertEquals(26, swatch.stitches)
        assertEquals(StitchPattern.GARTER, swatch.pattern)
        assertEquals(4.2, swatch.weightG!!, 0.0)
        val needle = data.stash.needles.single()
        assertEquals(NeedleType.CIRCULAR, needle.type)
        assertEquals(NeedleMaterial.BAMBOO, needle.material)
        assertEquals(2, needle.quantity)
    }

    @Test
    fun `photos and ids are not exported`() {
        val json = codec.encode(emptyList(), exportedAt = 1L, stash = stash())
        assertTrue(!json.contains("foto.jpg"))
        val yarn = codec.decodeAll(json).getOrThrow().stash.yarns.single()
        assertEquals(0L, yarn.yarn.id)
        assertEquals(null, yarn.yarn.photoPath)
        assertEquals(0L, yarn.swatches.single().id)
    }

    @Test
    fun `export is version 2`() {
        assertTrue(codec.encode(emptyList(), exportedAt = 1L).contains("\"version\": 2"))
    }

    @Test
    fun `a version 1 file has an empty stash`() {
        val json = """{"app":"majon","version":1,"exportedAt":1,"projects":[{"name":"X"}]}"""
        val data = codec.decodeAll(json).getOrThrow()
        assertEquals(1, data.projects.size)
        assertTrue(data.stash.yarns.isEmpty())
        assertTrue(data.stash.needles.isEmpty())
    }

    @Test
    fun `decode still returns only the projects`() {
        val json = codec.encode(sample(), exportedAt = 1L, stash = stash())
        assertEquals(1, codec.decode(json).getOrThrow().size)
    }

    @Test
    fun `stash values are sanitized and unknown enums fall back`() {
        val json = """{"app":"majon","version":2,"exportedAt":1,"projects":[],
          "yarns":[{"name":"  Lana ","weight":"WEIRD","stockSkeins":-3,
            "materials":[{"material":"UNOBTAINIUM","percent":250},{"material":"WOOL","percent":-5}],
            "swatches":[{"stitches":0,"rows":-4,"pattern":"MYSTERY","weightG":-1}]},
            {"name":"   "}],
          "needles":[{"type":"LASER","material":"PLASMA","quantity":0,"sizeMm":-2}]}"""
        val data = codec.decodeAll(json).getOrThrow()
        val yarn = data.stash.yarns.single() // the blank one is dropped
        assertEquals("Lana", yarn.yarn.name)
        assertEquals(null, yarn.yarn.weight)
        assertEquals(0.0, yarn.yarn.stockSkeins, 0.0)
        assertEquals(YarnMaterial.OTHER, yarn.yarn.materials[0].material)
        assertEquals(100, yarn.yarn.materials[0].percent)
        assertEquals(0, yarn.yarn.materials[1].percent)
        val swatch = yarn.swatches.single()
        assertEquals(1, swatch.stitches)
        assertEquals(1, swatch.rows)
        assertEquals(StitchPattern.OTHER, swatch.pattern)
        assertEquals(null, swatch.weightG)
        val needle = data.stash.needles.single()
        assertEquals(NeedleType.CIRCULAR, needle.type)
        assertEquals(null, needle.material)
        assertEquals(1, needle.quantity)
        assertEquals(null, needle.sizeMm)
    }

    private fun projectWithLinks(): FullProject = FullProject(
        project = Project(name = "Sciarpa"),
        parts = listOf(
            FullPart(
                part = Part(
                    projectId = 1L, name = "Corpo", totalRows = 510,
                    stitchMeasure = MeasureKey.WIDTH, stitchCm = 20.0,
                    rowMeasure = MeasureKey.LENGTH, rowCm = 150.0, swatchId = 8L,
                ),
                notes = emptyList(),
            ),
        ),
        // real ids as they come from the database: yarn 5, swatch 8, needle 3
        links = ProjectLinks(
            yarns = listOf(ProjectYarnLink(yarnId = 5L, isPrimary = true, swatchId = 8L, plannedSkeins = 3.5)),
            needleIds = listOf(3L),
        ),
    )

    @Test
    fun `measures and links round-trip as positions in the stash`() {
        val json = codec.encode(listOf(projectWithLinks()), exportedAt = 1L, stash = stash())
        val project = codec.decodeAll(json).getOrThrow().projects.single()

        val part = project.parts.single().part
        assertEquals(MeasureKey.WIDTH, part.stitchMeasure)
        assertEquals(20.0, part.stitchCm!!, 0.0)
        assertEquals(MeasureKey.LENGTH, part.rowMeasure)
        assertEquals(150.0, part.rowCm!!, 0.0)
        // swatch 8 is the first (and only) swatch of the stash: position 0
        assertEquals(0L, part.swatchId)

        val link = project.links.yarns.single()
        assertEquals(0L, link.yarnId)          // yarn 5 is the first yarn of the stash
        assertTrue(link.isPrimary)
        assertEquals(0L, link.swatchId)
        assertEquals(3.5, link.plannedSkeins!!, 0.0)
        assertEquals(listOf(0L), project.links.needleIds)   // needle 3 is the first needle
    }

    @Test
    fun `links to things that are not in the stash are left out`() {
        val orphan = projectWithLinks().copy(
            links = ProjectLinks(yarns = listOf(ProjectYarnLink(yarnId = 99L)), needleIds = listOf(77L)),
        )
        val json = codec.encode(listOf(orphan), exportedAt = 1L, stash = stash())
        val project = codec.decodeAll(json).getOrThrow().projects.single()
        assertTrue(project.links.yarns.isEmpty())
        assertTrue(project.links.needleIds.isEmpty())
    }

    @Test
    fun `a swatch that is not exported is dropped from the part`() {
        val json = codec.encode(listOf(projectWithLinks()), exportedAt = 1L, stash = StashSnapshot(emptyList(), emptyList()))
        val project = codec.decodeAll(json).getOrThrow().projects.single()
        assertEquals(null, project.parts.single().part.swatchId)
        assertEquals(20.0, project.parts.single().part.stitchCm!!, 0.0)
    }

    @Test
    fun `an old file without measures or links still decodes`() {
        val json = """{"app":"majon","version":2,"exportedAt":1,"projects":[{"name":"X","parts":[{"name":"P","totalRows":10}]}]}"""
        val project = codec.decodeAll(json).getOrThrow().projects.single()
        assertEquals(null, project.parts.single().part.stitchMeasure)
        assertEquals(ProjectLinks(), project.links)
    }

    @Test
    fun `bad measures are sanitized`() {
        val json = """{"app":"majon","version":2,"exportedAt":1,"projects":[{"name":"X",
          "parts":[{"name":"P","totalRows":10,"stitchMeasure":"DEPTH","stitchCm":-4,"rowMeasure":"HEIGHT","rowCm":0}],
          "yarns":[{"yarn":-1},{"yarn":2,"swatch":-3,"plannedSkeins":-1}],"needles":[-5,1]}]}"""
        val project = codec.decodeAll(json).getOrThrow().projects.single()
        val part = project.parts.single().part
        assertEquals(null, part.stitchMeasure)
        assertEquals(null, part.stitchCm)
        assertEquals(MeasureKey.HEIGHT, part.rowMeasure)
        assertEquals(null, part.rowCm)
        val link = project.links.yarns.single()
        assertEquals(2L, link.yarnId)
        assertEquals(null, link.swatchId)
        assertEquals(null, link.plannedSkeins)
        assertEquals(listOf(1L), project.links.needleIds)
    }

    @Test
    fun `a part with rows still to define keeps zero rows`() {
        val json = """{"app":"majon","version":2,"exportedAt":1,"projects":[{"name":"X","parts":[{"name":"P","totalRows":0}]}]}"""
        assertEquals(0, codec.decode(json).getOrThrow().single().parts.single().part.totalRows)
    }

    @Test
    fun `empty project list round-trips`() {
        val json = codec.encode(emptyList(), exportedAt = 1L)
        assertTrue(codec.decode(json).getOrThrow().isEmpty())
    }

    @Test
    fun `decode sanitizes invalid numbers and drops blank notes`() {
        val json = """
            {"app":"majon","exportedAt":1,"projects":[{"name":" X ","parts":[
              {"name":"P","totalRows":-3,"completedRows":9,"notes":[
                {"rowStart":0,"rowEnd":-3,"text":"ok"},
                {"rowStart":1,"rowEnd":2,"text":"  "}]}]}]}
        """.trimIndent()
        val project = codec.decode(json).getOrThrow().single()
        assertEquals("X", project.project.name)
        val part = project.parts.single()
        assertEquals(0, part.part.totalRows)
        assertEquals(0, part.part.completedRows)
        val note = part.notes.single()
        assertEquals(1, note.rowStart)
        assertEquals(1, note.rowEnd)
    }

    @Test
    fun `decode rejects project without name`() {
        val json = """{"app":"majon","exportedAt":1,"projects":[{"name":" "}]}"""
        assertTrue(codec.decode(json).isFailure)
    }
}
