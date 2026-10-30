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
