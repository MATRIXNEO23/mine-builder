# Review: richiesta GPT, blueprint, interfaccia e posa

## Ambito e prove
Sorgenti verificati sulla base 9605e5a (codice 0296872, CI 41031620396 verde). Review statica, non test Minecraft. Il test utente della villa aveva fallito sul parsing numerico; nessun JSON di quel tentativo è disponibile qui. Non si può certificare la qualità architettonica senza blueprint e mondo risultante.

## Correzioni di questa revisione
- Pulsanti abilitati in funzione di mondo, connessione, richiesta, lavoro in corso, blueprint validato, progetto selezionato e file locali di recupero. Stato aggiornato a ogni tick e prima del disegno.
- Costruisci richiede un piano validato e nessuna generazione/recupero/login/esecuzione attiva. La stessa regola viene controllata nel gestore: disabilitare il solo pulsante non bastava.
- Tutti i percorsi di assegnazione del piano (generazione, libreria, diari singoli, composti e storici) attraversano la validazione tecnica e di altezza prima di rendere disponibile il progetto.
- In caso di errore finale di validazione, il piano pendente viene cancellato e l'abilitazione revocata.
- Recupero paginato storico segnato occupato; chat segnata occupata; fine generazione applicata sul thread client insieme all'esito. Recupero manuale rifiutato durante un lavoro.
- Progetta richiede connessione e richiesta non vuota; replica richiede selezione; salva richiede progetto e nome; recuperi richiedono presenza dei relativi file. Controlli dei file a inizializzazione e uscita dallo stato occupato, non a ogni frame.
- ActionGateTest copre avvio, generazione, vecchio piano durante operazione, piano non validato, assenza del mondo e completamento.

## Problemi ancora aperti (non dichiararli risolti)
1. ALTA: nessun backup/rollback completo e riconoscimento certo dei blocchi artificiali; controllo conservativo parziale. Usare mondo di test.
2. ALTA: controllo liquidi usa la sezione corrente, non tutti i vicini del progetto. Piscine su confine di sezione possono essere rifiutate; ordine di posa e aggiornamenti fluidi da provare prima di cambiare il criterio di contenimento.
3. ALTA: stato di esecuzione statico e passaggi fra mondi/dimensioni richiedono test specifici; non è stata introdotta in questa patch una nuova identità persistente dei job.
4. MEDIA: il parser conserva proprietà valide di scale/slab/porte, ma quelle omesse assumono default Minecraft. Il prompt non dettaglia ancora tutti gli stati, e presenza di scale non dimostra collegamento fra piani. Occorre una fixture architettonica con prova reale di posa, rotazione e reload.
5. MEDIA: setBlockState con aggiornamenti vicini e verifica esatta di sezione possono interagire con porte, recinzioni, scale e acqua. Necessario test in gioco; non eliminare le verifiche per nascondere errori.
6. MEDIA: limite 2 milioni, assemblaggio globale in memoria, ripetute partizioni/validazioni e save chunk sincrono. Prestazioni reali da misurare; la CI non dimostra fluidità su i3.
7. MEDIA: la disponibilità dei recuperi è preliminare (presenza checkpoint/journal), non garanzia di integrità o di credenziali sufficienti per completare parti mancanti. Il recupero verifica poi il contenuto e mostra errori.
8. MEDIA: layout affiancato non provato su tutte le scale GUI; menu lunghi e risoluzioni basse richiedono collaudo visivo.
9. MEDIA: risposta GPT non valida ora conservata, ma non tutti i diari incompleti ripartono automaticamente dal solo master plan. Non promettere ripresa universale.

## Collaudo richiesto
1. Senza login/richiesta/blueprint: Progetta/Costruisci disabilitati.
2. Login e richiesta: Progetta attivo, Costruisci disabilitato.
3. Progettazione, geometria e recupero: Costruisci sempre disabilitato, nessuna richiesta concorrente.
4. Errore JSON/altezza: Costruisci resta disabilitato; log completo conservato.
5. Blueprint valido: Costruisci si abilita; dopo consegna al martello si disabilita.
6. Generare villa, ispezionare scale/slab/materiali e collegamenti, posare su mondo nuovo, verificare piscina e rotazione; poi interrompere e recuperare.
7. Ripetere libreria/diari e passaggi fra schermate e mondi. Nessun merge/adottabilità automatica derivante dalla sola CI.
