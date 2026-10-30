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
