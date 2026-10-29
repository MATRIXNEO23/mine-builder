package it.minebuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** Local singleplayer prototype: paced placement through the integrated server thread. */
public final class BuildExecutor {
    private static final int PER_BATCH = 16;
    private static Queue<BuildPlan.Placement> pending = new ArrayDeque<>();
    private static String worldKey = "";
    private static net.minecraft.server.MinecraftServer ownerServer;
    private static volatile boolean inFlight;
    private static volatile boolean preflightDone;
    private static volatile String lastError = "";
    private BuildExecutor() {}

    public static void start(MinecraftClient client, BuildPlan plan) {
        if (client.getServer() == null || !client.isInSingleplayer() || client.world == null)
            throw new IllegalStateException("Only integrated singleplayer worlds supported");
        String key = client.world.getRegistryKey().getValue().toString();
        if (inFlight || !pending.isEmpty())
            throw new IllegalStateException("A build is already running; finish or cancel it first");
        pending = new ArrayDeque<>(plan.placements());
        preflightDone = false;
        lastError = "";
        ownerServer = client.getServer();
        worldKey = key;
    }

    public static void tick(MinecraftClient client) {
        if (pending.isEmpty() || inFlight) return;
        var server = client.getServer();
        if (client.world == null || server == null || server != ownerServer
                || !worldKey.equals(client.world.getRegistryKey().getValue().toString())) {
            cancel();
            return;
        }
        var dimension = client.world.getRegistryKey();
        inFlight = true;
        server.execute(() -> {
            try {
                var world = server.getWorld(dimension);
                if (world == null || server != ownerServer) return;
                if (!preflightDone) {
                    for (var candidate : pending) {
                        BlockPos target = candidate.pos();
                        if (!world.isInBuildLimit(target)) {
                            lastError = "Progetto fuori dai limiti del mondo";
                            cancel();
                            return;
                        }
                        if (!world.isChunkLoaded(target)) return;
                        var existing = world.getBlockState(target);
                        if (!existing.isAir() && !existing.equals(candidate.state())) {
                            lastError = "Area occupata: " + target.toShortString();
                            cancel();
                            return;
                        }
                    }
                    preflightDone = true;
                }
                int processed = 0;
                while (processed < PER_BATCH && !pending.isEmpty()) {
                    var entry = pending.peek();
                    BlockPos pos = entry.pos();
                    // Never discard unloaded chunks: retry on a later tick.
                    if (!world.isChunkLoaded(pos)) break;
                    if (!world.isInBuildLimit(pos)) {
                        // Invalid positions cannot be placed; stop instead of reporting success.
                        cancel();
                        return;
                    }
                    if (!world.setBlockState(pos, entry.state(), 3)) break;
                    pending.remove();
                    processed++;
                }
            } finally {
                inFlight = false;
            }
        });
    }

    public static String lastError() { return lastError; }
    public static int remaining() { return pending.size(); }
    public static void cancel() { pending.clear(); ownerServer = null; preflightDone = false; }
}
