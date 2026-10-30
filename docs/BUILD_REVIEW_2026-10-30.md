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
