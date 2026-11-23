package com.ilsecondodasinistra.majon.data.export

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The JSON of a text tidied up, and on which line (1-based) each object and array starts, by path ("parts[0].notes[2]"). */
data class FormattedJson(val text: String, val lines: Map<String, Int>)

/**
 * Tidies pasted JSON so it can be read (and so a problem can be pointed at by line): indented, one thing per line,
 * short lists of numbers or words on one line. Words and a code block around it, as chat assistants add, are dropped.
 * Returns null when the text is not JSON, which is then left as it is.
 */
object JsonFormatter {

    fun format(text: String): FormattedJson? {
        val body = NotesCodec.extractJson(text)
        val root = try {
            Json.parseToJsonElement(body)
        } catch (e: Exception) {
            return null
        }
        if (root !is JsonObject && root !is JsonArray) return null
        val writer = Writer()
        writer.write(root, "$", 0)
        return FormattedJson(writer.text(), writer.lines)
    }

    private class Writer {
        private val sb = StringBuilder()
        private var line = 1
        val lines = mutableMapOf<String, Int>()

        fun text() = sb.toString()

        private fun newline(indent: Int) {
            sb.append('\n').append("  ".repeat(indent))
            line++
        }

        fun write(element: JsonElement, path: String, indent: Int) {
            when (element) {
                is JsonObject -> {
                    lines[path] = line
                    if (element.isEmpty()) {
                        sb.append("{}")
                        return
                    }
                    sb.append('{')
                    element.entries.forEachIndexed { i, (key, value) ->
                        if (i > 0) sb.append(',')
                        newline(indent + 1)
                        sb.append(JsonPrimitive(key).toString()).append(": ")
                        write(value, if (path == "$") key else "$path.$key", indent + 1)
                    }
                    newline(indent)
                    sb.append('}')
                }
                is JsonArray -> {
                    lines[path] = line
                    if (element.isEmpty()) {
                        sb.append("[]")
                    } else if (element.all { it is JsonPrimitive }) {
                        sb.append(element.joinToString(", ", "[", "]") { it.toString() })
                    } else {
                        sb.append('[')
                        element.forEachIndexed { i, item ->
                            if (i > 0) sb.append(',')
                            newline(indent + 1)
                            write(item, "$path[$i]", indent + 1)
                        }
                        newline(indent)
                        sb.append(']')
                    }
                }
                else -> sb.append(element.toString())
            }
        }
    }
}
