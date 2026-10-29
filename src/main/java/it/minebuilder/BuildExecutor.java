package it.minebuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
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
                        if (!existing.isAir() && !existing.equals(candidate.state())
                            && !isNaturalTerrain(existing)) {
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
                    // Recheck immediately before mutation: the player may have placed
                    // a block after preflight while the build was progressing.
                    var current = world.getBlockState(pos);
                    if (!current.isAir() && !current.equals(entry.state())
                            && !isNaturalTerrain(current)) {
                        lastError = "Costruzione interrotta: blocco protetto in " + pos.toShortString();
                        cancel();
                        return;
                    }
                    if (current.equals(entry.state())) {
                        pending.remove();
                        processed++;
                        continue;
                    }
                    if (!world.setBlockState(pos, entry.state(), 3)) {
                        lastError = "Impossibile posizionare un blocco in " + pos.toShortString();
                        cancel();
                        return;
                    }
                    pending.remove();
                    processed++;
                }
            } finally {
                inFlight = false;
            }
        });
    }

    /** Conservative terrain clearance within the planned blueprint volume only.
     * Artificial blocks, chests, ores, bedrock and fluids remain protected.
     */
    private static boolean isNaturalTerrain(BlockState state) {
        if (state.isIn(BlockTags.LEAVES) || state.isIn(BlockTags.FLOWERS)
            || state.isIn(BlockTags.SAPLINGS) || state.isIn(BlockTags.REPLACEABLE))
            return true;
        return state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.DIRT)
            || state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.PODZOL)
            || state.isOf(Blocks.ROOTED_DIRT) || state.isOf(Blocks.MYCELIUM)
            || state.isOf(Blocks.STONE) || state.isOf(Blocks.ANDESITE)
            || state.isOf(Blocks.DIORITE) || state.isOf(Blocks.GRANITE)
            || state.isOf(Blocks.DEEPSLATE) || state.isOf(Blocks.TUFF)
            || state.isOf(Blocks.CALCITE) || state.isOf(Blocks.GRAVEL)
            || state.isOf(Blocks.SAND) || state.isOf(Blocks.RED_SAND)
            || state.isOf(Blocks.CLAY) || state.isOf(Blocks.SNOW)
            || state.isOf(Blocks.SNOW_BLOCK) || state.isOf(Blocks.MOSS_BLOCK);
    }

    public static String lastError() { return lastError; }
    public static int remaining() { return pending.size(); }
    public static void cancel() { pending.clear(); ownerServer = null; preflightDone = false; }
}
