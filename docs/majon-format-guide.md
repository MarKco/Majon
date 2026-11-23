# Formati `.majon` e `.majon-notes`

Due formati JSON per scambiare lavori fra persone e per far scrivere le note da un assistente IA.
Il codice è in `data/export` (`BackupCodec`, `NotesCodec`, `NoteWire`); i file d'esempio, accettati dai
test (`FormatExamplesTest`), sono in `docs/examples/`. La guida che l'app dà all'IA è in
`app/src/main/res/raw/notes_guide.txt` (italiano) e `raw-en/notes_guide.txt` (inglese): **se cambi il formato
aggiornale insieme a questo file.**

## `.majon` — un progetto (o tutto il backup)

Il backup completo e il progetto condiviso sono lo stesso formato: cambia solo quanto c'è dentro.

```json
{
  "app": "majon",
  "version": 3,
  "exportedAt": 1795000000000,
  "yarns": [ ... ],
  "needles": [ ... ],
  "projects": [ ... ]
}
```

- `app` è esattamente `"majon"`; `version` è 3 (i file delle versioni 1 e 2 si leggono ancora).
- `yarns` e `needles` sono lo stash a cui il file fa riferimento. I progetti li citano **per posizione** nella lista
  (0 = il primo), mai per id: `"yarns": [{ "yarn": 0, "primary": true, "swatch": 0 }]`, `"needles": [0]`.
  `swatch` è la posizione nella lista piatta di tutti i campioni di tutte le lane, nell'ordine del file.
- Ogni campo che non c'è prende un valore di default: un file scritto a mano può essere minimo.
  Gli id e le date non si esportano.

### Progetto

`name` (obbligatorio), `icon` (`SWEATER`, `SCARF`, `HAT`… altrimenti `YARN`), `color`, `yarnType`, `needleSize`,
`parts`, `yarns`, `needles`, `finishing`.

- **Parte**: `name`, `totalRows` (0 = righe ancora da definire), `completedRows` (avanzamento),
  `copies` (quante volte si lavora, per due maniche = 2) e `currentCopy`, `hasPrepRow`/`hasFinalRow` con
  `prepNote`/`finalNote`, `castOnStitches` (maglie da avviare), `stitchMeasure`/`stitchCm`,
  `endStitchCm` (misura finale se la parte si allarga o restringe), `rowMeasure`/`rowCm`, `swatch`, `notes`.
  `stitchMeasure` è `WIDTH` o `CIRCUMFERENCE`, `rowMeasure` è `LENGTH` o `HEIGHT`; scambiati = parte lavorata di lato.
- **Rifinitura** (`finishing`): `{ "title": "Nascondere i fili", "done": false }`, nell'ordine in cui compaiono.
- **Lana**: `name`, `brand`, `colorName`, `colorArgb`, `weight`, `materials`, `skeinWeightG`, `skeinLengthM`, `care`,
  `swatches` (`stitches`, `rows`, `needleSizeMm`, `pattern`, `weightG`) e, solo nel backup, `stockSkeins`,
  `pricePerSkein`, `shop`, `dyeLot`, `notes`.
- **Ferro**: `type`, `material`, `sizeMm`, `lengthCm`, `quantity`, `brand`, `notes`. `"owned": false` (nei file
  condivisi) vuol dire «il progetto lo chiede, tu non ce l'hai»: importato, ha quantità 0.

### Condividere

Dal menu del progetto → «Condividi», con due scelte: **come modello** (nessun avanzamento: righe fatte a zero,
copia 1, rifiniture da fare) o **con il mio avanzamento**. Nel file ci sono solo le lane e i ferri che il progetto
usa, e **mai** scorta, prezzo, negozio, lotto di tintura e note personali.

### Importare

L'importazione **riusa** quello che lo stash ha già: una lana con stesso nome, marca e colore, un campione con
stessi punti/righe/ferro/punto, un ferro con stesso tipo/misura/lunghezza. Le lane e i ferri mancanti si creano
(a scorta 0 se il file è condiviso). Importare due volte lo stesso file non duplica lo stash.

## `.majon-notes` — solo le note

```json
{
  "app": "majon-notes",
  "version": 1,
  "parts": [
    {
      "part": "Dietro",
      "prepNote": "Avvia 164 maglie",
      "finalNote": null,
      "notes": [ { "rowStart": 1, "rowEnd": 40, "text": "Coste 1/1" } ]
    }
  ]
}
```

Le note sono raggruppate per **nome della parte**; chi importa sceglie il progetto e, per ogni parte del file, se
aggiungerla a una parte esistente (di default quella con lo stesso nome, senza badare a maiuscole e spazi), farne
una nuova o saltarla. Le note si **aggiungono**, non si toglie mai niente: quelle già presenti (stesse righe,
frequenza e testo) non si ripetono.

`app` e `version` si possono anche omettere. Il testo può arrivare dentro un blocco di codice con parole attorno
(come risponde una chat): l'app prende il JSON.

### Una nota

`text` è obbligatorio. Le righe si dicono in uno di **quattro modi** (le righe partono da 1):

| Modo | Campi | Esempio |
|---|---|---|
| Una riga | `rowStart` | `{ "rowStart": 7, "text": "Aumenta" }` |
| Intervallo | `rowStart`, `rowEnd`, `frequency` (`EVERY_ROW` di default, `ODD_ROWS`, `EVEN_ROWS`) | `{ "rowStart": 41, "rowEnd": 180, "frequency": "EVEN_ROWS", "text": "A rovescio" }` |
| Righe precise | `rows` | `{ "rows": [10, 20, 30], "text": "Segna" }` |
| Ogni N righe | `every` (almeno 2), `rowStart` (default 1), `rowEnd` (default: ultima riga della parte) | `{ "every": 6, "rowStart": 12, "text": "Diminuisci" }` |

Nel dubbio il tipo si ricava dai campi: `rows` ⇒ righe precise; `every` ≥ 2 ⇒ ogni N; altrimenti intervallo.
`frequency` può anche dire `SPECIFIC` o `EVERY_NTH` per esteso.

Il `.majon` scrive le note delle sue parti nello stesso modo: un `.majon` può essere importato «solo le note».

### Controlli (rigorosi: se c'è un errore, non si importa niente)

L'app mostra **tutti** gli errori insieme, con la riga del testo (dopo «Formatta e controlla») e il nome della
parte e il numero della nota: testo vuoto, riga 0 o negativa, riga finale prima dell'iniziale, `every` sotto 2,
`rows` vuoto, frequenza sconosciuta, parte senza nome, JSON non valido, versione più recente, file di un altro tipo.
L'elenco si può copiare per rimandarlo all'IA.

Non sono errori ma **avvisi** (la nota si importa lo stesso): una riga oltre l'ultima della parte (non si vedrà finché
non si allunga la parte), una nota per la riga 0 o finale in una parte che non l'ha (quella nota non si importa).
È invece un errore un «ogni N righe» senza riga finale in una parte senza righe.

## Il testo per l'IA

Dalle Impostazioni (generico) o dal menu di un progetto (con i nomi e le righe delle sue parti) → «Guida IA per le
note»: si copia il testo, lo si incolla in una chat insieme alle istruzioni del modello, poi si incolla la risposta in
Importa. Il testo spiega il formato, i quattro modi di dire le righe, come leggere le istruzioni («ogni 6 ferri»,
«ai ferri dispari», «ferri 41-180») e chiede di rispondere solo con il blocco di codice. Un test
(`NotesGuideTest`) controlla che l'esempio dentro la guida sia accettato dal parser.
