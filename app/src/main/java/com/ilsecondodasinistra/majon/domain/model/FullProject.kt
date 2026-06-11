package com.ilsecondodasinistra.majon.domain.model

data class FullProject(
    val project: Project,
    val parts: List<FullPart>,
)

data class FullPart(
    val part: Part,
    val notes: List<RowNote>,
)
