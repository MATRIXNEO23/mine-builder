# MATRIX Architect — addon separato per WorldEdit

Minecraft Java 1.20.1, Fabric, Java 17. Richiede Fabric API e **WorldEdit 7.2.15 Fabric** (versione verificata: Modrinth `Wrlqaul6`, JAR `worldedit-mod-7.2.15.jar`). WorldEdit rimane separato e non viene rinominato, modificato o incorporato nel JAR dell'addon. Nessun GPT, API a consumo o server esterno.

## Installazione e uso senza comandi
1. Mettere WorldEdit, Fabric API e `MATRIX-Architect-v0.1.0-Minecraft-1.20.1-Fabric.jar` nella cartella mods dell'istanza Prism. Riavviare dopo installazione.
2. Entrare in un mondo singleplayer di prova con comandi abilitati. Premere **K** per aprire il pannello: Carica schematic apre il browser file dentro Minecraft: cartella personale, Download, dischi/unita e cartella superiore. Aprire le cartelle, selezionare il .schem e premere Apri schematic selezionato. In alternativa trascinare un file. Non usa piu finestre AWT del sistema e non richiede di scrivere percorsi. Il pannello include console e copia registro.
3. Prendere la **Bacchetta MATRIX Architect** dall'inventario creativo (Strumenti) oppure dal pulsante Prendi bacchetta in Creativa. Tenerla nella mano principale.
4. Chiudere il pannello: camminare/volare normalmente e puntare un blocco entro 64 m. Una cornice verde mostra l'ingombro del file con la sua origine effettiva.
5. Clic destro su un blocco blocca il punto. **R** ruota 90 gradi; **frecce** spostano X/Z di un blocco; **Page Up/Page Down** regolano l'altezza; **L** riprende il puntamento e azzera l'offset verticale.
6. **P** conferma la posa con WorldEdit. Puntare o fare clic NON incolla. Include i blocchi aria e sovrascrive il terreno secondo lo schematic. Posizione e rotazione vengono congelate alla pressione di P.
7. **U** annulla l'ultima operazione della cronologia WorldEdit della sessione: può essere anche un'operazione fatta con WorldEdit fuori dall'addon. **K** oppure Maiusc+clic destro riapre il pannello. Tutti i tasti sono rimappabili in Opzioni → Comandi → MATRIX Architect.

## Confini e limiti reali
- Anteprima: **ingombro**, non rendering di tutti i blocchi. Gli spazi vuoti dipendono dal contenuto del file; un file incompleto non viene ricostruito automaticamente.
- Controlli: formato riconosciuto da WorldEdit, max64 MB compressi, max2 milioni di posizioni, dimensione e altezza mondo, confine e chunk caricati prima della posa. Entità mobili nello schematic rifiutate esplicitamente. Non è una garanzia di assenza di esaurimento memoria su file ostili.
- Solo server integrato singleplayer: multiplayer non implementato; rispetta i permessi WorldEdit. Il pannello e la bacchetta non aggirano i permessi.
- Incolla via WorldEdit EditSession/PasteBuilder; rotazione tramite AffineTransform. Il test automatico confronta la matematica dell'anteprima con quella dell'incolla per tutte le rotazioni di 90 gradi.
- WorldEdit può occupare il thread server durante operazioni grandi: vengono mostrati inizio, fine ed errori, **non un conteggio progressivo inventato dei blocchi**. Non è una posa a lotti né un comando annullabile mentre in esecuzione.
- Le modifiche sono registrate nella cronologia WorldEdit anche in caso di errore parziale. Undo non è un backup persistente e non è garantito dopo chiusura/crash. Fare una copia del mondo prima del collaudo; non è stato implementato un backup esterno nella GUI.
- Menu, cornice nel mondo, posa, undo e compatibilità reale vanno collaudati in Minecraft sul PC: compilazione e test matematici non equivalgono a collaudo nel mondo.

## Distribuzione
Workflow autonomo `.github/workflows/matrix-architect.yml`, release `matrix-architect-latest` e JAR/checksum nella cartella `downloads` del repository. Le altre due mod (Mine Builder e Blueprint Builder) e i loro JAR restano distinti. Sorgenti addon in questa cartella; licenza GPL-3.0-or-later, dipendenza WorldEdit distribuita separatamente da EngineHub.
