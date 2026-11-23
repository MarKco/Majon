# Lavorare in Majon spendendo meno token

Note interne di lavoro, scritte dopo una sessione in cui un solo filone (note per righe, formati `.majon` / `.majon-notes`,
import, condivisione, guida IA) ha consumato quasi metà del contesto. Servono a non ripetere gli stessi giri a vuoto.

## Dove sono andati i token (in ordine)

1. **Test e codice scritti a mano per ogni strato** (modello, DB, codec, ViewModel, schermata, stringhe IT+EN, doc). È il
   costo "giusto", ma conviene scriverli compatti e nello stesso passaggio del codice che provano.
2. **Build/test completi ripetuti** (`testDebugUnitTest` + `lintDebug` + `assembleDebug`) dopo ogni piccolo passo.
3. **Riletture di file grandi** per intero (`BackupCodec`, `CounterViewModel`, schermate) quando serviva una parte.
4. **Errori miei da rifare**: script di modifica falliti a metà, test con attese sbagliate o nomi non validi, un comando appeso.
5. **Esplorazione a mano di un altro progetto** (Proportion): molti output lunghi.

## Regole pratiche

### Build e test
- Un test solo: `./gradlew :app:testDebugUnitTest --tests '*NomeTest'`. La suite intera, `lintDebug` e `assembleDebug`
  **una volta sola, a fine lavoro**. (Gradle è incrementale: una suite intera dopo piccoli cambi dura ~10 s, ma ogni giro
  costa comunque il suo output.)
- Filtrare sempre l'output: `| grep -E "^e:|FAILED|BUILD|Test.*FAILED"`. Per sapere se un test è davvero partito non rilanciare:
  `grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*"' app/build/test-results/testDebugUnitTest/TEST-*Nome*.xml`.
- Un test appena scritto che passa al primo colpo va controllato (conteggio test > 0). Per un test di protezione (es. la
  migrazione) si prova una volta a rompere il codice di proposito e si vede che fallisce.
- Lint: `LocalContext.current.getString(...)` è un **errore** → usa `LocalResources.current`. `stringResource` non si chiama in una
  callback: si legge prima, in composizione.

### Leggere codice
- Mai `cat` di file grandi: `grep -n "fun \|class "` per la mappa, poi `Read` con `offset`/`limit` o `sed -n 'a,bp'`.
- La mappa del codice è già in `CLAUDE.md`; i file per schermata seguono sempre `Screen` + `ViewModel` + test omonimo.
- Chi usa un simbolo: `grep -rn "Simbolo" app/src/main app/src/test` (senza `--include`, vedi sotto).

### Modificare codice
- Per cambi piccoli usa `Edit`. Gli script Python vanno bene per più file, ma: **un file per script**, `s.index(...)` solo su
  ancore che esistono (se fallisce a metà, i file già scritti restano scritti e il resto no), e `assert` prima di scrivere.
- Stringhe: ogni chiave in `values/` e `values-en/`, **stesso ordine**. Si inseriscono tutte con un solo script dopo una chiave
  ancora, e si controlla la parità con tre righe di Python (stessi nomi, stesso ordine).
- Nomi dei test tra apici inversi: niente `: ; / . ,` fra le parole.

### Terminale (macOS / zsh)
- `grep --include=*.kt` dà «no matches found» in zsh: usa `grep -rn pattern cartella` o un percorso preciso.
- `sed -i ''` (con gli apici vuoti), `ls` senza `--time-style`.
- Un `cat > file` senza heredoc **resta appeso** in attesa di input: non scriverlo mai da solo.
- I comandi lunghi (build) hanno timeout di 120 s di default: passa `timeout` più alto o lanciali in background.

### Room, test di aggiornamento
- `app/schemas/.../N.json` si genera compilando. La migrazione si copia da lì (SQL identico) e il test
  `UpgradeFromEveryVersionTest` (Robolectric, JVM, senza emulatore) parte dagli schemi di ogni versione precedente.
  Ad ogni bump aggiungere il caso della versione appena superata.
- I test girano con la cartella `app/` come directory corrente: `File("schemas")`, `File("../docs/examples/...")`.

### Git
- Commit solo se richiesto. Escludere sempre `.idea/*` locali e `.kotlin/`. Messaggi in inglese, con la riga di attribuzione.
- `main` locale può essere avanti/indietro rispetto a `origin/main`: guardare `git log main..origin/main` prima di proporre un push.
  Preferire un merge a un rebase se i commit locali sono già referenziati da altri rami.

## Come organizzare una richiesta grossa

- **Spezzarla**: un filone alla volta (modello+DB → formato+codec → schermate → guida/doc) e committare dopo ciascuno. Il contesto
  resta piccolo e un errore non si porta dietro tutto il resto.
- **Domande subito, tutte insieme**: una o due tornate di `AskUserQuestion` (max 4 domande l'una) prima di scrivere codice costano
  meno di un rifacimento. Marco chiede di solito «domande finché non sei sicura al 99%»: farle, ma raggruppare e proporre un
  default raccomandato.
- **Scope che cresce a metà** (è successo tre volte): fermarsi, dire cosa cambia nel piano, poi proseguire; non aggiungere
  in silenzio.
- **Esplorare un altro repo con un sotto-agente** (o con `grep` mirati), non leggendo i file uno a uno: vedi sotto cosa serve da Proportion.

## Cosa serve da Proportion (non rileggerlo)

Majon ha preso da Proportion queste idee, già portate qui: formato JSON con marcatore e versione, guida per l'IA con esempio, anteprima
prima di importare con **tutti** gli errori insieme (non il primo solo), pulsante «Formatta» che sistema il testo e dà la riga degli
errori, tre filtri di apertura nel manifest (per nome senza tipo, per nome con tipo qualsiasi, per tipo proprio), `FileProvider` per la
condivisione. Nel repo di Proportion i file di riferimento sono la sua guida al formato, il codec di import e la schermata «incolla».
Se serve altro da lì, cercare per nome con `grep -rl` invece di aprire le cartelle.

## Stime utili

| Lavoro | Costo tipico |
|---|---|
| Una schermata di modifica nuova (VM + Screen + test + stringhe IT/EN) | medio |
| Estendere un modello di dominio persistito (DB + migrazione + backup + test di aggiornamento) | alto: farlo in un commit a parte |
| Nuovo formato di file con import/condivisione/guida | molto alto: spezzare in 3–4 richieste |
| Cambio di sola grafica o di un testo | basso: `Edit` + una build |
