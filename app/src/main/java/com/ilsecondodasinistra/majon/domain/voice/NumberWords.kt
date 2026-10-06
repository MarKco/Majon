package com.ilsecondodasinistra.majon.domain.voice

import java.text.Normalizer

/** Lowercases, strips accents and punctuation, and splits [text] into words. */
internal fun tokenize(text: String): List<String> =
    Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .replace("[^a-z0-9 ]".toRegex(), " ")
        .split(' ')
        .filter { it.isNotEmpty() }

private val ItalianUnits = listOf(
    "zero", "uno", "due", "tre", "quattro", "cinque", "sei", "sette", "otto", "nove",
    "dieci", "undici", "dodici", "tredici", "quattordici", "quindici", "sedici",
    "diciassette", "diciotto", "diciannove",
)
private val ItalianTens = mapOf(
    20 to "venti", 30 to "trenta", 40 to "quaranta", 50 to "cinquanta",
    60 to "sessanta", 70 to "settanta", 80 to "ottanta", 90 to "novanta",
)

/** Every Italian number word from 0 to 999, e.g. "ventotto", "centoventi", "duecentotre". */
private val ItalianNumbers: Map<String, Int> = buildMap {
    fun tensAndUnits(base: Int, prefix: String) {
        // prefix is "" or the hundreds word; "cento" drops its o before "ottanta"
        for (n in 0..99) {
            if (n == 0 && base == 0) continue
            val tail = when {
                n == 0 -> ""
                n < 20 -> ItalianUnits[n]
                else -> {
                    val tens = ItalianTens.getValue(n / 10 * 10)
                    val unit = n % 10
                    when (unit) {
                        0 -> tens
                        1, 8 -> tens.dropLast(1) + ItalianUnits[unit]
                        else -> tens + ItalianUnits[unit]
                    }
                }
            }
            val word = prefix + tail
            put(word, base + n)
            // "centottanta" / "centotto": the o of cento may be dropped before a vowel
            if (prefix.endsWith("cento") && tail.isNotEmpty() && tail.first() in "oiu") {
                put(prefix.dropLast(1) + tail, base + n)
            }
        }
    }
    tensAndUnits(0, "")
    tensAndUnits(100, "cento")
    for (h in 2..9) tensAndUnits(h * 100, ItalianUnits[h] + "cento")
}

private val EnglishUnits = mapOf(
    "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6,
    "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11, "twelve" to 12,
    "thirteen" to 13, "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
    "eighteen" to 18, "nineteen" to 19,
)
private val EnglishTens = mapOf(
    "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
    "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
)

private fun englishNumber(words: List<String>): Int? {
    if (words.isEmpty()) return null
    var total = 0
    var current = 0
    var sawNumber = false
    for (word in words) {
        when {
            word == "and" -> if (!sawNumber) return null
            word in EnglishUnits -> { current += EnglishUnits.getValue(word); sawNumber = true }
            word in EnglishTens -> { current += EnglishTens.getValue(word); sawNumber = true }
            word == "hundred" -> {
                if (!sawNumber) return null
                current = (if (current == 0) 1 else current) * 100
            }
            else -> return null
        }
    }
    total += current
    return if (sawNumber) total else null
}

/**
 * Reads a row number from spoken [words]: digits ("40"), Italian words ("quarantadue",
 * "cento venti") or English words ("forty two", "one hundred and twenty").
 * Returns null when the words are not entirely a number.
 */
internal fun parseSpokenNumber(words: List<String>): Int? {
    if (words.isEmpty()) return null
    if (words.size == 1) words[0].toIntOrNull()?.let { return it }
    ItalianNumbers[words.joinToString("")]?.let { return it }
    return englishNumber(words)
}
