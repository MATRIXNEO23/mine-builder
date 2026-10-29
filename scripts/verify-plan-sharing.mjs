/**
 * Mine Builder: verifica reale del piano ChatGPT tramite Sign in with ChatGPT.
 * Questo modulo viene chiamato dal futuro bridge locale DOPO il login OAuth.
 * Non gestisce il login e non usa chiavi API a consumo.
 * Fonte: https://developers.openai.com/cookbook/articles/sign-in-with-chatgpt
 */
export async function verifyPlanSharing(chatgpt) {
  const session = await chatgpt.getSession();
  if (!session?.sharing) {
    throw new Error("Consenso all’uso del piano ChatGPT assente: impossibile procedere.");
  }
  const models = await chatgpt.listModels();
  const model = models?.[0];
  if (!model?.slug) {
    throw new Error("Nessun modello disponibile per l’account selezionato.");
  }
  const result = await chatgpt.streamResponse({
    model: model.slug,
    input: "Rispondi esattamente: Mine Builder connesso.",
  });
  const response = result?.text?.trim();
  if (!response) {
    throw new Error("Richiesta AI non completata o risposta vuota.");
  }
  return { model: model.slug, response, success: true };
}

// Da invocare nel processo del bridge con il client @siwc/local già autenticato:
// const result = await verifyPlanSharing(chatgpt);
// NON stampare token, cookie o dati di autenticazione nei log.
