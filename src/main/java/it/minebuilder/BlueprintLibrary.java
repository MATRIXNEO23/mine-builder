package it.minebuilder;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.storage.WorldSavePath;
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

    public static List<String> names(MinecraftClient client) throws IOException {
        try (var stream = Files.list(directory(client))) {
            return stream.filter(p -> p.getFileName().toString().matches("[a-z0-9_-]+\\.json"))
                .map(p -> p.getFileName().toString().replaceFirst("\\.json$", ""))
                .sorted().limit(500).toList();
        }
    }

    public static void save(MinecraftClient client, String name, BuildPlan plan) throws IOException {
        checkName(name);
        if (plan.placements().size() > 2_000_000) throw new IOException("Blueprint too large");
        BlockPos origin = plan.placements().get(0).pos();
        JsonObject root = new JsonObject();
        root.addProperty("name", name);
        JsonArray blocks = new JsonArray();
        for (var placement : plan.placements()) {
            JsonObject b = new JsonObject();
            b.addProperty("x", placement.pos().getX() - origin.getX());
            b.addProperty("y", placement.pos().getY() - origin.getY());
            b.addProperty("z", placement.pos().getZ() - origin.getZ());
            b.addProperty("block", Registries.BLOCK.getId(placement.state().getBlock()).toString());
            blocks.add(b);
        }
        root.add("blocks", blocks);
        Path dir = directory(client);
        Path tmp = Files.createTempFile(dir, "blueprint-", ".tmp");
        try {
            Files.writeString(tmp, root.toString());
            try {
                Files.move(tmp, dir.resolve(name + ".json"), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, dir.resolve(name + ".json"), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(tmp); }
    }

    public static BuildPlan load(MinecraftClient client, String name, BlockPos origin) throws IOException {
        checkName(name);
        Path file = directory(client).resolve(name + ".json");
        if (!Files.isRegularFile(file) || Files.size(file) > 120_000_000)
            throw new IOException("Blueprint missing or too large");
        try {
            JsonArray blocks = JsonParser.parseString(Files.readString(file))
                .getAsJsonObject().getAsJsonArray("blocks");
            if (blocks == null || blocks.isEmpty() || blocks.size() > 2_000_000)
                throw new IOException("Invalid block count");
            List<BuildPlan.Placement> placements = new ArrayList<>();
            Set<BlockPos> seen = new HashSet<>();
            for (JsonElement e : blocks) {
                JsonObject b = e.getAsJsonObject();
                int x = b.get("x").getAsInt(), y = b.get("y").getAsInt(), z = b.get("z").getAsInt();
                if (Math.abs(x) > 2000 || Math.abs(y) > 2000 || Math.abs(z) > 2000)
                    throw new IOException("Blueprint coordinates out of range");
                Identifier id = Identifier.tryParse(b.get("block").getAsString());
                if (id == null || !Registries.BLOCK.containsId(id))
                    throw new IOException("Unknown block");
                BlockPos pos = origin.add(x, y, z);
                if (!seen.add(pos)) throw new IOException("Duplicate block");
                placements.add(new BuildPlan.Placement(pos, Registries.BLOCK.get(id).getDefaultState()));
            }
            return BuildPlan.fromPlacements(placements);
        } catch (RuntimeException e) { throw new IOException("Invalid blueprint", e); }
    }

    private static void checkName(String name) throws IOException {
        if (name == null || !name.matches("[a-z0-9_-]{1,64}"))
            throw new IOException("Use lowercase letters, digits, underscore or hyphen");
    }
}
