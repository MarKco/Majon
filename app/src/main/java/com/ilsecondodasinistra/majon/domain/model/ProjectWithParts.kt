package com.ilsecondodasinistra.majon.domain.model

data class ProjectWithParts(
    val project: Project,
    val parts: List<Part>,
) {
    val progress: Float
        get() = projectProgress(parts)
}
