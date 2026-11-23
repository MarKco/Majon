package com.ilsecondodasinistra.majon.data

import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.domain.model.FinishingTask
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.FullYarn
import com.ilsecondodasinistra.majon.domain.model.MaterialShare
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.RowNote
import com.ilsecondodasinistra.majon.domain.model.StashSnapshot
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnMaterial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sharing one project as a `.majon` file: what goes in, and what stays private. */
class BackupCodecShareTest {

    private val codec = BackupCodec()

    private val used = FullYarn(
        Yarn(
            id = 11L, name = "Bamboo", brand = "We Are Knitters", colorName = "Mustard", colorArgb = 0xFFE0A800.toInt(),
            weight = null, materials = listOf(MaterialShare(YarnMaterial.BAMBOO, null, 100)),
            skeinWeightG = 100, skeinLengthM = 130, care = "A mano",
            stockSkeins = 4.5, pricePerSkein = 9.9, shop = "La mia merceria", dyeLot = "L42", notes = "regalo di Anna",
        ),
        swatches = listOf(Swatch(id = 21L, yarnId = 11L, stitches = 31, rows = 34, needleSizeMm = 3.0, weightG = 4.0)),
    )
    private val unused = FullYarn(Yarn(id = 12L, name = "Altra lana", stockSkeins = 2.0), swatches = emptyList())
    private val needle = Needle(id = 31L, type = NeedleType.CIRCULAR, sizeMm = 3.0, lengthCm = 80, quantity = 3, brand = "KnitPro", notes = "i miei")
    private val otherNeedle = Needle(id = 32L, type = NeedleType.DPN, sizeMm = 2.5)
    private val stash = StashSnapshot(yarns = listOf(unused, used), needles = listOf(otherNeedle, needle))

    private val project = FullProject(
        project = Project(id = 7L, name = "Mint sweater", icon = ProjectIcon.SWEATER),
        parts = listOf(
            FullPart(
                Part(
                    id = 3L, projectId = 7L, name = "Manica", totalRows = 190, completedRows = 50, copies = 2, currentCopy = 2,
                    castOnStitches = 64, stitchMeasure = MeasureKey.CIRCUMFERENCE, stitchCm = 21.0, endStitchCm = 47.0,
                    rowMeasure = MeasureKey.LENGTH, rowCm = 56.0, swatchId = 21L,
                ),
                listOf(RowNote(partId = 3L, rowStart = 12, rowEnd = 190, frequency = com.ilsecondodasinistra.majon.domain.model.NoteFrequency.EVERY_NTH, step = 6, text = "aumenta")),
            ),
        ),
        links = ProjectLinks(yarns = listOf(ProjectYarnLink(11L, isPrimary = true, swatchId = 21L, plannedSkeins = 5.0)), needleIds = listOf(31L)),
        finishing = listOf(
            FinishingTask(projectId = 7L, title = "Cucire le spalle", done = true, position = 0),
            FinishingTask(projectId = 7L, title = "Nascondere i fili", position = 1),
        ),
    )

    private fun share(includeProgress: Boolean) =
        codec.encodeForSharing(project, stash, includeProgress = includeProgress, exportedAt = 1L)

    private fun read(text: String) = codec.decodeAll(text).getOrThrow()

    @Test
    fun `only the yarns and needles the project uses are in the file`() {
        val data = read(share(includeProgress = true))
        assertEquals(listOf("Bamboo"), data.stash.yarns.map { it.yarn.name })
        assertEquals(listOf(NeedleType.CIRCULAR), data.stash.needles.map { it.type })
    }

    @Test
    fun `what is private to the owner stays out`() {
        val text = share(includeProgress = true)
        assertFalse(text.contains("La mia merceria"))
        assertFalse(text.contains("regalo di Anna"))
        assertFalse(text.contains("L42"))
        assertFalse(text.contains("i miei"))
        val yarn = read(text).stash.yarns.single().yarn
        assertEquals(0.0, yarn.stockSkeins, 0.0)
        assertNull(yarn.pricePerSkein)
        assertNull(yarn.shop)
        assertNull(yarn.dyeLot)
        assertNull(yarn.notes)
    }

