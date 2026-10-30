# Mine Builder

> **ISTRUZIONI PER NUOVE ISTANZE / RIPRESA LAVORO:** Prima di modificare il codice o dichiarare lo stato del progetto, leggere [AGENTS.md](AGENTS.md) e nell'ordine [continuity/PROTOCOL.md](continuity/PROTOCOL.md), [continuity/STATE.md](continuity/STATE.md), [continuity/CHECKPOINTS.md](continuity/CHECKPOINTS.md) e [continuity/HANDOFF.md](continuity/HANDOFF.md). Verificare HEAD, file e test effettivi. Non ricominciare da zero né inventare funzionalità completate. Quando l'utente scrive **«fine istanza»**, eseguire la procedura di checkpoint GitHub e fornire il prompt per riaprire la nuova istanza. La procedura richiede una sessione attiva con accesso a GitHub; non gira autonomamente fra le chat.

Mod sperimentale per Minecraft Java Edition **1.20.1 / Fabric / Java 17**, avviata tramite Prism Launcher.

## Scopo
Un **Banco IA** posizionabile nel mondo Minecraft. Con clic destro apre una schermata di conversazione con l'agente: richieste come «Costruisci San Siro in scala 1:1», correzioni, pausa e ripresa. L'IA progetta in cloud; la mod costruisce nel mondo **locale**, che rimane giocabile offline.

## Vincoli non negoziabili
- **Zero costi aggiuntivi:** nessuna chiave API a consumo, nessun hosting obbligatorio, nessun acquisto di crediti, nessun fallback a provider a pagamento.
- **Connessione reale con l'agente nel primo MVP funzionale**, non funzione opzionale da rimandare alla fine.
- Prima di costruire il resto, effettuare il gate tecnico con **GPT via Sign in with ChatGPT per strumenti open source** (non Codex obbligatorio): consenso OAuth all'uso del piano, elenco dei modelli dell'account e turno di inferenza realmente completato. Non basta vedere un modello nell'elenco. La disponibilità effettiva non è ancora verificata sul PC dell'utente.
- Se autenticazione o quota inclusa non sono disponibili, informare l'utente e sospendere soltanto i task AI, senza cambiare metodo di pagamento.
- **Scala predefinita 1:1:** 1 blocco = 1 metro; dettagli fedeli nei limiti dei dati disponibili e della griglia Minecraft.
- **Persistenza locale:** progetti, sezioni, checkpoint, backup e ripresa verificando i blocchi effettivamente presenti nel mondo. Non alterare i salvataggi esistenti senza consenso.
- Nessun server Minecraft necessario. GitHub serve per il codice della mod; GitHub Actions potrà eventualmente eseguire calcoli deterministici su runner gratuiti, ma non sostituisce l'agente GPT. La mod inserirà automaticamente i blocchi nel mondo locale.
- Mod ottimizzata per HP Pavilion i3-2100: applicazione di blocchi a lotti, senza eseguire il modello AI sul PC.


## Modalità di costruzione

**Ricostruzione reale:** per richieste come «Riproduci San Siro» o «Ricrea San Vendemiano», usare dati e riferimenti verificabili e mantenere la scala 1:1 per quanto possibile. Distinguere dettagli documentati da quelli ricostruiti per ipotesi.

**Creazione originale:** per richieste come «Inventami un castello gotico», GPT definisce liberamente concept, pianta, volumi e stile, anche senza un edificio reale da copiare. Esempi: torri, guglie, archi a sesto acuto, contrafforti, vetrate, ponti e cortili. Il progetto strutturato viene verificato per collisioni, stabilità geometrica e limiti del mondo; gli algoritmi deterministici convertono poi le forme in blocchi di Minecraft. Per edifici inventati, 1 blocco ≈ 1 metro resta la convenzione dimensionale, ma misure e dimensioni sono decise dall'utente o proposte dall'agente, non vincolate a un originale reale.

L'esecutore a sezioni, il checkpoint locale e il recupero sono presenti nel codice, ma non ancora validati su tutti i casi di crash e interruzione. Le modifiche conversazionali di un progetto in corso restano limitate; il completamento architettonico non è certificato.

