package com.ilsecondodasinistra.majon.domain.model

/** Where the list of the project's parts goes in the guide text. */
const val NOTES_GUIDE_PARTS_MARKER = "{{PARTS}}"

/**
 * The guide to give an AI (the format of a `.majon-notes` file and how to read a pattern into it), with the parts of
 * a project written into it when there is one: [partsBlock] replaces the marker, or the marker goes when it is blank.
 */
fun buildNotesGuide(template: String, partsBlock: String): String =
    template.replace(NOTES_GUIDE_PARTS_MARKER, partsBlock.trim()).replace(Regex("\n{3,}"), "\n\n").trim()

/** One line per part, «- Davanti (120 rows)», for the guide of a project; rows still to define are not mentioned. */
fun guidePartLines(parts: List<Part>, rowsLabel: (Int) -> String): String =
    parts.joinToString("\n") { part ->
        if (part.totalRows > 0) "- ${part.name} (${rowsLabel(part.totalRows)})" else "- ${part.name}"
    }
