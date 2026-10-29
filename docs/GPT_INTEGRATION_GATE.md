# Collegamento GPT — verifica tecnica 29 ottobre 2026

## Fatto verificato
OpenAI documenta **Sign in with ChatGPT per applicazioni open source locali**, con uso del piano idoneo senza chiave API a consumo. Il login e il consenso all'utilizzo del piano sono separati. Documentazione:
- https://developers.openai.com/siwc/quickstart
- https://developers.openai.com/cookbook/articles/sign-in-with-chatgpt
- https://openai.com/policies/sign-in-with-chatgpt-terms/

Questo rende **plausibile** il requisito di un solo JAR Fabric, senza bridge installato separatamente: il codice Java della mod potrebbe gestire browser OAuth, callback locale e richieste HTTPS. **Non equivale a integrazione già funzionante.**

## Implementazione necessaria, in ordine
1. Studiare il contratto ufficiale di registrazione dinamica client open-source e callback localhost, senza inventare endpoint. Generare un host ID locale persistente e PKCE; usare browser di sistema per il consenso.
2. Conservare client ID e token **solo sul dispositivo**, mai in GitHub, chat o log; validare ID token, nonce, issuer, audience e scope effettivamente concessi. Rifiutare ogni fallback a crediti/API a consumo.
3. Eseguire scoperta modelli e una vera richiesta Responses API in streaming, con store=false; confermare un evento di completamento. Solo allora abilitare il tasto Invia nel Banco IA.
4. Richiedere a GPT un piano **strutturato** (dimensioni, palette, blocchi, coordinate relative); validare ID blocchi, limiti e dimensioni localmente. Non eseguire testo libero come comandi.
5. Anteprima dell'ingombro, scelta posizione e orientamento, controllo conservativo di strutture preesistenti, backup recuperabile e conferma esplicita prima di scrivere nel mondo. Costruire a lotti con journal/checkpoint e ripresa idempotente.
6. Test in Minecraft 1.20.1 Fabric/Prism: login reale, inferenza reale, prima costruzione, riavvio e recupero.

## Stato effettivo
- Confermato dall'utente: il clic destro apre Banco IA.
- **Non implementati**: OAuth, token storage, inferenza GPT, generazione piano, applicazione blocchi.
- Nessuna prova di accesso reale al piano Plus tramite la mod.
- Vincoli invariati: un solo JAR, zero spese aggiuntive, mondo locale protetto.
