package com.ilsecondodasinistra.majon.domain.model

/** A length a part is planned by. Which one is a width, a length, a height or a circumference depends on the garment. */
enum class MeasureKey { WIDTH, LENGTH, HEIGHT, CIRCUMFERENCE }

/** Names of the parts suggested for a project type (the UI shows them translated). */
enum class PresetPartName {
    MAIN, FRONT, BACK, LEFT_FRONT, RIGHT_FRONT, SLEEVE, LEG, FOOT, CUFF, HAND, BODY, STRAP, SOLE, UPPER
}

/** A part suggested for a project type, with the two measures that drive its stitches and rows. */
data class PresetPart(
    val name: PresetPartName,
    val stitchMeasure: MeasureKey,
    val rowMeasure: MeasureKey,
)

private fun part(name: PresetPartName, stitch: MeasureKey, row: MeasureKey) = PresetPart(name, stitch, row)

/** The parts (and the measures to take) that make sense for this kind of project. */
fun ProjectIcon.presetParts(): List<PresetPart> {
    val width = MeasureKey.WIDTH
    val length = MeasureKey.LENGTH
    val height = MeasureKey.HEIGHT
    val circ = MeasureKey.CIRCUMFERENCE
    return when (this) {
        ProjectIcon.SWEATER -> listOf(
            part(PresetPartName.FRONT, width, length),
            part(PresetPartName.BACK, width, length),
            part(PresetPartName.SLEEVE, width, length),
        )
        ProjectIcon.CARDIGAN -> listOf(
            part(PresetPartName.LEFT_FRONT, width, length),
            part(PresetPartName.RIGHT_FRONT, width, length),
            part(PresetPartName.BACK, width, length),
            part(PresetPartName.SLEEVE, width, length),
        )
        ProjectIcon.SCARF -> listOf(part(PresetPartName.MAIN, width, length))
        ProjectIcon.SHAWL -> listOf(part(PresetPartName.MAIN, width, length))
        ProjectIcon.BLANKET -> listOf(part(PresetPartName.MAIN, width, length))
        ProjectIcon.HAT -> listOf(part(PresetPartName.MAIN, circ, height))
        ProjectIcon.COWL -> listOf(part(PresetPartName.MAIN, circ, height))
        ProjectIcon.HEADBAND -> listOf(part(PresetPartName.MAIN, circ, height))
        ProjectIcon.SOCKS -> listOf(
            part(PresetPartName.LEG, circ, length),
            part(PresetPartName.FOOT, circ, length),
        )
        ProjectIcon.GLOVES -> listOf(
            part(PresetPartName.CUFF, circ, length),
            part(PresetPartName.HAND, circ, length),
        )
        ProjectIcon.BAG -> listOf(
            part(PresetPartName.BODY, width, height),
            part(PresetPartName.STRAP, width, length),
        )
        ProjectIcon.SLIPPERS -> listOf(
            part(PresetPartName.SOLE, width, length),
            part(PresetPartName.UPPER, width, length),
        )
        ProjectIcon.DISHCLOTH -> listOf(part(PresetPartName.MAIN, width, height))
        ProjectIcon.YARN -> listOf(part(PresetPartName.MAIN, width, height))
    }
}

/** What a part looks like, to sketch it where its measures are asked. Rows run up the page, stitches across. */
enum class PartShape {
    FRONT, BACK, LEFT_FRONT, RIGHT_FRONT, SLEEVE,
    RECTANGLE, LONG_RECTANGLE, SQUARE, TRIANGLE, DOME, BAND, TUBE, FOOT, MITTEN, BAG, STRAP, SOLE, UPPER,
}

/**
 * The shape of the part called [name] in a project of this [icon]; with no known name, that of the type's
 * first suggested part.
 */
fun partShape(icon: ProjectIcon, name: PresetPartName?): PartShape {
    val part = name ?: icon.presetParts().firstOrNull()?.name ?: PresetPartName.MAIN
    return when (part) {
        PresetPartName.FRONT -> PartShape.FRONT
        PresetPartName.BACK -> PartShape.BACK
        PresetPartName.LEFT_FRONT -> PartShape.LEFT_FRONT
        PresetPartName.RIGHT_FRONT -> PartShape.RIGHT_FRONT
        PresetPartName.SLEEVE -> PartShape.SLEEVE
        PresetPartName.LEG -> PartShape.TUBE
        PresetPartName.FOOT -> PartShape.FOOT
        PresetPartName.CUFF -> PartShape.BAND
        PresetPartName.HAND -> PartShape.MITTEN
        PresetPartName.BODY -> PartShape.BAG
        PresetPartName.STRAP -> PartShape.STRAP
        PresetPartName.SOLE -> PartShape.SOLE
        PresetPartName.UPPER -> PartShape.UPPER
        PresetPartName.MAIN -> when (icon) {
            ProjectIcon.SCARF -> PartShape.LONG_RECTANGLE
            ProjectIcon.SHAWL -> PartShape.TRIANGLE
            ProjectIcon.HAT -> PartShape.DOME
            ProjectIcon.HEADBAND, ProjectIcon.COWL -> PartShape.BAND
            ProjectIcon.DISHCLOTH -> PartShape.SQUARE
            else -> PartShape.RECTANGLE
        }
    }
}
