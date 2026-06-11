# SPECIFICHE FUNZIONALI - App Contarighe

## 1. Visione e Scopo
L'app "Contarighe" è uno strumento mobile nativo per dispositivi Android che supporta l'utente durante l'esecuzione di progetti di lavoro manuale (principalmente maglieria). L'app consente di tracciare il progresso attraverso il conteggio delle righe, gestire istruzioni speciali per singole righe o intervalli, e mantenere organizzazione di progetti complessi attraverso una struttura gerarchica.

---

## 2. Utenti e Use Cases

### Utente Primario
Persona che realizza progetti di maglieria e necessita di tracciare:
- Progresso di righe lavorate
- Istruzioni speciali per righe specifiche (es. diminuzioni, aumenti)
- Più progetti contemporaneamente
- Percentuale di avanzamento totale

### Use Cases Principali
1. **UC1: Creare un nuovo progetto** - Utente crea un progetto con nome e icona
2. **UC2: Definire parti di un progetto** - Utente suddivide il progetto (davanti, dietro, manica, collo)
3. **UC3: Aggiungere note alle righe** - Utente registra istruzioni speciali per righe specifiche
4. **UC4: Conteggiare righe** - Utente preme un pulsante per registrare il completamento di ogni riga
5. **UC5: Visualizzare istruzioni** - Utente visualizza istruzioni della riga corrente e successiva
6. **UC6: Monitorare progresso** - Utente visualizza percentuale di completamento del progetto
7. **UC7: Consultare informazioni progetto** - Utente visualizza dati come tipo di lana e numero ferro

---

## 3. Requisiti Funzionali

### RF1 - Gestione Progetti
- **RF1.1** L'app deve permettere la creazione di nuovi progetti
- **RF1.2** L'app deve permettere l'eliminazione di progetti
- **RF1.3** L'app deve permettere la modifica del nome di un progetto
- **RF1.4** L'app deve associare a ogni progetto un'icona e un colore
- **RF1.5** L'app deve visualizzare un elenco di tutti i progetti creati
- **RF1.6** L'app deve salvare i progetti in modo persistente

### RF2 - Gestione Parti di Progetto
- **RF2.1** L'app deve permettere di aggiungere più parti a un progetto
- **RF2.2** L'app deve permettere la modifica del nome di una parte
- **RF2.3** L'app deve permettere l'eliminazione di una parte
- **RF2.4** L'app deve associare a ogni parte un conteggio di righe totali
- **RF2.5** L'app deve permettere di visualizzare tutte le parti di un progetto

### RF3 - Contatore di Righe
- **RF3.1** L'app deve visualizzare un pulsante "+" per incrementare il conteggio righe
- **RF3.2** L'app deve incrementare il conteggio riga attuale quando l'utente preme "+"
- **RF3.3** L'app deve visualizzare il numero di riga attuale (es. "Riga 23")
- **RF3.4** L'app deve visualizzare il numero di riga successiva (es. "Prossima riga 24")
- **RF3.5** L'app deve permettere la modifica manuale del numero riga (es. reset)

### RF4 - Gestione Note per Righe
- **RF4.1** L'app deve permettere l'inserimento di note associate a una riga specifica
- **RF4.2** L'app deve permettere di inserire la stessa nota su più righe consecutive (es. "diminuisci di due" da riga 20 a 25)
- **RF4.3** L'app deve permettere di inserire note per righe dispari o pari (pattern basato su regola)
- **RF4.4** L'app deve visualizzare la nota della riga attuale immediatamente prima di iniziare la riga
- **RF4.5** L'app deve visualizzare la nota della prossima riga quando il contatore è sulla riga attuale
- **RF4.6** L'app deve permettere la modifica e l'eliminazione di note
- **RF4.7** L'app deve gestire note su uno stesso numero di riga in una parte diversa (no conflitti tra parti)

### RF5 - Notifiche e Messaggi
- **RF5.1** L'app deve mostrare un messaggio ogni volta che l'utente completa una riga (al press del "+")
- **RF5.2** Il messaggio deve indicare: "Fatta riga X", numero della prossima riga, e note della prossima riga (se presenti)
- **RF5.3** Il messaggio deve essere visibile prima che l'utente inizi la riga successiva
- **RF5.4** L'app deve permettere la dismissione del messaggio per proseguire
- **RF5.5** L'app deve opzionalmente supportare notifiche sonore/haptic feedback

