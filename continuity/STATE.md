## Milestone: build testabile grandi edifici — 30 ottobre 2026

**Anteprima isolata pubblicata:** [mine-builder-composite-preview](https://github.com/MATRIXNEO23/mine-builder/releases/tag/mine-builder-composite-preview), JAR Minecraft 1.20.1 Fabric Java 17 e SHA256SUMS, CI 40995555653 SUCCESS sul commit c2b859b8. Ramo `feature/composite-large-builds-20261030`, PR #1, **NON integrato in main**. La release preview non deve essere confusa con `mine-builder-latest`.

Risolte nel ramo: limite esplicito coordinate ±128 incoerente con ±2000; manifest dei componenti immutabile e validazione conflitti prima del journal; recupero sia `complete-geometry.json` sia `component-manifest.json`; ordinamento journal non basato su UUID; passaggio diretto del blueprint alla GUI; salvataggio blueprint GZIP in streaming e lettura retrocompatibile dei vecchi `.json`; retry sul contratto architettonico non valido e JSON malformato; workflow di preview che pubblica solo dopo test/compilazione.

**Evidenze:** CI 40995422772 SUCCESS (salvataggi compressi compilati), 40995488862 SUCCESS (contratto GPT e retry), 40995555653 SUCCESS (build e release preview). Test nel mondo Minecraft NON eseguiti qui. La build è **scaricabile per collaudo** e non certificata come sicura su mondi importanti.

**Limitazioni non risolte:** limite 2 milioni di blocchi totali, max 64 componenti, verità/fedeltà delle fonti, backup/rollback del mondo, distinzione totale fra blocchi naturali e piazzati, preflight progressivo su chunk lontani, salvataggi chunk che possono causare lag, quota inclusa GPT non verificata, verifica runtime su i3. Test SOLO su nuova istanza Prism e mondo nuovo.

**Prossimo passo:** collaudo manuale (casa, stadio in componenti, piccolo quartiere, arresto/riavvio, area occupata, profilo prestazionale). Conservare screenshot ed errori e correggere eventuali regressioni prima di valutare merge.

---

## Aggiornamento review grandi progetti — 30 ottobre 2026 (ramo di prova)

Fonte reale: main 7e7e5a3; il workflow grigio 40992403280 è CANCELLED (non un errore di compilazione), e il successivo 40992419917 è SUCCESS. Ultimi commit main ampliano coordinate grandi ma lasciano il limite esplicito a ±128 e singolo JSON geometrico.

**Implementazione in corso NON integrata in main:** branch `feature/composite-large-builds-20261030`, PR #1. Introdotti manifest globale di componenti, scelta GPT SINGLE/COMPONENTS, salvataggio atomico e recupero dei componenti, controllo bounds e conflitti prima di registrare parti valide, fix coordinate esplicite ±2000, consegna diretta BuildPlan alla GUI, protezione richieste concorrenti e ordinamento journal per modifica. CI preliminari 40994491465 e 40994624758: SUCCESS. Ogni commit successivo necessita di propria CI, e il test in Minecraft **non è eseguito**.

**Mancanze critiche:** limite totale 2 milioni ancora presente, qualità/giunzioni e fedeltà 1:1 non dimostrate, preflight globale/backup e ripresa dopo crash non collaudati, protezione blocchi artificiali incompleta, save chunk potenzialmente bloccante, inferenza reale e quota senza costi non confermate qui. Mai confondere compilazione con build sicura.

**Prossimo passo:** confermare CI finale della PR, provare JAR del ramo solo su copia di mondo, controllare crash/restart e giunzioni; valutare separatamente l'adozione e poi affrontare streaming geometrico senza caricare tutto in RAM. Nessuna scrittura distruttiva autorizzata su mondi importanti.

Documentazione dettagliata: `docs/LARGE_BUILDS_REVIEW_2026-10-30.md`.

---

## Aggiornamento migrazione v3 — seconda e terza fase
CI del commit 90ef7276 confermata SUCCESS. Architettura estratta in ArchitecturalPlanner e GeometryGenerator; aggiunti test DesignContractTest in CI. Ricerca estesa a Wikipedia (contesto testuale) e Commons (immagini); recupero origine scritto atomicamente prima del progress marker; retry limitato su blueprint JSON non valido. Codice fino a commit 66c14e31, CI ultima revisione da verificare.

NON DICHIARARE COMPLETATO: geometria multi-componenti per grandi città, ricerca universale e affidabilità di fonti/planimetrie, completa separazione dei journal dalla GUI, backup mondo/anteprima e validazione runtime Minecraft. Nessun test in gioco effettuato da questa istanza. Non installare automaticamente JAR sperimentale su mondi importanti. Prossima azione: CI verde, poi test in-game con copia di mondo, audit crash/resume e progettazione componenti grandi senza incoerenze.

## Stato aggiornato — 30 ottobre 2026 — migrazione v3 in corso
La sezione storica seguente è un archivio del 29 ottobre, NON lo stato attuale. Il repository contiene ora GUI Banco IA, OAuth, inferenza GPT, parser geometrico ed esecutore; CI precedente verde ma non test in-game. Audit dell'HEAD precedente: 51aa194b8d7535e136ae1bef341078fead755f28.

Interventi attuali: `DesignContract.java` (GPT sceglie NONE/INSPIRATION/RECONSTRUCTION e fornisce master plan), `BlueprintContract.java` (confine tecnico), `BancoIaScreen.java` (ricerca automatica; rimossi brief/master plan duplicati e fallback paginato irraggiungibile), `BuildExecutor.java` (validazione blueprint condivisa), `docs/ARCHITECTURE_V3.md` (contratti e limiti). Commit fino a 26ac2938; CI di questo set ancora da confermare. Nessun test in gioco eseguito in questa sessione.

NON COMPLETATO: estrazione completa dell'orchestrazione dalla GUI; ricerca web oltre Wikimedia; generazione incrementale robusta di progetti enormi; miglioramento salvataggi e identità mondo; test runtime e qualità. Non dichiarare migrazione conclusa. Prossima azione: verificare CI, correggere eventuali errori, poi migrare orchestrazione e progettare generazione geometrica a componenti senza perdita di continuità.

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

## Verifica ufficiale accesso GPT — 29 ottobre 2026
- Documentazione OpenAI ufficiale verificata per Sign in with ChatGPT in app open-source locali; uso del piano idoneo con consenso e scope dedicato, senza API key. Dettagli e sequenza in docs/GPT_INTEGRATION_GATE.md (commit 1d1e00a).
- Non è ancora stato implementato o testato OAuth nel JAR, né una risposta GPT reale. Non considerare risolto il gate.
- L'utente ha confermato test reale in Minecraft: Banco IA presente tramite comando give, clic destro apre la GUI. Aspetto e inventario sono in sviluppo.
- Prossima azione: implementare nel JAR flusso OAuth documentato con PKCE, verifica token/scope e test inferenza; quindi piano strutturato e costruzione sicura.
