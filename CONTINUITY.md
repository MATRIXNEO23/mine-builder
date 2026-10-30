## 2026-10-30 — Screenshot casa non riconoscibile: gate geometrico
Foto utente: struttura stretta e alta simile a torre/statua, non una casa. Commit fb9c988: validateSimpleHouse su progetto compatto PRIMA del salvataggio e costruzione; rifiuta solidi <90, footprint X <8/Z <7, altezza <4 o >12. Non garantisce tetto/porte/interior o qualità, ma blocca la specifica anomalia mostrata. Progetti preesistenti non modificati; test in gioco e CI richiesti.

## 2026-10-30 — Test utente: console invisibile, casa lenta e forma errata
Commit 41eb35c: pulsante Console LIVE/Registro sempre visibile sopra Chiudi in entrambe le schede; logging immediato delle fasi e degli errori (non solo durante render), validazione quota blueprint appena generato. Commit 141a1f6: richieste brevi per casa/casetta/house saltano chiamata GPT brief e chiamata GPT master-plan, usando contratto geometrico deterministico 11x9 e un'unica chiamata GPT per geometria; prompt esplicita interni vuoti, tetto, porte, finestre. Non cambia richieste architettoniche grandi. Qualità della casa da validare con screenshot/JSON, nessuna garanzia estetica. Serve JAR nuovo, non altra mod. CI da verificare.

## 2026-10-30 — Errore altezza fotografato e protezione nuovi mondi
Screenshot utente: Blocco fuori dai limiti -21,-73,-297, Y valida -64..319. Correzione 6285d0d: validazione preventiva di ogni posizione in BuildExecutor.start/resume e API validateHeight, prima di salvare/posare; lastErrorFor(client) nasconde errori di server/mondo precedente. Correzione 7fc208b: validazione preventiva anche prima di armare il martello e UI usa lastErrorFor. Non spostare automaticamente edifici o modificare progetti/salvataggi; richiedere quota adatta o rigenerazione. CI e test runtime da verificare.

## 2026-10-30 — Console generazione in tempo reale
Commit c08043e: BancoIaScreen integra pulsante Console generazione e schermata live con fase, secondi trascorsi, blocchi/pagine/sezioni, cronologia eventi e registro persistente config/minebuilder/generation-console.log. Le fasi vengono rilevate al cambio di generatorStage nel render; non sono percentuali GPT inventate. Log scrive anche se la console non è aperta quando BancoIaScreen è visualizzato; eventi intermedi non renderizzati potrebbero non essere catturati. CI e test in gioco ancora da verificare; non dichiarare build pronta finché non verde.

