package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.model.PartShape
import com.ilsecondodasinistra.majon.domain.model.paths
import org.junit.Assert.assertTrue
import org.junit.Test

class PartShapePathsTest {

    @Test
    fun `every shape has a filled outline`() {
        PartShape.entries.forEach { shape ->
            val paths = shape.paths()
            assertTrue("$shape has no paths", paths.isNotEmpty())
            assertTrue("$shape has no filled outline", paths.any { it.filled })
        }
    }

    @Test
    fun `paths stay inside a 100 by 100 box`() {
        val number = Regex("-?\\d+(?:\\.\\d+)?")
        PartShape.entries.forEach { shape ->
            shape.paths().forEach { path ->
                // Only the relative arc "a" is used (for round details); everything else is absolute
                assertTrue("$shape uses a relative command: ${path.d}", path.d.none { it in "mlcqshvt" })
                // The numbers of an arc are radii and flags, not positions: leave those paths out of the range check
                if ('a' !in path.d) {
                    number.findAll(path.d).map { it.value.toFloat() }.forEach { v ->
                        assertTrue("$shape: $v is outside the box", v in -5f..105f)
                    }
                }
            }
        }
    }
}