## Architettura proposta
1. **Blocco Banco IA + GUI Fabric:** campo per prompt, storico della conversazione, progetti, stato, pausa, riprendi, annulla.
2. **Bridge locale leggero:** connessione sicura tra mod e agente, con timeout, errori espliciti, token custoditi fuori da GitHub/log e consenso utente per accesso al piano.
3. **Agente GPT nel cloud (inferenza online):** il motore predefinito è un modello GPT disponibile tramite Sign in with ChatGPT per strumenti open source. Il modello interpreta la richiesta e genera un piano strutturato; le fonti esterne e gli strumenti di ricerca richiederanno integrazioni specifiche. Codex non è obbligatorio.
4. **Progetto persistente:** suddivisione in sezioni idempotenti e stato transazionale / journal: pianificata, in lavorazione, verificata, completata.
5. **Esecutore locale:** modifica il mondo gradualmente, rispetta i limiti di carico, salva checkpoint e può continuare offline quando i dati del progetto sono già stati scaricati.

## Priorità e test di accettazione
- **Gate 0 – costo e autenticazione:** consenso al piano ChatGPT tramite OAuth ufficiale da bridge locale, modello GPT effettivamente disponibile, richiesta reale completata senza chiave API a consumo o pagamenti supplementari. Disattivare l'uso dei crediti nelle impostazioni ChatGPT; in caso di limite o rifiuto, arrestare le richieste AI senza fallback a pagamento. Il test non è ancora stato eseguito.
- **MVP 1 – flusso end-to-end:** aprire Banco IA in Minecraft, inviare «Costruisci una torre 1:1», ricevere un progetto dall'agente e iniziare a costruire nel mondo locale.
- **MVP 2 – recupero:** interrompere a metà, chiudere il gioco, riaprire, chiedere «Riprendi» e completare senza duplicati o perdita del progetto.
- **MVP 3 – fedeltà:** generare una piccola struttura reale documentata, confrontarla con misure e riferimenti, apportare correzioni.
- **Obiettivi successivi:** San Siro e San Vendemiano, in scala 1:1, con riferimenti leciti (es. OpenStreetMap e fotografie riutilizzabili).

## Stato tecnico e verifiche ancora necessarie
Il codice del collegamento Minecraft–GPT è presente nel JAR (OAuth PKCE e inferenza in streaming), ma l'inferenza reale con quota inclusa e il funzionamento sul PC dell'utente non sono stati certificati in questa review. Il primo test deve verificare il consenso al piano ChatGPT e la ricezione di un evento response.completed dalla Responses API per un modello dell'account; store=false e stream=true sono obbligatori. Il login da solo non dimostra l'accesso all'inferenza, né garantisce web search. Il test tecnico predisposto in [scripts/verify-plan-sharing.mjs](scripts/verify-plan-sharing.mjs) richiede un client locale già autenticato tramite OAuth; il test Node indipendente richiede ancora un client JavaScript già autenticato, mentre la mod ha un proprio percorso OAuth Java; l'autenticazione e l'inferenza end-to-end non sono state verificate da questa review. Nessun token o credenziale deve essere copiato in GitHub o nella chat.

Per il Banco IA, il motore scelto è GPT. Codex rimane soltanto un'eventuale opzione futura.

**Sviluppo isolato del 30 ottobre 2026:** la generazione di grandi edifici per componenti è nella PR #1, non ancora in main né nella Release principale. Il manifest geometrico mantiene lo stesso piano architettonico e salva i componenti in un diario recuperabile. CI verde non sostituisce il test in Minecraft. Leggi [la review dei grandi progetti](docs/LARGE_BUILDS_REVIEW_2026-10-30.md).

Documentazione:
- https://developers.openai.com/siwc/quickstart
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations
- https://help.openai.com/en/articles/20001542-using-your-chatgpt-plan-in-other-apps-and-sites

## Test tecnico predisposto

Il file [`scripts/verify-plan-sharing.mjs`](scripts/verify-plan-sharing.mjs) verifica il consenso `session.sharing`, scopre un modello effettivamente disponibile e richiede una risposta completa via `chatgpt.streamResponse()` usando il client Sign in with ChatGPT. **Non è ancora eseguibile autonomamente:** manca il bridge locale che implementi il login OAuth, memorizzi i token in modo sicuro e fornisca il client già autenticato. Non richiedere mai all'utente di incollare token in chat o GitHub. Il test sul PC e il controllo dei limiti restano da eseguire.
