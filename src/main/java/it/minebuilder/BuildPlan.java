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
    private BuildPlan(List<Placement> placements) { this.placements = List.copyOf(placements); }
    public List<Placement> placements() { return placements; }

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
