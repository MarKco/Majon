package com.ilsecondodasinistra.majon.domain

import com.ilsecondodasinistra.majon.domain.voice.VoiceCommand
import com.ilsecondodasinistra.majon.domain.voice.parseSpokenNumber
import com.ilsecondodasinistra.majon.domain.voice.parseVoiceCommand
import com.ilsecondodasinistra.majon.domain.voice.tokenize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandTest {

    @Test
    fun `italian increment phrases`() {
        listOf("avanti", "Fatto!", "riga fatta", "più", "prossima", "avanti per favore").forEach {
            assertEquals(it, VoiceCommand.Increment, parseVoiceCommand(it))
        }
    }

    @Test
    fun `english increment phrases`() {
        listOf("next", "Done", "row done", "plus", "next row").forEach {
            assertEquals(it, VoiceCommand.Increment, parseVoiceCommand(it))
        }
    }

    @Test
    fun `decrement phrases in both languages`() {
        listOf("indietro", "annulla", "meno", "torna indietro", "back", "undo", "go back").forEach {
            assertEquals(it, VoiceCommand.Decrement, parseVoiceCommand(it))
        }
    }

    @Test
    fun `go to row with digits and words`() {
        assertEquals(VoiceCommand.GoToRow(40), parseVoiceCommand("vai alla riga 40"))
        assertEquals(VoiceCommand.GoToRow(42), parseVoiceCommand("vai alla riga quarantadue"))
        assertEquals(VoiceCommand.GoToRow(120), parseVoiceCommand("riga cento venti"))
        assertEquals(VoiceCommand.GoToRow(19), parseVoiceCommand("go to row 19"))
        assertEquals(VoiceCommand.GoToRow(35), parseVoiceCommand("go to row thirty five"))
        assertEquals(VoiceCommand.GoToRow(7), parseVoiceCommand("vai a sette"))
    }

    @Test
    fun `riga alone is not a go to command`() {
        assertNull(parseVoiceCommand("riga"))
    }

    @Test
    fun `information commands`() {
        assertEquals(VoiceCommand.ReadNote, parseVoiceCommand("leggi la nota"))
        assertEquals(VoiceCommand.ReadNote, parseVoiceCommand("cosa devo fare"))
        assertEquals(VoiceCommand.ReadNote, parseVoiceCommand("read note"))
        assertEquals(VoiceCommand.RemainingRows, parseVoiceCommand("quante righe mancano"))
        assertEquals(VoiceCommand.RemainingRows, parseVoiceCommand("how many rows left"))
    }

    @Test
    fun `reset and confirm`() {
        assertEquals(VoiceCommand.Reset, parseVoiceCommand("azzera il contatore"))
        assertEquals(VoiceCommand.Reset, parseVoiceCommand("start over"))
        assertEquals(VoiceCommand.Confirm, parseVoiceCommand("sì"))
        assertEquals(VoiceCommand.Confirm, parseVoiceCommand("conferma"))
        assertEquals(VoiceCommand.Confirm, parseVoiceCommand("yes"))
    }

    @Test
    fun `chatter and long sentences are ignored`() {
        assertNull(parseVoiceCommand("passami le forbici per favore"))
        assertNull(parseVoiceCommand("avanti con la serie però aspetta un momento che chiamano"))
        assertNull(parseVoiceCommand(""))
        assertNull(parseVoiceCommand("   "))
    }

    @Test
    fun `italian numbers from 0 to 999`() {
        assertEquals(0, parseSpokenNumber(listOf("zero")))
        assertEquals(11, parseSpokenNumber(listOf("undici")))
        assertEquals(21, parseSpokenNumber(listOf("ventuno")))
        assertEquals(28, parseSpokenNumber(listOf("ventotto")))
        assertEquals(33, parseSpokenNumber(listOf("trentatre")))
        assertEquals(100, parseSpokenNumber(listOf("cento")))
        assertEquals(101, parseSpokenNumber(listOf("centouno")))
        assertEquals(108, parseSpokenNumber(listOf("centootto")))
        assertEquals(180, parseSpokenNumber(listOf("centottanta")))
        assertEquals(256, parseSpokenNumber(listOf("duecento", "cinquanta", "sei")))
        assertEquals(999, parseSpokenNumber(listOf("novecentonovantanove")))
    }

    @Test
    fun `english numbers`() {
        assertEquals(13, parseSpokenNumber(listOf("thirteen")))
        assertEquals(42, parseSpokenNumber(listOf("forty", "two")))
        assertEquals(100, parseSpokenNumber(listOf("one", "hundred")))
        assertEquals(120, parseSpokenNumber(listOf("one", "hundred", "and", "twenty")))
        assertEquals(305, parseSpokenNumber(listOf("three", "hundred", "five")))
    }

    @Test
    fun `non numbers are rejected`() {
        assertNull(parseSpokenNumber(listOf("pippo")))
        assertNull(parseSpokenNumber(emptyList()))
        assertNull(parseSpokenNumber(listOf("forty", "banana")))
    }

    @Test
    fun `tokenize strips accents and punctuation`() {
        assertEquals(listOf("piu", "si", "e", "gia"), tokenize("Più, sì! E già."))
    }
}
