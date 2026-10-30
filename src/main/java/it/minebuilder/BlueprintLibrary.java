package it.minebuilder;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.WorldSavePath;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Per-world saved blueprints, portable relative coordinates, no online service. */
public final class BlueprintLibrary {
    private BlueprintLibrary() {}
    private static Path directory(MinecraftClient client) throws IOException {
        if (client.getServer() == null || !client.isInSingleplayer())
            throw new IOException("Singleplayer world required");
        Path dir = client.getServer().getSavePath(WorldSavePath.ROOT)
            .resolve("minebuilder").resolve("blueprints");
        Files.createDirectories(dir);
        return dir;
    }

    /** Both the old uncompressed format and the new compressed one remain readable. */
    public static List<String> names(MinecraftClient client) throws IOException {
        try (var stream = Files.list(directory(client))) {
            return stream.filter(p -> p.getFileName().toString()
                    .matches("[a-z0-9_-]+\\.json(\\.gz)?"))
                .map(p -> p.getFileName().toString().replaceFirst("\\.json(\\.gz)?$", ""))
                .distinct().sorted().limit(500).toList();
        }
    }

    /** Save each placement directly to a compressed stream, not a giant JSON tree. */
    public static void save(MinecraftClient client, String name, BuildPlan plan) throws IOException {
        checkName(name);
        if (plan == null || plan.placements().isEmpty()
                || plan.placements().size() > 2_000_000)
            throw new IOException("Dimensione blueprint non valida");
        BlockPos origin = plan.placements().get(0).pos();
        Path dir = directory(client);
        Path tmp = Files.createTempFile(dir, "blueprint-", ".tmp");
        try {
            try (var stream = new java.util.zip.GZIPOutputStream(
                    new java.io.BufferedOutputStream(Files.newOutputStream(tmp), 65536));
                 var output = new java.io.OutputStreamWriter(stream, java.nio.charset.StandardCharsets.UTF_8);
                 var writer = new com.google.gson.stream.JsonWriter(output)) {
                writer.beginObject();
                writer.name("name").value(name);
                writer.name("blocks").beginArray();
                for (var placement : plan.placements()) {
                    writer.beginObject();
                    writer.name("x").value((long) placement.pos().getX() - origin.getX());
                    writer.name("y").value((long) placement.pos().getY() - origin.getY());
                    writer.name("z").value((long) placement.pos().getZ() - origin.getZ());
                    writer.name("block").value(blockStateName(placement.state()));
                    writer.endObject();
                }
                writer.endArray();
                writer.endObject();
            }
            Path dest = dir.resolve(name + ".json.gz");
            try {
                Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            // A legacy .json with the same name is deliberately left untouched.
        } finally { Files.deleteIfExists(tmp); }
    }

    /**
     * Stream the GZIP/legacy JSON without materializing millions of JSON elements.
     * A damaged new compressed file is an error; never silently fall back to a stale
     * legacy file for the same name.
     */
    public static BuildPlan load(MinecraftClient client, String name, BlockPos origin) throws IOException {
        checkName(name);
        Path dir = directory(client);
        Path gz = dir.resolve(name + ".json.gz");
        boolean compressed = Files.exists(gz);
        Path file = compressed ? gz : dir.resolve(name + ".json");
        long maxBytes = compressed ? 200_000_000L : 120_000_000L;
        if (!Files.isRegularFile(file) || Files.size(file) > maxBytes)
            throw new IOException("Blueprint assente o troppo grande");
        List<BuildPlan.Placement> placements = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        try (var input = compressed
                    ? new java.util.zip.GZIPInputStream(new java.io.BufferedInputStream(
                        Files.newInputStream(file), 65536))
                    : new java.io.BufferedInputStream(Files.newInputStream(file), 65536);
             var text = new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8);
             var reader = new com.google.gson.stream.JsonReader(text)) {
            boolean foundBlocks = false;
            reader.beginObject();
            while (reader.hasNext()) {
                String field = reader.nextName();
                if (!field.equals("blocks")) {
                    reader.skipValue();
                    continue;
                }
                if (foundBlocks) throw new IOException("Array blocchi duplicato");
                foundBlocks = true;
                reader.beginArray();
                while (reader.hasNext()) {
                    reader.beginObject();
                    Integer x = null, y = null, z = null;
                    String block = null;
                    while (reader.hasNext()) {
                        String property = reader.nextName();
                        switch (property) {
                            case "x" -> x = reader.nextInt();
                            case "y" -> y = reader.nextInt();
                            case "z" -> z = reader.nextInt();
                            case "block" -> block = reader.nextString();
                            default -> reader.skipValue();
                        }
                    }
                    reader.endObject();
                    if (x == null || y == null || z == null || block == null)
                        throw new IOException("Blocco salvato incompleto");
                    if (Math.abs((long)x) > 2000 || Math.abs((long)y) > 2000
                            || Math.abs((long)z) > 2000)
                        throw new IOException("Coordinate blueprint fuori limite");
                    BlockPos pos = origin.add(x, y, z);
                    if (!seen.add(pos)) throw new IOException("Posizione duplicata");
                    placements.add(new BuildPlan.Placement(pos, parseSavedState(block)));
                    if (placements.size() > 2_000_000)
                        throw new IOException("Troppi blocchi nel blueprint");
                }
                reader.endArray();
            }
            reader.endObject();
            if (!foundBlocks || placements.isEmpty()) throw new IOException("Blueprint vuoto");
            return BuildPlan.fromPlacements(placements);
        } catch (RuntimeException ex) {
            throw new IOException("Blueprint salvato non valido", ex);
        }
    }

