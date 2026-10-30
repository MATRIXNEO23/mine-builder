# Checkpoint cronologici — Mine Builder

## CP-2026-10-29-001 — 29 ottobre 2026 — baseline della memoria di progetto

- **Ramo**: `main`.
- **Commit verificato prima del checkpoint**: `e837a7a0f5e158b84c9b9cdfb09c66ef835439d8`.
- **Contenuto verificato allora**: `README.md`, `docs/INSTALLAZIONE.md`, `scripts/verify-plan-sharing.mjs`. Nessun modulo Fabric compilabile o release.
- **Decisioni utente confermate**: Prism Launcher, Minecraft Java 1.20.1, Fabric + Fabric API funzionanti nel menu; Banco IA conversazionale; GPT collegato realmente fin dal primo MVP; zero costi aggiuntivi; mondo offline; costruzioni in sezioni con recupero; strutture reali 1:1 e creative inventate; GitHub per sviluppo.
- **Problema rilevato**: README cita un nome di script inesistente (`scripts/gpt-plan-smoke-test.mjs`); file presente `scripts/verify-plan-sharing.mjs`.
- **Operazione corrente**: introduzione di `AGENTS.md`, `continuity/PROTOCOL.md`, `continuity/STATE.md`, `continuity/CHECKPOINTS.md`, `continuity/HANDOFF.md`; rimane da verificare i commit con una lettura successiva.
- **Test reali**: utente ha confermato avvio del gioco con Fabric API; non ci sono prove di test mod, connessione account, compilazione o costruzione.
- **Next**: baseline Fabric compilabile; validazione ufficiale autenticazione GPT senza spese; primo Banco IA e costruzione automatica; checkpoint successivo dopo test.
- **Divieto di inferenza**: questo checkpoint descrive una progettazione e una preparazione, non una mod installabile.

> Nuovi checkpoint: aggiungere in coda, senza sovrascrivere quelli precedenti. Ogni voce deve specificare risultati verificati, artefatti e prossimo passo. Il commit dell'operazione è recuperabile dallo storico GitHub.

## CP-2026-10-29-002 — 29 ottobre 2026 — configurazione Fabric e gate inferenza

- File confermati nel repository: build.gradle, settings.gradle, gradle.properties, workflow .github/workflows/build.yml, sorgenti iniziali Fabric, scripts/verify-plan-sharing.mjs e test mock.
- Commit codice prima del checkpoint: 8be5d3bd1c03ea599e089bdf1b7b583c00da48e9.
- Test e compilazione: nessun risultato CI confermato; non dichiarare build riuscita.
- Login OAuth e inferenza ChatGPT Plus: non implementati e non verificati.
- Prossimo passo: verificare GitHub Actions, poi bridge OAuth con SDK ufficiale e prova autorizzata senza crediti aggiuntivi.


## CP-2026-10-30-008 — Blueprint Builder distinto e download permanente

# Nuova mod autonoma Blueprint Builder — 30 ottobre 2026

L'utente ha autorizzato una mod distinta che importa blueprint esterni, senza sovrascrivere Mine Builder, e richiede il JAR sempre nella cartella downloads. Correzione esplicita: deve sovrascrivere il terreno, non saltare blocchi occupati.

Implementato in `blueprint-builder/` su main: ID `blueprintbuilder`, package distinto, Java 17/Fabric/Minecraft 1.20.1. Tasto B rimappabile; selettore file nativo con percorso manuale e trascinamento come alternative; JSON/JSON.GZ; console affiancata; coordinate e rotazione; comandi abilitati per stato; posa a lotti, pausa, ripresa da disco, backup e ripristino. Nessun OAuth, GPT o rete nella nuova mod. Il codice e il JAR della mod Mine Builder precedente non sono sostituiti.

Sovrascrittura: l'intero ingombro rettangolare viene preparato (aria nelle celle non specificate), fino a 2 milioni di posizioni. Preflight e backup completo del volume inclusi block entity NBT prima della prima mutazione. Il backup fallito impedisce la posa; modifiche successive allo snapshot causano arresto esplicito, non buchi ignorati. Backup su disco e ripristino implementati, non ancora collaudati in un mondo reale.

CI `41039395919` SUCCESS sulla revisione di codice `203cf1ba10ad79e55e4ced8be18c03a233eeebfd`: build, test contratto import, test nel runtime Fabric con registro Minecraft reale (slab superiore/inferiore, scala est, hopper, quattro rotazioni e rifiuto proprietà invalida). Questo NON è un test della posa in un mondo o della UI sul PC utente.

