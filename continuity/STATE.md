# MATRIX Architect — rimozione limite schematic, 30 ottobre 2026

Codice limite: 55e63dd9567213104301667248c855c5000f8639; revisione finale e harness runtime: fc5505a7dca275166d1e16a11a19dff9fd7e17a1. Rimosso il tetto arbitrario di2milioni posizioni dal lettore WorldEdit. Non si ridimensiona ne altera il contenuto del file: dimensioni e origine conservate. Console: dimensioni, volume inclusa aria, RAM Java disponibile. SchematicCapacity verifica volume positivo e riserva heap di max(128MiB,10% massimo heap) dopo lettura e prima della posa; NON e una stima del picco di memoria ne della cronologia. OutOfMemoryError del lettore convertito in errore leggibile senza avviare posa. Limite64MB compressi resta. Nuovi test: grandi volumi ammessi con memoria simulata, RAM insufficiente rifiutata, lettura/scrittura WorldEdit reale oltre2milioni con origine conservata.

Limiti mantenuti: file intero in RAM, incolla sincrono, history undo della sessione non persistente; area deve avere chunk caricati, altezza e confine validi, entita mobili rifiutate. Non dichiarare supporto illimitato, caricamento fuori memoria o posa progressiva. Nessuna modifica alle altre mod/JAR.

Prossimo test utente: sostituire JAR, riavviare, F10 carica stesso San Siro e controllare dimensioni/RAM. Non dichiarare testato quel file o il mondo sul PC. Se errore chunk non caricati, occorre affrontare preparazione chunk e posa a lotti; se RAM insufficiente, misurare heap effettivo di Prism prima di suggerire allocazioni sul PC10GB. Tasti F6/F7/F9/F10/F12 restano senza conflitti come revisione precedente.

Verifiche: CI41063672780, compilazione e test memoria/browser/tasti/rotazione passati; runtime Minecraft1.20.1 Fabric WorldEdit7.2.15 passato (marker FABRIC_WORLDEDIT_LARGE_SCHEMATIC_PASS): clipboard201x51x201=2.060.451 posizioni aria, writer/reader Sponge con dimensioni e origine(9,3,7) conservate. NON prova materiali o posa. JAR e checksum pubblicati in downloads; SHA256 d2839d2200a3c0dbb3f8ce69c01ffb785d06b26581279686804a2827f291bcb3  MATRIX-Architect-v0.1.0-Minecraft-1.20.1-Fabric.jar. Gli harness precedenti fallivano per piattaforma non inizializzata e registri troppo precoci: corretti avviando il controllo al primo END_SERVER_TICK; processo test isolato termina con halt solo se property matrixarchitect.capacity-test e attiva. Timeout CI15min e runtime180s; build obsolete cancellate automaticamente. Nessun JAR fallito pubblicato.

---

## Archivio revisioni precedenti (stato storico)

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

## Aggiornamento migrazione v3 — seconda e terza fase
CI del commit 90ef7276 confermata SUCCESS. Architettura estratta in ArchitecturalPlanner e GeometryGenerator; aggiunti test DesignContractTest in CI. Ricerca estesa a Wikipedia (contesto testuale) e Commons (immagini); recupero origine scritto atomicamente prima del progress marker; retry limitato su blueprint JSON non valido. Codice fino a commit 66c14e31, CI ultima revisione da verificare.

NON DICHIARARE COMPLETATO: geometria multi-componenti per grandi città, ricerca universale e affidabilità di fonti/planimetrie, completa separazione dei journal dalla GUI, backup mondo/anteprima e validazione runtime Minecraft. Nessun test in gioco effettuato da questa istanza. Non installare automaticamente JAR sperimentale su mondi importanti. Prossima azione: CI verde, poi test in-game con copia di mondo, audit crash/resume e progettazione componenti grandi senza incoerenze.

## Stato aggiornato — 30 ottobre 2026 — migrazione v3 in corso
La sezione storica seguente è un archivio del 29 ottobre, NON lo stato attuale. Il repository contiene ora GUI Banco IA, OAuth, inferenza GPT, parser geometrico ed esecutore; CI precedente verde ma non test in-game. Audit dell'HEAD precedente: 51aa194b8d7535e136ae1bef341078fead755f28.

Interventi attuali: `DesignContract.java` (GPT sceglie NONE/INSPIRATION/RECONSTRUCTION e fornisce master plan), `BlueprintContract.java` (confine tecnico), `BancoIaScreen.java` (ricerca automatica; rimossi brief/master plan duplicati e fallback paginato irraggiungibile), `BuildExecutor.java` (validazione blueprint condivisa), `docs/ARCHITECTURE_V3.md` (contratti e limiti). Commit fino a 26ac2938; CI di questo set ancora da confermare. Nessun test in gioco eseguito in questa sessione.

NON COMPLETATO: estrazione completa dell'orchestrazione dalla GUI; ricerca web oltre Wikimedia; generazione incrementale robusta di progetti enormi; miglioramento salvataggi e identità mondo; test runtime e qualità. Non dichiarare migrazione conclusa. Prossima azione: verificare CI, correggere eventuali errori, poi migrare orchestrazione e progettare generazione geometrica a componenti senza perdita di continuità.

# Stato corrente — Mine Builder

Aggiornato 29 ottobre 2026. Repository canonica: MATRIXNEO23/mine-builder, main.