## 2026-10-30 — Recovery da ultimo verde e nuova implementazione isolata
Dopo CI rossa 95d04839, 55e02752 e 5ae20941, log: IntegratedServer non espone getAverageTickTime() né getTickTimes() con i mapping attuali. Ripristinato integralmente BuildExecutor.java da commit VERDE 766b77c5 in commit 9d5c397; quindi introdotto in commit 4a6c737 solo cooldownTicks (2 o 5 tick se l'ultimo batch supera budget 2ms/6ms), senza dipendenze da metodi server non verificati. Rimane il batch adattivo 4..32. Non è controllo MSPT globale, ma autoregolazione della posa; non promettere zero lag. Aspettare nuova CI prima di distribuire. Non cancellati diari né geometrie.

## 2026-10-30 — Nuovo controllo tick-based MSPT (richiesto prima della prossima build testabile)
Commit 95d04839: BuildExecutor ora misura server.getAverageTickTime() nel thread server; >=48ms pausa 5 tick, >=55ms pausa 20 tick, >=40ms forza batch minimo 4, >=30ms massimo 8. Mantiene budget 2ms per batch e salvataggi/checkpoint preesistenti. Nuova build CI da verificare, NON consegnare artefatto precedente 766b77c5 come se contenesse queste modifiche. Limiti: MSPT medio è un indicatore ritardato, non garanzia di lag zero; verificare nel gioco.

## 2026-10-30 — Streaming adattivo senza mcfunction
BuildExecutor.java commit 6e27389: sostituito PER_BATCH fisso 16 con batchSize adattivo 4..32, budget indicativo 2ms per tick server, misurazione System.nanoTime e adattamento dopo ogni batch. Resta la verifica per sezione e salvataggio del checkpoint. Nessun .mcfunction necessario: il gioco applica direttamente blocchi dal BuildPlan completo. LIMITI: operazioni individuali setBlockState e salvataggio chunk a fine sezione possono comunque creare lag; 2ms non è hard real-time; verifica compilazione CI e prova su mondo reale. Non cancellare salvataggi.

## 2026-10-30 — Geometria completa in una specifica compatta
Nuovo percorso di generazione per NUOVI progetti (commit 6db67c91, 556b8b77): dopo brief e master plan GPT produce un UNICO JSON di cuboids/shapes/blocks per l'intero edificio; BuildPlan.parseComplete espande localmente fino a 2 milioni di blocchi e validateSections verifica partizionamento 16x16. JSON sorgente salvato atomicamente in generation-journal/<id>/complete-geometry.json, origine e master-plan conservati. La costruzione si abilita solo dopo generazione completa. Vecchio percorso a pagine rimane nel codice per compatibilità/recovery, ma non è il percorso normale. I materiali vengono assegnati una volta nel JSON globale, non generati per sezione. LIMITI: specifica JSON generata da GPT può essere incompleta o errata, non c'è prova automatica di fedeltà storica né controllo di tutte le giunzioni; BuildPlan impone coordinate relative ±128 X/Z, Y -32..128 e max 2 milioni di blocchi; per edifici più grandi servono estensioni senza aggirare la sicurezza. Attendere CI e test Minecraft. Nessun costo aggiuntivo. Non cancellare diari vecchi.

## 2026-10-30 — Build dopo progetto completo (commit 52aec519)
Su richiesta esplicita dell'utente: per NUOVI progetti non esporre più la prima pagina come costruibile, non approvare progressive build prima del completamento; il pulsante Costruisci attende fine generazione. La geometria viene prima unificata, verificata tramite BuildPlan.validateSections(), poi costruita con l'esistente BuildExecutor che suddivide in sezioni 16x16. Vecchi journal progressivi non cancellati né convertiti. Trade-off dichiarato: non c'è più costruzione contemporanea alla generazione nei nuovi progetti; maggiore sicurezza/coerenza prioritaria. Non affermare che il master plan testuale assicuri la precisione storica o che il compilatore parametrico universale sia già implementato. CI e test runtime ancora da verificare.

## Aggiornamento fase motore geometrico (2026-10-30)
Commit 415c328: controllo conflitti tra pagine PRIMA della pubblicazione nel journal (prima avveniva dopo: pagina errata poteva essere costruita). Commit c9260de: al completamento delle pagine, ricostruzione mappa unica di blocchi, validateSections(), partizionamento deterministico in chunk X/Z 16x16, salvataggio sections-manifest.json con coordinate e conteggi. Il motore BuildPlan.sections() era già presente e ora produce un manifest persistente. ATTENZIONE: il percorso progressive continua ancora a costruire pagine GPT, non le sezioni del manifest; il progetto geometrico è ancora prodotto da GPT in più risposte. Non dichiarare raggiunto l'obiettivo finale di geometria unica progettata/compilata deterministicamente prima della costruzione. Restano: generatore parametrico completo, copertura e giunzioni, validazione globale pre-build, recovery affidabile, test in gioco. Non cancellare diari esistenti.

## Aggiornamento 2026-10-30 — Nuova direzione autorizzata
L'utente ha autorizzato a passare da pagine GPT indipendenti a progetto globale prima della suddivisione deterministica in sezioni. Primo passo implementato: generazione di un master plan architettonico globale, salvato nel journal come master-plan.txt prima della prima pagina; prompt di geometria vincolato al piano; ripresa journal ricarica il master plan. Commit a22bf7a e 80da7e4. Il progetto globale è attualmente TESTUALE, non ancora un manifest geometrico tipizzato: NON dichiarare che la suddivisione deterministica, i controlli di giunzione e la verifica completa sono già implementati. Restano da realizzare un modello geometrico globale validato, compilazione deterministica in blocchi, partizionamento per coordinate, verifiche giunzioni/copertura, compatibilità con progressive build e recovery atomico. Nessun costo extra. CI e test in-game da verificare.
Correzioni precedenti su limiti verticali: commit 5b790121 e precedenti, origine nuovi progetti alzata se necessario, errore con coordinate. Non riposizionare automaticamente i vecchi journal.

# CONTINUITÀ PROGETTO — Mine Builder
Aggiornamento: 2026-10-29 (Europa/Roma)
Repository canonica: MATRIXNEO23/mine-builder
Branch: main
Stato: lavoro ripreso su autorizzazione dell'utente. NON considerare concluso o validato il progetto.

## Obiettivo vincolante
Mod Minecraft Fabric 1.20.1, mondo singleplayer offline, GPT via accesso già disponibile senza API a consumo, nessun costo aggiuntivo. Progettazione di strutture originali o ricostruzioni reali 1:1 (1 blocco = 1 metro), fedeltà ai dettagli prioritaria rispetto alla velocità. Costruzione progressiva, salvataggio duraturo e ripresa dopo interruzioni. Proteggere il mondo e le costruzioni esistenti. Non modificare funzioni acqua/anteprima fantasma senza richiesta.

## Problemi segnalati dall'utente e prove
- Modalità Originale/Riferimenti non chiaramente distinguibili nell'interfaccia.
- Progettazione si interrompeva con `Conflicting geometry at 30, -60, -269`; BuildPlan.addCell aggiornato per sovrapposizioni ordinate (cuboids -> shapes -> explicit blocks), commit 77a13537, compilazione riuscita.
- Generazione si fermava dopo prima pagina di 15.232 blocchi, errore `Blueprint exceeds 20000 blocks`; introdotti retry per pagine troppo grandi (max 4), commit f3a9272f. Verificare che retry funzionino realmente, senza perdere dati già validi.
- Utente vuole vedere in GUI fase di ricerca, progettazione, pagina, pagine salvate e blocchi validati: commit 2051e47e, compilazione riuscita.
- L'utente ha mostrato il pulsante `Dettagli e controlli` visibile nell'ultima schermata; contiene errori, ma la leggibilità e i messaggi diagnostici sono migliorabili.
- `Salva progetto` esiste sotto scheda Salvati; utente non lo trovava. Rinominata scheda `Salvati / Salva progetto`. Aggiunto salvataggio automatico del blueprint completo nel percorso progressivo, commit ac7c6b7d. Verificare.
- Richiesta principale: dopo prima pagina, un'unica conferma `Costruisci`, costruzione contemporanea alla generazione di ulteriori pagine; recovery e checkpoint persistenti.

## Implementazione recente
- ProgressiveBuild.java (7519c5f): coordinatore per pagine del journal, approvazione persistente, indice built.txt, tentativo di resume.
- MineBuilderClient registra ProgressiveBuild::tick (3988a8c8), compilazione riuscita.
- BancoIaScreen approva e avvia da prima pagina mentre GPT prosegue (6143e190), compilazione riuscita.
- Recupero lavori approvati riaprendo UI (7dd76264), compilazione riuscita.
- Fix compilazione parsedPage (869ff58a), compilazione riuscita.
- Retry pagine GPT sovradimensionate f3a9272f: il workflow 40942066642 è ROSSO, ma il codice ha compilato; errore nel passaggio `Save JAR inside repository downloads folder` HTTP 409 perché main avanzava durante pubblicazione.
- Fix pubblicazione JAR 7f264688: serializzazione workflow tramite concurrency group, retry con SHA fresco e branch main. Verificare nuovo workflow.
- Salvataggio automatico blueprint completo ac7c6b7d: CI in corso all'ultimo controllo.

## IMPORTANTI LIMITI TECNICI APERTI: NON NASCONDERLI
1. ProgressiveBuild usa BuildExecutor.start per ogni pagina. BuildExecutor salva ogni pagina sotto lo stesso `active-build`, il checkpoint contiene un indice delle sezioni 16x16; il coordinatore scrive `built.txt` dopo finished(). Recupero in caso di crash tra completamento pagina e scrittura built.txt richiede verifica rigorosa. Non testato nel gioco.
2. ProgressiveBuild.recover cerca approved.txt e chiama approve, poi resume. In caso di più lavori approvati la scelta non è ancora sicura; la pagina completata potrebbe essere interpretata come lavoro da rifare. NON affermare recovery perfetto.
3. Ogni pagina GPT può contenere più sezioni 16x16 e conflitti geometrici con altre pagine; la protezione da conflitti esiste nel generatore, ma la sequenza per pagine non garantisce coerenza architettonica globale.
4. Non c'è ancora un manifest geometrico completo con ID sezione e copertura deterministica per chiedere *solo* le sezioni mancanti. Il modello potrebbe saltare o duplicare dettagli.
5. `PAGED:` viene elaborato dopo aver assemblato tutto il testo in memoria; per progetti grandi è oneroso. Serve streaming journal-first.
6. La costruzione progressiva va testata in Minecraft reale, non solo compilata. Il mondo deve essere protetto da modifiche a strutture artificiali.
7. `Salva progetto` in scheda Salvati salva solo quando `lastPlan` esiste. Salvataggio automatico dopo generazione completa va validato.
8. Gestire la chiusura dell'interfaccia durante generazione e gli errori in modo visibile, senza perdere pagine.
9. Il workflow di pubblicazione deve essere verde; non confondere errori del publishing con errori di compilazione.

## Prossime verifiche consigliate (senza adottare modifiche automaticamente)
1. Controllare Actions sul commit 7f264688 e successivi, leggere log completi in caso di errore.
2. Verificare comportamento del coordinatore in assenza di nuove pagine e con GPT che fallisce.
3. Eseguire test manuali in mondo di prova: prima pagina, costruzione simultanea, blocchi protetti, crash tra checkpoint e built.txt, riavvio, cambio dimensione, salvataggio e replica.
4. Migliorare diagnostica GUI: indicare errore preciso e pagine recuperabili.
5. Progettare manifest sezioni, test anti-regressione, conferma utente prima di ogni cambiamento potenzialmente distruttivo.

## Regole di lavoro
Usare repository come fonte canonica, leggere codice e HEAD prima di interventi. Non dichiarare riuscita una funzione soltanto perché la CI compila. Non cancellare journal, checkpoint, mondi o progetti. Nessuna API a pagamento, hosting o addebiti. Se un requisito non è chiaro, chiedere anziché inventare. L'utente ha autorizzato la ripresa e la nuova architettura; non introdurre modifiche distruttive.

## Ultima azione
Workflow `Retry oversized GPT geometry pages with smaller batches and clarify save project location` commit c7d08ce3: ROSSO per compilazione Java `parsedPage` mancante, risolto da 869ff58a (verde). Workflow successivo `Validate page before journaling and retry oversized geometry with smaller pages` commit f3a9272f: ROSSO per HTTP 409 nel publishing, NON per compilazione. Commit 7f264688 tenta di risolvere il publishing. Questo file è il punto di ripresa.
