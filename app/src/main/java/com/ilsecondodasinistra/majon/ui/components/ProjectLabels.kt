package com.ilsecondodasinistra.majon.ui.components

import androidx.annotation.StringRes
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.FinishingStep
import com.ilsecondodasinistra.majon.domain.model.MeasureKey
import com.ilsecondodasinistra.majon.domain.model.PresetPartName
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon

/** The name of the kind of project the icon stands for. */
@StringRes
fun ProjectIcon.typeLabelRes(): Int = when (this) {
    ProjectIcon.SWEATER -> R.string.project_type_sweater
    ProjectIcon.SCARF -> R.string.project_type_scarf
    ProjectIcon.HAT -> R.string.project_type_hat
    ProjectIcon.SOCKS -> R.string.project_type_socks
    ProjectIcon.GLOVES -> R.string.project_type_gloves
    ProjectIcon.BLANKET -> R.string.project_type_blanket
    ProjectIcon.BAG -> R.string.project_type_bag
    ProjectIcon.YARN -> R.string.project_type_yarn
    ProjectIcon.HEADBAND -> R.string.project_type_headband
    ProjectIcon.COWL -> R.string.project_type_cowl
    ProjectIcon.SHAWL -> R.string.project_type_shawl
    ProjectIcon.CARDIGAN -> R.string.project_type_cardigan
    ProjectIcon.SLIPPERS -> R.string.project_type_slippers
    ProjectIcon.DISHCLOTH -> R.string.project_type_dishcloth
}

@StringRes
fun MeasureKey.labelRes(): Int = when (this) {
    MeasureKey.WIDTH -> R.string.measure_width
    MeasureKey.CIRCUMFERENCE -> R.string.measure_circumference
    MeasureKey.LENGTH -> R.string.measure_length
    MeasureKey.HEIGHT -> R.string.measure_height
}

@StringRes
fun PresetPartName.labelRes(): Int = when (this) {
    PresetPartName.MAIN -> R.string.part_name_main
    PresetPartName.FRONT -> R.string.part_name_front
    PresetPartName.BACK -> R.string.part_name_back
    PresetPartName.LEFT_FRONT -> R.string.part_name_left_front
    PresetPartName.RIGHT_FRONT -> R.string.part_name_right_front
    PresetPartName.SLEEVE -> R.string.part_name_sleeve
    PresetPartName.LEG -> R.string.part_name_leg
    PresetPartName.FOOT -> R.string.part_name_foot
    PresetPartName.CUFF -> R.string.part_name_cuff
    PresetPartName.HAND -> R.string.part_name_hand
    PresetPartName.BODY -> R.string.part_name_body
    PresetPartName.STRAP -> R.string.part_name_strap
    PresetPartName.SOLE -> R.string.part_name_sole
    PresetPartName.UPPER -> R.string.part_name_upper
}

fun FinishingStep.labelRes(): Int = when (this) {
    FinishingStep.SEW_SHOULDERS -> R.string.finishing_step_sew_shoulders
    FinishingStep.PICK_UP_NECK -> R.string.finishing_step_pick_up_neck
    FinishingStep.NECK_RIBBING -> R.string.finishing_step_neck_ribbing
    FinishingStep.SEW_SLEEVES -> R.string.finishing_step_sew_sleeves
    FinishingStep.SEW_SIDES -> R.string.finishing_step_sew_sides
    FinishingStep.WEAVE_ENDS -> R.string.finishing_step_weave_ends
    FinishingStep.WASH_AND_DRY -> R.string.finishing_step_wash_and_dry
}
