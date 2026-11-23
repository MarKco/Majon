package com.ilsecondodasinistra.majon.data

import com.ilsecondodasinistra.majon.data.export.BackupCodec
import com.ilsecondodasinistra.majon.data.export.JsonFormatter
import com.ilsecondodasinistra.majon.data.export.NotesCodec
import com.ilsecondodasinistra.majon.data.export.NotesDecodeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tidying the JSON a person pasted, and telling on which line of the tidy text each part and note is. */
class JsonFormatterTest {

    private val compact = """{"app":"majon-notes","version":1,"parts":[{"part":"Davanti","notes":[{"rowStart":7,"text":"aumenta"},{"rows":[10,20,30],"text":"segna"}]},{"part":"Manica","notes":[{"rowStart":1,"text":"coste"}]}]}"""

    @Test
    fun `compact JSON comes out indented, one thing per line`() {
        val formatted = JsonFormatter.format(compact)!!.text
        assertEquals(
            """
            {
              "app": "majon-notes",
              "version": 1,
              "parts": [
                {
                  "part": "Davanti",
                  "notes": [
                    {
                      "rowStart": 7,
                      "text": "aumenta"
                    },
                    {
                      "rows": [10, 20, 30],
                      "text": "segna"
                    }
                  ]
                },
                {
                  "part": "Manica",
                  "notes": [
                    {
                      "rowStart": 1,
                      "text": "coste"
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
            formatted,
        )
    }

    @Test
    fun `the formatted text means the same as the original`() {
        val codec = NotesCodec(BackupCodec())
        val a = (codec.decode(compact) as NotesDecodeResult.Success).file
        val b = (codec.decode(JsonFormatter.format(compact)!!.text) as NotesDecodeResult.Success).file
        assertEquals(a, b)
    }

    @Test
    fun `each part and note knows its line`() {
        val lines = JsonFormatter.format(compact)!!.lines
        assertEquals(1, lines["$"])
        assertEquals(5, lines["parts[0]"])
        assertEquals(8, lines["parts[0].notes[0]"])
        assertEquals(12, lines["parts[0].notes[1]"])
        assertEquals(18, lines["parts[1]"])
        assertEquals(21, lines["parts[1].notes[0]"])
    }

    @Test
    fun `words and a code block around the JSON are dropped`() {
        val formatted = JsonFormatter.format("Ecco il file:\n```json\n$compact\n```\nBuon lavoro!")!!.text
        assertTrue(formatted.startsWith("{\n  \"app\""))
        assertTrue(formatted.endsWith("}"))
    }

    @Test
    fun `text that is not JSON is not touched`() {
        assertNull(JsonFormatter.format("questo non è json"))
        assertNull(JsonFormatter.format("{\"a\": }"))
        assertNull(JsonFormatter.format(""))
    }

    @Test
    fun `strings, numbers, empty things and special characters survive`() {
        val formatted = JsonFormatter.format("""{"a":[],"b":{},"c":null,"d":true,"e":"riga \"uno\"\nè 2,5","f":[1,[2,3]]}""")!!
        assertTrue(formatted.text.contains("\"a\": []"))
        assertTrue(formatted.text.contains("\"b\": {}"))
        assertTrue(formatted.text.contains("\"c\": null"))
        assertTrue(formatted.text.contains("\"d\": true"))
        assertTrue(formatted.text.contains("riga \\\"uno\\\"\\nè 2,5"))
        assertNotNull(JsonFormatter.format(formatted.text))
    }

    @Test
    fun `formatting twice changes nothing`() {
        val once = JsonFormatter.format(compact)!!.text
        assertEquals(once, JsonFormatter.format(once)!!.text)
    }
}
