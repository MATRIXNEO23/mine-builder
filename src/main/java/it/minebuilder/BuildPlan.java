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
        // Requested swimming pool: separate rectangular basin beside the house.
        if (root.has("pool") && root.get("pool").getAsBoolean()) {
            BlockState basin = Registries.BLOCK.get(new Identifier("minecraft:light_blue_concrete")).getDefaultState();
            BlockState water = Registries.BLOCK.get(new Identifier("minecraft:water")).getDefaultState();
            BlockState deck = Registries.BLOCK.get(new Identifier("minecraft:smooth_quartz")).getDefaultState();
            int px = w + 4, pz = 1, pw = 7, pd = Math.max(7, d - 2);
            for (int x = px - 1; x <= px + pw; x++)
                for (int z = pz - 1; z <= pz + pd; z++) {
                    boolean rim = x == px - 1 || x == px + pw || z == pz - 1 || z == pz + pd;
                    put(cells, origin, x, 0, z, rim ? deck : water);
                    if (!rim) {
                        put(cells, origin, x, -1, z, water);
                        put(cells, origin, x, -2, z, basin);
                    }
                    for (int y = 1; y <= 3; y++)
                        put(cells, origin, x, y, z, air);
                }
        }
        List<Placement> result = new ArrayList<>();
        cells.forEach((pos, state) -> result.add(new Placement(pos, state)));
        return new BuildPlan(result);
    }

    /** Large ornamental cherry tree: organic trunk, spreading branches and pink canopy. */
    public static BuildPlan cherryTree(BlockPos origin) {
        Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
        BlockState trunk = Registries.BLOCK.get(new Identifier("minecraft:cherry_log")).getDefaultState();
        BlockState leaves = Registries.BLOCK.get(new Identifier("minecraft:cherry_leaves")).getDefaultState();
        int height = 17;
        for (int y = 0; y <= height; y++) {
            int x = y > 10 ? 1 : 0;
            int z = y > 13 ? 1 : 0;
            put(cells, origin, x, y, z, trunk);
            if (y < 8) {
                put(cells, origin, x + 1, y, z, trunk);
                put(cells, origin, x, y, z + 1, trunk);
            }
        }
        int[][] branches = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}};
        for (int i = 0; i < branches.length; i++) {
            int dx = branches[i][0], dz = branches[i][1];
            int startY = 10 + (i % 4);
            int length = 5 + (i % 3);
            for (int step = 1; step <= length; step++) {
                int y = startY + step / 3;
                put(cells, origin, dx * step, y, dz * step, trunk);
            }
            int cx = dx * length, cy = startY + length / 3, cz = dz * length;
            for (int x = -3; x <= 3; x++)
                for (int y = -2; y <= 3; y++)
                    for (int z = -3; z <= 3; z++) {
                        double distance = x*x / 10.0 + y*y / 6.0 + z*z / 10.0;
                        if (distance <= 1.0 && ((x * 17 + y * 11 + z * 7 + i) & 7) != 0) {
                            BlockPos at = origin.add(cx + x, cy + y, cz + z);
                            cells.putIfAbsent(at, leaves);
                        }
                    }
        }
        for (int x = -4; x <= 4; x++)
            for (int y = 14; y <= 21; y++)
                for (int z = -4; z <= 4; z++) {
                    double distance = x*x / 19.0 + (y-17)*(y-17) / 15.0 + z*z / 19.0;
                    if (distance <= 1.0) cells.putIfAbsent(origin.add(x, y, z), leaves);
                }
        List<Placement> result = new ArrayList<>();
        cells.forEach((pos, state) -> result.add(new Placement(pos, state)));
        return fromPlacements(result);
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

    /** Universal compact geometry format: blocks and cuboids, independent of object type. */
    public static BuildPlan parse(String json, BlockPos origin) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
        JsonArray blocks = root.has("blocks") ? root.getAsJsonArray("blocks") : new JsonArray();
        JsonArray cuboids = root.has("cuboids") ? root.getAsJsonArray("cuboids") : new JsonArray();
        if (blocks.size() > 20000 || cuboids.size() > 1024)
            throw new IllegalArgumentException("Blueprint input too large");
        for (JsonElement element : cuboids) {
            JsonObject c = element.getAsJsonObject();
            int x = c.get("x").getAsInt(), y = c.get("y").getAsInt(), z = c.get("z").getAsInt();
            int w = c.get("w").getAsInt(), h = c.get("h").getAsInt(), d = c.get("d").getAsInt();
            if (w < 1 || h < 1 || d < 1 || (long)w*h*d > 20000)
                throw new IllegalArgumentException("Invalid cuboid dimensions");
            BlockState state = blueprintBlock(c.get("block").getAsString());
            for (int dx = 0; dx < w; dx++)
                for (int dy = 0; dy < h; dy++)
                    for (int dz = 0; dz < d; dz++)
                        addCell(cells, origin, x + dx, y + dy, z + dz, state);
        }
        for (JsonElement element : blocks) {
            JsonObject b = element.getAsJsonObject();
            addCell(cells, origin, b.get("x").getAsInt(), b.get("y").getAsInt(),
                b.get("z").getAsInt(), blueprintBlock(b.get("block").getAsString()));
        }
        if (cells.isEmpty()) throw new IllegalArgumentException("Empty blueprint");
        return fromPlacements(cells.entrySet().stream()
            .map(e -> new Placement(e.getKey(), e.getValue())).toList());
    }

    private static BlockState blueprintBlock(String name) {
        Identifier id = Identifier.tryParse(name);
        if (id == null || !Registries.BLOCK.containsId(id))
            throw new IllegalArgumentException("Invalid block ID: " + name);
        return Registries.BLOCK.get(id).getDefaultState();
    }

    private static void addCell(Map<BlockPos, BlockState> cells, BlockPos origin,
                                int x, int y, int z, BlockState state) {
        if (Math.abs(x) > 128 || y < -32 || y > 128 || Math.abs(z) > 128)
            throw new IllegalArgumentException("Blueprint exceeds safe bounds");
        BlockPos pos = origin.add(x, y, z);
        if (cells.put(pos, state) != null)
            throw new IllegalArgumentException("Overlapping geometry at " + pos.toShortString());
        if (cells.size() > 20000)
            throw new IllegalArgumentException("Blueprint exceeds 20000 blocks");
    }
}
