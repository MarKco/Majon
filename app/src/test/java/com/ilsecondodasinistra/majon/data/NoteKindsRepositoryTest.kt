package com.ilsecondodasinistra.majon.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ilsecondodasinistra.majon.data.db.MajonDatabase
import com.ilsecondodasinistra.majon.data.repository.MajonRepositoryImpl
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.Project
import com.ilsecondodasinistra.majon.domain.model.RowNote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoteKindsRepositoryTest {

    private lateinit var db: MajonDatabase
    private lateinit var repository: MajonRepositoryImpl
    private var partId = 0L

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MajonDatabase::class.java).allowMainThreadQueries().build()
        repository = MajonRepositoryImpl(
            db, db.projectDao(), db.partDao(), db.noteDao(),
            db.yarnDao(), db.swatchDao(), db.needleDao(), db.projectLinkDao(), clock = { 1L },
        )
        partId = repository.upsertPart(Part(projectId = repository.upsertProject(Project(name = "P")), name = "A", totalRows = 190))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `a note on exact rows keeps its rows`() = runTest {
        repository.upsertNote(RowNote(partId = partId, rowStart = 10, rowEnd = 30, frequency = NoteFrequency.SPECIFIC, rows = listOf(10, 20, 30), text = "x"))
        val note = repository.observeNotes(partId).first().single()
        assertEquals(NoteFrequency.SPECIFIC, note.frequency)
        assertEquals(listOf(10, 20, 30), note.rows)
    }

    @Test
    fun `a note every Nth row keeps its step`() = runTest {
        repository.upsertNote(RowNote(partId = partId, rowStart = 12, rowEnd = 190, frequency = NoteFrequency.EVERY_NTH, step = 6, text = "x"))
        val note = repository.observeNotes(partId).first().single()
        assertEquals(NoteFrequency.EVERY_NTH, note.frequency)
        assertEquals(6, note.step)
    }

    @Test
    fun `a plain note has a step of one and no rows`() = runTest {
        repository.upsertNote(RowNote(partId = partId, rowStart = 3, rowEnd = 3, text = "x"))
        val note = repository.observeNotes(partId).first().single()
        assertEquals(1, note.step)
        assertEquals(emptyList<Int>(), note.rows)
    }
}
