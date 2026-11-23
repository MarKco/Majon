package com.ilsecondodasinistra.majon.data.export

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Where the text of a `.majon` or `.majon-notes` file opened from outside the app (a file manager, a chat, an email)
 * waits until the import screen takes it.
 */
@Singleton
class ImportInbox @Inject constructor() {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending

    fun offer(text: String) {
        _pending.value = text
    }

    /** The waiting text, if any; it is not there afterwards. */
    fun take(): String? = _pending.value.also { _pending.value = null }
}
