# Mine Builder

Mod sperimentale per Minecraft Java Edition **1.20.1 / Fabric / Java 17**, avviata tramite Prism Launcher.

## Scopo
Un **Banco IA** posizionabile nel mondo Minecraft. Con clic destro apre una schermata di conversazione con l'agente: richieste come «Costruisci San Siro in scala 1:1», correzioni, pausa e ripresa. L'IA progetta in cloud; la mod costruisce nel mondo **locale**, che rimane giocabile offline.

## Vincoli non negoziabili
- **Zero costi aggiuntivi:** nessuna chiave API a consumo, nessun hosting obbligatorio, nessun acquisto di crediti, nessun fallback a provider a pagamento.
- **Connessione reale con l'agente nel primo MVP funzionale**, non funzione opzionale da rimandare alla fine.
- Prima di costruire il resto, effettuare il gate tecnico di accesso con il piano ChatGPT: Sign in with ChatGPT per strumenti open source e, se adeguato, Codex App Server con accesso OAuth. Verificare un turno di inferenza realmente completato; l'elenco modelli non prova l'accesso.
- Se autenticazione o quota inclusa non sono disponibili, informare l'utente e sospendere soltanto i task AI, senza cambiare metodo di pagamento.
- **Scala predefinita 1:1:** 1 blocco = 1 metro; dettagli fedeli nei limiti dei dati disponibili e della griglia Minecraft.
- **Persistenza locale:** progetti, sezioni, checkpoint, backup e ripresa verificando i blocchi effettivamente presenti nel mondo. Non alterare i salvataggi esistenti senza consenso.
- Nessun server Minecraft né repository GitHub necessari durante il gioco; GitHub serve per il codice della mod.
- Mod ottimizzata per HP Pavilion i3-2100: applicazione di blocchi a lotti, senza eseguire il modello AI sul PC.

## Architettura proposta
1. **Blocco Banco IA + GUI Fabric:** campo per prompt, storico della conversazione, progetti, stato, pausa, riprendi, annulla.
2. **Bridge locale leggero:** connessione sicura tra mod e agente, con timeout, errori espliciti, token custoditi fuori da GitHub/log e consenso utente per accesso al piano.
3. **Agente Codex nel cloud (inferenza online):** interpreta la richiesta, consulta fonti accessibili e genera un piano strutturato, con controlli sulla qualità e sulla provenienza dei dati.
4. **Progetto persistente:** suddivisione in sezioni idempotenti e stato transazionale / journal: pianificata, in lavorazione, verificata, completata.
5. **Esecutore locale:** modifica il mondo gradualmente, rispetta i limiti di carico, salva checkpoint e può continuare offline quando i dati del progetto sono già stati scaricati.

## Priorità e test di accettazione
- **Gate 0 – costo e autenticazione:** login con ChatGPT e richiesta reale completata senza API key e senza credito/pagamento supplementare. Se non funziona, bloccare l'espansione dell'MVP; non sostituire con API a pagamento.
- **MVP 1 – flusso end-to-end:** aprire Banco IA in Minecraft, inviare «Costruisci una torre 1:1», ricevere un progetto dall'agente e iniziare a costruire nel mondo locale.
- **MVP 2 – recupero:** interrompere a metà, chiudere il gioco, riaprire, chiedere «Riprendi» e completare senza duplicati o perdita del progetto.
- **MVP 3 – fedeltà:** generare una piccola struttura reale documentata, confrontarla con misure e riferimenti, apportare correzioni.
- **Obiettivi successivi:** San Siro e San Vendemiano, in scala 1:1, con riferimenti leciti (es. OpenStreetMap e fotografie riutilizzabili).

## Verifiche ancora necessarie
Non è stato ancora sviluppato né testato un collegamento Minecraft–Codex in questo repository. L'autenticazione e le capacità di ricerca autonoma devono essere provate davvero; la disponibilità del login non garantisce che ogni funzionalità richiesta sia inclusa nella quota.

Documentazione:
- https://developers.openai.com/siwc/quickstart
- https://developers.openai.com/siwc/token-sharing-open-source/codex-app-server
- https://help.openai.com/en/articles/20001542-using-your-chatgpt-plan-in-other-apps-and-sites
