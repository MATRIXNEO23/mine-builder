/**
 * Gate 0: test of actual inference with a Sign in with ChatGPT OAuth access token.
 * No API key, no billing fallback, no token printed or persisted.
 * Requires Node 18+ and an OAuth access token supplied by the local login bridge.
 * Reference: https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference
 */
const BASE = "https://api.openai.com/v1";

export async function verifyPlanSharing(accessToken, fetchImpl = fetch) {
  if (!accessToken || typeof accessToken !== "string") {
    throw new Error("OAuth token missing. Complete official ChatGPT consent first.");
  }
  const headers = { Authorization: `Bearer ${accessToken}` };
  const catalog = await fetchImpl(`${BASE}/models`, { headers });
  if (!catalog.ok) throw new Error(`Model catalog unavailable (HTTP ${catalog.status})`);
  const data = await catalog.json();
  const model = data.models?.find(m => m.visibility === "list" && m.slug);
  if (!model) throw new Error("No eligible model listed for this ChatGPT account.");

  const response = await fetchImpl(`${BASE}/responses`, {
    method: "POST",
    headers: { ...headers, "Content-Type": "application/json" },
    body: JSON.stringify({
      model: model.slug,
      input: [{ role: "user", content: "Rispondi esattamente: Mine Builder connesso." }],
      store: false,
      stream: true
    })
  });
  if (!response.ok) throw new Error(`Inference rejected (HTTP ${response.status})`);
  if (!response.body) throw new Error("Missing response event stream.");

  let buffer = "";
  let output = "";
  let completed = false;
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  try {
    while (true) {
      const { done, value } = await reader.read();
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done }).replace(/\r\n/g, "\n");
      let boundary;
      while ((boundary = buffer.indexOf("\n\n")) !== -1) {
        const frame = buffer.slice(0, boundary);
        buffer = buffer.slice(boundary + 2);
        const line = frame.split("\n").filter(s => s.startsWith("data:")).map(s => s.slice(5).trim()).join("");
        if (!line || line === "[DONE]") continue;
        const event = JSON.parse(line);
        if (event.type === "response.output_text.delta") output += event.delta || "";
        if (event.type === "response.failed" || event.type === "response.incomplete" || event.type === "error") {
          throw new Error(`Inference did not complete: ${event.response?.error?.code || event.type}`);
        }
        if (event.type === "response.completed") completed = true;
      }
      if (done) break;
    }
  } finally {
    reader.releaseLock();
  }
  if (!completed) throw new Error("Stream ended without response.completed.");
  if (!output.trim()) throw new Error("Completed inference returned no text.");
  return { model: model.slug, response: output.trim(), success: true };
}
