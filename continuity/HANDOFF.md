# MATRIX Architect — rimozione limite schematic, 30 ottobre 2026

Codice limite: 55e63dd9567213104301667248c855c5000f8639; revisione finale e harness runtime: fc5505a7dca275166d1e16a11a19dff9fd7e17a1. Rimosso il tetto arbitrario di2milioni posizioni dal lettore WorldEdit. Non si ridimensiona ne altera il contenuto del file: dimensioni e origine conservate. Console: dimensioni, volume inclusa aria, RAM Java disponibile. SchematicCapacity verifica volume positivo e riserva heap di max(128MiB,10% massimo heap) dopo lettura e prima della posa; NON e una stima del picco di memoria ne della cronologia. OutOfMemoryError del lettore convertito in errore leggibile senza avviare posa. Limite64MB compressi resta. Nuovi test: grandi volumi ammessi con memoria simulata, RAM insufficiente rifiutata, lettura/scrittura WorldEdit reale oltre2milioni con origine conservata.

Limiti mantenuti: file intero in RAM, incolla sincrono, history undo della sessione non persistente; area deve avere chunk caricati, altezza e confine validi, entita mobili rifiutate. Non dichiarare supporto illimitato, caricamento fuori memoria o posa progressiva. Nessuna modifica alle altre mod/JAR.

Prossimo test utente: sostituire JAR, riavviare, F10 carica stesso San Siro e controllare dimensioni/RAM. Non dichiarare testato quel file o il mondo sul PC. Se errore chunk non caricati, occorre affrontare preparazione chunk e posa a lotti; se RAM insufficiente, misurare heap effettivo di Prism prima di suggerire allocazioni sul PC10GB. Tasti F6/F7/F9/F10/F12 restano senza conflitti come revisione precedente.

Verifiche: CI41063672780, compilazione e test memoria/browser/tasti/rotazione passati; runtime Minecraft1.20.1 Fabric WorldEdit7.2.15 passato (marker FABRIC_WORLDEDIT_LARGE_SCHEMATIC_PASS): clipboard201x51x201=2.060.451 posizioni aria, writer/reader Sponge con dimensioni e origine(9,3,7) conservate. NON prova materiali o posa. JAR e checksum pubblicati in downloads; SHA256 d2839d2200a3c0dbb3f8ce69c01ffb785d06b26581279686804a2827f291bcb3  MATRIX-Architect-v0.1.0-Minecraft-1.20.1-Fabric.jar. Gli harness precedenti fallivano per piattaforma non inizializzata e registri troppo precoci: corretti avviando il controllo al primo END_SERVER_TICK; processo test isolato termina con halt solo se property matrixarchitect.capacity-test e attiva. Timeout CI15min e runtime180s; build obsolete cancellate automaticamente. Nessun JAR fallito pubblicato.

## Ripresa obbligatoria
Recupera MATRIXNEO23/mine-builder main, leggi AGENTS.md, README e continuity/PROTOCOL.md, STATE, ultimo CHECKPOINT e questo HANDOFF; verifica HEAD, CI41063672780 e JAR/checksum attuali. Prosegui dal collaudo utente di caricamento San Siro; conserva file, scala, dettagli e altre mod. Non confondere importazione verificata con posa in-world riuscita. Non creare rami separati senza richiesta.

---

## Archivio storico

# Comandi senza conflitti e blocco grandi schematic — 30 ottobre 2026

Segnalazioni utente: il caricamento di una struttura grande viene rifiutato per ingombro oltre2milioni; il tasto P apre Interazioni sociali. L'utente richiede tasti non usati da Minecraft. CORRETTO in questa revisione solo il gruppo di tasti; il limite2milioni rimane un open loop prioritario, non dichiararlo risolto o chiedere di ridurre file/scala.

