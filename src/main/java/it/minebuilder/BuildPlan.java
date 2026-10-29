package it.minebuilder;

import com.google.gson.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.state.property.Properties;
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
    /** Deterministic post-generation report; never claims architectural correctness. */
    public String geometryReport() {
        if (placements.isEmpty()) return "Empty blueprint";
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        int solids = 0, air = 0;
        Set<BlockPos> positions = new HashSet<>();
        int duplicates = 0;
        for (Placement placement : placements) {
            BlockPos p = placement.pos();
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
            if (placement.state().isAir()) air++; else solids++;
            if (!positions.add(p)) duplicates++;
        }
        return "Ingombro " + (maxX - minX + 1) + "x" + (maxY - minY + 1)
            + "x" + (maxZ - minZ + 1) + " blocchi (X/Y/Z); "
            + solids + " solidi, " + air + " aria, "
            + duplicates + " posizioni duplicate. "
            + "Verifica visiva e percorribilita ancora necessarie.";
    }


    /** Basic topology checks for architectural plans, based only on generated block states. */
    public String architectureWarnings() {
        Map<BlockPos, BlockState> cells = new HashMap<>();
        for (Placement placement : placements) cells.put(placement.pos(), placement.state());
        int doors = 0, stairs = 0, ladders = 0, transparent = 0;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        Set<Integer> occupiedLevels = new HashSet<>();
        for (Placement placement : placements) {
            BlockState state = placement.state();
            if (state.isAir()) continue;
            String name = Registries.BLOCK.getId(state.getBlock()).getPath();
            if (name.endsWith("_door")) doors++;
            if (name.endsWith("_stairs")) stairs++;
            if (name.equals("ladder") || name.equals("scaffolding")) ladders++;
            if (name.contains("glass") || name.endsWith("_pane")) transparent++;
            occupiedLevels.add(placement.pos().getY());
            minY = Math.min(minY, placement.pos().getY());
            maxY = Math.max(maxY, placement.pos().getY());
        }
        List<String> warnings = new ArrayList<>();
        if (doors == 0) warnings.add("nessuna porta esplicita (verificare gli ingressi)");
        if (maxY - minY >= 9 && stairs == 0 && ladders == 0)
            warnings.add("struttura alta senza scale o scale a pioli rilevate");
        if (transparent == 0) warnings.add("nessuna finestra vetrata rilevata");
        int unsupported = 0;
        for (Placement placement : placements) {
            BlockPos p = placement.pos();
            if (placement.state().isAir()) continue;
            String name = Registries.BLOCK.getId(placement.state().getBlock()).getPath();
            if ((name.endsWith("_door") || name.equals("ladder")) && !cells.containsKey(p.down()))
                unsupported++;
        }
        if (unsupported > 0) warnings.add(unsupported + " elementi di accesso senza base nel progetto");
        return warnings.isEmpty()
            ? "Controlli architettonici di base: nessuna anomalia rilevata (non prova di percorribilita)."
            : "Avvisi architettonici: " + String.join("; ", warnings) + ".";
    }

    /** Stable 16x16 horizontal sections, ordered for reproducible staged builds. */
    public List<BuildPlan> sections() {
        Map<Long, List<Placement>> groups = new TreeMap<>();
        for (Placement placement : placements) {
            long x = Math.floorDiv(placement.pos().getX(), 16);
            long z = Math.floorDiv(placement.pos().getZ(), 16);
            long key = ((x & 0xffffffffL) << 32) | (z & 0xffffffffL);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(placement);
        }
        List<BuildPlan> result = new ArrayList<>();
        for (List<Placement> group : groups.values()) result.add(fromPlacements(group));
        return List.copyOf(result);
    }

    /** Deterministic equality of coordinates and complete block states, regardless of order. */
    public boolean matchesExactly(BuildPlan other) {
        if (other == null) return false;
        Map<BlockPos, BlockState> expected = new HashMap<>();
        for (Placement p : placements) {
            BlockState previous = expected.putIfAbsent(p.pos(), p.state());
            if (previous != null && !previous.equals(p.state())) return false;
        }
        Map<BlockPos, BlockState> actual = new HashMap<>();
        for (Placement p : other.placements) {
            BlockState previous = actual.putIfAbsent(p.pos(), p.state());
            if (previous != null && !previous.equals(p.state())) return false;
        }
        return expected.equals(actual);
    }

    /** Round-trip check: every section must reconstruct exactly the original blueprint. */
    public void validateSections() {
        List<Placement> reconstructed = new ArrayList<>(placements.size());
        for (BuildPlan section : sections()) reconstructed.addAll(section.placements());
        if (!matchesExactly(fromPlacements(reconstructed)))
            throw new IllegalStateException("Section reconstruction differs from approved blueprint");
    }

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
        JsonArray shapes = root.has("shapes") ? root.getAsJsonArray("shapes") : new JsonArray();
        if (shapes.size() > 1024) throw new IllegalArgumentException("Too many shapes");
        for (JsonElement element : shapes) {
            JsonObject shape = element.getAsJsonObject();
            String type = shape.get("type").getAsString();
            int x = shape.get("x").getAsInt(), y = shape.get("y").getAsInt(),
                z = shape.get("z").getAsInt();
            BlockState state = blueprintBlock(shape.get("block").getAsString());
            if (type.equals("line")) {
                int x2 = shape.get("x2").getAsInt(), y2 = shape.get("y2").getAsInt(),
                    z2 = shape.get("z2").getAsInt();
                int steps = Math.max(Math.abs(x2-x), Math.max(Math.abs(y2-y), Math.abs(z2-z)));
                if (steps > 256) throw new IllegalArgumentException("Line too long");
                for (int i = 0; i <= steps; i++) {
                    double t = steps == 0 ? 0 : (double)i / steps;
                    addCell(cells, origin, (int)Math.round(x+(x2-x)*t),
                        (int)Math.round(y+(y2-y)*t), (int)Math.round(z+(z2-z)*t), state);
                }
            } else if (type.equals("elliptic_ring")) {
                // General-purpose elliptical wall, arcade tier or stadium/arena ring.
                // Centre x/z, base y, outer radii rx/rz, radial wall thickness and height.
                int rx = shape.get("rx").getAsInt(), rz = shape.get("rz").getAsInt();
                int thickness = shape.get("thickness").getAsInt();
                int height = shape.get("height").getAsInt();
                if (rx < 2 || rz < 2 || rx > 128 || rz > 128
                    || thickness < 1 || thickness >= Math.min(rx, rz)
                    || height < 1 || height > 128)
                    throw new IllegalArgumentException("Invalid elliptic ring dimensions");
                // Limit work and bounds before expanding the geometry.
                if ((long)(2 * rx + 1) * (2 * rz + 1) * height > 4_000_000L)
                    throw new IllegalArgumentException("Elliptic ring too large for one section");
                for (int dx = -rx; dx <= rx; dx++)
                    for (int dz = -rz; dz <= rz; dz++) {
                        double outer = (double)dx * dx / ((double)rx * rx)
                            + (double)dz * dz / ((double)rz * rz);
                        double inner = (double)dx * dx / ((double)(rx-thickness) * (rx-thickness))
                            + (double)dz * dz / ((double)(rz-thickness) * (rz-thickness));
                        if (outer > 1.0 || inner < 1.0) continue;
                        for (int dy = 0; dy < height; dy++)
                            addCell(cells, origin, x + dx, y + dy, z + dz, state);
                    }
            } else if (type.equals("sphere") || type.equals("ellipsoid") || type.equals("cylinder")) {
                int rx = shape.get("rx").getAsInt();
                int ry = shape.has("ry") ? shape.get("ry").getAsInt() : rx;
                int rz = shape.has("rz") ? shape.get("rz").getAsInt() : rx;
                if (rx < 1 || ry < 1 || rz < 1 || rx > 64 || ry > 64 || rz > 64)
                    throw new IllegalArgumentException("Invalid shape radius");
                boolean hollow = shape.has("hollow") && shape.get("hollow").getAsBoolean();
                for (int dx=-rx; dx<=rx; dx++)
                    for (int dy=-ry; dy<=ry; dy++)
                        for (int dz=-rz; dz<=rz; dz++) {
                            double distance = (double)dx*dx/(rx*rx) + (double)dz*dz/(rz*rz);
                            if (!type.equals("cylinder")) distance += (double)dy*dy/(ry*ry);
                            if (distance > 1.0) continue;
                            if (hollow) {
                                double inner = (double)dx*dx/Math.max(1,(rx-1)*(rx-1))
                                    + (double)dz*dz/Math.max(1,(rz-1)*(rz-1));
                                if (!type.equals("cylinder"))
                                    inner += (double)dy*dy/Math.max(1,(ry-1)*(ry-1));
                                if (inner <= 1.0 && (type.equals("cylinder") || Math.abs(dy)<ry)) continue;
                            }
                            addCell(cells, origin, x+dx, y+dy, z+dz, state);
                        }
            } else throw new IllegalArgumentException("Unknown shape: " + type);
        }
        // Explicit detailing overrides generated geometry; duplicate explicit conflicts remain errors.
        Set<BlockPos> explicit = new HashSet<>();
        for (JsonElement element : blocks) {
            JsonObject b = element.getAsJsonObject();
            int x = b.get("x").getAsInt(), y = b.get("y").getAsInt(), z = b.get("z").getAsInt();
            if (Math.abs((long)x) > 128 || y < -32 || y > 128 || Math.abs((long)z) > 128)
                throw new IllegalArgumentException("Blueprint exceeds safe bounds at " + x + "," + y + "," + z);
            BlockPos pos = origin.add(x, y, z);
            BlockState state = blueprintBlock(b.get("block").getAsString());
            if (!explicit.add(pos) && !cells.get(pos).equals(state))
                throw new IllegalArgumentException("Conflicting explicit blocks at " + pos.toShortString());
            cells.put(pos, state);
            if (cells.size() > 20000)
                throw new IllegalArgumentException("Blueprint exceeds 20000 blocks");
        }
        if (cells.isEmpty()) throw new IllegalArgumentException("Empty blueprint");
        return fromPlacements(cells.entrySet().stream()
            .map(e -> new Placement(e.getKey(), e.getValue())).toList());
    }

    private static BlockState blueprintBlock(String name) {
        String blockId = name;
        String stateSpec = "";
        int bracket = name.indexOf('[');
        if (bracket >= 0) {
            if (!name.endsWith("]")) throw new IllegalArgumentException("Invalid block state: " + name);
            blockId = name.substring(0, bracket);
            stateSpec = name.substring(bracket + 1, name.length() - 1);
        }
        Identifier id = Identifier.tryParse(blockId);
        if (id == null || !Registries.BLOCK.containsId(id))
            throw new IllegalArgumentException("Invalid block ID: " + name);
        BlockState state = Registries.BLOCK.get(id).getDefaultState();
        if (!stateSpec.isBlank()) {
            for (String assignment : stateSpec.split(",")) {
                String[] pair = assignment.trim().split("=", 2);
                if (pair.length != 2) throw new IllegalArgumentException("Invalid property: " + assignment);
                var property = state.getBlock().getStateManager().getProperty(pair[0].trim());
                if (property == null) throw new IllegalArgumentException("Unknown block property: " + pair[0]);
                state = withParsedProperty(state, property, pair[1].trim());
            }
        }
        if (state.getBlock() instanceof LeavesBlock && state.contains(Properties.PERSISTENT))
            state = state.with(Properties.PERSISTENT, true);
        return state;
    }

    private static <T extends Comparable<T>> BlockState withParsedProperty(
            BlockState state, net.minecraft.state.property.Property<T> property, String value) {
        T parsed = property.parse(value).orElseThrow(() ->
            new IllegalArgumentException("Invalid value " + value + " for " + property.getName()));
        return state.with(property, parsed);
    }

    private static void addCell(Map<BlockPos, BlockState> cells, BlockPos origin,
                                int x, int y, int z, BlockState state) {
        if (Math.abs(x) > 128 || y < -32 || y > 128 || Math.abs(z) > 128)
            throw new IllegalArgumentException("Blueprint exceeds safe bounds");
        BlockPos pos = origin.add(x, y, z);
        BlockState previous = cells.putIfAbsent(pos, state);
        if (previous != null && !previous.equals(state))
            throw new IllegalArgumentException("Conflicting geometry at " + pos.toShortString());
        if (cells.size() > 20000)
            throw new IllegalArgumentException("Blueprint exceeds 20000 blocks");
    }
}
