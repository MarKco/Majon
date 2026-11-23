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
| Test | 273 unit test, 0 falliti |

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
  model/         Project, Part, RowNote, FullProject, Stash (Yarn, Swatch, Needle), validazioni, progresso
  repository/    interfaccia MajonRepository
data/
  db/            Room: entities, DAO, MajonDatabase, mapper entity<->domain
  repository/    MajonRepositoryImpl (unica sorgente dati, clock iniettato)
  settings/      SettingsRepository su DataStore (lingua, tema, toggle)
  export/        BackupCodec (JSON encode/decode, tollerante a valori ignoti)
di/              AppModule + RepositoryModule (Hilt)
ui/
  theme/         palette craft (Cheerful default, Classic terracotta/salvia/crema, Forest), dark, dynamic color
  components/    ProjectIconBadge, AnimatedProgressBar, ConfirmDialog, EmptyState,
                 GaugeSwatchDiagram, Glass (barra vetro), LocalImage
  navigation/    MajonNavGraph (route + transizioni slide/fade)
  home/ editproject/ projectdetail/ counter/ notes/ settings/ stash/
                 una coppia Screen + ViewModel per schermata
MajonApplication applica la lingua salvata prima di ogni Activity
MainActivity     AppCompatActivity (necessaria per per-app locales) + Compose
```

### Modello dati

```
Project (1) ──< Part (1) ──< RowNote
```

- `Part.completedRows` è il contatore dei **passi**: riga di preparazione (opzionale,
  «riga 0») + righe 1..N + riga finale (opzionale). `totalRows` = solo le righe 1..N,
  `totalSteps` = con gli extra. `currentStep: RowStep` (`Prep | Row(n) | Final`) dice
  cosa si sta lavorando; `rowsDone` = righe vere finite; `isLastStep`.
  Attivare/togliere la preparazione a lavoro iniziato sposta il conteggio di 1
  (`Part.withRowOptions`) per restare sulla stessa riga
- `RowNote` = testo + intervallo `rowStart..rowEnd` + frequenza
  (`EVERY_ROW | ODD_ROWS | EVEN_ROWS`). Range e frequenza componibili:
  "righe dispari dalla 20 alla 30" è una sola nota.
- Cascade delete: progetto → parti → note (FK Room).
- Percentuale progetto = somma passi fatti / somma passi totali (pesata).
- `nextPart(parts, current)`: prima parte non finita creata dopo la corrente (ordine di id).

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

- Adaptive icon: gomitolo corallo avvolto a caso con un ferro salvia, su crema. Si rigenera da `docs/icon/make_icon.py` (`v2` = fili caotici, in uso; `v1` = archi regolari, che somigliava a un mappamondo; sorgenti SVG in `docs/icon/`)
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
./gradlew :app:testDebugUnitTest   # unit test (273)
./gradlew :app:lintDebug           # 0 errori
./gradlew :app:assembleDebug       # APK
```

Nota: Robolectric non supporta ancora SDK 37 → pin a 36 in
`app/src/test/resources/robolectric.properties`.

## Stash: lane, campioni, ferri (schema DB v3)

```
Yarn (1) ──< YarnMaterial      blend: fibra + % opzionale + posizione
     (1) ──< Swatch            campione 10x10 cm: punti, righe, ferri, punto, peso
Needle                         tipo, materiale, mm, cm, quantità, marca
```

- Tutto è opzionale tranne il nome della lana e il tipo dei ferri. `stockSkeins` è
  `Double` (frazioni); grammi e metri rimasti derivano da peso/metraggio del gomitolo
  (`Yarn.stockGrams/stockMeters`). Lo stock cala solo con un'azione dell'utente
  (`adjustYarnStock`, clamp a 0 in SQL).
- `MajonRepository` espone osservazione/CRUD per lane, campioni, ferri e
  `getStash()/importStash()` per il backup. `upsertYarn` sostituisce il blend in transazione.