Nuovi default MATRIX Architect: F10 pannello, F7 ruota, F6 posiziona, F9 undo, F12 sblocca. Frecce e PgUp/PgDn restano spostamento/altezza. Migrazione delle vecchie assegnazioni K/R/P/U/L al primo tick, comprese quelle salvate in options; controllo dei tasti effettivamente occupati da Minecraft/altre mod e selezione alternativa libera (tasti funzione o tastierino). Se tutte le alternative sono occupate il comando resta non assegnato. Personalizzazioni senza conflitti conservate; quelle in conflitto disabilitate nell'addon senza modificare i controlli delle altre funzioni. HUD mostra assegnazioni effettive anziche testo fisso. Verificare sul PC la migrazione e le opzioni; i test non avviano la GUI.

Codice `50010bd973d965426d7b328936977de1a8570054`; CI `41057564097` SUCCESS (test selezione tasti liberi, browser file e rotazioni). JAR aggiornato in downloads, checksum `e60e56514f249441db6582d3172e2869b3178a4a9b2ab0f89a1f8fe9e750ac91  MATRIX-Architect-v0.1.0-Minecraft-1.20.1-Fabric.jar`. Installare sostituendo vecchio JAR e riavviando. Nessun cambiamento a geometria/posa o limite d'ingombro in questa correzione.

Prossimo prioritario: gestione configurabile e sostenibile degli schematic grandi senza ridimensionamento; riesaminare caricamento in RAM, storia undo e incolla sincrono WorldEdit prima di dichiarare supporto scalabile. L'errore sul limite viene emesso dopo lettura clipboard e prima della posa: non e prova che il file sia invalido. Conservare separatamente mod Mine Builder, Blueprint Builder e MATRIX Architect.

---

# Correzione selettore schematic — 30 ottobre 2026

Utente: Carica schematic non apre alcun selettore sul suo PC. Sostituito AWT FileDialog con browser file nativo della GUI Minecraft: cartella personale, Downloads, dischi/unita, cartella superiore, cartelle e soli .schem/.schematic, paginazione, selezione e pulsante Apri. Nessuna digitazione percorso richiesta. Lettura directory asincrona; errori cartelle non accessibili visibili; cancel non avvia import. Il vecchio campo percorso e stato choosing sono stati rimossi. Trascinamento file resta disponibile.

Codice `e5f5ddfde61114e2788ced698633a9aa2fa708af`, run `41054486841`: compilazione e verifyFileBrowser riusciti (directory con spazi, filtro estensioni case-insensitive, ordine cartelle/file, cartella inesistente e elenco dischi); test rotazioni esistente riuscito. JAR aggiornato e checksum verificato: `5b6fb3eed16bcfffff086f5871a6ca67ad9269c99d44ce2caac904d99136f108  MATRIX-Architect-v0.1.0-Minecraft-1.20.1-Fabric.jar`. Il test UI sul PC utente resta necessario. Il nome JAR rimane invariato: sostituire il vecchio e riavviare Minecraft, K → Carica schematic → cartelle → seleziona file → Apri schematic selezionato.

---

# MATRIX Architect — addon WorldEdit con bacchetta, 30 ottobre 2026

Utente: creare una mod esterna che utilizza WorldEdit, senza modificarlo o rinominarlo; strumento apposito in mano, movimento libero durante posizionamento, ruota/sposta/conferma nel mondo, console ed errori. Mantenere JAR nella cartella downloads. Non confondere addon con Blueprint Builder (JSON) o Mine Builder (GPT).

Nuovo sottoprogetto `matrix-architect/`, ID `matrixarchitect`, dipendenza WorldEdit separata. Minecraft 1.20.1 Fabric Java17, WorldEdit 7.2.15 Fabric verificato (Modrinth `Wrlqaul6`, sorgente ufficiale EngineHub commit5ca4dff). JAR utente dichiarato caricato non trovato né su main né sul ramo composite: non affermare di averlo analizzato.

