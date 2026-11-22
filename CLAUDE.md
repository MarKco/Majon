# Majon

Contarighe per maglieria, Android nativo (Kotlin, Compose + Material 3 Expressive, Room, Hilt, DataStore).
Package `com.ilsecondodasinistra.majon`, modulo unico `:app`. Lingua dell'interfaccia: italiano di default, inglese in `values-en/`.
Documentazione estesa: `docs/DEVELOPMENT.md` (architettura e decisioni), manuali in `docs/manual/`, specifiche originali in `starting-docs/`.

## Comandi

```bash
./gradlew :app:testDebugUnitTest   # unit test (Robolectric, ~100)
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

## Mappa del codice (`app/src/main/java/.../majon/`)

| Dove | Cosa |
|---|---|
| `domain/model`, `domain/repository` | modelli e logica pura (nessuna dipendenza Android), interfaccia `MajonRepository` |
| `domain/model/Stash.kt` | lane, campioni, ferri, formattazione gomitoli/mm, `parseDecimal` |
| `domain/voice` | parser dei comandi vocali (puro) |
| `data/db` | Room: `Entities.kt` (entity + mapper `toDomain/toEntity`), `Daos.kt`, `MajonDatabase.kt` |
| `data/repository` | `MajonRepositoryImpl` (clock iniettato) |
| `data/export` | `BackupCodec`: export/import JSON |
| `data/settings` | `SettingsRepository` su DataStore (lingua, tema, palette, toggle) |
| `di` | Hilt: `AppModule` (DB, DAO, DataStore, clock), `RepositoryModule` |
| `ui/stash` | schermate dello stash (lista, dettaglio lana, editor lana) + ViewModel; `ui/components/Glass.kt` = barra a vetro (Haze) |
| `ui/<schermata>` | una coppia `Screen` + `ViewModel` per schermata; `ui/navigation/MajonNavGraph.kt` = unico NavHost |
| `ui/components` | componenti condivisi (`ConfirmDialog`, `EmptyState`, `AnimatedProgressBar`, `KnitArt`, `Effects`) |
| `ui/theme` | tre palette (Cheerful/Classic/Forest) + dark + dynamic color |

Barra inferiore Progetti | Stash: vedi `MajonNavGraph` (rotte radice `home` e `stash`, `LocalBottomBarHeight`).
Il contatore lavora su una **Part** (route `counter/{partId}`), non sul progetto.

## Convenzioni (da rispettare sempre)

- **TDD**: scrivi prima il test (domain pura, ViewModel con `FakeMajonRepository` + Turbine, repository con Room in-memory + Robolectric).
- **Stringhe**: ogni chiave in `values/strings.xml` (IT) **e** `values-en/strings.xml` (EN), stesso nome e stesso ordine. Nomi snake_case con prefisso di area.
- **Room**: ogni modifica di schema ⇒ bump di `version`, `Migration` esplicita aggiunta a `MIGRATIONS` in `data/db/Migrations.kt` (SQL copiato dallo schema `N.json` generato), nuovo `app/schemas/.../N.json`, test in `androidTest/.../MajonDatabaseMigrationTest.kt`. **Mai** `fallbackToDestructiveMigration`.
- **Enum persistiti come stringhe** con fallback a un default se il valore è sconosciuto (`runCatching { valueOf }`), così file e DB di versioni diverse non crashano.
- **Backup JSON**: ogni campo nuovo in `BackupCodec` ha un valore di default (i backup vecchi devono restare leggibili); ids e timestamp non si esportano.
- Ogni metodo nuovo di `MajonRepository` va implementato anche in `app/src/test/.../testutil/FakeMajonRepository.kt`.
- **Moduli di inserimento = schermate a tutto schermo**, mai dialog. La dialog va bene solo per conferme e per **un solo** campo (es. «Ho usato…», «Vai alla riga») o per un selettore di un solo valore (color picker). Da 2 campi in su: schermata. Un form con più sezioni usa `FormSection` (`ui/components/FormSection.kt`) e un ViewModel con `form` (stringhe come digitate) + `save(): Boolean`.
- **Tastiera**: mai deve coprire il campo in scrittura né i suoi suggerimenti (autocomplete): `Modifier.imePadding()` sul contenuto scrollabile; Back con tastiera aperta chiude la tastiera (`KeyboardBackHandler()` in `ui/components/Keyboard.kt`). Da provare a mano.
- Robolectric non supporta SDK 37: pin a 36 in `app/src/test/resources/robolectric.properties`.
- Commit e messaggi in inglese; documentazione in italiano.

## Semantica del contatore

`Part.completedRows` = passi completati, `currentRow = completedRows + 1` (1-based). Il numero mostrato in grande è la riga **in lavorazione**.
Le note per riga (`RowNote`) si riferiscono ai numeri di riga normali 1..`totalRows`.

## Cose non verificabili da unit test

Animazioni, riconoscimento vocale, tasti volume, aspetto delle palette: vanno provate a mano su dispositivo/emulatore.