JAR verificato presente in `downloads/Blueprint-Builder-v0.1.0-Minecraft-1.20.1-Fabric.jar`, 54159 byte; SHA256 `446a80124a9f21e18c398823a86e15dd7ac48fb637c6d7f91f307741a59621a4`. Checksum accanto al JAR. Release separata `blueprint-builder-latest`. Il workflow autonomo aggiorna entrambi dopo tutti i test. Esempio: `blueprint-builder/examples/test-pedestal.json`.

Limiti: solo singleplayer; .schem/.litematic/.nbt non supportati; file max64 MB decompressi, ingombro max2 milioni; NBT/entità nel blueprint esterno rifiutati; i backup conservano gli NBT dei blocchi presenti ma non entità mobili, tick o inventari giocatore. Chunk lontani richiedono avvicinamento. Stati dinamici, fluidi, gravità, UI a risoluzioni basse, memoria su i3, ripresa/crash e ripristino vanno provati; la verifica finale confronta i blocchi ma non dimostra stabilità futura di ogni macchina/redstone.

Prossimo: installare il nuovo JAR accanto o separatamente a Mine Builder; aprire un mondo di prova, B, importare test-pedestal.json, costruire e confrontare slab/scala/hopper; poi testare sovrascrittura terreno, pausa/ripresa e backup. Per generazioni grandi e fedeltà reale servono blueprint di qualità; non usare CI verde come certificazione architettonica.

Mine Builder originale con generazione GPT: il ramo `feature/composite-large-builds-20261030` resta distinto e non è stato fuso da questa operazione. Le sue precedenti correzioni UI sono nel checkpoint CP-2026-10-30-007. L'utente non aveva richiesto quel ramo; non attribuirgli questa scelta.

---



## CP-2026-10-30-009 — MATRIX Architect WorldEdit addon

# MATRIX Architect — addon WorldEdit con bacchetta, 30 ottobre 2026

Utente: creare una mod esterna che utilizza WorldEdit, senza modificarlo o rinominarlo; strumento apposito in mano, movimento libero durante posizionamento, ruota/sposta/conferma nel mondo, console ed errori. Mantenere JAR nella cartella downloads. Non confondere addon con Blueprint Builder (JSON) o Mine Builder (GPT).

Nuovo sottoprogetto `matrix-architect/`, ID `matrixarchitect`, dipendenza WorldEdit separata. Minecraft 1.20.1 Fabric Java17, WorldEdit 7.2.15 Fabric verificato (Modrinth `Wrlqaul6`, sorgente ufficiale EngineHub commit5ca4dff). JAR utente dichiarato caricato non trovato né su main né sul ramo composite: non affermare di averlo analizzato.

Implementato: bacchetta inventario creativo e pulsante di consegna in Creativa; K pannello con selettore file/percorso/drag, registro e copia testo. Mano principale: puntamento entro64 blocchi e ingombro verde (NON anteprima di tutti i blocchi), clic destro blocca punto, R ruota90, frecce X/Z, PgUp/Dn altezza, L sblocca e azzera altezza, P conferma posa. K o Maiusc+clic riapre pannello. Posa tramite WorldEdit API ClipboardReader/EditSession/PasteBuilder, include aria per sovrascrivere terreno; storico registrato anche per errori parziali. U annulla ultima operazione WorldEdit della sessione, anche esterna all'addon. Permessi WorldEdit rispettati, solo singleplayer. Le pressioni dei tasti inattivi vengono consumate per evitare incolla differito dopo aver selezionato la bacchetta.

CI `41045382482` SUCCESS, codice `cd1fd44539b75b66ed479bfdf389bca6532d9024`: compilazione e test comparativo fra matematica ingombro e AffineTransform WorldEdit per tutte le rotazioni. Nessun test client visuale o posa reale nel mondo.

JAR verificato in `downloads/MATRIX-Architect-v0.1.0-Minecraft-1.20.1-Fabric.jar`, 22012 byte; SHA256 `f68d6eb37f671705311555c017d9007f44058c4eaaef270d8d872110bea11cae`. Checksum nella stessa cartella; release autonoma `matrix-architect-latest`. WorldEdit non incluso: installare il JAR ufficiale separatamente. Gli altri due JAR restano intatti.