- UI (`ui/stash/`): `StashScreen` (schede Lane/Ferri), `NeedleEditScreen`, `SwatchEditScreen` (il quadrato 10×10 è il modulo: `GaugeSwatchInput`), `PartEditScreen` (`ui/partedit/`);
  gli editor sono schermate, non dialog (dialog solo per conferme e per UN campo; il color picker è l'unica eccezione: un solo valore scelto su un quadrato saturazione/luminosità + barra tonalità),
  `NoteEditScreen` (`ui/notes/`) per le note di riga,
  schermate con campi: `Modifier.imePadding()` + `KeyboardBackHandler()` (`ui/components/Keyboard.kt`), così la tastiera non copre il campo e Back la chiude prima di uscire,
  `YarnDetailScreen` (scorta, campioni col `GaugeSwatchDiagram`), `YarnEditScreen`
  (editor a schermo intero, foto da Photo Picker copiata in `filesDir`).
  Gli input numerici restano testo nei ViewModel (`parseDecimal` accetta "2,5" e "2.5").
- **Barra inferiore** Progetti | Stash in `MajonNavGraph`: `NavigationBar` trasparente
  sovrapposta ai contenuti con effetto vetro **Haze** (`ui/components/Glass.kt`, stessa
  idea di Discography). I contenuti leggono `LocalBottomBarHeight` per lasciare spazio.
- **Backup v2** (`BackupCodec.encode/decodeAll`): aggiunge `yarns` (con `materials` e
  `swatches`) e `needles`; i file v1 si leggono (stash vuoto). Foto e id non si esportano.
  `decode()` restituisce ancora solo i progetti.
- Migrazione `MIGRATION_2_3`: SQL copiato dallo schema generato `3.json`.

## Progetti: collegamenti allo stash, misure, stime, duplicazione (schema DB v4)

```
Project ──< project_yarns (yarn, isPrimary, swatchId?, plannedSkeins?)     cascade da progetto e lana
        ──< project_needles (needle)                                        cascade da progetto e ferro
Part: + stitchMeasure/stitchCm, rowMeasure/rowCm, swatchId?                 (colonne, senza tabelle)
  «Lavorata di lato» (`Part.worksSideways`) non è una colonna: si ricava dalle misure. Standard = punti su larghezza/giro e righe su lunghezza/altezza; di lato = scambiate (punti su lunghezza/altezza, righe su larghezza/giro). Lo switch (`SidewaysSwitch`) scambia ruoli e cm insieme, quindi stime e lana (area) restano coerenti; vale anche alla creazione delle parti suggerite (`oriented(sideways)`).
  Copie (`Part.copies`, `currentCopy`): una parte lavorata più volte (due maniche). Il contatore chiede conferma a fine copia (`nextCopyPrompt`, come `nextPartPrompt`: a riga ultima non cambia nulla finché non confermi), poi `startNextCopy()` riparte da riga 1; «Azzera» riparte la copia in corso; `progress` conta tutte le copie, `copyProgress` solo quella in corso; la stima della lana moltiplica l'area per le copie. `withCopies()` fa avanzare una parte già finita alla copia nuova.
  `castOnStitches` (maglie da avviare, come scrive il modello, mostrate nel contatore e nella card) e `endStitchCm` (misura finale sulla direzione dei punti: area = media × lunghezza; `PartEstimate.endStitches`); lo scambio «di lato» azzera `endStitchCm`.
  Schema v5 (migrazione 4→5): colonne di `parts` e tabella `finishing_tasks` (checklist «Rifiniture» per progetto: `FinishingTask`, `ProjectIcon.finishingSteps()` per i passi suggeriti, nel `ProjectDetailViewModel`; duplicando un progetto i passi tornano da fare; nel backup `finishing`).
```

- **Tipo di progetto = `ProjectIcon`** (14 valori: gli 8 originali + HEADBAND, COWL, SHAWL,
  CARDIGAN, SLIPPERS, DISHCLOTH). `ProjectIcon.presetParts()` (`domain/model/ProjectPresets.kt`)
  dà le parti suggerite con le due misure che guidano punti e righe (`MeasureKey`:
  WIDTH/CIRCUMFERENCE nel senso dei punti, LENGTH/HEIGHT nel senso delle righe). I nomi
  sono enum (`PresetPartName`); le traduzioni stanno in `ui/components/ProjectLabels.kt`.
- **Stime** (`domain/model/GaugeEstimate.kt`, pure): `punti = round(cm/10 × punti_campione)`,
  `righe = round(cm/10 × righe_campione)`; lana = `Σ area parti / 100 × peso campione`;
  gomitoli = grammi / peso gomitolo, oppure `plannedSkeins` del modello se c'è (vince).
  `yarnCheck` → `Enough | Missing(missingRoundedUp al quarto) | Unknown`. Solo la lana
  **principale** si stima; `planYarns` costruisce un `YarnPlan` per ogni lana collegata.
  Il campione di un collegamento è quello scelto, altrimenti il primo con peso, altrimenti
  il primo (`defaultSwatch` per le misure del progetto).
- `ProjectLinks.normalized()`: una sola lana principale, niente doppioni.
- `MajonRepository`: `observeProjectLinks/setProjectLinks`, `observeNeedleUsage` (ferro →
  nomi dei progetti **non finiti** che lo usano), `duplicateProject(id, copyName)` (copia
  parti con misure, note per riga, extra rows e collegamenti, progresso a 0),
  `importBackup(projects, stash)`.
- **Backup**: nel file i collegamenti sono **posizioni** nelle liste `yarns`/`needles` (e
  nell'elenco piatto dei campioni, in ordine di file), non id del DB. `encode` converte
  id → posizioni usando lo stash passato; `importBackup` importa prima lo stash e poi
  rimappa le posizioni sugli id appena creati, scartando i collegamenti orfani.
  `FullProject.links` contiene id reali se letto dal DB e posizioni se letto da un file.
- UI: `ProjectLinksScreen` (`ui/projectlinks/`, con `ProjectLinksViewModel`) per scegliere lane e ferri, invitata dalla scheda «Con cosa lo lavori?» nel dettaglio;
  una parte con `totalRows = 0` significa «righe da definire» (parti suggerite create in un tap: `addSuggestedParts`); a fine progetto `CounterViewModel.finishPrompt` (id progetto) apre `FinishScreen` (`ui/finish/`), che chiede quanta lana è stata usata per ogni filato (`FinishViewModel.save()`), mai in automatico;
  la Home mostra tipo e lana principale (`observePrimaryYarnNames`); scheda `YarnPlanCard` e riga di
  stima su ogni parte nel dettaglio; editor parte con parti suggerite, misure, stima e
  «Usa N righe come totale»; «Nuovo progetto simile» in Home e nel dettaglio (la copia si
  apre in modifica).
- Migrazione `MIGRATION_3_4` (SQL copiato da `4.json`), testata su dispositivo.

## Contatore: numero della riga, righe extra, parte successiva

- **Numero grande** (`CounterScreen.RowNumber`): colore tenue = «in lavorazione». Al `+` il
  dato si salva subito, ma ciò che si vede resta sul numero vecchio per
  `DONE_FLASH_MILLIS` (320 ms) in colore pieno («Fatta ✓»), poi `AnimatedContent`
  passa a X+1 tenue. Un secondo `+` durante il lampo lo salta. Riga 0 usa `tertiary`,
  riga finale `secondary`, righe normali `primary`.
- **Parte successiva**: `CounterViewModel.increment()` sull'ultimo passo, se `nextPart`
  esiste, non avanza ma pubblica `nextPartPrompt` (dialog). `confirmNextPart()` completa la
  parte, attende `NEXT_PART_DELAY_MILLIS` (festeggiamento) ed emette `navigateToPart`;
  il NavGraph sostituisce il contatore (`popUpTo` inclusive) così Back torna al progetto.
  A voce: `Confirm` conferma, qualunque altro comando annulla; tasti volume: su = sì, giù = no.
- Schema DB v2 (`MIGRATION_1_2`): `parts.hasPrepRow`, `hasFinalRow`, `prepNote`, `finalNote`.
  `PartDao.adjustCompletedRows` fa il clamp su `totalRows + hasPrepRow + hasFinalRow`.

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

## Note per righe precise, import, condivisione (`.majon`, `.majon-notes`)

- **Note**: `RowNote` vale per un intervallo (tutte, pari, dispari), per le righe di un elenco (`SPECIFIC`, campo `rows`)
  o per una riga ogni N (`EVERY_NTH`, campo `step`). Schema v6 (`notes.step`, `notes.rowList`). L'editor
  (`NoteEditScreen`) sceglie il tipo; `parseRowList` capisce «10, 20 30-35»; l'elenco le descrive in parole
  (`noteRowsLabel`).
- **Aggiornamenti**: `UpgradeFromEveryVersionTest` costruisce il database come lo lasciava ogni versione
  precedente (dagli schemi esportati), ci mette progetto, parte e nota, lo apre con tutte le migrazioni e controlla
  che i dati ci siano. Togliendo `MIGRATION_5_6` il test fallisce, quindi non è vuoto.
- **Formati**: vedi `docs/majon-format-guide.md`. `.majon` = `BackupCodec` (stesso formato per il backup e per un
  progetto condiviso, `encodeForSharing` toglie scorta/prezzi/negozio e segna i ferri come non posseduti);
  `.majon-notes` = `NotesCodec`, letto in modo rigoroso (tutti gli errori insieme, con parte e numero di nota).
- **Importazione** (`ui/importer`): `ImportViewModel` riconosce il tipo (`NotesCodec.detect`), per un progetto mostra
  cosa porta allo stash (`importBackup` **riusa** lane, campioni e ferri già presenti: `sameYarnAs`, `sameSwatchAs`,
  `sameNeedleAs`), per le note abbina le parti per nome (`suggestChoices`) e fa il piano (`planNotesImport`, puro:
  duplicati, avvisi, errori) che `applyNotesImport` esegue senza mai togliere niente. «Formatta e controlla»
  (`JsonFormatter`) riscrive il testo indentato e dà a ogni errore la sua riga.
- **Da fuori l'app**: `MainActivity` legge i file `.majon` / `.majon-notes` aperti (tre filtri nel manifest, come per
  Proportion) e li lascia in `ImportInbox`; il grafo di navigazione apre Importa. La condivisione scrive il file in
  `cache/shared` e lo passa al foglio di condivisione con il `FileProvider`.
- **Guida IA**: `NotesGuideScreen` (generica da Impostazioni, con le parti del progetto dal suo menu).
