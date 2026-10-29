package it.minebuilder;

import com.google.gson.*;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Strict, bounded blueprint parsed from model output. No arbitrary commands. */
public final class BuildPlan {
    public record Placement(BlockPos pos, BlockState state) {}
    private final List<Placement> placements;
    public static BuildPlan fromPlacements(List<Placement> placements) {
        if (placements == null || placements.isEmpty() || placements.size() > 2_000_000)
            throw new IllegalArgumentException("Invalid blueprint size");
        return new BuildPlan(placements);
    }
    private BuildPlan(List<Placement> placements) { this.placements = List.copyOf(placements); }
    public List<Placement> placements() { return placements; }

    /** Parametric house: GPT chooses dimensions/materials, Java builds coherent geometry. */
    public static BuildPlan house(String json, BlockPos origin) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int w = root.get("width").getAsInt(), d = root.get("depth").getAsInt();
        int h = root.get("height").getAsInt();
        if (w < 5 || w > 15 || d < 5 || d > 15 || h < 3 || h > 8)
            throw new IllegalArgumentException("House dimensions out of range");
        BlockState wall = material(root, "wall", "minecraft:oak_planks");
        BlockState roof = material(root, "roof", "minecraft:dark_oak_planks");
        BlockState floor = material(root, "floor", "minecraft:stone_bricks");
        String roofStyle = root.has("roof_style") ? root.get("roof_style").getAsString() : "gable";
        if (!Set.of("gable", "hip", "flat").contains(roofStyle))
            throw new IllegalArgumentException("Unsupported roof style");
        BlockState trim = material(root, "trim", "minecraft:oak_log");
        BlockState glass = Registries.BLOCK.get(new Identifier("minecraft:glass_pane")).getDefaultState();
        BlockState air = Registries.BLOCK.get(new Identifier("minecraft:air")).getDefaultState();
        Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
        // Three-block-deep foundation: fills shallow gaps on uneven terrain.
        // Existing artificial structures remain protected by BuildExecutor preflight.
        BlockState foundation = Registries.BLOCK.get(
            new Identifier("minecraft:cobblestone")).getDefaultState();
        for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) {
            for (int depth = 1; depth <= 3; depth++)
                put(cells, origin, x, -depth, z, foundation);
            put(cells, origin, x, 0, z, floor);
            for (int y = 1; y <= h + 2; y++) put(cells, origin, x, y, z, air);
            for (int y = 1; y <= h; y++) {
                if (x == 0 || z == 0 || x == w - 1 || z == d - 1)
                    put(cells, origin, x, y, z, wall);
            }

        }
        // Pitched gable roof with one-block overhang, ridge running along Z.
        // Clear the roof volume first so existing leaves cannot remain inside.
        int rise = (w + 1) / 2;
        for (int x = -1; x <= w; x++) for (int z = -1; z <= d; z++)
            for (int y = h + 1; y <= h + rise + 2; y++)
                put(cells, origin, x, y, z, air);
        for (int x = -1; x <= w; x++) for (int z = -1; z <= d; z++) {
            int xRise = Math.min(x + 1, w - x);
            int zRise = Math.min(z + 1, d - z);
            int level = roofStyle.equals("flat") ? 0
                : roofStyle.equals("hip") ? Math.min(xRise, zRise) : xRise;
            put(cells, origin, x, h + 1 + level, z, roof);
        }
        if (roofStyle.equals("gable")) for (int x = 0; x < w; x++) {
            int top = h + Math.min(x + 1, w - x);
            for (int y = h + 1; y < top; y++) {
                put(cells, origin, x, y, 0, wall);
                put(cells, origin, x, y, d - 1, wall);
            }
        }
        for (int y = 1; y <= h; y++) {
            put(cells, origin, 0, y, 0, trim);
            put(cells, origin, w - 1, y, 0, trim);
            put(cells, origin, 0, y, d - 1, trim);
            put(cells, origin, w - 1, y, d - 1, trim);
        }
        // Centered, two-block-high entrance.
        int doorX = w / 2;
        put(cells, origin, doorX, 1, 0, air);
        put(cells, origin, doorX, 2, 0, air);
        // Side windows at eye height.
        for (int z = 2; z < d - 2; z += 3) {
            put(cells, origin, 0, 2, z, glass);
            put(cells, origin, w - 1, 2, z, glass);
        }
        for (int x = 2; x < w - 2; x += 3) {
            put(cells, origin, x, 2, d - 1, glass);
        }
        List<Placement> result = new ArrayList<>();
        cells.forEach((pos, state) -> result.add(new Placement(pos, state)));
        return new BuildPlan(result);
    }

    private static void put(Map<BlockPos, BlockState> cells, BlockPos origin,
                            int x, int y, int z, BlockState state) {
        cells.put(origin.add(x, y, z), state);
    }

    private static BlockState material(JsonObject root, String key, String fallback) {
        String value = root.has(key) ? root.get(key).getAsString() : fallback;
        Identifier id = Identifier.tryParse(value);
        if (id == null || !Registries.BLOCK.containsId(id))
            throw new IllegalArgumentException("Unknown material: " + value);
        return Registries.BLOCK.get(id).getDefaultState();
    }

    public static BuildPlan parse(String json, BlockPos origin) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray blocks = root.getAsJsonArray("blocks");
        if (blocks == null || blocks.size() == 0 || blocks.size() > 512)
            throw new IllegalArgumentException("Blueprint must contain 1-512 blocks");
        List<Placement> result = new ArrayList<>();
        Set<BlockPos> unique = new HashSet<>();
        for (JsonElement element : blocks) {
            JsonObject b = element.getAsJsonObject();
            int x = b.get("x").getAsInt(), y = b.get("y").getAsInt(), z = b.get("z").getAsInt();
            if (Math.abs(x) > 32 || y < 0 || y > 32 || Math.abs(z) > 32)
                throw new IllegalArgumentException("Blueprint exceeds safe bounds");
            Identifier id = Identifier.tryParse(b.get("block").getAsString());
            if (id == null || !Registries.BLOCK.containsId(id) || id.toString().equals("minecraft:air"))
                throw new IllegalArgumentException("Invalid block ID");
            BlockPos pos = origin.add(x, y, z);
            if (!unique.add(pos)) throw new IllegalArgumentException("Duplicate placement");
            result.add(new Placement(pos, Registries.BLOCK.get(id).getDefaultState()));
        }
        return new BuildPlan(result);
    }
}
