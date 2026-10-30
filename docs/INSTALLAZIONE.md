# Mine Builder — installazione e collaudo

## Quale build usare

La versione principale (`main`) e l'anteprima grandi edifici/paesi sono distinte. La nuova architettura per componenti è sperimentale nella [PR #1](https://github.com/MATRIXNEO23/mine-builder/pull/1).

**Anteprima separata (solo dopo CI verde):** [Mine Builder — composite preview](https://github.com/MATRIXNEO23/mine-builder/releases/tag/mine-builder-composite-preview). Contiene il JAR Fabric 1.20.1 e un file `SHA256SUMS.txt`. La release potrebbe non esistere ancora se la compilazione/validazione non è terminata.

**Versione principale:** [Elenco Releases](https://github.com/MATRIXNEO23/mine-builder/releases).

Una CI verde dimostra compilazione e controlli automatici. Non prova che l'IA abbia prodotto una casa corretta o un paese fedele, né che la posa sia sicura su mondi esistenti.

## Installazione in sicurezza

1. In **Prism Launcher** crea un'istanza di prova **Minecraft Java 1.20.1 + Fabric + Java 17**, separata da quella abituale.
2. Aggiungi **Fabric API** per 1.20.1 e **un solo** JAR Mine Builder, tramite **Modifica istanza → Mod**. Non installare due versioni della mod insieme.
3. Crea **un mondo nuovo dedicato ai test**; non usare il mondo principale o una copia senza un backup esterno verificato. Il motore attuale può sostituire alcuni tipi di terreno e non ha ancora un backup/rollback affidabile.
4. Apri il Banco IA, collega ChatGPT attraverso il browser e verifica che una risposta GPT arrivi davvero. Non inserire in chat, repository o log chiavi/API token. Nessun acquisto di crediti o API a consumo è autorizzato: se il piano non consente l'inferenza, fermarsi.
5. Testa prima una struttura piccola, poi un complesso a più edifici. GPT decide `GEOMETRY=SINGLE` per geometrie semplici oppure `GEOMETRY=COMPONENTS` per grandi strutture; il sistema salva un manifest e i componenti nel diario del mondo.
6. Prima di costruire, verifica con **Dettagli e controlli** e **Console LIVE** il numero dei blocchi e l'eventuale presenza di errori. Se viene indicato progetto incompleto, quota esaurita, altezza invalida o limite di 2 milioni, non cercare di forzare la posa.
7. Con il **martello Builder** scegli posizione e rotazione; avvia esclusivamente nel mondo di prova. Controlla facciate, porte, tetti, giunzioni, interni e zone circostanti.
8. Per testare il recupero, interrompi prima la generazione o la costruzione nel mondo di prova, riavvia e usa **Recupera diario GPT** o **Riprendi costruzione**. Se compare un errore, conserva journal e log per la diagnosi; non cancellare manualmente file dal mondo.

## Cosa è implementato e cosa no

- Presente nel codice: Banco IA, OAuth locale con PKCE, richieste GPT in streaming, progetto architettonico, JSON geometrico, generazione per componenti (nell'anteprima), checkpoint, copia blueprint in formato compresso con lettura del formato storico.
- Ancora da dimostrare nel gioco: inferenza con quota inclusa, ripresa dopo crash reale, prestazioni su hardware datato, fedeltà architettonica.
- Non completato: gestione illimitata di interi paesi, backup/rollback del mondo, protezione perfetta di blocchi artificiali indistinguibili da terreno, costruzione interamente non bloccante. Limite attuale **2 milioni di posizioni** per progetto.

**Nessun costo aggiuntivo:** niente API a consumo, server a pagamento o fallback con addebiti. Minecraft resta locale, e i progetti salvati possono essere consultati anche offline.

## Diagnostica

- Log console: `config/minebuilder/generation-console.log` nell'istanza Prism.
- Diari generazione: `saves/<nome-mondo>/minebuilder/generation-journal/`.
- Blueprint: `saves/<nome-mondo>/minebuilder/blueprints/` (nuovi file `.json.gz`; vecchi `.json` leggibili).
- Workflow CI: [GitHub Actions](https://github.com/MATRIXNEO23/mine-builder/actions).
- Architettura e limiti: [Review grandi edifici](LARGE_BUILDS_REVIEW_2026-10-30.md).
