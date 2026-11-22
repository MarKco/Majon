package com.ilsecondodasinistra.majon.ui.finish

import com.ilsecondodasinistra.majon.domain.model.planYarns
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import kotlinx.coroutines.flow.first

/** A yarn of the finished project, to ask how much of it was used. */
data class FinishYarn(
    val yarnId: Long,
    val name: String,
    /** The estimate for the project (rounded up to a quarter skein), when it can be worked out. */
    val suggestedSkeins: Double?,
    val stockSkeins: Double,
)

/** The yarns linked to the project that are still in stock, each with the estimate of what the project used. */
suspend fun MajonRepository.yarnsToAskAbout(projectId: Long): List<FinishYarn> {
    val project = observeProject(projectId).first() ?: return emptyList()
    val links = observeProjectLinks(projectId).first()
    if (links.yarns.isEmpty()) return emptyList()
    val yarns = observeYarns().first().associateBy { it.id }
    val swatches = links.yarns.associate { l -> l.yarnId to observeSwatches(l.yarnId).first() }
    return planYarns(project.parts, links, yarns, swatches)
        .filter { it.yarn.stockSkeins > 0.0 }
        .map {
            FinishYarn(
                yarnId = it.yarn.id,
                name = listOfNotNull(it.yarn.name, it.yarn.colorName).joinToString(" "),
                suggestedSkeins = it.neededSkeins?.let { n -> kotlin.math.ceil(n * 4 - 1e-9) / 4 },
                stockSkeins = it.yarn.stockSkeins,
            )
        }
}
