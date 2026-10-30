package it.minebuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.tag.BlockTags;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;
import net.minecraft.util.WorldSavePath;

/** Local singleplayer prototype: paced placement through the integrated server thread. */
public final class BuildExecutor {
    // Adaptive streaming: bounded placements AND a short server-thread time budget.
    private static final int MIN_BATCH = 4;
    private static final int MAX_BATCH = 32;
    private static final long TIME_BUDGET_NS = 2_000_000L;
    private static int batchSize = 16;
    private static volatile int cooldownTicks = 0;
    private static Queue<BuildPlan.Placement> pending = new ArrayDeque<>();
    private static Queue<BuildPlan> remainingSections = new ArrayDeque<>();
    private static int completedSections;
    private static int totalSections;
    private static List<BuildPlan.Placement> currentSection = List.of();
    private static BuildPlan activePlan;
    private static String activeDimension = "";
    private static final String RESUME_NAME = "active-build";
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
        try {
            Path existing = progressPath(client);
            if (Files.isRegularFile(existing)) {
                List<String> previous = Files.readAllLines(existing);
                if (previous.size() == 2 && !previous.get(1).equals("COMPLETE")) {
                    boolean finished = false;
                    try {
                        int count = Integer.parseInt(previous.get(1));
                        BuildPlan prior = BlueprintLibrary.load(client, RESUME_NAME,
                            readSavedOrigin(client));
                        finished = count >= prior.sections().size();
                    } catch (RuntimeException ignored) {
                        // Invalid checkpoints are not safe to overwrite.
                    }
                    if (!finished)
                        throw new IOException("Esiste un lavoro sospeso: usa Riprendi costruzione");
                }
            }
            BlueprintLibrary.save(client, RESUME_NAME, plan);
            saveProgress(client, key, 0);
            Path originFile = progressPath(client).resolveSibling("active-build.origin");
            BlockPos origin = plan.placements().get(0).pos();
            Files.writeString(originFile, origin.getX() + "," + origin.getY() + "," + origin.getZ());
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile salvare il progetto prima di costruire", e);
        }
        batchSize = 16;
        cooldownTicks = 0;
        activePlan = plan;
        activeDimension = key;
        remainingSections = new ArrayDeque<>(plan.sections());
        totalSections = remainingSections.size();
        completedSections = 0;
        currentSection = remainingSections.remove().placements();
        pending = new ArrayDeque<>(currentSection);
        preflightDone = false;
        lastError = "";
        ownerServer = client.getServer();
        worldKey = key;
    }

    public static void tick(MinecraftClient client) {
        if (inFlight) return;
        if (cooldownTicks > 0) { cooldownTicks--; return; }
        if (pending.isEmpty() && !remainingSections.isEmpty()) {
            currentSection = remainingSections.remove().placements();
        pending = new ArrayDeque<>(currentSection);
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
                            lastError = "Blocco fuori dai limiti: " + target.toShortString()
                                + " (Y valida da " + world.getBottomY() + " a "
                                + (world.getTopY() - 1) + "). Diario conservato; non costruire questa pagina.";
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
                long started = System.nanoTime();
                while (processed < batchSize && !pending.isEmpty()) {
                    if (processed > 0 && System.nanoTime() - started >= TIME_BUDGET_NS) break;
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
                // Scale down under load; increase slowly when placements are cheap.
                long elapsed = System.nanoTime() - started;
                if (elapsed > TIME_BUDGET_NS) {
                    batchSize = Math.max(MIN_BATCH, batchSize / 2);
                    // Skip upcoming client ticks when this placement batch was costly.
                    // No version-specific Minecraft server timing API required.
                    cooldownTicks = elapsed > TIME_BUDGET_NS * 3 ? 5 : 2;
                }
                else if (processed == batchSize && elapsed < TIME_BUDGET_NS / 2)
                    batchSize = Math.min(MAX_BATCH, batchSize + 2);
                if (pending.isEmpty()) {
                    for (var expected : currentSection) {
                        if (!world.isChunkLoaded(expected.pos())
                            || !world.getBlockState(expected.pos()).equals(expected.state())) {
                            lastError = "Verifica sezione fallita in " + expected.pos().toShortString();
                            cancel();
                            return;
                        }
                    }
                    try {
                        // Persist world changes before advancing the checkpoint.
                        world.getChunkManager().save(true);
                        saveProgress(client, worldKey, completedSections + 1);
                        completedSections++;
                    } catch (IOException e) {
                        lastError = "Salvataggio avanzamento fallito: " + e.getMessage();
                        cancel();
                    }
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

    private static Path progressPath(MinecraftClient client) throws IOException {
        if (client.getServer() == null || !client.isInSingleplayer())
            throw new IOException("Mondo locale non disponibile");
        Path dir = client.getServer().getSavePath(WorldSavePath.ROOT).resolve("minebuilder");
        Files.createDirectories(dir);
        return dir.resolve("active-build.progress");
    }

    private static void saveProgress(MinecraftClient client, String dimension, int done)
            throws IOException {
        Path file = progressPath(client);
        Path tmp = Files.createTempFile(file.getParent(), "progress-", ".tmp");
        try {
            Files.writeString(tmp, dimension + System.lineSeparator() + done + System.lineSeparator());
            try { Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(tmp); }
    }

    private static BlockPos readSavedOrigin(MinecraftClient client) throws IOException {
        Path originFile = progressPath(client).resolveSibling("active-build.origin");
        if (!Files.isRegularFile(originFile)) throw new IOException("Origine non salvata");
        String[] coords = Files.readString(originFile).trim().split(",");
        if (coords.length != 3) throw new IOException("Origine non valida");
        try {
            return new BlockPos(Integer.parseInt(coords[0]),
                Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
        } catch (NumberFormatException e) {
            throw new IOException("Origine non valida", e);
        }
    }

    /** Resume only on explicit request; completed sections are not regenerated. */
    public static void resume(MinecraftClient client) throws IOException {
        if (inFlight || !pending.isEmpty() || !remainingSections.isEmpty())
            throw new IOException("Costruzione gia in corso");
        Path file = progressPath(client);
        if (!Files.isRegularFile(file)) throw new IOException("Nessun lavoro sospeso");
        List<String> lines = Files.readAllLines(file);
        if (lines.size() != 2) throw new IOException("Avanzamento non valido");
        String dimension = client.world.getRegistryKey().getValue().toString();
        if (!dimension.equals(lines.get(0))) throw new IOException("Dimensione diversa dal progetto");
        int done;
        try { done = Integer.parseInt(lines.get(1)); }
        catch (NumberFormatException e) { throw new IOException("Avanzamento non valido", e); }
        // Saved blueprint uses the first block as its reference point.
        // Persisting the original absolute origin is essential for exact resumption.
        BlockPos origin = readSavedOrigin(client);
        BuildPlan plan = BlueprintLibrary.load(client, RESUME_NAME, origin);
        List<BuildPlan> sections = plan.sections();
        if (done < 0 || done > sections.size()) throw new IOException("Avanzamento non valido");
        if (done == sections.size()) throw new IOException("Costruzione gia completata");
        plan.validateSections();
        batchSize = 16;
        cooldownTicks = 0;
        activePlan = plan;
        activeDimension = dimension;
        ownerServer = client.getServer();
        worldKey = dimension;
        totalSections = sections.size();
        completedSections = done;
        remainingSections = new ArrayDeque<>(sections.subList(done + 1, sections.size()));
        currentSection = sections.get(done).placements();
        pending = new ArrayDeque<>(currentSection);
        preflightDone = false;
        lastError = "";
    }

    /** Only true after all sections were verified and their progress saved. */
    public static boolean finished() {
        return ownerServer != null && !inFlight && pending.isEmpty()
            && remainingSections.isEmpty() && completedSections == totalSections
            && totalSections > 0 && lastError.isEmpty();
    }

    /** Exposes active execution state without allowing another job to overwrite it. */
    public static boolean busy() {
        return inFlight || !pending.isEmpty() || !remainingSections.isEmpty();
    }

    public static String lastError() { return lastError; }
    public static boolean preflightPassed() { return preflightDone; }
    public static int remaining() { return pending.size() + remainingSections.stream()
        .mapToInt(section -> section.placements().size()).sum(); }
    public static String sectionProgress() { return completedSections + "/" + totalSections; }
    public static void cancel() { pending.clear(); remainingSections.clear(); currentSection = List.of(); ownerServer = null; preflightDone = false; }
}
