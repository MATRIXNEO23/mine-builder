package it.minebuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Local singleplayer prototype: paced placement through the integrated server thread. */
public final class BuildExecutor {
    private static final int PER_BATCH = 16;
    private static Queue<BuildPlan.Placement> pending = new ArrayDeque<>();
    private static String worldKey = "";
    private BuildExecutor() {}

    public static void start(MinecraftClient client, BuildPlan plan) {
        if (client.getServer() == null || !client.isInSingleplayer() || client.world == null)
            throw new IllegalStateException("Only integrated singleplayer worlds supported");
        String key = client.world.getRegistryKey().getValue().toString();
        pending = new ArrayDeque<>(plan.placements());
        worldKey = key;
    }

    public static void tick(MinecraftClient client) {
        if (pending.isEmpty() || client.world == null || client.getServer() == null) return;
        if (!worldKey.equals(client.world.getRegistryKey().getValue().toString())) {
            pending.clear();
            return;
        }
        var server = client.getServer();
        var dimension = client.world.getRegistryKey();
        List<BuildPlan.Placement> batch = new ArrayList<>();
        for (int i = 0; i < PER_BATCH && !pending.isEmpty(); i++) batch.add(pending.remove());
        server.execute(() -> {
            var world = server.getWorld(dimension);
            if (world == null) return;
            for (var entry : batch) {
                BlockPos pos = entry.pos();
                if (world.isInBuildLimit(pos) && world.isChunkLoaded(pos))
                    world.setBlockState(pos, entry.state(), 3);
            }
        });
    }

    public static int remaining() { return pending.size(); }
    public static void cancel() { pending.clear(); }
}
