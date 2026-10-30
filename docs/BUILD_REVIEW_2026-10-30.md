## Verifica finale incrementale — dopo commit 0a2a92e3
- CI run 40991245373 sul commit 0a2a92e3: SUCCESS.
- Corretto bug UI: `BuildExecutor.cancel()` azzerava `ownerServer` e `lastErrorFor()` nascondeva immediatamente la causa del blocco (area occupata, chunk non pronto, checkpoint fallito). Ora conserva l'identità server per mostrare il messaggio; `start()` resetta l'errore.
- Rimangono limiti reali: città/stadi grandi, fonti planimetriche affidabili, test in gioco, garanzia zero costi sull'endpoint API. Nessuna certificazione di completamento.
- La CI della correzione deve essere verificata.

## Review aggiuntiva — commit successivo a a982109f
- Run 40990761082 sul commit a982109f: SUCCESS. Le due run intermedie 5f5cd7a2 e abb926eb: CANCELLED, non FAILED.
- Rimosso parser PAGED obsoleto dal flusso di generazione nuovo.
- Estratto GenerationJournal: scrittura atomica di master plan, richiesta, origine, modalità ricerca, specifica e geometria; mantiene i journal precedenti e usa cartella del mondo.
- CI sul nuovo HEAD da verificare.
- Ancora NON implementata generazione multi-componente affidabile di città intere, verifica planimetrie 1:1, test runtime Minecraft, budget salvataggi non bloccante. Non dichiarare completamento.


# Mine Builder — build review 2026-10-30

## Baseline verificata
- GitHub Actions run 40990401598, commit 42625e7d: SUCCESS.
- Le quattro run intermedie precedenti sono CANCELLED, non errori di compilazione.
- Revisione statica delle sorgenti main, non prova di qualità in Minecraft.

## Componenti presenti e collegati
- `DesignContract`: decisione GPT NONE / INSPIRATION / RECONSTRUCTION.
- `ArchitecturalPlanner`: piano architettonico e ricerca Commons + Wikipedia.
- `GeometryGenerator`: geometria compatta GPT, parsing e retry limitato.
- `BlueprintContract`: validazione tecnica e partizionamento deterministico.
- `BuildExecutor`: preflight, batch adattivi e checkpoint locali.
- `BancoIaScreen`: avvia e coordina, ma non è ancora UI pura.
- `DesignContractTest` è eseguito da GitHub Actions dopo `gradle build`.

## Correzione eseguita durante review
Rimosso il vecchio prompt della schermata che contraddiceva il contratto canonico: imponeva 20.000 blocchi, chiedeva JSON già nella fase di richiesta e autorizzava piccole sezioni incomplete. Il prompt canonico di geometria è ora in `GeometryGenerator`, con indicazioni su blocchi, stati, scale interne, aperture e limiti tecnici.

## Mancanze confermate, non occultate
1. Generazione affidabile di interi paesi e stadi enormi: ancora un unico JSON e limite 2 milioni di posizioni; non dichiarare supporto illimitato.
2. Fonti web: solo Commons + Wikipedia; non esiste motore universale per reperire e verificare planimetrie.
3. Orchestrazione/journal ancora parzialmente nella schermata; generatore ed architetto estratti.
4. Save chunk sincrono in `BuildExecutor`; non dimostrato budget massimo senza lag.
5. Nessuna prova di test end-to-end in Minecraft o di ricostruzione 1:1 fedele.
6. Verifica del canale GPT: compilazione e login non provano quota gratuita/inclusa o capacità browser/vision per ogni utente.

## Gate successivo
- Attendere CI sul nuovo HEAD.
- Eseguire test in un mondo copia: villa originale, ricostruzione con misure documentate, interruzione e ripresa, terreno occupato, mondo superflat.
- Prima di sviluppare città/stadi grandi: definire un manifest immutabile di componenti architettonici, generazione per componente, verifica intersezioni e checkpoint senza cambiare il piano.
- Non sostituire il JAR stabile su mondi importanti prima dei test.
