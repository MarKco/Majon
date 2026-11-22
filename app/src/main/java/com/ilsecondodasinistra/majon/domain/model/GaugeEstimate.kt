package com.ilsecondodasinistra.majon.domain.model

import kotlin.math.ceil
import kotlin.math.roundToInt

/** How a project's yarn is tied to it: which yarn, the swatch used for its gauge, and a planned amount. */
data class ProjectYarnLink(
    val yarnId: Long,
    val isPrimary: Boolean = false,
    /** Swatch of this yarn used to turn measures into stitches and rows. */
    val swatchId: Long? = null,
    /** Skeins the pattern asks for, when known; wins over the estimate. */
    val plannedSkeins: Double? = null,
)

/** The yarns and needles a project is made with (ids from the stash). */
data class ProjectLinks(
    val yarns: List<ProjectYarnLink> = emptyList(),
    val needleIds: List<Long> = emptyList(),
)

/** One link per yarn and per needle, and exactly one primary yarn (the first marked, else the first). */
fun ProjectLinks.normalized(): ProjectLinks {
    val unique = yarns.distinctBy { it.yarnId }
    val primaryId = unique.firstOrNull { it.isPrimary }?.yarnId ?: unique.firstOrNull()?.yarnId
    return ProjectLinks(
        yarns = unique.map { it.copy(isPrimary = it.yarnId == primaryId) },
        needleIds = needleIds.distinct(),
    )
}

/** Stitches and rows a part is expected to take, from its measures and a swatch. */
data class PartEstimate(val stitches: Int?, val rows: Int?, val endStitches: Int? = null)

private fun scaled(cm: Double?, perTenCm: Int): Int? =
    cm?.takeIf { it > 0.0 }?.let { (it / 10.0 * perTenCm).roundToInt() }

/** What this part should take, given the swatch it is knitted at; null parts when something is missing. */
fun Part.estimate(swatch: Swatch?): PartEstimate = PartEstimate(
    stitches = swatch?.let { scaled(stitchCm, it.stitches) },
    rows = swatch?.let { scaled(rowCm, it.rows) },
    endStitches = swatch?.let { scaled(endStitchCm?.takeIf { cm -> cm > 0.0 }, it.stitches) },
)

private fun Part.areaCm2(): Double? {
    val start = stitchCm?.takeIf { it > 0.0 } ?: return null
    val h = rowCm?.takeIf { it > 0.0 } ?: return null
    // A part that widens or narrows is worked out at its mean width; every copy takes its share of yarn
    val w = endStitchCm?.takeIf { it > 0.0 }?.let { (start + it) / 2.0 } ?: start
    return w * h * copies.coerceAtLeast(1)
}

/**
 * Grams of yarn needed: the knitted area of the measured parts divided into 10 x 10 cm swatches, times
 * the swatch weight. Null when nothing can be worked out (no swatch weight, no measured part).
 */
fun yarnNeededGrams(parts: List<Part>, swatch: Swatch?): Double? {
    val weight = swatch?.weightG ?: return null
    val area = parts.mapNotNull { it.areaCm2() }.takeIf { it.isNotEmpty() }?.sum() ?: return null
    return area / 100.0 * weight
}

/** Skeins this yarn is needed in: what the pattern says, else the estimated grams over the skein weight. */
fun neededSkeins(yarn: Yarn, plannedSkeins: Double?, neededGrams: Double?): Double? =
    plannedSkeins ?: neededGrams?.let { grams -> yarn.skeinWeightG?.let { grams / it } }

/** Whether the stock covers a project. */
sealed interface YarnCheck {
    data object Unknown : YarnCheck
    data class Enough(val needed: Double, val have: Double) : YarnCheck
    data class Missing(val needed: Double, val have: Double) : YarnCheck {
        val missing: Double get() = needed - have

        /** The missing amount rounded up to a quarter skein, what you would buy. */
        val missingRoundedUp: Double get() = ceil(missing * 4 - 1e-9) / 4
    }
}

fun yarnCheck(neededSkeins: Double?, have: Double): YarnCheck = when {
    neededSkeins == null -> YarnCheck.Unknown
    have + 1e-9 >= neededSkeins -> YarnCheck.Enough(neededSkeins, have)
    else -> YarnCheck.Missing(neededSkeins, have)
}

/** One yarn of a project with the swatch its gauge comes from, and whether the stock covers it. */
data class YarnPlan(
    val yarn: Yarn,
    val link: ProjectYarnLink,
    val swatch: Swatch?,
    val neededGrams: Double?,
    val neededSkeins: Double?,
    val check: YarnCheck,
)

/** The swatch a link uses: the chosen one, else the first of the yarn that has a weight, else the first. */
private fun swatchFor(link: ProjectYarnLink, swatches: List<Swatch>): Swatch? =
    swatches.firstOrNull { it.id == link.swatchId }
        ?: swatches.firstOrNull { it.weightG != null }
        ?: swatches.firstOrNull()

/** The swatch used to turn a project's measures into stitches and rows: its main yarn's. */
fun defaultSwatch(links: ProjectLinks, swatchesByYarn: Map<Long, List<Swatch>>): Swatch? {
    val primary = links.yarns.firstOrNull { it.isPrimary } ?: links.yarns.firstOrNull() ?: return null
    return swatchFor(primary, swatchesByYarn[primary.yarnId].orEmpty())
}

/**
 * For each yarn of the project: what is needed and whether the stock covers it. The main yarn can be
 * estimated from the measures of the parts and its swatch weight; any yarn can have a planned amount
 * from the pattern, which wins. Links to yarns that no longer exist are skipped.
 */
fun planYarns(
    parts: List<Part>,
    links: ProjectLinks,
    yarns: Map<Long, Yarn>,
    swatchesByYarn: Map<Long, List<Swatch>>,
): List<YarnPlan> = links.yarns.mapNotNull { link ->
    val yarn = yarns[link.yarnId] ?: return@mapNotNull null
    val swatch = swatchFor(link, swatchesByYarn[link.yarnId].orEmpty())
    val grams = if (link.isPrimary) yarnNeededGrams(parts, swatch) else null
    val skeins = neededSkeins(yarn, link.plannedSkeins, grams)
    YarnPlan(yarn, link, swatch, grams, skeins, yarnCheck(skeins, yarn.stockSkeins))
}
