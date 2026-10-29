package it.minebuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.tag.BlockTags;
import java.util.*;

/** Local singleplayer prototype: paced placement through the integrated server thread. */
public final class BuildExecutor {
    private static final int PER_BATCH = 16;
    private static Queue<BuildPlan.Placement> pending = new ArrayDeque<>();
    private static Queue<BuildPlan> remainingSections = new ArrayDeque<>();
    private static int completedSections;
    private static int totalSections;
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
        if (inFlight || !pending.isEmpty() || !remainingSections.isEmpty())
            throw new IllegalStateException("A build is already running; finish or cancel it first");
        plan.validateSections();
        remainingSections = new ArrayDeque<>(plan.sections());
        totalSections = remainingSections.size();
        completedSections = 0;
        pending = new ArrayDeque<>(remainingSections.remove().placements());
        preflightDone = false;
        lastError = "";
        ownerServer = client.getServer();
        worldKey = key;
    }

    public static void tick(MinecraftClient client) {
        if (inFlight) return;
        if (pending.isEmpty() && !remainingSections.isEmpty()) {
            pending = new ArrayDeque<>(remainingSections.remove().placements());
            preflightDone = false;
        }
        if (pending.isEmpty()) return;
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
                    // Prevent uncontained fluids from flowing into the surrounding world.
                    Map<BlockPos, BlockState> planned = new HashMap<>();
                    for (var candidate : pending) planned.put(candidate.pos(), candidate.state());
                    for (var candidate : pending) {
                        if (candidate.state().getFluidState().isEmpty()) continue;
                        for (Direction side : new Direction[]{Direction.DOWN, Direction.NORTH,
                                Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                            BlockPos neighbor = candidate.pos().offset(side);
                            BlockState boundary = planned.get(neighbor);
                            if (boundary == null) {
                                if (!world.isChunkLoaded(neighbor)) return;
                                boundary = world.getBlockState(neighbor);
                            }
                            if (boundary.isAir() || boundary.isIn(BlockTags.REPLACEABLE)) {
                                lastError = "Liquido non contenuto vicino a " + neighbor.toShortString();
                                cancel();
                                return;
                            }
                        }
                    }
                    int blockedCount = 0;
                    BlockPos firstBlocked = null;
                    for (var candidate : pending) {
                        BlockPos target = candidate.pos();
                        if (!world.isInBuildLimit(target)) {
                            lastError = "Progetto fuori dai limiti del mondo";
                            cancel();
                            return;
                        }
                        if (!world.isChunkLoaded(target)) {
                            lastError = "In attesa del caricamento del terreno in " + target.toShortString()
                                + ". Avvicinati all’area scelta; nessun blocco modificato.";
                            return;
                        }
                        var existing = world.getBlockState(target);
                        if (!existing.isAir() && !existing.equals(candidate.state())
                            && !isNaturalTerrain(existing)
                            && !isTreeTrunk(world, target, existing)) {
                            blockedCount++;
                            if (firstBlocked == null) firstBlocked = target;
                        }
                    }
                    if (blockedCount > 0) {
                        lastError = "Area occupata: " + blockedCount
                            + " blocchi protetti; primo in " + firstBlocked.toShortString()
                            + ". Nessuna modifica eseguita.";
                        cancel();
                        return;
                    }
                    lastError = "";
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
                            && !isNaturalTerrain(current)
                            && !isTreeTrunk(world, pos, current)) {
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
                if (pending.isEmpty()) completedSections++;
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

    /** Only replace natural logs when a nearby leaf canopy is confirmed.
     * Logs used in ordinary buildings remain protected without this evidence.
     */
    private static boolean isTreeTrunk(net.minecraft.server.world.ServerWorld world,
                                       BlockPos pos, BlockState state) {
        if (!state.isIn(BlockTags.LOGS)) return false;
        for (int dy = 0; dy <= 8; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos sample = pos.add(dx, dy, dz);
                    if (world.isChunkLoaded(sample)
                        && world.getBlockState(sample).isIn(BlockTags.LEAVES))
                        return true;
                }
            }
        }
        return false;
    }

    public static String lastError() { return lastError; }
    public static boolean preflightPassed() { return preflightDone; }
    public static int remaining() { return pending.size() + remainingSections.stream()
        .mapToInt(section -> section.placements().size()).sum(); }
    public static String sectionProgress() { return completedSections + "/" + totalSections; }
    public static void cancel() { pending.clear(); remainingSections.clear(); ownerServer = null; preflightDone = false; }
}