Limiti: max64MB file e2milioni posizioni, entita mobili nello schematic rifiutate; chunk intero ingombro devono essere caricati e altezza/confine validi prima della posa. Posa WorldEdit sincrona sul server: log inizio/fine/errori, non contatore progressivo inventato, nessuna cancellazione durante esecuzione. Undo e cronologia sessione, NON backup persistente dopo crash. L'addon non aggiunge un backup mondo. Qualita/fedelta del file non corretta automaticamente. Test prossimi: nuovo mondo con comandi, carica file piccolo, impugna bacchetta, verifica ingombro a0/90/180/270, sposta/altezza, P e U, area con terreno, file invalido, chunk distanti, layout UI; poi strutture grandi.

---



## CP-2026-10-30-010 — selettore integrato Minecraft

# Correzione selettore schematic — 30 ottobre 2026

Utente: Carica schematic non apre alcun selettore sul suo PC. Sostituito AWT FileDialog con browser file nativo della GUI Minecraft: cartella personale, Downloads, dischi/unita, cartella superiore, cartelle e soli .schem/.schematic, paginazione, selezione e pulsante Apri. Nessuna digitazione percorso richiesta. Lettura directory asincrona; errori cartelle non accessibili visibili; cancel non avvia import. Il vecchio campo percorso e stato choosing sono stati rimossi. Trascinamento file resta disponibile.

Codice `e5f5ddfde61114e2788ced698633a9aa2fa708af`, run `41054486841`: compilazione e verifyFileBrowser riusciti (directory con spazi, filtro estensioni case-insensitive, ordine cartelle/file, cartella inesistente e elenco dischi); test rotazioni esistente riuscito. JAR aggiornato e checksum verificato: `5b6fb3eed16bcfffff086f5871a6ca67ad9269c99d44ce2caac904d99136f108  MATRIX-Architect-v0.1.0-Minecraft-1.20.1-Fabric.jar`. Il test UI sul PC utente resta necessario. Il nome JAR rimane invariato: sostituire il vecchio e riavviare Minecraft, K → Carica schematic → cartelle → seleziona file → Apri schematic selezionato.

---



## CP-2026-10-30-011 — tasti senza conflitti; limite grande aperto

# Comandi senza conflitti e blocco grandi schematic — 30 ottobre 2026

Segnalazioni utente: il caricamento di una struttura grande viene rifiutato per ingombro oltre2milioni; il tasto P apre Interazioni sociali. L'utente richiede tasti non usati da Minecraft. CORRETTO in questa revisione solo il gruppo di tasti; il limite2milioni rimane un open loop prioritario, non dichiararlo risolto o chiedere di ridurre file/scala.

Nuovi default MATRIX Architect: F10 pannello, F7 ruota, F6 posiziona, F9 undo, F12 sblocca. Frecce e PgUp/PgDn restano spostamento/altezza. Migrazione delle vecchie assegnazioni K/R/P/U/L al primo tick, comprese quelle salvate in options; controllo dei tasti effettivamente occupati da Minecraft/altre mod e selezione alternativa libera (tasti funzione o tastierino). Se tutte le alternative sono occupate il comando resta non assegnato. Personalizzazioni senza conflitti conservate; quelle in conflitto disabilitate nell'addon senza modificare i controlli delle altre funzioni. HUD mostra assegnazioni effettive anziche testo fisso. Verificare sul PC la migrazione e le opzioni; i test non avviano la GUI.

Codice `50010bd973d965426d7b328936977de1a8570054`; CI `41057564097` SUCCESS (test selezione tasti liberi, browser file e rotazioni). JAR aggiornato in downloads, checksum `e60e56514f249441db6582d3172e2869b3178a4a9b2ab0f89a1f8fe9e750ac91  MATRIX-Architect-v0.1.0-Minecraft-1.20.1-Fabric.jar`. Installare sostituendo vecchio JAR e riavviando. Nessun cambiamento a geometria/posa o limite d'ingombro in questa correzione.

Prossimo prioritario: gestione configurabile e sostenibile degli schematic grandi senza ridimensionamento; riesaminare caricamento in RAM, storia undo e incolla sincrono WorldEdit prima di dichiarare supporto scalabile. L'errore sul limite viene emesso dopo lettura clipboard e prima della posa: non e prova che il file sia invalido. Conservare separatamente mod Mine Builder, Blueprint Builder e MATRIX Architect.

---

