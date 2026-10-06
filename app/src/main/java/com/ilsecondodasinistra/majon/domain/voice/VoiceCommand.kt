package com.ilsecondodasinistra.majon.domain.voice

/** What the user can ask the counter to do by voice. */
sealed interface VoiceCommand {
    data object Increment : VoiceCommand
    data object Decrement : VoiceCommand
    data class GoToRow(val row: Int) : VoiceCommand
    data object ReadNote : VoiceCommand
    data object RemainingRows : VoiceCommand
    data object Reset : VoiceCommand
    data object Confirm : VoiceCommand
}

private val FillerWords = setOf(
    "per", "favore", "please", "la", "il", "lo", "the", "a", "alla", "al", "to", "ok", "okay",
)

private val IncrementPhrases = setOf(
    "avanti", "fatto", "fatta", "su", "piu", "prossima", "prossimo", "riga fatta", "riga finita",
    "finita", "finito", "next", "done", "forward", "plus", "up", "next row", "row done", "one more",
)
private val DecrementPhrases = setOf(
    "indietro", "annulla", "meno", "giu", "torna indietro", "annulla riga", "back", "undo",
    "minus", "down", "go back", "undo row",
)
private val ReadNotePhrases = setOf(
    "leggi nota", "leggi note", "nota", "note", "leggi", "cosa devo fare", "che devo fare",
    "read note", "read notes", "read", "notes", "what to do", "what do i do",
)
private val RemainingPhrases = setOf(
    "quante righe mancano", "righe mancanti", "righe rimaste", "mancano", "quante mancano",
    "how many rows left", "rows left", "remaining", "how many left", "rows remaining",
)
private val ResetPhrases = setOf(
    "azzera", "azzera contatore", "azzera il contatore", "ricomincia", "da capo", "reset",
    "reset counter", "start over",
)
private val ConfirmPhrases = setOf("conferma", "confermo", "si", "sicuro", "yes", "confirm", "sure")

private val GoToPrefixes = listOf(
    listOf("vai", "riga"), listOf("vai"), listOf("riga"), listOf("go", "row"), listOf("go"),
    listOf("row"), listOf("salta", "riga"), listOf("salta"), listOf("jump"),
)

/**
 * Turns what the speech recognizer heard into a command, or null when it is not one.
 * Both Italian and English vocabularies are accepted: the recognizer already runs in the
 * app language, and the two do not clash on these few words.
 *
 * Only short utterances count, so chatter in the room is unlikely to move the counter.
 */
fun parseVoiceCommand(text: String): VoiceCommand? {
    val words = tokenize(text).filterNot { it in FillerWords || it == "e" }
    if (words.isEmpty() || words.size > 6) return null
    parseGoToRow(words)?.let { return it }
    val phrase = words.joinToString(" ")
    return when (phrase) {
        in ConfirmPhrases -> VoiceCommand.Confirm
        in ResetPhrases -> VoiceCommand.Reset
        in RemainingPhrases -> VoiceCommand.RemainingRows
        in ReadNotePhrases -> VoiceCommand.ReadNote
        in DecrementPhrases -> VoiceCommand.Decrement
        in IncrementPhrases -> VoiceCommand.Increment
        else -> null
    }
}

private fun parseGoToRow(words: List<String>): VoiceCommand.GoToRow? {
    for (prefix in GoToPrefixes) {
        if (words.size > prefix.size && words.take(prefix.size) == prefix) {
            val row = parseSpokenNumber(words.drop(prefix.size)) ?: continue
            return VoiceCommand.GoToRow(row)
        }
    }
    return null
}