    @Test
    fun `what describes the yarn and the needle stays in`() {
        val data = read(share(includeProgress = true))
        val yarn = data.stash.yarns.single()
        assertEquals("We Are Knitters", yarn.yarn.brand)
        assertEquals("Mustard", yarn.yarn.colorName)
        assertEquals(0xFFE0A800.toInt(), yarn.yarn.colorArgb)
        assertEquals(100, yarn.yarn.skeinWeightG)
        assertEquals("A mano", yarn.yarn.care)
        assertEquals(YarnMaterial.BAMBOO, yarn.yarn.materials.single().material)
        assertEquals(31, yarn.swatches.single().stitches)
        assertEquals(4.0, yarn.swatches.single().weightG!!, 0.0)
        val needle = data.stash.needles.single()
        assertEquals(3.0, needle.sizeMm!!, 0.0)
        assertEquals(80, needle.lengthCm)
        assertEquals("KnitPro", needle.brand)
    }

    @Test
    fun `a needle in a shared file is one the reader does not own yet`() {
        assertEquals(0, read(share(includeProgress = true)).stash.needles.single().quantity)
    }

    @Test
    fun `a needle in a backup keeps its quantity`() {
        val backup = codec.encode(emptyList(), exportedAt = 1L, stash = stash)
        val needles = read(backup).stash.needles
        assertEquals(listOf(1, 3), needles.map { it.quantity }.sorted())
    }

    @Test
    fun `the project, its parts, notes and links come along`() {
        val data = read(share(includeProgress = true))
        val shared = data.projects.single()
        assertEquals("Mint sweater", shared.project.name)
        val part = shared.parts.single().part
        assertEquals(2, part.copies)
        assertEquals(64, part.castOnStitches)
        assertEquals(47.0, part.endStitchCm!!, 0.0)
        assertEquals(190, part.totalRows)
        assertEquals(6, shared.parts.single().notes.single().step)
        assertEquals(5.0, shared.links.yarns.single().plannedSkeins!!, 0.0)
        assertEquals(1, shared.links.needleIds.size)
        assertEquals(listOf("Cucire le spalle", "Nascondere i fili"), shared.finishing.map { it.title })
    }

    @Test
    fun `with the progress, the work done and the ticks come along`() {
        val shared = read(share(includeProgress = true)).projects.single()
        assertEquals(50, shared.parts.single().part.completedRows)
        assertEquals(2, shared.parts.single().part.currentCopy)
        assertEquals(listOf(true, false), shared.finishing.map { it.done })
    }

    @Test
    fun `as a model, everything starts from zero`() {
        val shared = read(share(includeProgress = false)).projects.single()
        assertEquals(0, shared.parts.single().part.completedRows)
        assertEquals(1, shared.parts.single().part.currentCopy)
        assertEquals(2, shared.parts.single().part.copies)
        assertTrue(shared.finishing.none { it.done })
        assertEquals(2, shared.finishing.size)
    }

    @Test
    fun `the links and the swatch of a part point at the right things in the shared file`() {
        val data = read(share(includeProgress = false))
        val shared = data.projects.single()
        assertEquals(0L, shared.links.yarns.single().yarnId)       // position 0 of the yarns of the file
        assertEquals(0L, shared.links.yarns.single().swatchId)
        assertEquals(0L, shared.parts.single().part.swatchId)
        assertEquals(listOf(0L), shared.links.needleIds)
    }

    @Test
    fun `sharing says what it is`() {
        val text = share(includeProgress = false)
        assertTrue(text.contains("\"app\": \"majon\""))
        assertTrue(text.contains("\"version\": 3"))
    }
}
