package com.ilsecondodasinistra.majon.data

import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.data.export.DetectedImport
import com.ilsecondodasinistra.majon.data.export.NotesCodec
import com.ilsecondodasinistra.majon.domain.model.NoteFrequency
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The example files in docs/examples are the ones people (and AIs) copy from: they have to be accepted by the app. */
class FormatExamplesTest {

    private val codec = NotesCodec(BackupCodec())

    private fun example(name: String) = File("../docs/examples/$name").readText()

    @Test
    fun `the notes example is a good notes file`() {
        val detected = codec.detect(example("example.majon-notes")) as DetectedImport.Notes
        assertEquals(listOf("Dietro", "Manica"), detected.file.parts.map { it.name })
        assertEquals("Avvia 164 maglie su ferri di legno", detected.file.parts.first().prepNote)
        assertTrue(detected.file.parts.first().notes.any { it.frequency == NoteFrequency.EVERY_NTH })
        assertTrue(detected.file.parts.first().notes.any { it.frequency == NoteFrequency.SPECIFIC })
    }

    @Test
    fun `the project example is a good project file`() {
        val detected = codec.detect(example("example.majon")) as DetectedImport.Project
        val project = detected.data.projects.single()
        assertEquals("Mint sweater", project.project.name)
        val manica = project.parts.first { it.part.name == "Manica" }.part
        assertEquals(2, manica.copies)
        assertEquals(64, manica.castOnStitches)
        assertEquals(47.0, manica.endStitchCm!!, 0.0)
        assertEquals(3, project.finishing.size)
        assertEquals(1, detected.data.stash.yarns.size)
        assertEquals(0, detected.data.stash.needles.single().quantity) // not owned yet
        assertEquals(0L, project.links.yarns.single().yarnId)
        assertEquals(0L, project.parts.first().part.swatchId)
    }

    @Test
    fun `the notes of the project example can be taken on their own`() {
        val detected = codec.detect(example("example.majon")) as DetectedImport.Project
        val notes = codec.notesOf(detected.data.projects.single())
        assertEquals(listOf("Dietro", "Manica"), notes.parts.map { it.name })
        assertEquals(4, notes.parts.first().notes.size)
    }
}
