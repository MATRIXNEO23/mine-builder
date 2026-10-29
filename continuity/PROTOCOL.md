# Protocollo di continuità — Mine Builder

## Priorità e identità
- Progetto canonico: `MATRIXNEO23/mine-builder`, ramo `main`.
- Ordine di recupero: repository reale e log dei commit → ultimo checkpoint confermato → `STATE.md` → `README.md` e codice → istruzioni conversazionali recenti compatibili.
- Non usare i riassunti della chat come prova che un file o una build esistano.

## Come inizia una nuova istanza
1. Leggere `README.md` e `AGENTS.md`.
2. Leggere `continuity/PROTOCOL.md`, `continuity/STATE.md`, `continuity/CHECKPOINTS.md` e `continuity/HANDOFF.md`.
3. Ispezionare HEAD attuale, tree, sorgenti, CI e risultati dei test. Verificare che gli elementi elencati in STATE esistano. Fare `git status` se si dispone di checkout.
4. Se stato e repository divergono, registrare la divergenza e riconciliare **prima di modificare**. Non ricreare o sovrascrivere lavoro presente.
5. Proseguire dal prossimo passo irrisolto, includendo gli eventuali blocchi. Prima di dichiarare "finito", eseguire e documentare verifiche reali.

## Operazione «fine istanza»
Questa procedura si attiva **quando l'utente scrive «fine istanza» all'assistente in una sessione che ha accesso alla repo**. Non è un trigger GitHub né un'automazione attiva in background.

1. Leggere HEAD e le modifiche effettuate in questa sessione; confrontare lo stato attuale e le evidenze di test.
2. Aggiornare `continuity/STATE.md`: stato vero, commit di riferimento precedente, attività concluse, attività pendenti, ostacoli, prossima azione *concreta*, vincoli e test.
3. Appendere una nuova voce datata in `continuity/CHECKPOINTS.md` con ID univoco, stato, evidenze e commit di riferimento. Non eliminare i checkpoint storici.
4. Aggiornare `continuity/HANDOFF.md` con un **prompt completo da copiare** nella nuova conversazione, che chiede recupero obbligatorio dalla repo, confronto con HEAD e ripresa, senza ricostruire per supposizioni.
5. Fare commit dei tre file. Verificare mediante nuova lettura che il commit e i contenuti esistano su GitHub. Il checkpoint e il commit sono il riferimento persistente: se il SHA dell'HEAD cambia nel commit stesso, non usare un SHA inventato nel documento.
6. Rispondere con l'esito effettivo del salvataggio, il commit ottenuto, la prossima attività e il prompt di riavvio già pronto. Se un passaggio fallisce, dire quali modifiche non sono state salvate e offrire un prompt di recupero che segnali il limite.

## Checkpoint intermedi
Eseguire la stessa verifica e aggiornamento di STATE + cronologia dopo milestone sostanziali (progetto compilabile, test reali, decisioni che cambiano architettura), anche senza «fine istanza».

## Regole di affidabilità
- `FATTO` solo per commit e artefatti realmente esistenti; `TESTATO` solo con evidenza dei risultati; `PREVISTO` per funzionalità non presenti.
- Non aggiungere automaticamente risorse a pagamento; blocco senza addebiti se finisce la quota.
- Non salvare token, password, segreti, dati personali o file del mondo nella repo pubblica.
- La memoria GitHub preserva la **continuità del progetto**, non rende il sistema immune ad allucinazioni. Ogni nuova istanza deve leggere e verificare i file.
