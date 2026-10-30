package it.blueprintbuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Strict JSON fields: no truncation, invented defaults or missing-field crashes. */
final class BlueprintFields {
    private BlueprintFields() {}
    static int requiredInt(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Campo numerico obbligatorio assente o non valido: " + key + " in " + object.toString().substring(0, Math.min(240, object.toString().length())));
        try { return value.getAsBigDecimal().intValueExact(); }
        catch (ArithmeticException | NumberFormatException ex) {
            throw new IllegalArgumentException("Richiesto intero valido per " + key, ex);
        }
    }


    static String requiredString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank())
            throw new IllegalArgumentException("Campo testo obbligatorio assente o non valido: " + key);
        return value.getAsString();
    }
}
