package com.ilsecondodasinistra.majon.domain.model

/** One thing left to do once the knitting is done (sew the shoulders, weave in the ends...). */
data class FinishingTask(
    val id: Long = 0L,
    val projectId: Long,
    val title: String,
    val done: Boolean = false,
    /** Order in the list, from 0. */
    val position: Int = 0,
)

/** The typical finishing steps the app can suggest (the UI shows them translated). */
enum class FinishingStep {
    SEW_SHOULDERS, PICK_UP_NECK, NECK_RIBBING, SEW_SLEEVES, SEW_SIDES, WEAVE_ENDS, WASH_AND_DRY
}

/** The finishing steps that make sense for this kind of project, in the order they are done. */
fun ProjectIcon.finishingSteps(): List<FinishingStep> = when (this) {
    ProjectIcon.SWEATER, ProjectIcon.CARDIGAN -> listOf(
        FinishingStep.SEW_SHOULDERS,
        FinishingStep.PICK_UP_NECK,
        FinishingStep.NECK_RIBBING,
        FinishingStep.SEW_SLEEVES,
        FinishingStep.SEW_SIDES,
        FinishingStep.WEAVE_ENDS,
    )
    else -> listOf(FinishingStep.WEAVE_ENDS, FinishingStep.WASH_AND_DRY)
}
