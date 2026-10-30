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

## CP-2026-10-30-003 — 30 ottobre 2026 — verifica della progettazione per componenti (isolata)

- Main canonico prima dei lavori: `7e7e5a3`. Ramo review: `feature/composite-large-builds-20261030`; PR #1, senza merge.
- Ultimo workflow main verde: 40992419917. Il workflow 40992403280 è CANCELLED, non FAILED.
- Prime CI nuove riuscite: 40994491465 e 40994624758, test Gradle e manifest incluso; ultima CI e prova nel mondo ancora necessarie.
- Codice sperimentale: componenti con un piano comune, bounds, conflitti, journal persistente, correzione coordinate dettagli, recupero e UI. **Non considerare certificata la qualità.**
- Non distrutti file precedenti, nessun JAR principale sostituito, nessuna nuova spesa introdotta.
- Resta: verificare regressioni, crash/recovery, protezione costruzioni artificiali, velocità su i3, progetto paese oltre 2 milioni di blocchi.
- Prossimo passo: test su copia del mondo, audit per eventuale merge, generazione realmente disk-based per città intere.


## CP-2026-10-30-004 — 30 ottobre 2026 — anteprima JAR generazione a componenti

- Main prima degli esperimenti: `7e7e5a3`; main non modificato in questa milestone.
- Sviluppo isolato: PR #1 `feature/composite-large-builds-20261030`.
- CI build completa sul commit `c2b859b8`: run **40995555653 SUCCESS**, compresi test mock, compile Fabric, test DesignContract e ComponentManifest e pubblicazione preview.
- Release testabile verificata: `https://github.com/MATRIXNEO23/mine-builder/releases/tag/mine-builder-composite-preview`, JAR e checksum. Nessun JAR principale sostituito.
- Correzioni: recupero dei diari singoli e multipli, file blueprint GZIP in streaming con lettura storica, validazione degli header GPT, gestione JSON malformato, nessun taglio silenzioso oltre limite, sistema a componenti.
- Non classificare come `testato nel mondo`: serve prima l'esecuzione Minecraft e prova di sicurezza/restart su mondo nuovo.
- Prossimo: test gioco su un mondo dedicato, correggere crash/lag/preflight, valutare merge solo con decisione distinta.

## CP-2026-10-30-005 — 30 ottobre 2026 — consegna di fine sessione, build preview conservata

- L'utente ha chiesto espressamente di salvare la continuità prima di dormire: questo checkpoint è il punto ufficiale di ripresa.
- Repository: `MATRIXNEO23/mine-builder`. Ramo **attivo** `feature/composite-large-builds-20261030`, PR #1 DRAFT e NON fusa. HEAD verificato **prima** del checkpoint: `d219bbe1d6521f4a488bd9d95dd443ae3fa56a14`. Main resta `7e7e5a3aa4d8315de121e9d52a67d43ba0b9b510`.
- **Ultima CI verificata:** run `40995790996` SUCCESS sul commit `d219bbe1d6521f4a488bd9d95dd443ae3fa56a14`, preceduto da `40995679462` SUCCESS e `40995555653` SUCCESS (pubblicazione JAR).
- **Release conservata:** `https://github.com/MATRIXNEO23/mine-builder/releases/tag/mine-builder-composite-preview`, JAR Fabric 1.20.1 da 134942 byte e `SHA256SUMS.txt` (SHA256 `61a890ee96173518ebe16176195a882f87080268fceaf7a9a993b12f1739fe81`). La preview non sostituisce la Release di main; tag inizialmente creato su `c2b859b8`.
- Implementazioni di codice precedenti al checkpoint: progetto globale a componenti, manif. immutabile 2..64 parti, limiti dettagli ±2000, controlli conflitto pre-journal, recupero journal singolo/composito, GZIP streaming per blueprint con compatibilità vecchi JSON, validazione contratti GPT e retry malformati, build CI in verde.
- **NESSUN test in Minecraft reale documentato** per la preview. Inferenza GPT con quota inclusa non validata. Limite aggregato 2 milioni, performance, backup/rollback, protezione blocchi artificiali, giunzioni architettoniche e fedeltà 1:1 ancora non risolti.
- Per la ripresa: prima leggere `continuity/HANDOFF.md` e `continuity/STATE.md` **sul ramo attivo**, verificare di nuovo l'HEAD e CI; poi test della preview **solo su mondo nuovo** e registrare risultati. Non eseguire merge né sovrascrivere mondi precedenti. Nessun costo aggiuntivo.
- Questa operazione salva **solo documentazione di continuità**, senza toccare codice, JAR o mondi. SHA della presente operazione da verificare nella cronologia GitHub, non inventare un identificatore nel checkpoint.