## Confermato nel repository
- Base Fabric: src/main/java/it/minebuilder/MineBuilder.java e src/main/resources/fabric.mod.json.
- Configurazione build: settings.gradle, gradle.properties, build.gradle.
- Workflow GitHub Actions: .github/workflows/build.yml (test Node e build Gradle).
- Gate inferenza: scripts/verify-plan-sharing.mjs; test mock: scripts/verify-plan-sharing.test.mjs.
- Ultima modifica di codice osservata prima di questo aggiornamento: commit 8be5d3bd1c03ea599e089bdf1b7b583c00da48e9.

## Non verificato / non implementato
- Non è stato letto un esito riuscito di GitHub Actions; nessun JAR confermato.
- Test Node scritti, ma non ancora verificati come eseguiti con successo in CI.
- Bridge locale OAuth Sign in with ChatGPT assente: nessun login account reale.
- Nessuna richiesta GPT reale riuscita; accesso Plus, quote e disponibilità modelli non verificati.
- Nessun blocco Banco IA, GUI o costruzione automatica implementati.

## Vincoli
Zero costi aggiuntivi; niente API key a consumo o fallback a pagamento. Non inserire credenziali in GitHub. Minecraft Java 1.20.1, Fabric, Java 17, Prism; mondo locale. Non confondere mock con GPT reale.

## Prossima azione concreta
1. Ottenere esito del workflow GitHub Actions e correggere eventuali errori.
2. Consultare documentazione ufficiale aggiornata di Sign in with ChatGPT; implementare bridge OAuth locale usando soltanto interfacce documentate, senza inventare endpoint o token.
3. Testare login autorizzato e inferenza reale con consenso utente e senza crediti extra.
4. Realizzare Banco IA e GUI dopo il gate tecnico; aggiornare checkpoint dopo milestone verificata.

## Archivio permanente versioni — 29 ottobre 2026
- Aggiunto `.github/workflows/release.yml` per pubblicare JAR con nome versione e SHA256SUMS nelle GitHub Releases su tag vX.Y.Z, solo dopo test/build.
- Documentato in `docs/INSTALLAZIONE.md`. Commit release workflow: `85feacaede0d6a96999dd705ff2128f022199a73`.
- **Nessuna Release o JAR pubblicati/verificati ancora**; Actions artifact temporaneo non equivale a Release.
- URL permanente elenco versioni: https://github.com/MATRIXNEO23/mine-builder/releases

## Requisito confermato dall'utente — protezione del mondo (29 ottobre 2026)
- L'utente sceglie posizione e orientamento della struttura prima dell'avvio; prevedere anteprima dell'ingombro e conferma.
- La costruzione può scavare, livellare e sovrascrivere terreno naturale per fare spazio.
- Non deve sovrascrivere costruzioni preesistenti senza consenso esplicito. Minecraft non distingue sempre blocchi naturali da blocchi piazzati: in caso di incertezza bloccare la modifica e chiedere conferma.
- Prima di ogni modifica distruttiva, mostrare l'area coinvolta e predisporre un backup recuperabile; non promettere riconoscimento perfetto.
- Questo è un requisito di progetto approvato, NON una funzione già implementata o testata. È un gate obbligatorio prima della costruzione automatica.

## Progresso codice Banco IA — 29 ottobre 2026
- Commit `53c8915`: registrazione del blocco e oggetto Banco IA; sei asset blocco/item/localizzazione/loot aggiunti fino a `28bc373`.
- Commit `840c8d7`: interazione clic destro sul blocco; risponde con messaggio di indisponibilità GPT e NON modifica il mondo.
- Questi commit sono codice scritto e salvato, NON ancora confermati da compilazione o test in Minecraft. Nessuna GUI, pianificazione o protezione automatica implementata.
- Prima di qualsiasi scrittura nel mondo implementare controlli d'ingombro, classificazione conservativa, conferma esplicita e backup verificato.

## Sicurezza mondo — codice preliminare (29 ottobre 2026)
- Commit `6c7c724`: aggiunta `WorldSafety.java`, classificazione conservativa e SOLA LETTURA di blocchi vuoti, terreno comune e blocchi protetti/sconosciuti.
- Non viene ancora usata per costruire o cancellare blocchi; non dimostra l'origine dei blocchi (pietra piazzata da giocatore e pietra naturale possono essere indistinguibili).
- Mancano scansione dell'area, anteprima, backup, conferma, test compilazione e test in gioco. Nessuna modifica distruttiva è abilitata.
- Commit `10042a0`: test asset Banco IA salvati, non ancora collegati alla CI; tentativo di aggiornare workflow bloccato, non considerarlo riuscito.

## Decisione utente: installazione in un solo JAR
Il collegamento di rete deve essere interno alla mod, senza programma ponte separato. Commit codice: 1951a1c. Autenticazione GPT e inferenza reale ancora da implementare e verificare.

## Verifica ufficiale accesso GPT — 29 ottobre 2026
- Documentazione OpenAI ufficiale verificata per Sign in with ChatGPT in app open-source locali; uso del piano idoneo con consenso e scope dedicato, senza API key. Dettagli e sequenza in docs/GPT_INTEGRATION_GATE.md (commit 1d1e00a).
- Non è ancora stato implementato o testato OAuth nel JAR, né una risposta GPT reale. Non considerare risolto il gate.
- L'utente ha confermato test reale in Minecraft: Banco IA presente tramite comando give, clic destro apre la GUI. Aspetto e inventario sono in sviluppo.
- Prossima azione: implementare nel JAR flusso OAuth documentato con PKCE, verifica token/scope e test inferenza; quindi piano strutturato e costruzione sicura.
