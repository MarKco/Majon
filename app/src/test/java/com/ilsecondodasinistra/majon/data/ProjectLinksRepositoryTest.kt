package com.ilsecondodasinistra.majon.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.repository.MajonRepositoryImpl
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.FullYarn
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProjectLinksRepositoryTest {

    private lateinit var db: MajonDatabase
    private lateinit var repository: MajonRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MajonDatabase::class.java).allowMainThreadQueries().build()
        repository = MajonRepositoryImpl(
            db, db.projectDao(), db.partDao(), db.noteDao(),
            db.yarnDao(), db.swatchDao(), db.needleDao(), db.projectLinkDao(), clock = { 1_000L },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun project(name: String = "Sciarpa") = repository.upsertProject(Project(name = name, icon = ProjectIcon.SCARF))
    private suspend fun yarn(name: String = "Bamboo") = repository.upsertYarn(Yarn(name = name, skeinWeightG = 100))

    // --- measures on parts ---

    @Test
    fun `a part keeps its measures and swatch`() = runTest {
        val p = project()
        val y = yarn()
        val s = repository.upsertSwatch(Swatch(yarnId = y, stitches = 26, rows = 34))
        val id = repository.upsertPart(
            Part(
                projectId = p, name = "Corpo", totalRows = 510,
                stitchMeasure = MeasureKey.WIDTH, stitchCm = 20.0,
                rowMeasure = MeasureKey.LENGTH, rowCm = 150.0, swatchId = s,
            ),
        )
        val part = repository.observePart(id).first()!!
        assertEquals(MeasureKey.WIDTH, part.stitchMeasure)
        assertEquals(20.0, part.stitchCm!!, 0.0)
        assertEquals(MeasureKey.LENGTH, part.rowMeasure)
        assertEquals(150.0, part.rowCm!!, 0.0)
        assertEquals(s, part.swatchId)
    }

    @Test
    fun `a part without measures has none`() = runTest {
        val id = repository.upsertPart(Part(projectId = project(), name = "Corpo", totalRows = 10))
        val part = repository.observePart(id).first()!!
        assertNull(part.stitchMeasure)
        assertNull(part.rowCm)
        assertNull(part.swatchId)
    }

    // --- links ---

    @Test
    fun `links round-trip with the primary yarn and a planned amount`() = runTest {
        val p = project()
        val y1 = yarn("A")
        val y2 = yarn("B")
        val n = repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = 3.0))
        repository.setProjectLinks(
            p,
            ProjectLinks(
                yarns = listOf(ProjectYarnLink(y1), ProjectYarnLink(y2, isPrimary = true, plannedSkeins = 3.5)),
                needleIds = listOf(n),
            ),
        )
        val links = repository.observeProjectLinks(p).first()
        assertEquals(listOf(y1, y2), links.yarns.map { it.yarnId }.sorted())
        assertEquals(y2, links.yarns.single { it.isPrimary }.yarnId)
        assertEquals(3.5, links.yarns.single { it.yarnId == y2 }.plannedSkeins!!, 0.0)
        assertEquals(listOf(n), links.needleIds)
    }

    @Test
    fun `setting links again replaces the old ones`() = runTest {
        val p = project()
        val y1 = yarn("A")
        val y2 = yarn("B")
        repository.setProjectLinks(p, ProjectLinks(yarns = listOf(ProjectYarnLink(y1))))
        repository.setProjectLinks(p, ProjectLinks(yarns = listOf(ProjectYarnLink(y2))))
        assertEquals(listOf(y2), repository.observeProjectLinks(p).first().yarns.map { it.yarnId })
    }

    @Test
    fun `a project with no links has empty links`() = runTest {
        assertEquals(ProjectLinks(), repository.observeProjectLinks(project()).first())
    }

    @Test
    fun `links are normalized when saved`() = runTest {
        val p = project()
        val y = yarn()
        repository.setProjectLinks(p, ProjectLinks(yarns = listOf(ProjectYarnLink(y), ProjectYarnLink(y)), needleIds = emptyList()))
        val links = repository.observeProjectLinks(p).first()
        assertEquals(1, links.yarns.size)
        assertTrue(links.yarns.single().isPrimary)
    }

    @Test
    fun `deleting a project, a yarn or a needle removes only its links`() = runTest {
        val p1 = project("Uno")
        val p2 = project("Due")
        val y = yarn()
        val n = repository.upsertNeedle(Needle(type = NeedleType.DPN, sizeMm = 2.0))
        val links = ProjectLinks(yarns = listOf(ProjectYarnLink(y)), needleIds = listOf(n))
        repository.setProjectLinks(p1, links)
        repository.setProjectLinks(p2, links)

        repository.deleteProject(p1)
        assertEquals(ProjectLinks(), repository.observeProjectLinks(p1).first())
        assertEquals(1, repository.observeProjectLinks(p2).first().yarns.size)

        repository.deleteYarn(y)
        assertTrue(repository.observeProjectLinks(p2).first().yarns.isEmpty())
        assertEquals(listOf(n), repository.observeProjectLinks(p2).first().needleIds)

        repository.deleteNeedle(n)
        assertTrue(repository.observeProjectLinks(p2).first().needleIds.isEmpty())
    }

    // --- needle usage ---

    @Test
    fun `needle usage lists the unfinished projects using each needle`() = runTest {
        val running = project("In corso")
        val finished = project("Finita")
        val notStarted = project("Da iniziare")
        repository.upsertPart(Part(projectId = running, name = "P", totalRows = 10, completedRows = 3))
        repository.upsertPart(Part(projectId = finished, name = "P", totalRows = 10, completedRows = 10))
        repository.upsertPart(Part(projectId = notStarted, name = "P", totalRows = 10))
        val n = repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = 4.0))
        val free = repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = 5.0))
        listOf(running, finished, notStarted).forEach {
            repository.setProjectLinks(it, ProjectLinks(needleIds = listOf(n)))
        }
        val usage = repository.observeNeedleUsage().first()
        assertEquals(setOf("In corso", "Da iniziare"), usage[n]!!.toSet())
        assertNull(usage[free])
    }

    @Test
    fun `a project with no parts counts as unfinished`() = runTest {
        val p = project("Vuoto")
        val n = repository.upsertNeedle(Needle(type = NeedleType.STRAIGHT, sizeMm = 3.0))
        repository.setProjectLinks(p, ProjectLinks(needleIds = listOf(n)))
        assertEquals(listOf("Vuoto"), repository.observeNeedleUsage().first()[n])
    }

    // --- duplicate ---

    @Test
    fun `duplicating copies the structure, notes and links but not the progress`() = runTest {
        val p = repository.upsertProject(
            Project(name = "Sciarpa blu", icon = ProjectIcon.SCARF, yarnType = "Merino", needleSize = "4"),
        )
        val y = yarn()
        val s = repository.upsertSwatch(Swatch(yarnId = y, stitches = 26, rows = 34))
        val n = repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = 4.0))
        repository.setProjectLinks(p, ProjectLinks(yarns = listOf(ProjectYarnLink(y, swatchId = s, plannedSkeins = 3.0)), needleIds = listOf(n)))
        val partId = repository.upsertPart(
            Part(
                projectId = p, name = "Corpo", totalRows = 100, completedRows = 40,
                hasPrepRow = true, prepNote = "Avvia", hasFinalRow = true, finalNote = "Chiudi",
                stitchMeasure = MeasureKey.WIDTH, stitchCm = 20.0, rowMeasure = MeasureKey.LENGTH, rowCm = 150.0, swatchId = s,
            ),
        )
        repository.upsertNote(RowNote(partId = partId, rowStart = 2, rowEnd = 2, text = "aumenta"))

        val copyId = repository.duplicateProject(p, "Sciarpa blu (copia)")!!
        assertNotEquals(p, copyId)

        val copy = repository.observeProject(copyId).first()!!
        assertEquals("Sciarpa blu (copia)", copy.project.name)
        assertEquals(ProjectIcon.SCARF, copy.project.icon)
        assertEquals("Merino", copy.project.yarnType)
        val part = copy.parts.single()
        assertNotEquals(partId, part.id)
        assertEquals(0, part.completedRows)
        assertEquals(100, part.totalRows)
        assertTrue(part.hasPrepRow && part.hasFinalRow)
        assertEquals("Avvia", part.prepNote)
        assertEquals(20.0, part.stitchCm!!, 0.0)
        assertEquals(s, part.swatchId)
        assertEquals(listOf("aumenta"), repository.observeNotes(part.id).first().map { it.text })

        val links = repository.observeProjectLinks(copyId).first()
        assertEquals(y, links.yarns.single().yarnId)
        assertEquals(3.0, links.yarns.single().plannedSkeins!!, 0.0)
        assertEquals(listOf(n), links.needleIds)

        // The original is untouched
        assertEquals(40, repository.observePart(partId).first()!!.completedRows)
        assertEquals(1, repository.observeNotes(partId).first().size)
    }

    @Test
    fun `duplicating a missing project does nothing`() = runTest {
        assertNull(repository.duplicateProject(999L, "x"))
    }

    // --- backup ---

    @Test
    fun `full projects carry their links and parts carry their measures`() = runTest {
        val p = project()
        val y = yarn()
        repository.setProjectLinks(p, ProjectLinks(yarns = listOf(ProjectYarnLink(y))))
        repository.upsertPart(Part(projectId = p, name = "A", totalRows = 5, stitchCm = 10.0))
        val full = repository.getFullProjects().single()
        assertEquals(y, full.links.yarns.single().yarnId)
        assertEquals(10.0, full.parts.single().part.stitchCm!!, 0.0)
    }

    @Test
    fun `importing a backup maps link indices to the new stash ids`() = runTest {
        // Put something in the stash first so that new ids can't match the indices by luck
        repository.upsertYarn(Yarn(name = "Vecchia"))
        repository.upsertNeedle(Needle(type = NeedleType.DPN, sizeMm = 9.0))

        val stash = StashSnapshot(
            yarns = listOf(
                FullYarn(Yarn(name = "Prima"), swatches = emptyList()),
                FullYarn(Yarn(name = "Seconda"), swatches = listOf(Swatch(yarnId = 0, stitches = 22, rows = 30), Swatch(yarnId = 0, stitches = 24, rows = 32))),
            ),
            needles = listOf(Needle(type = NeedleType.CIRCULAR, sizeMm = 4.0)),
        )
        // indices: yarn 1 = "Seconda", its second swatch = global swatch ordinal 1, needle 0
        val project = FullProject(
            project = Project(name = "Importato"),
            parts = listOf(FullPart(Part(projectId = 0, name = "P", totalRows = 5, swatchId = 1L), emptyList())),
            links = ProjectLinks(
                yarns = listOf(ProjectYarnLink(yarnId = 1L, isPrimary = true, swatchId = 1L, plannedSkeins = 2.0)),
                needleIds = listOf(0L),
            ),
        )
        val counts = repository.importBackup(listOf(project), stash)
        assertEquals(1, counts.projects)
        assertEquals(2, counts.yarns)
        assertEquals(1, counts.needles)

        val imported = repository.observeProjects().first().single { it.project.name == "Importato" }
        val link = repository.observeProjectLinks(imported.project.id).first().yarns.single()
        val linkedYarn = repository.observeYarn(link.yarnId).first()!!
        assertEquals("Seconda", linkedYarn.name)
        val swatch = repository.observeSwatches(link.yarnId).first().single { it.id == link.swatchId }
        assertEquals(24, swatch.stitches)
        assertEquals(link.swatchId, imported.parts.single().swatchId)
        val needle = repository.observeNeedles().first().single { it.id == repository.observeProjectLinks(imported.project.id).first().needleIds.single() }
        assertEquals(4.0, needle.sizeMm!!, 0.0)
    }

    @Test
    fun `a link pointing nowhere in the backup is dropped`() = runTest {
        val project = FullProject(
            project = Project(name = "Orfano"),
            parts = listOf(FullPart(Part(projectId = 0, name = "P", totalRows = 5, swatchId = 7L), emptyList())),
            links = ProjectLinks(yarns = listOf(ProjectYarnLink(yarnId = 5L)), needleIds = listOf(3L)),
        )
        repository.importBackup(listOf(project), StashSnapshot(emptyList(), emptyList()))
        val imported = repository.observeProjects().first().single()
        assertEquals(ProjectLinks(), repository.observeProjectLinks(imported.project.id).first())
        assertNull(imported.parts.single().swatchId)
    }
}