Implementato: bacchetta inventario creativo e pulsante di consegna in Creativa; K pannello con selettore file/percorso/drag, registro e copia testo. Mano principale: puntamento entro64 blocchi e ingombro verde (NON anteprima di tutti i blocchi), clic destro blocca punto, R ruota90, frecce X/Z, PgUp/Dn altezza, L sblocca e azzera altezza, P conferma posa. K o Maiusc+clic riapre pannello. Posa tramite WorldEdit API ClipboardReader/EditSession/PasteBuilder, include aria per sovrascrivere terreno; storico registrato anche per errori parziali. U annulla ultima operazione WorldEdit della sessione, anche esterna all'addon. Permessi WorldEdit rispettati, solo singleplayer. Le pressioni dei tasti inattivi vengono consumate per evitare incolla differito dopo aver selezionato la bacchetta.

CI `41045382482` SUCCESS, codice `cd1fd44539b75b66ed479bfdf389bca6532d9024`: compilazione e test comparativo fra matematica ingombro e AffineTransform WorldEdit per tutte le rotazioni. Nessun test client visuale o posa reale nel mondo.

JAR verificato in `downloads/MATRIX-Architect-v0.1.0-Minecraft-1.20.1-Fabric.jar`, 22012 byte; SHA256 `f68d6eb37f671705311555c017d9007f44058c4eaaef270d8d872110bea11cae`. Checksum nella stessa cartella; release autonoma `matrix-architect-latest`. WorldEdit non incluso: installare il JAR ufficiale separatamente. Gli altri due JAR restano intatti.

Limiti: max64MB file e2milioni posizioni, entita mobili nello schematic rifiutate; chunk intero ingombro devono essere caricati e altezza/confine validi prima della posa. Posa WorldEdit sincrona sul server: log inizio/fine/errori, non contatore progressivo inventato, nessuna cancellazione durante esecuzione. Undo e cronologia sessione, NON backup persistente dopo crash. L'addon non aggiunge un backup mondo. Qualita/fedelta del file non corretta automaticamente. Test prossimi: nuovo mondo con comandi, carica file piccolo, impugna bacchetta, verifica ingombro a0/90/180/270, sposta/altezza, P e U, area con terreno, file invalido, chunk distanti, layout UI; poi strutture grandi.

---

# Prompt di ripresa per una nuova istanza

Copia integralmente questo testo in una nuova conversazione quando vuoi riprendere Mine Builder:

> Riprendi il progetto **Mine Builder** nella repository GitHub **MATRIXNEO23/mine-builder**, ramo `main`. NON partire dal contesto di questa chat e NON reinventare il progetto. Prima di modificare o rispondere sullo stato, leggi **README.md**, **AGENTS.md**, **continuity/PROTOCOL.md**, **continuity/STATE.md**, **continuity/CHECKPOINTS.md** (ultimo checkpoint) e **continuity/HANDOFF.md**. Controlla l'HEAD reale, i file e gli eventuali test; se qualcosa non coincide, riconcilia senza sovrascrivere. Mantieni tutti i vincoli: **zero costi aggiuntivi**, prima versione con **GPT realmente connesso tramite il piano ChatGPT disponibile**, Minecraft Java **1.20.1 / Fabric / Prism Launcher**, Banco IA, costruzioni reali 1:1 e originali, mondo locale giocabile offline e checkpoint di costruzione. Continua **esattamente dal prossimo passo irrisolto in continuity/STATE.md**, distinguendo realizzato, testato e ancora da fare. A ogni milestone aggiorna memoria e checkpoint. Quando scrivo **«fine istanza»**, applica `continuity/PROTOCOL.md`: aggiorna lo stato GitHub, registra un checkpoint, rigenera questo prompt e riportami il commit confermato. Se non puoi leggere o scrivere la repo, dichiaralo: non inventare progressi né salvataggi.

**Nota:** questo file è un promemoria persistente, non garantisce che le nuove conversazioni aprano automaticamente GitHub. Il prompt deve essere fornito alla nuova istanza oppure un agente configurato per il repository deve leggere `AGENTS.md`.
