# Stato corrente — Mine Builder

Aggiornato il **29 ottobre 2026**. Fonte: ispezione della repository `MATRIXNEO23/mine-builder` sul ramo `main`. Checkpoint di base: `CP-2026-10-29-001`.
HEAD verificato **prima** di aggiungere la memoria: `e837a7a0f5e158b84c9b9cdfb09c66ef835439d8`. Dopo il commit della memoria consultare l'HEAD corrente, che sarà diverso.

## Progetto e vincoli confermati
- Prima versione: Minecraft Java 1.20.1, Fabric e Fabric API, Prism Launcher, Java 17 come target.
- L'utente ha confermato che l'istanza Minecraft con Fabric e Fabric API arriva al menu principale; **non è stata verificata in questa sessione la JVM esatta installata**.
- Il PC dell'utente è un HP Pavilion con i3-2100: non eseguire modelli AI locali; costruire a piccoli lotti.
- Banco IA collocabile nel mondo: interfaccia conversazionale GPT con stato lavori, comandi di modifica, pausa e ripresa.
- Mod con GPT collegato **nella prima versione realmente funzionante**, non soltanto una GUI vuota. Piano ChatGPT Plus già posseduto, senza costi aggiuntivi, API key a consumo o crediti extra.
- Mondo Minecraft salvato localmente e utilizzabile offline dopo le costruzioni. GitHub serve allo sviluppo; Actions gratuite facoltative per elaborazioni deterministiche.
- Due modalità: ricostruzione di luoghi reali (San Siro e San Vendemiano) **1:1**; costruzioni inventate (es. castello gotico), con dettagli elevati.
- Salvataggi, checkpoint e recupero per parti senza perdere il progetto; backup prima di modifiche pericolose.
- «fine istanza» deve attivare checkpoint GitHub e generazione del prompt di ripresa.

## Stato implementazione verificato
**FATTO (in repo)**:
- `README.md`: obiettivo, architettura e requisiti.
- `docs/INSTALLAZIONE.md`: guida utente.
- `scripts/verify-plan-sharing.mjs`: codice preliminare di verifica del piano GPT, non autonomo.
- File di continuità creati nel checkpoint CP-2026-10-29-001.

**NON FATTO / NON TESTATO**:
- Non esiste un progetto Fabric compilabile con `build.gradle`, sorgenti Java e `fabric.mod.json`; **nessun .jar Mine Builder** installabile è stato generato.
- Nessun Banco IA funzionante nel gioco.
- Nessun bridge locale di autenticazione OAuth realmente avviato né connessione GPT verificata sull'account dell'utente.
- Lo script `verify-plan-sharing.mjs` non è una dimostrazione di connettività: non gestisce login e non risulta eseguito.
- Nessuna costruzione automatica, sistema checkpoint in-game, backup o controllo qualità implementati.

## Rischi / verifiche aperte
- Prima di implementare l'accesso account, verificare SDK e permessi correnti con documentazione ufficiale; non presumere disponibilità di tutte le funzioni GPT o web search in piano Plus.
- Garantire zero fatturazione aggiuntiva: il solo accesso account non è sufficiente; il test va effettuato con consenso.
- Per Fabric su 1.20.1 validare versioni Gradle/Loom/JDK e compatibilità delle dipendenze prima di dichiarare un artefatto pronto.
- Correggere il riferimento inattuale a `scripts/gpt-plan-smoke-test.mjs` nel README, che non corrisponde al file realmente presente. Utilizzare `scripts/verify-plan-sharing.mjs`.

## Prossimo passo operativo
1. Creare uno **scheletro Fabric 1.20.1 compilabile** nella repo (codice del Banco IA e UI, con collegamento estensibile, senza sostituire l'inferenza GPT con un mock spacciato per reale).
2. Preparare il bridge OAuth locale con il toolkit ufficiale e testarne la connessione reale con consenso dell'utente, senza credenziali in GitHub.
3. Verificare con build automatica che il .jar venga generato correttamente, poi test end-to-end nell'istanza Prism su mondo di prova.
4. Dopo ogni milestone aggiornare questo STATE e aggiungere checkpoint, rispettando `AGENTS.md`.

## Cosa dire all'utente al momento della ripresa
«Minecraft Java 1.20.1 con Fabric e Fabric API è pronto sul tuo PC. La repository contiene ancora documentazione e test preliminari, non la mod installabile. Proseguo dal primo componente di codice, verificando la compatibilità e il collegamento reale GPT senza costi extra.»
