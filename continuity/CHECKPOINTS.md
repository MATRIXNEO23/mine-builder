# Checkpoint cronologici — Mine Builder

## CP-2026-10-29-001 — 29 ottobre 2026 — baseline della memoria di progetto

- **Ramo**: `main`.
- **Commit verificato prima del checkpoint**: `e837a7a0f5e158b84c9b9cdfb09c66ef835439d8`.
- **Contenuto verificato allora**: `README.md`, `docs/INSTALLAZIONE.md`, `scripts/verify-plan-sharing.mjs`. Nessun modulo Fabric compilabile o release.
- **Decisioni utente confermate**: Prism Launcher, Minecraft Java 1.20.1, Fabric + Fabric API funzionanti nel menu; Banco IA conversazionale; GPT collegato realmente fin dal primo MVP; zero costi aggiuntivi; mondo offline; costruzioni in sezioni con recupero; strutture reali 1:1 e creative inventate; GitHub per sviluppo.
- **Problema rilevato**: README cita un nome di script inesistente (`scripts/gpt-plan-smoke-test.mjs`); file presente `scripts/verify-plan-sharing.mjs`.
- **Operazione corrente**: introduzione di `AGENTS.md`, `continuity/PROTOCOL.md`, `continuity/STATE.md`, `continuity/CHECKPOINTS.md`, `continuity/HANDOFF.md`; rimane da verificare i commit con una lettura successiva.
- **Test reali**: utente ha confermato avvio del gioco con Fabric API; non ci sono prove di test mod, connessione account, compilazione o costruzione.
- **Next**: baseline Fabric compilabile; validazione ufficiale autenticazione GPT senza spese; primo Banco IA e costruzione automatica; checkpoint successivo dopo test.
- **Divieto di inferenza**: questo checkpoint descrive una progettazione e una preparazione, non una mod installabile.

> Nuovi checkpoint: aggiungere in coda, senza sovrascrivere quelli precedenti. Ogni voce deve specificare risultati verificati, artefatti e prossimo passo. Il commit dell'operazione è recuperabile dallo storico GitHub.

## CP-2026-10-29-002 — 29 ottobre 2026 — configurazione Fabric e gate inferenza

- File confermati nel repository: build.gradle, settings.gradle, gradle.properties, workflow .github/workflows/build.yml, sorgenti iniziali Fabric, scripts/verify-plan-sharing.mjs e test mock.
- Commit codice prima del checkpoint: 8be5d3bd1c03ea599e089bdf1b7b583c00da48e9.
- Test e compilazione: nessun risultato CI confermato; non dichiarare build riuscita.
- Login OAuth e inferenza ChatGPT Plus: non implementati e non verificati.
- Prossimo passo: verificare GitHub Actions, poi bridge OAuth con SDK ufficiale e prova autorizzata senza crediti aggiuntivi.


## CP-2026-10-30-008 — Blueprint Builder distinto e download permanente

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

