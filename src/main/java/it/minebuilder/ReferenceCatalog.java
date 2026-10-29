package it.minebuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local reference catalog; never invent measurements. */
public final class ReferenceCatalog {
    private ReferenceCatalog() {}

    public static ReferenceMeasurements load(Path configDir, String id) throws IOException {
        if (id == null || !id.matches("[a-z0-9_-]{1,64}"))
            throw new IOException("Invalid reference identifier");
        Path file = configDir.resolve("minebuilder").resolve("references").resolve(id + ".json");
        if (!Files.isRegularFile(file) || Files.size(file) > 65536)
            throw new IOException("Reference unavailable or too large");
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            return new ReferenceMeasurements(json.get("name").getAsString(),
                json.get("width_m").getAsInt(), json.get("depth_m").getAsInt(),
                json.get("height_m").getAsInt(), json.get("source").getAsString());
        } catch (RuntimeException e) {
            throw new IOException("Malformed reference", e);
        }
    }
}
