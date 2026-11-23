package com.ilsecondodasinistra.majon.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.repository.MajonRepositoryImpl
import com.ilsecondodasinistra.majon.domain.model.FullPart
import com.ilsecondodasinistra.majon.domain.model.FullProject
import com.ilsecondodasinistra.majon.domain.model.FullYarn
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.ProjectLinks
import com.ilsecondodasinistra.majon.domain.model.ProjectYarnLink
import com.ilsecondodasinistra.majon.domain.model.StashSnapshot
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Importing a project reuses the yarns and needles the stash already has instead of making doubles. */
@RunWith(RobolectricTestRunner::class)
class ImportMatchingRepositoryTest {

    private lateinit var db: MajonDatabase
    private lateinit var repository: MajonRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MajonDatabase::class.java).allowMainThreadQueries().build()
        repository = MajonRepositoryImpl(
            db, db.projectDao(), db.partDao(), db.noteDao(),
            db.yarnDao(), db.swatchDao(), db.needleDao(), db.projectLinkDao(), clock = { 1L },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val swatch = Swatch(yarnId = 0L, stitches = 31, rows = 34, needleSizeMm = 3.0)

    private fun sharedProject(swatches: List<Swatch> = listOf(swatch), needleQuantity: Int = 0) =
        FullProject(
            project = Project(name = "Mint sweater"),
            parts = listOf(FullPart(Part(projectId = 0L, name = "Davanti", totalRows = 190, swatchId = 0L), emptyList())),
            links = ProjectLinks(yarns = listOf(ProjectYarnLink(yarnId = 0L, isPrimary = true, swatchId = 0L)), needleIds = listOf(0L)),
        ) to StashSnapshot(
            yarns = listOf(FullYarn(Yarn(name = "Bamboo", brand = "We Are Knitters", colorName = "Mustard"), swatches)),
            needles = listOf(Needle(type = NeedleType.CIRCULAR, sizeMm = 3.0, lengthCm = 80, quantity = needleQuantity)),
        )

    @Test
    fun `a yarn the stash has is reused and keeps its stock`() = runTest {
        val mine = repository.upsertYarn(Yarn(name = "bamboo", brand = "We are knitters", colorName = "mustard", stockSkeins = 3.5, shop = "Mia"))
        repository.upsertSwatch(swatch.copy(yarnId = mine))
        val (project, stash) = sharedProject()
        val counts = repository.importBackup(listOf(project), stash)

        assertEquals(0, counts.yarns)
        val yarns = repository.observeYarns().first()
        assertEquals(1, yarns.size)
        assertEquals(3.5, yarns.single().stockSkeins, 0.0)
        val imported = repository.observeProjects().first().single()
        assertEquals(mine, repository.observeProjectLinks(imported.project.id).first().yarns.single().yarnId)
    }

    @Test
    fun `the swatch of a reused yarn is the one it already has, so a part keeps pointing at a swatch`() = runTest {
        val mine = repository.upsertYarn(Yarn(name = "Bamboo", brand = "We Are Knitters", colorName = "Mustard"))
        val mySwatch = repository.upsertSwatch(swatch.copy(yarnId = mine, weightG = 4.0))
        val (project, stash) = sharedProject()
        repository.importBackup(listOf(project), stash)

        assertEquals(1, repository.observeSwatches(mine).first().size)
        val imported = repository.observeProjects().first().single()
        assertEquals(mySwatch, imported.parts.single().swatchId)
        assertEquals(mySwatch, repository.observeProjectLinks(imported.project.id).first().yarns.single().swatchId)
    }

    @Test
    fun `a swatch the reused yarn lacks is added to it`() = runTest {
        val mine = repository.upsertYarn(Yarn(name = "Bamboo", brand = "We Are Knitters", colorName = "Mustard"))
        repository.upsertSwatch(swatch.copy(yarnId = mine, stitches = 28))
        val (project, stash) = sharedProject()
        repository.importBackup(listOf(project), stash)

        assertEquals(setOf(28, 31), repository.observeSwatches(mine).first().map { it.stitches }.toSet())
        val imported = repository.observeProjects().first().single()
        val used = repository.observeSwatches(mine).first().single { it.id == imported.parts.single().swatchId }
        assertEquals(31, used.stitches)
    }

    @Test
    fun `a yarn the stash lacks is made, with the stock the file says`() = runTest {
        val (project, stash) = sharedProject()
        val counts = repository.importBackup(listOf(project), stash)
        assertEquals(1, counts.yarns)
        val yarn = repository.observeYarns().first().single()
        assertEquals("Bamboo", yarn.name)
        assertEquals(0.0, yarn.stockSkeins, 0.0)
    }

    @Test
    fun `a needle the stash has is reused, one it lacks is made with the quantity of the file`() = runTest {
        val mine = repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = 3.0, lengthCm = 80, quantity = 2))
        val (project, stash) = sharedProject()
        val counts = repository.importBackup(listOf(project), stash)
        assertEquals(0, counts.needles)
        assertEquals(listOf(2), repository.observeNeedles().first().map { it.quantity })
        val imported = repository.observeProjects().first().single()
        assertEquals(listOf(mine), repository.observeProjectLinks(imported.project.id).first().needleIds)
    }

    @Test
    fun `a new needle from a shared file is one the person does not own yet`() = runTest {
        val (project, stash) = sharedProject(needleQuantity = 0)
        val counts = repository.importBackup(listOf(project), stash)
        assertEquals(1, counts.needles)
        assertEquals(0, repository.observeNeedles().first().single().quantity)
    }

    @Test
    fun `importing the same file twice makes no doubles in the stash`() = runTest {
        val (project, stash) = sharedProject()
        repository.importBackup(listOf(project), stash)
        repository.importBackup(listOf(project), stash)
        assertEquals(1, repository.observeYarns().first().size)
        assertEquals(1, repository.observeNeedles().first().size)
        assertEquals(1, repository.observeSwatches(repository.observeYarns().first().single().id).first().size)
        assertEquals(2, repository.observeProjects().first().size)
    }
}
