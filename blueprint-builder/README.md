# Blueprint Builder — Minecraft Java 1.20.1 / Fabric / Java 17

Mod autonoma con ID `blueprintbuilder`, package `it.blueprintbuilder`, propri salvataggi e JAR. Può convivere con Mine Builder: non usa token, GPT o richieste di rete. Progetto nuovo autorizzato dall'utente: importare file già generati e costruirli con console sempre visibile.

## Uso
1. Installare Fabric API 1.20.1 e `Blueprint-Builder-v0.1.0-Minecraft-1.20.1-Fabric.jar` nell'istanza Prism. Lasciare il JAR Mine Builder esistente se si desidera: ID distinti.
2. Entrare in un mondo di prova singleplayer. Premere **B** (rimappabile in Opzioni → Comandi → Blueprint Builder).
3. Premere **Carica blueprint** per il selettore file. In alternativa incollare il percorso completo nel campo oppure trascinare un solo file nella schermata. **Apri cartella blueprint** apre `config/blueprintbuilder/imports`.
4. Aspettare validazione. Impostare X/Y/Z come origine delle coordinate del file, scegliere rotazione 0/90/180/270, poi **Costruisci qui**. Il gioco può restare aperto durante la creazione esterna del file; le nuove installazioni del JAR richiedono riavvio.
5. La console mostra validazione, controllo globale area, attesa chunk, posa e verifica finale. Pausa/Continua agisce sul lavoro corrente. Riprendi salvato rilegge il progetto nel mondo, ricontrolla l'area e salta blocchi già identici: non richiede GPT.

## Formato accettato
JSON UTF-8 o GZIP JSON (`.json.gz`). Non importa `.schem`, `.litematic` o `.nbt`. Limite 64 MB decompressi e 2 milioni di posizioni. Accetta il formato geometrico Mine Builder: arrays `cuboids`, `shapes`, `blocks`; oggetto JSON radice obbligatorio. Metadati facoltativi `format_version:1`, `minecraft_version:"1.20.1"`. Se dichiarati e incompatibili, il file viene rifiutato.

Esempio completo: `examples/test-pedestal.json`. Schema compatto e stati blocco descritti nel sorgente `BuildPlan.GEOMETRY_SCHEMA`. Materiali del registro reale Minecraft, proprietà dentro la stringa `block`, ad esempio `minecraft:quartz_slab[type=top]` o `minecraft:quartz_stairs[facing=east,half=bottom,shape=straight]`. Il builder conserva e ruota gli stati. Gli hopper sono blocchi importabili; inventari, entità e NBT non sono implementati e il relativo contenuto è rifiutato. Non promette la corretta posa di ogni blocco dinamico senza prove specifiche.

## Protezione e limiti
Il progetto libera e ricostruisce l'intero parallelepipedo di ingombro: le posizioni non specificate diventano aria, cosi il terreno non taglia interni e aperture. L'ingombro massimo e 2 milioni di posizioni: oltre questo limite viene rifiutato senza ridimensionamento. Sovrascrive il terreno e i blocchi nell'ingombro su richiesta dell'utente. Prima di modificare il mondo salva un backup locale dell'intero volume, inclusi gli NBT dei contenitori. Se il backup fallisce, la posa non parte. Ripristina ultimo backup ripristina quell'area e ne salva prima lo stato corrente. Non tocca entita mobili (animali, item) o inventari dei giocatori. Backup in `saves/<mondo>/blueprintbuilder/backups/`.

La mod si ferma e mostra un errore se una posizione cambia dopo il backup: non ignora blocchi e non dichiara completate strutture parziali. Un errore durante posa puo comunque lasciare una costruzione parziale; usare ripresa o backup. Il ripristino dei contenitori e implementato ma da collaudare nel gioco; non usare mondi importanti prima della prova.

La costruzione viene applicata a lotti con budget del tick; prima i solidi dal basso verso l'alto, poi liquidi, con posa forzata degli stati per preservare orientamenti. La verifica finale confronta l'intero progetto con il mondo e NON dichiara successo se gli aggiornamenti del gioco hanno cambiato stati. Blocchi con gravità/supporti, redstone, collegamenti e fluidi richiedono test reali. I chunk non caricati mettono in attesa: avvicinarsi o aumentare distanza di simulazione, non vengono forzati automaticamente. Il salvataggio completo finale dei chunk può causare una pausa.

Progetti persistenti in `saves/<mondo>/blueprintbuilder/`; nessuna cartella Mine Builder modificata. Il progetto sospeso non viene sovrascritto da un nuovo lavoro. Il resume dopo arresto deve ancora essere collaudato in Minecraft; i file sono salvati prima dell'avvio, la verifica al riavvio è idempotente.

## Build e distribuzione
Il workflow autonomo `.github/workflows/blueprint-builder.yml` compila solo questo sottoprogetto, esegue test contratto e pubblica il JAR in una release distinta `blueprint-builder-latest` e in `downloads/Blueprint-Builder-v0.1.0-Minecraft-1.20.1-Fabric.jar`. Non sostituisce i JAR Mine Builder.
