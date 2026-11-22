package com.ilsecondodasinistra.majon.ui.stash

import androidx.annotation.StringRes
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.NeedleMaterial
import com.ilsecondodasinistra.majon.domain.model.NeedleType
import com.ilsecondodasinistra.majon.domain.model.StitchPattern
import com.ilsecondodasinistra.majon.domain.model.YarnMaterial
import com.ilsecondodasinistra.majon.domain.model.YarnWeight

@StringRes
fun YarnMaterial.labelRes(): Int = when (this) {
    YarnMaterial.WOOL -> R.string.material_wool
    YarnMaterial.MERINO -> R.string.material_merino
    YarnMaterial.ALPACA -> R.string.material_alpaca
    YarnMaterial.MOHAIR -> R.string.material_mohair
    YarnMaterial.CASHMERE -> R.string.material_cashmere
    YarnMaterial.COTTON -> R.string.material_cotton
    YarnMaterial.LINEN -> R.string.material_linen
    YarnMaterial.SILK -> R.string.material_silk
    YarnMaterial.BAMBOO -> R.string.material_bamboo
    YarnMaterial.VISCOSE -> R.string.material_viscose
    YarnMaterial.ACRYLIC -> R.string.material_acrylic
    YarnMaterial.POLYAMIDE -> R.string.material_polyamide
    YarnMaterial.POLYESTER -> R.string.material_polyester
    YarnMaterial.OTHER -> R.string.material_other
}

@StringRes
fun YarnWeight.labelRes(): Int = when (this) {
    YarnWeight.LACE -> R.string.weight_lace
    YarnWeight.FINGERING -> R.string.weight_fingering
    YarnWeight.SPORT -> R.string.weight_sport
    YarnWeight.DK -> R.string.weight_dk
    YarnWeight.WORSTED -> R.string.weight_worsted
    YarnWeight.ARAN -> R.string.weight_aran
    YarnWeight.BULKY -> R.string.weight_bulky
    YarnWeight.SUPER_BULKY -> R.string.weight_super_bulky
}

@StringRes
fun StitchPattern.labelRes(): Int = when (this) {
    StitchPattern.STOCKINETTE -> R.string.pattern_stockinette
    StitchPattern.GARTER -> R.string.pattern_garter
    StitchPattern.RIB -> R.string.pattern_rib
    StitchPattern.SEED -> R.string.pattern_seed
    StitchPattern.CABLE -> R.string.pattern_cable
    StitchPattern.OTHER -> R.string.pattern_other
}

@StringRes
fun NeedleType.labelRes(): Int = when (this) {
    NeedleType.CIRCULAR -> R.string.needle_circular
    NeedleType.STRAIGHT -> R.string.needle_straight
    NeedleType.DPN -> R.string.needle_dpn
    NeedleType.INTERCHANGEABLE -> R.string.needle_interchangeable
}

@StringRes
fun NeedleMaterial.labelRes(): Int = when (this) {
    NeedleMaterial.METAL -> R.string.needle_mat_metal
    NeedleMaterial.BAMBOO -> R.string.needle_mat_bamboo
    NeedleMaterial.WOOD -> R.string.needle_mat_wood
    NeedleMaterial.PLASTIC -> R.string.needle_mat_plastic
    NeedleMaterial.CARBON -> R.string.needle_mat_carbon
    NeedleMaterial.OTHER -> R.string.needle_mat_other
}
