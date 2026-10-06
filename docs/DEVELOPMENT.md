# Majon — Documentazione di sviluppo

Contarighe per maglieria, Android nativo. Questo documento descrive cosa è stato
costruito, come è strutturato e le decisioni prese rispetto alle specifiche di
partenza (`starting-docs/`).

## Panoramica

| | |
|---|---|
| Package | `com.ilsecondodasinistra.majon` |
| minSdk / target / compile | 24 / 37 / 37 |
| Linguaggio | Kotlin 2.4.20 |
| UI | Jetpack Compose + Material 3 |
| Build | Gradle 9.8.0, AGP 9.4.1 (Kotlin built-in) |
| Test | 102 unit test, 0 falliti |

## Stack

- **Compose BOM 2026.09.00** — Material 3 (1.5.0-alpha29 per le API Expressive), animation, navigation-compose 2.10.2
- **Room 2.8.5** con **KSP** — persistenza SQLite
- **Hilt 2.60.1** — dependency injection
- **DataStore Preferences 1.2.1** — impostazioni
- **kotlinx.serialization 1.11.0** — export/import JSON
- **AppCompat 1.7.1** — per-app locales (cambio lingua in-app)
- Test: JUnit4, **Robolectric 4.17**, **Turbine**, MockK, coroutines-test

## Architettura

MVVM + Clean Architecture leggera, package singolo modulo `:app`:

```
domain/          modelli puri + logica (zero dipendenze Android)
  model/         Project, Part, RowNote, FullProject, validazioni, progresso
  repository/    interfaccia MajonRepository
data/
  db/            Room: entities, DAO, MajonDatabase, mapper entity<->domain
  repository/    MajonRepositoryImpl (unica sorgente dati, clock iniettato)
  settings/      SettingsRepository su DataStore (lingua, tema, toggle)
  export/        BackupCodec (JSON encode/decode, tollerante a valori ignoti)
di/              AppModule + RepositoryModule (Hilt)
ui/
  theme/         palette craft (terracotta/salvia/crema), dark, dynamic color
  components/    ProjectIconBadge, AnimatedProgressBar, ConfirmDialog, EmptyState
  navigation/    MajonNavGraph (route + transizioni slide/fade)
  home/ editproject/ projectdetail/ counter/ notes/ settings/
                 una coppia Screen + ViewModel per schermata
MajonApplication applica la lingua salvata prima di ogni Activity
MainActivity     AppCompatActivity (necessaria per per-app locales) + Compose
```

### Modello dati

```
Project (1) ──< Part (1) ──< RowNote
```

- `Part.completedRows` è il contatore; `currentRow = completedRows + 1`
- `RowNote` = testo + intervallo `rowStart..rowEnd` + frequenza
  (`EVERY_ROW | ODD_ROWS | EVEN_ROWS`). Range e frequenza componibili:
  "righe dispari dalla 20 alla 30" è una sola nota.
- Cascade delete: progetto → parti → note (FK Room).
- Percentuale progetto = somma righe fatte / somma righe totali (pesata).

### Logica chiave (domain, pura e testata)

- `RowNote.appliesTo(row)` — matching nota/riga
- `List<RowNote>.notesForRow(row)` — note attive ordinate per rowStart
- `validateNote(text, start, end, total)` — errori tipizzati
  (`TEXT_BLANK`, `START_BELOW_ONE`, `END_BEFORE_START`, `OUT_OF_RANGE`)
- `projectProgress(parts)` — clamp 0..1, gestisce totale zero

## Decisioni rispetto alle specifiche

| Spec | Decisione | Perché |
|---|---|---|
| RF5: dialog bloccante a ogni riga | **Feedback non bloccante**: nota riga corrente sempre visibile + preview prossima riga + haptic | Press del "+" ogni 1-2 minuti per ore: un dialog da chiudere ogni volta è attrito inaccettabile. L'audio chiedeva "un messaggio, uno spazio", non un dialog |
| — | Aggiunto pulsante **"−"** (undo) e "Vai alla riga…" | Pressioni accidentali frequenti nell'uso reale; assente nelle spec |
| RF4.2 + RF4.3 separate | Modello unico range + frequenza | Copre anche i casi combinati dell'audio |
| minSdk 26 | **minSdk 24** | Più copertura, zero costo |
| Package `com.contarighe` | Mantenuto `com.ilsecondodasinistra.majon` | Progetto esistente |
| kapt, Compose 1.6, versioni 2023 | KSP + stack 2026 | Tech spec datata; richiesto "librerie più recenti" |
| Sub-parti | Non implementate | Esplicitamente giudicate inutili nell'audio |

Richieste extra (non nelle spec): multilingua IT/EN con switch in-app,
export/import JSON, animazioni, tema craft + dark + Material You opzionale.

## i18n

