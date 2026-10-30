package it.minebuilder;
import com.google.gson.JsonParser;
public final class BlueprintFieldsTest {
    public static void main(String[] args) {
        var good = JsonParser.parseString("{\"x\":-12,\"block\":\"minecraft:stone\"}").getAsJsonObject();
        if (BlueprintFields.requiredInt(good, "x") != -12) throw new AssertionError();
        if (!BlueprintFields.requiredString(good, "block").equals("minecraft:stone")) throw new AssertionError();
        reject("{}", "x"); reject("{\"x\":null}", "x");
        reject("{\"x\":1.5}", "x"); reject("{\"x\":2147483648}", "x");
        reject("{\"x\":\"1\"}", "x"); reject("{\"x\":true}", "x");
        try { BlueprintFields.requiredString(good, "missing"); throw new AssertionError(); }
        catch (IllegalArgumentException expected) {}
        System.out.println("Blueprint field checks passed");
    }
    private static void reject(String json, String key) {
        try { BlueprintFields.requiredInt(JsonParser.parseString(json).getAsJsonObject(), key); }
        catch (IllegalArgumentException expected) {
            if (!expected.getMessage().contains(key)) throw new AssertionError("Missing field name");
            return;
        }
        throw new AssertionError("Invalid number accepted: " + json);
    }
}
