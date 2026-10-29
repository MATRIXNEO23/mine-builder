# Mine Builder — installazione e primo avvio (guida utente)

> **Stato: non ancora installabile.** La repository contiene documentazione e un modulo preliminare per verificare il collegamento GPT, non una mod `.jar` pronta. Non eseguire installazioni aggiuntive finché non viene pubblicata una release verificata.

## Cosa hai già
- Prism Launcher installato e Minecraft Java Edition 1.20.1 funzionante sul tuo PC.
- Un account ChatGPT Plus (l'accesso di Mine Builder tramite il piano è ancora da collaudare realmente).

## Cosa servirà, quando sarà pronta una release
1. Aprire **Prism Launcher**, scegliere **Aggiungi istanza** e creare un'istanza separata per **Minecraft 1.20.1** con **Fabric**, mantenendo intatta l'installazione attuale.
2. In **Modifica istanza → Impostazioni → Java**, verificare che l'istanza usi **Java 17**. Scaricarlo solo se manca, dopo aver verificato il sistema operativo.
3. In **Modifica istanza → Mods**, aggiungere **Fabric API** compatibile con Minecraft 1.20.1 e il futuro **MineBuilder.jar**. Le dipendenze effettive saranno indicate nella release; non installare WorldEdit se non viene richiesto.
4. Creare un **mondo di prova** oppure fare un backup del proprio mondo prima di attivare la costruzione automatica.
5. Avviare il gioco, posizionare il **Banco IA** (quando implementato), aprire l'interfaccia e seguire il login ufficiale a ChatGPT se richiesto. Non copiare mai password, token o chiavi nella chat o su GitHub.
6. Eseguire il **test di connessione reale**: ottenere una risposta completata dal modello GPT disponibile nel proprio piano, senza API key fatturata e senza acquisto di crediti.
7. Solo dopo il superamento del test: provare **«Costruisci una torre»**, mettere in pausa, chiudere e riaprire Minecraft, poi usare **«Riprendi»**. Verificare che i blocchi restino nel mondo offline.

## Programmi aggiuntivi sul PC
- **Per giocare:** l'obiettivo è che bastino Prism Launcher, Java 17 e i file della mod/dipendenze. Non sarà necessario installare Git, Gradle o un editor di codice.
- **Per il login GPT:** è ancora da stabilire se il componente di collegamento possa essere incorporato nella mod o debba essere distribuito come piccolo programma locale separato. Non presentare questa parte come già funzionante.
- **Nessun server Minecraft a pagamento, nessuna API key a consumo.** Se il servizio non può operare con il piano già posseduto, il sistema deve fermarsi senza generare costi.

## Stato delle attività
- [x] Requisiti e architettura documentati.
- [ ] Test autentico di accesso GPT con account Plus.
- [ ] Collegamento locale sicuro e Banco IA Fabric.
- [ ] Prima release `.jar` compilata e testata.
- [ ] Test costruzione, salvataggio e ripresa.

**Importante:** non modificare i mondi esistenti né installare programmi non ancora verificati. Le istruzioni definitive potranno essere aggiornate dopo la prima build reale.