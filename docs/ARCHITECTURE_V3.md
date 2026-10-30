# Mine Builder — architettura v3 (migrazione incrementale)

## Decisione
Conservare Fabric, autenticazione esistente, BuildPlan, libreria blueprint, BuildExecutor e salvataggi. Non riscrivere la mod e non alterare i mondi. Rimuovere il percorso di generazione morto, eliminare i template per categoria e separare progressivamente le responsabilità dalla GUI.

## Contratto GPT -> progetto
`DesignContract` rappresenta richiesta originale, master plan immutabile e scelta GPT tra NONE, INSPIRATION, RECONSTRUCTION. La prima risposta GPT inizia con `RESEARCH=<MODE>` e contiene un progetto globale. Se ricerca utile, il sistema consulta Wikimedia Commons (fonti e una immagine compatibile) e chiede a GPT di consolidare il progetto; le fotografie sono ispirazione per edifici inventati e prova parziale per ricostruzioni reali. Le misure non verificate rimangono stime. Ricerca non obbligatoria per ogni richiesta.

## Contratto progetto -> blueprint
Il progetto globale determina tipologia, geometria, assi, livelli, materiali, circolazione e dettagli. GPT restituisce un solo JSON canonico di cuboids/shapes/blocks; BuildPlan lo espande in coordinate. `BlueprintContract` esegue solo validazioni tecniche, senza dimensioni minime per case o giudizi estetici. Le sezioni 16x16 sono derivate localmente dalle coordinate, mai inventate indipendentemente da GPT.

## Contratto blueprint -> esecutore
`BuildExecutor` riceve esclusivamente un BuildPlan già verificato, effettua controllo quota, area, conflitti con strutture e chunks; posiziona sul server integrato a lotti e registra checkpoint. Nessun comando arbitrario del modello viene eseguito. L'utente conferma l'area col martello. Progetti e journal esistenti restano intatti.

## Implementato in questa fase
- DesignContract e BlueprintContract introdotti.
- Scelta autonoma di ricerca e ispirazione, senza toggle manuale.
- Brief e master plan duplicati unificati nel piano GPT; eventuale consolidamento solo quando si ricerca.
- Rimosso vecchio generatore paginato irraggiungibile dalla generazione nuova.
- Conversione ed esecuzione condividono il confine tecnico BlueprintContract.

## Seconda fase implementata
- `ArchitecturalPlanner`: ricerca e pianificazione GPT indipendenti dalla schermata.
- `GeometryGenerator`: geometria canonica e validazione tecnica fuori dalla schermata.
- Test `DesignContractTest` introdotto e collegato alla CI; esito ancora da verificare.

## Terza fase implementata
- Ricerca ampliata con contesto enciclopedico gratuito Wikipedia, oltre a immagini Wikimedia Commons. La copertura non equivale a una ricerca web universale.
- Scrittura atomica dell'origine del progetto prima del checkpoint di avanzamento, per evitare un checkpoint apparentemente riprendibile senza origine.
- Un tentativo di rigenerazione compatta quando il JSON geometrico non supera i limiti tecnici; nessuna costruzione parziale viene pubblicata.

## Estensione dimensioni (quarta fase)
- Rimosso il limite arbitrario di +/-128 blocchi X/Z e -32..128 Y dal parser geometrico: coordinate relative fino a +/-2000, coerenti con la libreria blueprint. L\u0027esecutore continua a verificare i limiti reali di altezza del mondo.
- Rimangono il tetto di 2 milioni di blocchi espansi, il limite di risposta JSON e i limiti dei singoli primitivi. Questo NON equivale a supportare automaticamente intere città.

## NON ancora completato
- Orchestrazione completa e journal ancora nella GUI: progettazione e generazione geometrica sono ora estratte in `ArchitecturalPlanner` e `GeometryGenerator`.
- Ricerca web generale: Wikimedia Commons e Wikipedia non garantiscono planimetrie, misure né immagini pertinenti.
- Blueprint di grandi città/stadi con geometria incrementale affidabile: il singolo JSON può eccedere il limite output GPT.
- World identity forte per tutti i journal, preflight e salvataggi completamente non bloccanti, backup e anteprima esaustiva.
- Test di qualità architettonica e prova in Minecraft; una CI verde attesta compilazione, non fedeltà.
- Prova che il percorso Sign in with ChatGPT usi solo quota inclusa su ogni account: non presumerlo.

## Criteri di adozione
1. CI verde; test contratto, parser e recupero.
2. Test nel gioco: villa originale ispirata a fonti; casa senza ricerca; edificio reale con stime dichiarate; riavvio e ripresa; area occupata; mondo superflat.
3. Nessuna perdita o cancellazione di salvataggi precedenti.
4. Zero API a consumo, crediti extra o hosting a pagamento.

## Estensione sperimentale su PR #1 — grandi strutture (30 ottobre 2026)

La via canonica per progetti piccoli resta un singolo JSON. Se GPT sceglie `GEOMETRY=COMPONENTS`, il progetto viene invece suddiviso in un manifest globale immutabile (2..64 componenti con ID, descrizione e bounding box); ciascun componente viene dettagliato e salvato separatamente. La geometria completa è controllata per bounds, conflitti e sezioni prima di poter avviare la posa; il motore non costruisce componenti indipendenti mentre la progettazione è incompleta.

Questa estensione è **nel ramo sperimentale**, non in main. Mantiene ancora 2 milioni di blocchi totali e l'assemblaggio in RAM. Il supporto realmente scalabile per interi paesi e la validazione architettonica con fonti verificabili sono ulteriori fasi; nessun risultato in-game è stato attestato. Vedi `docs/LARGE_BUILDS_REVIEW_2026-10-30.md`.
