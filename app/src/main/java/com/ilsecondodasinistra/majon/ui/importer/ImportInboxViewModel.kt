package com.ilsecondodasinistra.majon.ui.importer

import androidx.lifecycle.ViewModel
import com.ilsecondodasinistra.majon.data.export.ImportInbox
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Lets the navigation graph see that a file opened from outside the app is waiting to be imported. */
@HiltViewModel
class ImportInboxViewModel @Inject constructor(val inbox: ImportInbox) : ViewModel()
