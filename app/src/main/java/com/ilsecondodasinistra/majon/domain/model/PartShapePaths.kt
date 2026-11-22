package com.ilsecondodasinistra.majon.domain.model

/** One piece of a part's sketch, in SVG path syntax (absolute commands) inside a 100 x 100 box, y down. */
data class ShapePath(val d: String, val filled: Boolean = true)

private fun fill(d: String) = ShapePath(d, filled = true)
private fun line(d: String) = ShapePath(d, filled = false)

/** The drawing of a part: filled outlines first, then the lines (fringe, rib, handle...) on top. */
fun PartShape.paths(): List<ShapePath> = when (this) {
    // Body with armholes, sloping shoulders and a deep round neck
    PartShape.FRONT -> listOf(fill("M8,100 L8,42 C8,33 15,30 16,20 L30,5 C35,26 65,26 70,5 L84,20 C85,30 92,33 92,42 L92,100 Z"))
    // The same, with a shallow neck
    PartShape.BACK -> listOf(fill("M8,100 L8,42 C8,33 15,30 16,20 L30,5 C38,12 62,12 70,5 L84,20 C85,30 92,33 92,42 L92,100 Z"))
    // Half a body: the centre front is the straight right edge, the neck slopes down to it
    PartShape.LEFT_FRONT -> listOf(fill("M6,100 L6,42 C6,33 13,30 14,20 L28,5 L52,5 L94,48 L94,100 Z"))
    PartShape.RIGHT_FRONT -> listOf(fill("M94,100 L94,42 C94,33 87,30 86,20 L72,5 L48,5 L6,48 L6,100 Z"))
    // Cuff at the bottom, widening to the underarm, with the rounded sleeve cap
    PartShape.SLEEVE -> listOf(fill("M33,100 L8,34 C16,18 34,10 50,10 C66,10 84,18 92,34 L67,100 Z"))
    PartShape.RECTANGLE -> listOf(fill("M6,10 L94,10 L94,92 L6,92 Z"))
    // A scarf with fringe at the end
    PartShape.LONG_RECTANGLE -> listOf(
        fill("M36,0 L64,0 L64,90 L36,90 Z"),
        line("M40,90 L40,100 M47,90 L47,100 M53,90 L53,100 M60,90 L60,100"),
    )
    // A dishcloth with its hanging loop
    PartShape.SQUARE -> listOf(
        fill("M20,14 L80,14 L80,94 L20,94 Z"),
        line("M28,14 C22,0 36,0 32,14"),
    )
    // A shawl: straight top edge, edges curving in to the point
    PartShape.TRIANGLE -> listOf(fill("M3,8 L97,8 C84,44 64,78 50,100 C36,78 16,44 3,8 Z"))
    // A hat seen from the side, with its brim and a pompom
    PartShape.DOME -> listOf(
        fill("M12,100 L12,54 C12,22 30,8 50,8 C70,8 88,22 88,54 L88,100 Z"),
        fill("M42,6 a8,8 0 1,0 16,0 a8,8 0 1,0 -16,0"),
        line("M12,80 L88,80"),
    )
    // A band worn round the head or neck, slightly curved, with a ribbed edge
    PartShape.BAND -> listOf(
        fill("M3,34 C30,24 70,24 97,34 L97,72 C70,62 30,62 3,72 Z"),
        line("M14,38 L14,68 M28,36 L28,66 M42,35 L42,65 M58,35 L58,65 M72,36 L72,66 M86,38 L86,68"),
    )
    // The leg of a sock: ribbed cuff at the top
    PartShape.TUBE -> listOf(
        fill("M28,0 L72,0 L72,100 L28,100 Z"),
        line("M34,2 L34,22 M42,2 L42,22 M50,2 L50,22 M58,2 L58,22 M66,2 L66,22 M28,24 L72,24"),
    )
    // The foot of a sock, ending in a round toe
    PartShape.FOOT -> listOf(fill("M26,0 L74,0 L74,66 C74,90 64,100 50,100 C36,100 26,90 26,66 Z"))
    // A mitten: rounded top and a thumb
    PartShape.MITTEN -> listOf(
        fill("M28,100 L28,42 C28,14 39,4 53,4 C67,4 78,14 78,42 L78,100 Z"),
        fill("M28,76 L9,60 C3,52 9,42 18,47 L28,58 Z"),
    )
    // A bag with its handle
    PartShape.BAG -> listOf(
        fill("M10,36 L90,36 L90,100 L10,100 Z"),
        line("M30,36 C30,4 70,4 70,36"),
    )
    PartShape.STRAP -> listOf(fill("M41,0 L59,0 L59,100 L41,100 Z"))
    // The sole of a slipper: a foot print
    PartShape.SOLE -> listOf(fill("M34,4 C52,-1 72,8 73,26 C74,44 61,55 60,68 C59,84 56,98 44,98 C32,98 28,84 30,68 C32,52 20,42 22,26 C23,12 28,6 34,4 Z"))
    // The upper of a slipper: rounded vamp
    PartShape.UPPER -> listOf(fill("M12,100 L12,46 C12,16 32,4 52,4 C74,4 90,18 90,46 L90,100 Z"))

}
