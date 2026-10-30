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

## NON ancora completato
- Motore di generazione estratto interamente da BancoIaScreen; ora la GUI conserva l'orchestrazione.
- Ricerca web generale multi-fonte: oggi la scoperta automatica usa soltanto Wikimedia Commons, che non garantisce immagini o planimetrie pertinenti.
- Blueprint di grandi città/stadi con geometria incrementale affidabile: il singolo JSON può eccedere il limite output GPT.
- World identity forte per tutti i journal, preflight e salvataggi completamente non bloccanti, backup e anteprima esaustiva.
- Test di qualità architettonica e prova in Minecraft; una CI verde attesta compilazione, non fedeltà.
- Prova che il percorso Sign in with ChatGPT usi solo quota inclusa su ogni account: non presumerlo.

## Criteri di adozione
1. CI verde; test contratto, parser e recupero.
2. Test nel gioco: villa originale ispirata a fonti; casa senza ricerca; edificio reale con stime dichiarate; riavvio e ripresa; area occupata; mondo superflat.
3. Nessuna perdita o cancellazione di salvataggi precedenti.
4. Zero API a consumo, crediti extra o hosting a pagamento.
