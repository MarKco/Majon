package com.ilsecondodasinistra.majon.domain.model

/**
 * A project with its parts, notes and what it is made with. In [links] the yarn, swatch and needle ids
 * are real ids when read from the database, and positions in the stash snapshot when read from a backup.
 */
data class FullProject(
    val project: Project,
    val parts: List<FullPart>,
    val links: ProjectLinks = ProjectLinks(),
    val finishing: List<FinishingTask> = emptyList(),
)

data class FullPart(
    val part: Part,
    val notes: List<RowNote>,
)
