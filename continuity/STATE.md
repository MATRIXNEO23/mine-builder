# Stato corrente — Mine Builder

Aggiornato 29 ottobre 2026. Repository canonica: MATRIXNEO23/mine-builder, main.

## Confermato nel repository
- Base Fabric: src/main/java/it/minebuilder/MineBuilder.java e src/main/resources/fabric.mod.json.
- Configurazione build: settings.gradle, gradle.properties, build.gradle.
- Workflow GitHub Actions: .github/workflows/build.yml (test Node e build Gradle).
- Gate inferenza: scripts/verify-plan-sharing.mjs; test mock: scripts/verify-plan-sharing.test.mjs.
- Ultima modifica di codice osservata prima di questo aggiornamento: commit 8be5d3bd1c03ea599e089bdf1b7b583c00da48e9.

## Non verificato / non implementato
- Non è stato letto un esito riuscito di GitHub Actions; nessun JAR confermato.
- Test Node scritti, ma non ancora verificati come eseguiti con successo in CI.
- Bridge locale OAuth Sign in with ChatGPT assente: nessun login account reale.
- Nessuna richiesta GPT reale riuscita; accesso Plus, quote e disponibilità modelli non verificati.
- Nessun blocco Banco IA, GUI o costruzione automatica implementati.

## Vincoli
Zero costi aggiuntivi; niente API key a consumo o fallback a pagamento. Non inserire credenziali in GitHub. Minecraft Java 1.20.1, Fabric, Java 17, Prism; mondo locale. Non confondere mock con GPT reale.

## Prossima azione concreta
1. Ottenere esito del workflow GitHub Actions e correggere eventuali errori.
2. Consultare documentazione ufficiale aggiornata di Sign in with ChatGPT; implementare bridge OAuth locale usando soltanto interfacce documentate, senza inventare endpoint o token.
3. Testare login autorizzato e inferenza reale con consenso utente e senza crediti extra.
4. Realizzare Banco IA e GUI dopo il gate tecnico; aggiornare checkpoint dopo milestone verificata.

## Archivio permanente versioni — 29 ottobre 2026
- Aggiunto `.github/workflows/release.yml` per pubblicare JAR con nome versione e SHA256SUMS nelle GitHub Releases su tag vX.Y.Z, solo dopo test/build.
- Documentato in `docs/INSTALLAZIONE.md`. Commit release workflow: `85feacaede0d6a96999dd705ff2128f022199a73`.
- **Nessuna Release o JAR pubblicati/verificati ancora**; Actions artifact temporaneo non equivale a Release.
- URL permanente elenco versioni: https://github.com/MATRIXNEO23/mine-builder/releases

## Requisito confermato dall'utente — protezione del mondo (29 ottobre 2026)
- L'utente sceglie posizione e orientamento della struttura prima dell'avvio; prevedere anteprima dell'ingombro e conferma.
- La costruzione può scavare, livellare e sovrascrivere terreno naturale per fare spazio.
- Non deve sovrascrivere costruzioni preesistenti senza consenso esplicito. Minecraft non distingue sempre blocchi naturali da blocchi piazzati: in caso di incertezza bloccare la modifica e chiedere conferma.
- Prima di ogni modifica distruttiva, mostrare l'area coinvolta e predisporre un backup recuperabile; non promettere riconoscimento perfetto.
- Questo è un requisito di progetto approvato, NON una funzione già implementata o testata. È un gate obbligatorio prima della costruzione automatica.

## Progresso codice Banco IA — 29 ottobre 2026
- Commit `53c8915`: registrazione del blocco e oggetto Banco IA; sei asset blocco/item/localizzazione/loot aggiunti fino a `28bc373`.
- Commit `840c8d7`: interazione clic destro sul blocco; risponde con messaggio di indisponibilità GPT e NON modifica il mondo.
- Questi commit sono codice scritto e salvato, NON ancora confermati da compilazione o test in Minecraft. Nessuna GUI, pianificazione o protezione automatica implementata.
- Prima di qualsiasi scrittura nel mondo implementare controlli d'ingombro, classificazione conservativa, conferma esplicita e backup verificato.

## Sicurezza mondo — codice preliminare (29 ottobre 2026)
- Commit `6c7c724`: aggiunta `WorldSafety.java`, classificazione conservativa e SOLA LETTURA di blocchi vuoti, terreno comune e blocchi protetti/sconosciuti.
- Non viene ancora usata per costruire o cancellare blocchi; non dimostra l'origine dei blocchi (pietra piazzata da giocatore e pietra naturale possono essere indistinguibili).
- Mancano scansione dell'area, anteprima, backup, conferma, test compilazione e test in gioco. Nessuna modifica distruttiva è abilitata.
- Commit `10042a0`: test asset Banco IA salvati, non ancora collegati alla CI; tentativo di aggiornare workflow bloccato, non considerarlo riuscito.

## Decisione utente: installazione in un solo JAR
Il collegamento di rete deve essere interno alla mod, senza programma ponte separato. Commit codice: 1951a1c. Autenticazione GPT e inferenza reale ancora da implementare e verificare.
