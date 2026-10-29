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
