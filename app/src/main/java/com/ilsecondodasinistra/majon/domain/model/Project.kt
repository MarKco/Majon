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

/** The kind of project; it also decides the icon and which parts and measures are suggested. */
enum class ProjectIcon {
    SWEATER, SCARF, HAT, SOCKS, GLOVES, BLANKET, BAG, YARN,
    HEADBAND, COWL, SHAWL, CARDIGAN, SLIPPERS, DISHCLOTH,
}

enum class ProjectColor {
    TERRACOTTA, SAGE, OCEAN, SUNFLOWER, FUCHSIA, LAVENDER, MOSS, CORAL, SLATE, COCOA
}
