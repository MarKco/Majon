package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.Part
import com.ilsecondodasinistra.majon.domain.model.buildNotesGuide
import com.ilsecondodasinistra.majon.domain.model.guidePartLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesGuideTest {

    @Test
    fun `the parts of the project go where the marker is`() {
        val guide = buildNotesGuide("prima\n\n{{PARTS}}\n\ndopo", "Parti:\n- Davanti (120 righe)")
        assertEquals("prima\n\nParti:\n- Davanti (120 righe)\n\ndopo", guide)
    }

    @Test
    fun `with no parts the marker goes without leaving a hole`() {
        val guide = buildNotesGuide("prima\n\n{{PARTS}}\n\ndopo", "")
        assertEquals("prima\n\ndopo", guide)
        assertFalse(guide.contains("{{"))
    }

    @Test
    fun `the lines list each part with its rows when known`() {
        val parts = listOf(
            Part(projectId = 1L, name = "Davanti", totalRows = 192),
            Part(projectId = 1L, name = "Collo", totalRows = 0),
        )
        assertEquals("- Davanti (192 righe)\n- Collo", guidePartLines(parts) { "$it righe" })
    }

    @Test
    fun `the real guides are complete`() {
        // The templates shipped with the app: the format marker, the four ways to say rows and the parts marker
        for (path in listOf("src/main/res/raw/notes_guide.txt", "src/main/res/raw-en/notes_guide.txt")) {
            val text = java.io.File(path).readText()
            assertTrue(path, text.contains("{{PARTS}}"))
            assertTrue(path, text.contains("\"majon-notes\""))
            for (word in listOf("rowStart", "rowEnd", "rows", "every", "ODD_ROWS", "EVEN_ROWS", "prepNote")) {
                assertTrue("$path mentions $word", text.contains(word))
            }
        }
    }

    @Test
    fun `the example in the guides is a file the app accepts`() {
        for (path in listOf("src/main/res/raw/notes_guide.txt", "src/main/res/raw-en/notes_guide.txt")) {
            val text = java.io.File(path).readText()
            val example = Regex("```json\\n([\\s\\S]*?)```").find(text)!!.groupValues[1]
            val result = com.ilsecondodasinistra.majon.data.export.NotesCodec(com.ilsecondodasinistra.majon.data.export.BackupCodec()).decode(example)
            assertTrue("$path example: $result", result is com.ilsecondodasinistra.majon.data.export.NotesDecodeResult.Success)
        }
    }
}
