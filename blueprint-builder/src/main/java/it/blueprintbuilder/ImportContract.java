package it.blueprintbuilder;
import com.google.gson.*;
/** Metadata and unsupported content must fail explicitly, never be silently discarded. */
final class ImportContract {
 static void validate(JsonObject root) {
  if (root.has("format_version") && BlueprintFields.requiredInt(root,"format_version") != 1)
   throw new IllegalArgumentException("Versione formato non supportata (richiesta 1)");
  if (root.has("minecraft_version") && !"1.20.1".equals(BlueprintFields.requiredString(root,"minecraft_version")))
   throw new IllegalArgumentException("Blueprint per una versione Minecraft diversa da 1.20.1");
  for (String field : new String[]{"entities","block_entities","nbt"})
   if (root.has(field)) throw new IllegalArgumentException("Contenuto non supportato: " + field + "; nessuna importazione parziale");
  for (String field : new String[]{"blocks","cuboids","shapes"}) {
   if (!root.has(field)) continue;
   if (!root.get(field).isJsonArray()) throw new IllegalArgumentException("Richiesto array: "+field);
   for (var entry : root.getAsJsonArray(field)) {
    if (!entry.isJsonObject()) throw new IllegalArgumentException("Elemento non valido in "+field);
    for (String key : entry.getAsJsonObject().keySet())
     if (key.equals("nbt") || key.equals("properties") || key.equals("state"))
      throw new IllegalArgumentException("Usare proprieta nella stringa block, non nel campo "+key);
   }
  }
 }
}
