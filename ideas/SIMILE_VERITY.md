# Progetto futuro — Simile Verity (titolo provvisorio)

**Stato:** idea raccolta il 29 ottobre 2026; **in pausa**. Non avviare implementazione fino a nuova richiesta dell'utente, **dopo il completamento di Mine Builder**. Progetto distinto dalla mod di costruzione; questo file è solo un promemoria e non introduce dipendenze o funzionalità nel codice di Mine Builder.

## Concept
Mod horror Minecraft con entità dall'aspetto quasi umano, simile a un giocatore, richiamabile attraverso un oggetto nell'inventario. Può conversare liberamente con il giocatore e ricordare interazioni ed eventi. Nel tempo sviluppa un legame crescente e comportamenti progressivamente inquietanti, possessivi e imprevedibili. Ispirazione generale dichiarata dall'utente: mod horror «Verity»; evitare di presumere dettagli non verificati o copiarne asset/codice.

## Obiettivi tecnici
- **Leggerezza:** adatta anche a PC datati; niente modello linguistico che gira continuamente.
- **Memoria persistente locale:** piccoli file JSON o SQLite con ricordi rilevanti, conversazioni, promesse, eventi, assenze e progressione; recupero selettivo, limiti di dimensione, salvataggi sicuri.
- **Stato emotivo simulato:** affetto, fiducia, gelosia, ossessione, paura dell'abbandono e soglie di escalation narrative. È una simulazione di gioco, non sentimenti reali dell'IA.
- **Motore di gioco deterministico:** Minecraft gestisce movimenti, apparizioni, inseguimenti, animazioni e azioni permesse. L'IA produce intenzioni/dialoghi strutturati, mai comandi arbitrari o distruttivi.
- **Attivazione IA solo a eventi:** conversazione, eventi importanti o cambiamenti di fase; nessuna inferenza a ogni tick.
- **Autonomia occasionale:** l'entità potrebbe comparire spontaneamente, osservare o seguire il giocatore; idea considerata interessante, **non ancora decisione definitiva**.
- **Evoluzione horror graduale:** inizialmente amichevole, poi più attaccata e inquietante; ricorda le assenze del giocatore e reagisce in modo coerente.
- **Protezione del mondo:** evitare alterazioni irreversibili, griefing e perdita di salvataggi; preferire eventi scenici reversibili.

## Questioni da definire quando si riprenderà
- Modello IA locale leggero, remoto o ibrido; requisiti di rete e costi da verificare prima di scegliere.
- Interfaccia di dialogo (testo/voce), grafica del personaggio, animazioni e frequenza delle apparizioni.
- Intensità horror, controllo del giocatore e modalità per disattivare l'entità.
- Versione Minecraft/Fabric e compatibilità con hardware dell'utente.

## Priorità
**Prima terminare Mine Builder.** Questa idea non deve interromperne lo sviluppo né essere confusa con i requisiti del Banco IA.