- `values/strings.xml` = italiano (default), `values-en/` = inglese
- Switch in Impostazioni → `AppCompatDelegate.setApplicationLocales`
  (ricreazione istantanea dell'Activity)
- Persistenza doppia: DataStore (fonte di verità, riapplicata in
  `MajonApplication.onCreate`) + `autoStoreLocales` service per API < 33
- `android:localeConfig` espone le lingue nelle impostazioni di sistema (API 33+)
- `androidResources.localeFilters += listOf("it", "en")` limita le risorse

## Export / Import

Formato JSON (`BackupCodec`), marker `"app": "majon"`, `version: 1`:

```json
{
  "app": "majon", "version": 1, "exportedAt": 1760000000000,
  "projects": [{
    "name": "Maglione", "icon": "SWEATER", "color": "SAGE",
    "yarnType": "Merino", "needleSize": "4.5",
    "parts": [{
      "name": "Davanti", "totalRows": 120, "completedRows": 42,
      "notes": [{"rowStart": 20, "rowEnd": 30, "frequency": "ODD_ROWS", "text": "..."}]
    }]
  }]
}
```

- Import = **merge** (aggiunge progetti, non tocca gli esistenti), id riassegnati
- Icone/colori/frequenze sconosciuti → fallback ai default (forward compatibility)
- I/O file via Storage Access Framework (`CreateDocument` / `OpenDocument`),
  nessun permesso richiesto

## Icona launcher

- Adaptive icon: gomitolo crema + ferri salvia su terracotta
  (`drawable/ic_launcher_background|foreground|monochrome.xml`)
- Layer monochrome per le icone temizzate (API 33+)
- Legacy webp (API 24-25) generati da SVG con ImageMagick, 5 densità.
  Sorgenti SVG ricreabili: design identico ai vector drawable

## Testing (TDD)

Test scritti prima dell'implementazione, per layer:

| Suite | Cosa copre |
|---|---|
| `domain/RowNoteTest` | matching note: singola, range, pari/dispari, combinazioni |
| `domain/ProgressTest` | percentuali parte/progetto, clamp, edge case |
| `domain/NoteValidationTest` | errori di range |
| `data/MajonRepositoryTest` | CRUD, cascade, timestamp, ordinamento (Room in-memory + Robolectric) |
| `data/SettingsRepositoryTest` | default e persistenza DataStore |
| `data/BackupCodecTest` | round-trip, fallback, file malformati |
| `data/ImportExportRepositoryTest` | snapshot completo + import merge |
| `ui/*ViewModelTest` | tutti i 6 ViewModel con `FakeMajonRepository` + Turbine |

```bash
./gradlew :app:testDebugUnitTest   # 71 test
./gradlew :app:lintDebug           # 0 errori
./gradlew :app:assembleDebug       # APK
```

Nota: Robolectric non supporta ancora SDK 37 → pin a 36 in
`app/src/test/resources/robolectric.properties`.

## Comandi vocali e tasti volume

Controllo a mani libere del contatore, attivo solo dalla schermata del contatore.

```
domain/voice/   parseVoiceCommand(testo) -> VoiceCommand?   (pura, testata)
                NumberWords: numeri a parole IT (0-999) ed EN, cifre
ui/voice/       ContinuousSpeechListener  SpeechRecognizer con riavvio automatico
                Speaker (TextToSpeech)    risposte vocali, disattivabili
ui/input/       VolumeKeyHost             l'activity cede i tasti volume alla schermata
CounterViewModel.onVoiceCommand(cmd) -> voiceReplies (SharedFlow<VoiceReply>)
```

- **Parser**: accetta insieme i vocabolari IT ed EN (il riconoscitore gira già nella
  lingua dell'app) e solo frasi corte (≤ 6 parole), per ignorare le chiacchiere.
  Prova le tre alternative del riconoscitore finché una è un comando.
- **Azzeramento**: «azzera» chiede conferma a voce; qualunque altro comando, o 10 s
  (`RESET_CONFIRM_MILLIS`), annullano. «annulla» durante la conferma annulla
  l'azzeramento invece di togliere una riga.
- **Ascolto**: `ContinuousSpeechListener` preferisce il riconoscitore on-device
  (Android 12+); se manca la lingua offline ripiega su quello di sistema (con rete).
  Errori transitori: riavvio con backoff, poi stop dopo 5 errori consecutivi.
  Si ferma con `ON_STOP`, quindi il microfono non resta aperto in background.
- **Permesso**: `RECORD_AUDIO` richiesto a runtime solo all'attivazione, con una
  schermata che spiega cosa succede. `<queries>` per RecognitionService e TTS (API 30+).
- **Tasti volume**: `MainActivity.dispatchKeyEvent` li intercetta solo mentre il
  contatore li ha reclamati (impostazione "Tasti volume", spenta di default).
- **Limiti noti**: il riconoscimento vero e proprio non è testabile in unit test
  né sull'emulatore senza pacchetti lingua; parser e ViewModel sono coperti da
  test, il listener si prova a mano su un dispositivo.

## Problemi incontrati e soluzioni

1. **AndroidX 2026 richiede AGP 9.1+ / compileSdk 37** → upgrade Gradle 9.5.1 + AGP 9.2.1.
2. **AGP 9 ha Kotlin built-in**: il plugin `org.jetbrains.kotlin.android` va
   rimosso; restano `kotlin.plugin.compose` e `kotlin.plugin.serialization`.
3. **Hilt 2.59 non leggeva metadata Kotlin 2.4**: risolto con Hilt 2.60.1 + Kotlin 2.4.20.
4. **Tema XML**: l'Activity è `AppCompatActivity` (serve per i locales), quindi
   il tema base è `Theme.AppCompat.DayNight.NoActionBar`; tutto il resto è Compose.

## Possibili sviluppi futuri

- Release build firmata + Play Store listing
- Test strumentati Compose (`androidTest/`, dipendenze già configurate)
- Foto per progetto, statistiche, widget home screen (post-V1 nelle spec)
