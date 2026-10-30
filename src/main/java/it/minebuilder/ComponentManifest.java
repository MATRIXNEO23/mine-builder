package it.minebuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A typed, fixed component plan in shared relative world coordinates.
 * GPT can produce detail separately but cannot redesign these immutable bounds.
 */
public record ComponentManifest(List<Component> components) {
    public static final int MAX_COMPONENTS = 64;

    public record Component(String id, String description,
                            int minX, int maxX, int minY, int maxY,
                            int minZ, int maxZ) {
        public Component {
            if (id == null || !id.matches("[a-z0-9][a-z0-9_-]{0,47}"))
                throw new IllegalArgumentException("ID componente non valido");
            if (description == null || description.isBlank() || description.length() > 500)
                throw new IllegalArgumentException("Descrizione componente non valida");
            if (minX > maxX || minY > maxY || minZ > maxZ
                || Math.abs((long)minX) > 2000 || Math.abs((long)maxX) > 2000
                || Math.abs((long)minY) > 2000 || Math.abs((long)maxY) > 2000
                || Math.abs((long)minZ) > 2000 || Math.abs((long)maxZ) > 2000)
                throw new IllegalArgumentException("Limiti componente non validi: " + id);
        }

        public boolean contains(long x, long y, long z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
        }
    }

    public ComponentManifest {
        if (components == null || components.size() < 2
                || components.size() > MAX_COMPONENTS)
            throw new IllegalArgumentException("Richiesti da 2 a 64 componenti");
        components = List.copyOf(components);
        Set<String> ids = new HashSet<>();
        for (Component component : components)
            if (!ids.add(component.id()))
                throw new IllegalArgumentException("Componente duplicato: " + component.id());
    }

    public static ComponentManifest parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray components = root.getAsJsonArray("components");
        if (components == null || components.size() < 2
                || components.size() > MAX_COMPONENTS)
            throw new IllegalArgumentException("Manifest componenti incompleto");
        List<Component> parts = new ArrayList<>();
        for (var item : components) {
            JsonObject c = item.getAsJsonObject();
            parts.add(new Component(
                c.get("id").getAsString(), c.get("description").getAsString(),
                c.get("min_x").getAsInt(), c.get("max_x").getAsInt(),
                c.get("min_y").getAsInt(), c.get("max_y").getAsInt(),
                c.get("min_z").getAsInt(), c.get("max_z").getAsInt()));
        }
        return new ComponentManifest(parts);
    }
}