### RF6 - Calcolo Percentuale di Completamento
- **RF6.1** L'app deve permettere l'inserimento del numero totale di righe per ogni parte
- **RF6.2** L'app deve calcolare la percentuale di completamento per ogni parte: (righe_completate / righe_totali) * 100
- **RF6.3** L'app deve calcolare la percentuale di completamento totale del progetto: somma righe completate / somma righe totali di tutte le parti * 100
- **RF6.4** L'app deve visualizzare il progresso tramite barra percentuale e valore numerico
- **RF6.5** L'app deve aggiornare il progresso in tempo reale quando l'utente incrementa il contatore

### RF7 - Icone e Personalizzazione Visiva
- **RF7.1** L'app deve fornire un set di icone predefinite (maglione, sciarpa, berretto, calzini, guanti)
- **RF7.2** L'app deve permettere l'associazione di un'icona a un progetto
- **RF7.3** L'app deve permettere di personalizzare il colore dell'icona per ogni progetto
- **RF7.4** L'app deve supportare una palette di almeno 8 colori (es. verde, rosso, blu, arancione, fucsia, giallo, grigio, nero)
- **RF7.5** L'app deve visualizzare le icone colorate nell'elenco progetti

### RF8 - Informazioni Aggiuntive del Progetto
- **RF8.1** L'app deve permettere l'inserimento di tipo di lana (campo testuale)
- **RF8.2** L'app deve permettere l'inserimento del numero ferro (campo numerico)
- **RF8.3** L'app deve permettere opzionalmente l'inserimento della tipologia di ferro (circolare/dritto) - non prioritario
- **RF8.4** L'app deve visualizzare queste informazioni nella schermata di dettaglio del progetto
- **RF8.5** L'app deve permettere la modifica di queste informazioni in qualsiasi momento

### RF9 - Navigazione e UI
- **RF9.1** L'app deve visualizzare una schermata home con elenco di tutti i progetti
- **RF9.2** L'app deve permitere la navigazione dal progetto alle singole parti
- **RF9.3** L'app deve visualizzare una schermata di conteggio quando un utente seleziona una parte
- **RF9.4** L'app deve permettere il ritorno alla schermata precedente tramite back button
- **RF9.5** L'app deve visualizzare chiaramente lo stato del contatore sulla schermata principale di conteggio
- **RF9.6** L'app deve fornire FAB (Floating Action Button) o pulsante visibile per aggiungere nuovi progetti

### RF10 - Persistenza Dati
- **RF10.1** L'app deve salvare tutti i dati in modo permanente sul dispositivo
- **RF10.2** L'app deve caricare i dati salvati all'avvio dell'app
- **RF10.3** L'app deve sincronizzare i dati in tempo reale (no delay)
- **RF10.4** L'app deve permettere l'esportazione di dati (opzionale, per release future)

---

## 4. Requisiti Non Funzionali

### RNF1 - Performance
- L'app deve rispondere ai comandi entro 200ms
- Il caricamento della lista progetti deve completarsi in < 500ms
- L'incremento del contatore deve registrarsi istantaneamente

### RNF2 - Usabilità
- L'app deve essere usabile con una sola mano (pulsante "+" in posizione comodamente raggiungibile)
- Il testo deve avere una dimensione minima di 14sp
- I pulsanti devono avere dimensione minima di 48x48dp per tocco confortevole

### RNF3 - Compatibilità
- L'app deve supportare Android API level 26 (Android 8.0) come minimo
- L'app deve supportare dispositivi con schermi da 4.5" a 6.7"
- L'app deve supportare sia layout portrait che landscape

### RNF4 - Sicurezza
- I dati personali non sono richiesti
- I dati sono solo locali, nessuna trasmissione online
- L'app non richiede permessi critici (camera, localizzazione, contatti)

---

## 5. Definizioni e Glossario

