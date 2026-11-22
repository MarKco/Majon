package com.ilsecondodasinistra.majon.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.repository.MajonRepositoryImpl
import com.ilsecondodasinistra.majon.domain.model.MaterialShare
import com.ilsecondodasinistra.majon.domain.model.Needle
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.StashSnapshot
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.Swatch
import com.ilsecondodasinistra.majon.domain.model.Yarn
import com.ilsecondodasinistra.majon.domain.model.YarnMaterial
import com.ilsecondodasinistra.majon.domain.model.YarnWeight
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StashRepositoryTest {

    private lateinit var db: MajonDatabase
    private lateinit var repository: MajonRepositoryImpl
    private var now = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MajonDatabase::class.java).allowMainThreadQueries().build()
        repository = MajonRepositoryImpl(
            db = db,
            projectDao = db.projectDao(),
            partDao = db.partDao(),
            noteDao = db.noteDao(),
            yarnDao = db.yarnDao(),
            swatchDao = db.swatchDao(),
            needleDao = db.needleDao(),
            projectLinkDao = db.projectLinkDao(),
            clock = { now },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun bamboo() = Yarn(
        name = "Bamboo",
        brand = "We Are Knitters",
        colorName = "Mustard",
        colorArgb = 0xFFE0A800.toInt(),
        weight = YarnWeight.DK,
        materials = listOf(MaterialShare(YarnMaterial.BAMBOO, percent = 100)),
        skeinWeightG = 100,
        skeinLengthM = 350,
        stockSkeins = 2.5,
    )

    // --- yarns ---

    @Test
    fun `a yarn round-trips with its blend in order`() = runTest {
        val blend = listOf(
            MaterialShare(YarnMaterial.WOOL, percent = 70),
            MaterialShare(YarnMaterial.OTHER, customName = "Yak", percent = 20),
            MaterialShare(YarnMaterial.SILK),
        )
        val id = repository.upsertYarn(bamboo().copy(materials = blend))
        val yarn = repository.observeYarn(id).first()!!
        assertEquals("Bamboo", yarn.name)
        assertEquals(YarnWeight.DK, yarn.weight)
        assertEquals(2.5, yarn.stockSkeins, 0.0)
        assertEquals(blend, yarn.materials)
        assertEquals(1_000L, yarn.createdAt)
    }

    @Test
    fun `updating a yarn replaces its blend and keeps createdAt`() = runTest {
        val id = repository.upsertYarn(bamboo())
        now = 5_000L
        repository.upsertYarn(
            bamboo().copy(id = id, materials = listOf(MaterialShare(YarnMaterial.COTTON, percent = 100)), createdAt = 1_000L),
        )
        val yarn = repository.observeYarn(id).first()!!
        assertEquals(listOf(MaterialShare(YarnMaterial.COTTON, percent = 100)), yarn.materials)
        assertEquals(1_000L, yarn.createdAt)
        assertEquals(5_000L, yarn.updatedAt)
    }

    @Test
    fun `yarns are listed by name ignoring case`() = runTest {
        repository.upsertYarn(Yarn(name = "zebra"))
        repository.upsertYarn(Yarn(name = "Alpaca"))
        repository.upsertYarn(Yarn(name = "merino"))
        assertEquals(listOf("Alpaca", "merino", "zebra"), repository.observeYarns().first().map { it.name })
    }

    @Test
    fun `stock can be adjusted by fractions but never below zero`() = runTest {
        val id = repository.upsertYarn(bamboo())
        repository.adjustYarnStock(id, -0.25)
        assertEquals(2.25, repository.observeYarn(id).first()!!.stockSkeins, 0.0001)
        repository.adjustYarnStock(id, -10.0)
        assertEquals(0.0, repository.observeYarn(id).first()!!.stockSkeins, 0.0)
        repository.adjustYarnStock(id, 1.5)
        assertEquals(1.5, repository.observeYarn(id).first()!!.stockSkeins, 0.0001)
    }

    @Test
    fun `deleting a yarn removes its blend and swatches`() = runTest {
        val id = repository.upsertYarn(bamboo())
        repository.upsertSwatch(Swatch(yarnId = id, stitches = 26, rows = 34))
        repository.deleteYarn(id)
        assertNull(repository.observeYarn(id).first())
        assertTrue(repository.observeSwatches(id).first().isEmpty())
        assertEquals(0, db.query("SELECT * FROM yarn_materials", null).use { it.count })
    }

    // --- swatches ---

    @Test
    fun `swatches round-trip`() = runTest {
        val id = repository.upsertYarn(bamboo())
        repository.upsertSwatch(
            Swatch(
                yarnId = id, stitches = 26, rows = 34, needleSizeMm = 3.0,
                pattern = StitchPattern.GARTER, weightG = 4.2, notes = "lavato",
            ),
        )
        val swatch = repository.observeSwatches(id).first().single()
        assertEquals(26, swatch.stitches)
        assertEquals(34, swatch.rows)
        assertEquals(3.0, swatch.needleSizeMm!!, 0.0)
        assertEquals(StitchPattern.GARTER, swatch.pattern)
        assertEquals(4.2, swatch.weightG!!, 0.0)
    }

    @Test
    fun `a swatch can be edited and deleted`() = runTest {
        val id = repository.upsertYarn(bamboo())
        val swatchId = repository.upsertSwatch(Swatch(yarnId = id, stitches = 20, rows = 30))
        repository.upsertSwatch(Swatch(id = swatchId, yarnId = id, stitches = 22, rows = 30))
        assertEquals(22, repository.observeSwatches(id).first().single().stitches)
        repository.deleteSwatch(swatchId)
        assertTrue(repository.observeSwatches(id).first().isEmpty())
    }

    // --- needles ---

    @Test
    fun `needles round-trip and sort by size with unknown sizes last`() = runTest {
        repository.upsertNeedle(Needle(type = NeedleType.STRAIGHT))
        repository.upsertNeedle(
            Needle(type = NeedleType.CIRCULAR, sizeMm = 4.5, lengthCm = 80, material = NeedleMaterial.BAMBOO, brand = "KnitPro", quantity = 2),
        )
        repository.upsertNeedle(Needle(type = NeedleType.DPN, sizeMm = 2.5))
        val needles = repository.observeNeedles().first()
        assertEquals(listOf(2.5, 4.5, null), needles.map { it.sizeMm })
        val circular = needles[1]
        assertEquals(NeedleType.CIRCULAR, circular.type)
        assertEquals(80, circular.lengthCm)
        assertEquals(NeedleMaterial.BAMBOO, circular.material)
        assertEquals(2, circular.quantity)
    }

    @Test
    fun `a needle can be deleted`() = runTest {
        val id = repository.upsertNeedle(Needle(type = NeedleType.DPN, sizeMm = 2.0))
        repository.deleteNeedle(id)
        assertTrue(repository.observeNeedles().first().isEmpty())
    }

    // --- backup ---

    @Test
    fun `stash snapshot exports and imports as new records`() = runTest {
        val id = repository.upsertYarn(bamboo())
        repository.upsertSwatch(Swatch(yarnId = id, stitches = 26, rows = 34, needleSizeMm = 3.0))
        repository.upsertNeedle(Needle(type = NeedleType.CIRCULAR, sizeMm = 4.0))

        val snapshot = repository.getStash()
        assertEquals(1, snapshot.yarns.size)
        assertEquals(1, snapshot.yarns.single().swatches.size)
        assertEquals(1, snapshot.needles.size)

        val counts = repository.importStash(snapshot)
        assertEquals(1, counts.yarns)
        assertEquals(1, counts.needles)
        assertEquals(2, repository.observeYarns().first().size)
        assertEquals(2, repository.observeNeedles().first().size)
        // Each imported yarn brought its own swatch
        val imported = repository.observeYarns().first().maxByOrNull { it.id }!!
        assertEquals(1, repository.observeSwatches(imported.id).first().size)
    }

    @Test
    fun `importing an empty stash does nothing`() = runTest {
        val counts = repository.importStash(StashSnapshot(emptyList(), emptyList()))
        assertEquals(0, counts.yarns)
        assertEquals(0, counts.needles)
        assertTrue(repository.observeYarns().first().isEmpty())
    }

}
