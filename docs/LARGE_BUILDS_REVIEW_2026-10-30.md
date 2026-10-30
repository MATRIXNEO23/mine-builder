## Release separata verificata — 30 ottobre 2026

- [Release preview](https://github.com/MATRIXNEO23/mine-builder/releases/tag/mine-builder-composite-preview) prodotta dal run GitHub Actions 40995555653 (SUCCESS, SHA c2b859b8).
- JAR `Mine-Builder-v0.1.0-Minecraft-1.20.1-Fabric.jar` e SHA256SUMS.txt pubblicati senza modificare la release `mine-builder-latest` di main.
- Il percorso a componente e la lettura dei blueprint compressi ora **compilano**; nessun test di costruzione/recupero Minecraft reale eseguito.
- Corretto il recovery dei precedenti diari `complete-geometry.json`, precedentemente ignorati; parser dei due header GPT con retry, parser geometria con retry sui JSON malformati.
- Caveat: questa release è esclusivamente testabile, non sicura per mondi importanti, e non elimina limiti di blocchi e memoria.

---

# Mine Builder — Review grandi edifici / paesi (30 ottobre 2026)

## Fonte e stato
Repository MATRIXNEO23/mine-builder, main verificato inizialmente a 7e7e5a3. Ramo sperimentale `feature/composite-large-builds-20261030`, pull request #1: https://github.com/MATRIXNEO23/mine-builder/pull/1.
Il workflow grigio 40992403280 relativo a 5582d5ad risulta **CANCELLED**, non FAILED. Il codice del commit è incorporato dal successivo run verde 40992419917 (su f46eb4ee). Main non va ripristinato a una versione precedente.

## Bug e limiti emersi dal codice
1. `BuildPlan.parseGeometry` manteneva ±128 e Y -32..128 per `blocks` espliciti, mentre `cuboids/shapes` accettavano ±2000. Gli edifici grandi perdevano dettagli validi lontani dall'origine: corretto nel ramo.
2. Singola risposta geometrica per interi stadi/paesi; limite tecnico di 2 milioni di posizioni e 2 MB per specifica. Non adatto a città enormi: introdotto percorso sperimentale per componenti, il limite aggregato resta.
3. Journal recuperati per ordinamento alfabetico di UUID, non cronologico: ora usa data dell'ultima modifica con fallback.
4. UI ricostruiva il blueprint una seconda volta dalla stringa JSON sull'event loop client: il piano già validato viene ora consegnato direttamente.
5. `streamingConfirmed` era un ramo morto (nessuna assegnazione a true): rimosso, mantenendo lettura dei journal storici.
6. Possibile pubblicazione di una parte con conflitti rispetto alle parti precedenti: ora validazione di limiti e sovrapposizioni avviene **prima** del salvataggio atomico; se non è valida, rifiutare/riprovare senza sovrascrivere i file precedenti.

## Architettura sperimentale implementata
- `DesignContract.requiresComponents()`: modalità `GEOMETRY=COMPONENTS` decisa da GPT in pianificazione, non template fisso per tipo di edificio.
- `ComponentManifest`: 2..64 sotto-progetti con coordinate e descrizioni coerenti col piano globale; ID unici; bounds ±2000.
- `ComponentGeometryGenerator`: GPT genera un manifest una volta, poi un JSON per componente con ID e `complete=true`. Validazioni di bounds, intersezioni, limite aggregato, e build plan finale prima di abilitare `Costruisci`.
- `GenerationJournal`: `component-manifest.json` e `component-XX.json` persistiti nel salvataggio del mondo, con scrittura atomica; recupero dei componenti validi.
- `BancoIaScreen`: integra la generazione e il recupero; non esegue costruzione prima della validazione globale.
- CI su PR: compila e testa senza sovrascrivere la Release principale.

## Test e adozione
- CI 40994491465 e 40994624758: SUCCESS sul ramo di review (Fabric/Gradle, mock OAuth, DesignContract e ComponentManifest).
- Le CI dei commit successivi devono essere verificate; **nessuna verifica in Minecraft reale** su questa versione, né test autentico di quota GPT, qualità o recupero in caso di crash.
- Test di accettazione su **copia di mondo**: edificio a più ali, stadio suddiviso in tribune/copertura, piccolo quartiere; controllo facciate, porte, tetti, giunzioni e volumi; interruzione a metà componenti, riapertura e recupero; chunk non caricati, sezione occupata, quota invalida, prestazioni su hardware vecchio.
- **Non fare merge o installare sul mondo importante** senza review separata di rischio, continuità e risultati dei test.

## Ancora necessario
1. Molti più di 2 milioni di blocchi con stream da disco/componenti senza assemblare tutto in RAM; verifica coordinate e hashing manifest per salvataggi enormi.
2. Verifica planimetrica e qualità 1:1 con fonti autentiche, gap e giunzioni; non assumere che `complete=true` implichi completezza.
3. Preflight dell'intero progetto, backup recuperabile, conferma esplicita e distinzione conservativa dei blocchi del giocatore. `BuildExecutor` tratta ancora alcuni blocchi comuni come terreno naturale e salva chunk sincronicamente.
4. Identità mondo/dimensione forte e snapshot per ciascun progetto; recovery transazionale del singolo componente, non solo file atomici.
5. Test runtime reali, test a carico e blocco certo di qualsiasi fallback a pagamento.

**Vincoli invariati:** Minecraft 1.20.1 Fabric, singolo JAR, nessun costo aggiuntivo, progetti e mondi esistenti intatti.
