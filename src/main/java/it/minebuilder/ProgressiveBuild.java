package it.minebuilder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/**
 * Journal-backed sequential page executor. GPT may write the next page while
 * the integrated server builds the current one. No page is built twice.
 */
public final class ProgressiveBuild {
    private static Path job;
    private static int nextPage;
    private static boolean awaitingPage;
    private static boolean approved;
    private static String world;
    private static String error = "";
    private ProgressiveBuild() {}

    public static void approve(MinecraftClient client, Path directory) throws IOException {
        if (client.getServer() == null || !client.isInSingleplayer() || client.world == null)
            throw new IOException("Solo mondo locale");
        if (job != null && !job.equals(directory)) throw new IOException("Altro progetto progressivo attivo");
        Path marker = directory.resolve("approved.txt");
        Files.writeString(marker, client.world.getRegistryKey().getValue().toString());
        job = directory;
        world = client.world.getRegistryKey().getValue().toString();
        nextPage = readIndex(directory);
        approved = true;
        awaitingPage = false;
        error = "";
    }

    public static void recover(MinecraftClient client, Path directory) throws IOException {
        Path marker = directory.resolve("approved.txt");
        if (!Files.isRegularFile(marker)) return;
        if (client.world == null || !Files.readString(marker).trim()
                .equals(client.world.getRegistryKey().getValue().toString())) return;
        approve(client, directory);
        if (!BuildExecutor.busy() && !BuildExecutor.finished()) {
            try {
                BuildExecutor.resume(client);
                awaitingPage = true;
            } catch (IOException ignored) {
                // No unfinished page; the next tick will inspect the journal.
            }
        }
    }

    public static void tick(MinecraftClient client) {
        if (!approved || job == null || client.getServer() == null || client.world == null) return;
        if (!world.equals(client.world.getRegistryKey().getValue().toString())) return;
        if (awaitingPage) {
            if (!BuildExecutor.lastError().isEmpty()) {
                error = BuildExecutor.lastError();
                approved = false;
                return;
            }
            if (!BuildExecutor.finished()) return;
            try {
                writeIndex(job, nextPage + 1);
                nextPage++;
                awaitingPage = false;
            } catch (IOException e) {
                error = e.getMessage();
                approved = false;
                return;
            }
        }
        if (BuildExecutor.busy()) return;
        Path file = job.resolve(String.format(Locale.ROOT, "%04d.json", nextPage));
        if (!Files.isRegularFile(file)) return;
        try {
            String json = Files.readString(file);
            var object = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (!object.has("complete")) throw new IOException("Pagina senza complete");
            String[] xyz = Files.readString(job.resolve("origin.txt")).trim().split(",");
            BlockPos origin = new BlockPos(Integer.parseInt(xyz[0]),
                Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
            BuildPlan page = BuildPlan.parse(json, origin);
            BuildExecutor.start(client, page);
            awaitingPage = true;
        } catch (Exception e) {
            error = e.getMessage();
            approved = false;
        }
    }

    public static boolean active() { return approved; }
    public static String error() { return error; }
    public static void pause() { approved = false; job = null; awaitingPage = false; }

    private static int readIndex(Path dir) throws IOException {
        Path file = dir.resolve("built.txt");
        if (!Files.isRegularFile(file)) return 0;
        int index = Integer.parseInt(Files.readString(file).trim());
        if (index < 0 || index > 100) throw new IOException("Indice non valido");
        return index;
    }
    private static void writeIndex(Path dir, int index) throws IOException {
        Path temp = Files.createTempFile(dir, "built-", ".tmp");
        try {
            Files.writeString(temp, Integer.toString(index));
            try {
                Files.move(temp, dir.resolve("built.txt"), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, dir.resolve("built.txt"), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }
}
