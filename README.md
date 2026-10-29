# Mine Builder

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

## Verifiche ancora necessarie
Il collegamento Minecraft–GPT non è ancora implementato né collaudato sul PC dell'utente. Il primo test deve verificare il consenso al piano ChatGPT e la ricezione di un evento response.completed dalla Responses API per un modello dell'account; store=false e stream=true sono obbligatori. Il login da solo non dimostra l'accesso all'inferenza, né garantisce web search. Il test tecnico predisposto in scripts/gpt-plan-smoke-test.mjs richiede un token OAuth ottenuto dal futuro bridge locale, mai una chiave API. Nessun token o credenziale deve essere copiato in GitHub o nella chat.

Per il Banco IA, il motore scelto è GPT. Codex rimane soltanto un'eventuale opzione futura.

Documentazione:
- https://developers.openai.com/siwc/quickstart
- https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
- https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations
- https://help.openai.com/en/articles/20001542-using-your-chatgpt-plan-in-other-apps-and-sites

## Test tecnico predisposto

Il file [`scripts/verify-plan-sharing.mjs`](scripts/verify-plan-sharing.mjs) verifica il consenso `session.sharing`, scopre un modello effettivamente disponibile e richiede una risposta completa via `chatgpt.streamResponse()` usando il client Sign in with ChatGPT. **Non è ancora eseguibile autonomamente:** manca il bridge locale che implementi il login OAuth, memorizzi i token in modo sicuro e fornisca il client già autenticato. Non richiedere mai all'utente di incollare token in chat o GitHub. Il test sul PC e il controllo dei limiti restano da eseguire.
