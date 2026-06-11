package com.ilsecondodasinistra.majon.domain.model

data class Project(
    val id: Long = 0L,
    val name: String,
    val icon: ProjectIcon = ProjectIcon.SWEATER,
    val color: ProjectColor = ProjectColor.TERRACOTTA,
    val yarnType: String? = null,
    val needleSize: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

enum class ProjectIcon {
    SWEATER, SCARF, HAT, SOCKS, GLOVES, BLANKET, BAG, YARN
}

enum class ProjectColor {
    TERRACOTTA, SAGE, OCEAN, SUNFLOWER, FUCHSIA, LAVENDER, MOSS, CORAL, SLATE, COCOA
}