| Termine | Definizione |
|---------|-----------|
| **Progetto** | Contenitore principale per un lavoro di maglieria completo |
| **Parte** | Suddivisione di un progetto (es. manica, davanti, dietro) |
| **Riga** | Unità di base del conteggio - rappresenta una riga lavorata |
| **Nota** | Istruzione speciale associata a una o più righe specifiche |
| **Ferro** | Numero identificativo degli aghi usati (es. ferro 3, ferro 4) |
| **Icona** | Simbolo visivo che rappresenta il tipo di progetto |
| **Percentuale di completamento** | Rapporto tra righe completate e righe totali previste |

---

## 6. Mockup Logico Flusso

```
[Schermata Home - Lista Progetti]
    ↓ (seleziona progetto)
[Dettaglio Progetto - Lista Parti]
    ↓ (seleziona parte)
[Schermata Conteggio - Contatore Righe]
    ↓ (preme "+")
[Messaggio Conferma - Riga X completata, Prossima riga Y, Note]
    ↓ (dismiss)
[Schermata Conteggio - Contatore Aggiornato]
```

---

## 7. Scenari di Utilizzo Dettagliati

### Scenario 1: Creazione di un progetto di maglieria
1. Utente apre l'app e visualizza la home
2. Utente preme il pulsante "+" per aggiungere un nuovo progetto
3. Utente inserisce il nome (es. "Maglione rosso")
4. Utente seleziona un'icona (maglione)
5. Utente seleziona un colore (rosso)
6. Utente preme "Salva"
7. Il progetto appare nella lista sulla home

### Scenario 2: Aggiunta di parti e istruzioni
1. Utente apre il progetto appena creato
2. Utente preme "Aggiungi parte" e crea:
    - "Davanti" - 120 righe totali
    - "Dietro" - 120 righe totali
    - "Manica sinistra" - 80 righe totali
    - "Manica destra" - 80 righe totali
3. Utente seleziona la parte "Davanti"
4. Utente preme "Aggiungi nota"
5. Utente inserisce: "Righe 1-15: costine semplici"
6. Utente preme "Aggiungi nota" di nuovo
7. Utente inserisce: "Righe 20-30: diminuisci di due ogni riga"
8. Utente seleziona "Righe da 20 a 30" e applica la nota

### Scenario 3: Conteggio durante il lavoro
1. Utente seleziona "Davanti" per iniziare il conteggio
2. L'app visualizza: "Riga 1/120"
3. Utente preme "+" dopo aver completato la prima riga
4. Un messaggio appare: "✓ Fatta riga 1 | Prossima riga 2 | Nota: Costine semplici"
5. Utente chiude il messaggio (tap)
6. Il contatore passa a "Riga 2/120"
7. La percentuale di completamento della parte aggiorna: 0.8%
8. La percentuale totale del progetto aggiorna: 0.2%
9. Processo si ripete per ogni riga...

### Scenario 4: Nota con regola
1. Utente accede alla parte "Davanti"
2. Utente vuole aggiungere una nota per "righe dispari" con testo "fila a punto diritto"
3. Utente preme "Aggiungi nota con regola"
4. Utente seleziona il tipo di regola: "Righe dispari"
5. Utente inserisce la nota: "fila a punto diritto"
6. Sistema applica automaticamente la nota a tutte le righe dispari
7. Durante il conteggio, quando utente raggiunge una riga dispari (es. riga 15), visualizza la nota

---

## 8. Eccezioni e Casi Limite

| Caso | Comportamento Atteso |
|------|---------------------|
| Cancellazione di un progetto | Deve richiedere conferma; cancellare permanentemente tutte le parti e dati associati |
| Reset contatore | Deve richiedere conferma; reimpostare a 0 mantenendo le note |
| Nota assegnata a riga oltre il totale | L'app deve mostrare avvertimento; non permettere salvataggio se fuori range |
| Modifica totale righe di una parte | Se ridotto, mostrare avvertimento se righe completate superano il nuovo totale |
| App chiusura durante conteggio | Lo stato deve essere salvato automaticamente; al riavvio, riprendere da dove si era lasciato |

---

## 9. Versione e Changelog
- **Versione** 1.0
- **Data** 2025-11-13
- **Status** Initial specification