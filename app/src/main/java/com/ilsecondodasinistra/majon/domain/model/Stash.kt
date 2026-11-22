package com.ilsecondodasinistra.majon.domain.model

import kotlin.math.abs
import kotlin.math.roundToInt

/** Fibres a yarn can be made of. [OTHER] pairs with a free-text name in [MaterialShare.customName]. */
enum class YarnMaterial {
    WOOL, MERINO, ALPACA, MOHAIR, CASHMERE, COTTON, LINEN, SILK, BAMBOO, VISCOSE, ACRYLIC, POLYAMIDE, POLYESTER, OTHER
}

/** One fibre of a blend; the percentage is optional ("wool and silk" without knowing how much). */
data class MaterialShare(
    val material: YarnMaterial,
    val customName: String? = null,
    val percent: Int? = null,
)

/** Standard yarn thickness categories, thinnest first. */
enum class YarnWeight { LACE, FINGERING, SPORT, DK, WORSTED, ARAN, BULKY, SUPER_BULKY }

/**
 * A yarn in the stash. Everything but [name] is optional. [stockSkeins] can be fractional
 * (a skein and a half left); the grams and metres left are derived from the skein size.
 */
data class Yarn(
    val id: Long = 0L,
    val name: String,
    val brand: String? = null,
    val colorName: String? = null,
    /** ARGB colour of the yarn, for the swatch dot in lists. */
    val colorArgb: Int? = null,
    val dyeLot: String? = null,
    val weight: YarnWeight? = null,
    val materials: List<MaterialShare> = emptyList(),
    val skeinWeightG: Int? = null,
    val skeinLengthM: Int? = null,
    val stockSkeins: Double = 0.0,
    val pricePerSkein: Double? = null,
    val shop: String? = null,
    val care: String? = null,
    val notes: String? = null,
    /** Internal file copied from the photo picker (not exported in backups). */
    val photoPath: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val stockGrams: Double? get() = skeinWeightG?.let { it * stockSkeins.coerceAtLeast(0.0) }
    val stockMeters: Double? get() = skeinLengthM?.let { it * stockSkeins.coerceAtLeast(0.0) }
}

/** Stitch patterns a swatch can be knitted in. */
enum class StitchPattern { STOCKINETTE, GARTER, RIB, SEED, CABLE, OTHER }

/**
 * Gauge swatch: stitches and rows counted over a 10 x 10 cm square for one yarn, with the needles
 * and stitch pattern used. [weightG] is the swatch's weight, used to estimate how much yarn a project needs.
 */
data class Swatch(
    val id: Long = 0L,
    val yarnId: Long,
    val stitches: Int,
    val rows: Int,
    val needleSizeMm: Double? = null,
    val pattern: StitchPattern = StitchPattern.STOCKINETTE,
    val weightG: Double? = null,
    val notes: String? = null,
)

enum class NeedleType { CIRCULAR, STRAIGHT, DPN, INTERCHANGEABLE }

enum class NeedleMaterial { METAL, BAMBOO, WOOD, PLASTIC, CARBON, OTHER }

/** Knitting needles in the stash. Everything but the [type] is optional. */
data class Needle(
    val id: Long = 0L,
    val type: NeedleType,
    val material: NeedleMaterial? = null,
    val sizeMm: Double? = null,
    /** Cable length for circulars, needle length for straights. */
    val lengthCm: Int? = null,
    val quantity: Int = 1,
    val brand: String? = null,
    val notes: String? = null,
)

/** Sum of the shares that state a percentage. */
fun materialsTotalPercent(materials: List<MaterialShare>): Int = materials.sumOf { it.percent ?: 0 }

fun materialsExceedHundred(materials: List<MaterialShare>): Boolean = materialsTotalPercent(materials) > 100

/** Skeins as text: whole number plus ¼ ½ ¾ when it fits, else up to two trimmed decimals ("1,33"). */
fun formatSkeins(value: Double, decimalSeparator: Char = ','): String {
    val v = value.coerceAtLeast(0.0)
    // Work in hundredths so 1.999 becomes 2 and 0.3 stays 0,3
    val hundredths = (v * 100).roundToInt()
    val whole = hundredths / 100
    val frac = hundredths % 100
    val glyph = when (frac) {
        25 -> "¼"
        50 -> "½"
        75 -> "¾"
        else -> null
    }
    if (frac == 0) return "$whole"
    if (glyph != null) return if (whole == 0) glyph else "$whole$glyph"
    val decimals = if (frac % 10 == 0) "${frac / 10}" else frac.toString().padStart(2, '0')
    return "$whole$decimalSeparator$decimals"
}

/** Needle size in mm without a useless ".0" ("3", "3,5", "2,75"). */
fun formatMillimeters(value: Double, decimalSeparator: Char = ','): String {
    val hundredths = (value * 100).roundToInt()
    val whole = hundredths / 100
    val frac = abs(hundredths % 100)
    if (frac == 0) return "$whole"
    val decimals = if (frac % 10 == 0) "${frac / 10}" else frac.toString().padStart(2, '0')
    return "$whole$decimalSeparator$decimals"
}


/** A yarn together with its gauge swatches, as stored in a backup. */
data class FullYarn(val yarn: Yarn, val swatches: List<Swatch> = emptyList())

/** Everything in the stash, for backup and restore. */
data class StashSnapshot(val yarns: List<FullYarn>, val needles: List<Needle>)

/** How many records an import added. */
data class StashImportCounts(val yarns: Int, val needles: Int)

/** How many records a full backup import added. */
data class BackupImportCounts(val projects: Int, val yarns: Int, val needles: Int)

/** Reads a number the way people type it: "2,5" or "2.5". Null when blank or not a number. */
fun parseDecimal(text: String): Double? {
    val t = text.trim().replace(',', '.')
    if (t.isEmpty()) return null
    return t.toDoubleOrNull()?.takeIf { it.isFinite() }
}

/** A number as text for an input field: no useless ".0", at most two decimals, always a dot. */
fun formatDecimalInput(value: Double): String {
    val hundredths = (value * 100).roundToInt()
    val whole = hundredths / 100
    val frac = abs(hundredths % 100)
    if (frac == 0) return "$whole"
    val decimals = if (frac % 10 == 0) "${frac / 10}" else frac.toString().padStart(2, '0')
    return "$whole.$decimals"
}

/** Opaque ARGB from hue (degrees, wraps), saturation and value (0..1). */
fun hsvToArgb(hue: Float, saturation: Float, value: Float): Int {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    fun ch(f: Float) = ((f + m) * 255f).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
}

/** Hue (degrees), saturation and value (0..1) of an ARGB colour; the alpha is ignored. */
fun argbToHsv(argb: Int): Triple<Float, Float, Float> {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    return Triple(if (h < 0f) h + 360f else h, if (max == 0f) 0f else d / max, max)
}
