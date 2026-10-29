# Istruzioni obbligatorie per ogni nuova istanza / agente

**Questo progetto usa GitHub come fonte di verità. Il contenuto della chat NON sostituisce il repository.**

## Avvio di ogni istanza (prima di scrivere codice o fare affermazioni sullo stato)
1. Aprire la repository `MATRIXNEO23/mine-builder`, ramo `main`, e leggere il README.
2. Leggere integralmente `continuity/PROTOCOL.md`, `continuity/STATE.md`, `continuity/CHECKPOINTS.md` (ultimo checkpoint) e `continuity/HANDOFF.md`.
3. Verificare l'HEAD reale e i file effettivamente presenti. Confrontare lo stato documentato con i commit e i test. **Se ci sono incongruenze, NON inventare né ripartire da zero: riconciliare prima di procedere.**
4. Riprendere dal campo "Prossimo passo" di `continuity/STATE.md`; preservare decisioni, vincoli e lavoro già completato.
5. Separare sempre `proposto`, `codice scritto`, `testato`, `funzionante sull'utente`. Non trasformare una proposta in un risultato confermato.

## Regole vincolanti
- Zero costi aggiuntivi: nessun acquisto di API, crediti, server o servizi. Non usare fallback a pagamento.
- La prima versione utilizzabile deve collegarsi **realmente** a un modello GPT tramite piano ChatGPT supportato. Accesso e quota devono essere testati, non presunti.
- Minecraft Java 1.20.1, Fabric, Prism Launcher; il mondo resta locale/offline. Ricostruzioni reali 1:1; creazioni fantasy libere; costruzione a sezioni con salvataggi.
- Non mettere credenziali, token, chat private, file di salvataggio dei mondi o dati personali nel repository pubblico.
- Proteggere i salvataggi locali e non cancellare contenuti esistenti senza autorizzazione.
- **A ogni milestone verificata**, aggiornare `continuity/STATE.md` e aggiungere un checkpoint nella cronologia.
- Se l'utente scrive esattamente **«fine istanza»**, eseguire `continuity/PROTOCOL.md` (checkpoint e prompt per nuova istanza) prima di rispondere. Se la scrittura fallisce, dichiararlo; non sostenere che sia stato salvato.
- Questo file è un'istruzione per gli agenti che accedono alla repo, non garantisce che ChatGPT apra automaticamente il repository in ogni nuova chat. Per il cold start usare il prompt in `continuity/HANDOFF.md`.

## Prevenzione allucinazioni
Qualsiasi fatto nuovo su codice, connessioni, versioni e test va riscontrato nei file, nei log o in fonti documentate; niente comandi inventati. Fermarsi e segnalare i dati mancanti se una decisione critica dipende da informazioni non verificabili.
