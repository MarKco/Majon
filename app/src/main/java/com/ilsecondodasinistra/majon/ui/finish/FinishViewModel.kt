package com.ilsecondodasinistra.majon.ui.finish

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilsecondodasinistra.majon.domain.model.formatDecimalInput
import com.ilsecondodasinistra.majon.domain.model.parseDecimal
import com.ilsecondodasinistra.majon.domain.repository.MajonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** The "how much yarn did you use" form: the yarns to ask about and the amounts as typed (by yarn id). */
data class FinishForm(
    val yarns: List<FinishYarn> = emptyList(),
    val amounts: Map<Long, String> = emptyMap(),
    val loaded: Boolean = false,
)

@HiltViewModel
class FinishViewModel @Inject constructor(
    private val repository: MajonRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: Long = checkNotNull(savedStateHandle["projectId"])

    private val _form = MutableStateFlow(FinishForm())
    val form: StateFlow<FinishForm> = _form

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved.asSharedFlow()

    init {
        viewModelScope.launch {
            val yarns = repository.yarnsToAskAbout(projectId)
            _form.value = FinishForm(
                yarns = yarns,
                amounts = yarns.associate { it.yarnId to it.suggestedSkeins?.let(::formatDecimalInput).orEmpty() },
                loaded = true,
            )
        }
    }

    fun setAmount(yarnId: Long, text: String) {
        _form.value = _form.value.let { it.copy(amounts = it.amounts + (yarnId to text)) }
    }

    /** Takes the typed amounts off the stock; an empty, invalid or zero amount takes nothing. Always succeeds. */
    fun save(): Boolean {
        val amounts = _form.value.amounts
        viewModelScope.launch {
            amounts.forEach { (yarnId, text) ->
                val skeins = parseDecimal(text) ?: 0.0
                if (skeins > 0.0) repository.adjustYarnStock(yarnId, -skeins)
            }
            _saved.emit(Unit)
        }
        return true
    }
}