    private static String blockStateName(net.minecraft.block.BlockState state) {
        StringBuilder result = new StringBuilder(Registries.BLOCK.getId(state.getBlock()).toString());
        if (!state.getEntries().isEmpty()) {
            result.append('[');
            boolean first = true;
            for (var entry : state.getEntries().entrySet()) {
                if (!first) result.append(',');
                first = false;
                result.append(entry.getKey().getName()).append('=').append(propertyValue(entry.getKey(), entry.getValue()));
            }
            result.append(']');
        }
        return result.toString();
    }

    private static <T extends Comparable<T>> String propertyValue(
            net.minecraft.state.property.Property<T> property, Comparable<?> value) {
        return property.name((T) value);
    }

    private static net.minecraft.block.BlockState parseSavedState(String name) throws IOException {
        String idPart = name.contains("[") ? name.substring(0, name.indexOf('[')) : name;
        Identifier id = Identifier.tryParse(idPart);
        if (id == null || !Registries.BLOCK.containsId(id))
            throw new IOException("Unknown saved block: " + idPart);
        var state = Registries.BLOCK.get(id).getDefaultState();
        if (name.contains("[")) {
            if (!name.endsWith("]")) throw new IOException("Invalid saved block state");
            String spec = name.substring(name.indexOf('[') + 1, name.length() - 1);
            for (String assignment : spec.split(",")) {
                String[] pair = assignment.split("=", 2);
                if (pair.length != 2) throw new IOException("Invalid saved property");
                var property = state.getBlock().getStateManager().getProperty(pair[0]);
                if (property == null) throw new IOException("Unknown saved property: " + pair[0]);
                state = applySavedProperty(state, property, pair[1]);
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> net.minecraft.block.BlockState applySavedProperty(
            net.minecraft.block.BlockState state, net.minecraft.state.property.Property<T> property, String value)
            throws IOException {
        T parsed = property.parse(value).orElseThrow(() -> new IOException("Invalid saved property value"));
        return state.with(property, parsed);
    }

    private static void checkName(String name) throws IOException {
        if (name == null || !name.matches("[a-z0-9_-]{1,64}"))
            throw new IOException("Use lowercase letters, digits, underscore or hyphen");
    }
}
