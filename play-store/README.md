# Asset Play Store

Tutto il materiale per la scheda Play Store di Majon.

## Contenuto

| Percorso | Cosa | Requisito Play |
|---|---|---|
| `icon/ic_launcher-playstore-512.png` | Icona store | 512×512 PNG, ≤1MB, niente angoli arrotondati (la mask la applica Google) |
| `feature-graphic/feature-graphic-it.png` | Immagine in evidenza IT | 1024×500 PNG |
| `feature-graphic/feature-graphic-en.png` | Immagine in evidenza EN | 1024×500 PNG |
| `screenshots/it/0*.png` | 5 screenshot incorniciati IT | 1080×1920 (9:16), pronti per upload |
| `screenshots/en/0*.png` | 5 screenshot incorniciati EN | 1080×1920 (9:16), pronti per upload |
| `screenshots/*/raw-*.png` | Screenshot grezzi 1080×2280 | sorgenti, non caricarli |
| `listing-it.md` | Titolo, descrizione breve e completa IT | limiti verificati |
| `listing-en.md` | Titolo, descrizione breve e completa EN | limiti verificati |
| `src/*.svg` | Sorgenti vettoriali di icone e gomitolo | rasterizzare con `magick -density 1200 -background none src/majon_play_icon.svg -resize 512x512 PNG32:out.png` |

## Upload

1. Play Console → Presenza sullo store → Scheda principale
2. Lingua predefinita consigliata: italiano; aggiungere inglese come traduzione
3. Caricare per ogni lingua: titolo + descrizioni da `listing-*.md`, screenshot dalla cartella corrispondente, feature graphic localizzata
4. L'icona store è unica per tutte le lingue

Screenshot ripresi da emulatore Pixel 4 con dati dimostrativi
("Maglione a righe" con 3 parti e note attive sulla riga 19, "Sciarpa arcobaleno").
