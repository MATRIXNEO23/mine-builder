package it.minebuilder;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.minebuilder.auth.ChatGptInference;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds immutable, independently generated components from one shared plan.
 * Atomic per-component journal allows safe continuation after interruption.
 * Never modifies any Minecraft block.
 */
public final class ComponentGeometryGenerator {
    private static final int TOTAL_LIMIT = 2_000_000;
    private static final int JSON_LIMIT = 2_000_000;
    private ComponentGeometryGenerator() {}

    public static GeometryGenerator.Result generate(String token, String model,
            DesignContract design, BlockPos origin, Consumer<String> progress,
            GenerationJournal journal) throws Exception {
        Path manifestPath = journal.directory().resolve("component-manifest.json");
        String manifestJson;
        ComponentManifest manifest;
        if (Files.isRegularFile(manifestPath)) {
            manifestJson = Files.readString(manifestPath);
            manifest = ComponentManifest.parse(manifestJson);
            progress.accept("Manifest componenti recuperato dal diario");
        } else {
            progress.accept("GPT: suddivisione architettonica globale");
            Exception failure = null;
            manifestJson = null;
            manifest = null;
            for (int attempt = 0; attempt < 2; attempt++) {
                String response = ChatGptInference.ask(token, model,
                    "Use this IMMUTABLE master architectural plan: " + design.masterPlan()
                    + "\nOriginal request: " + design.request()
                    + "\nProduce ONLY a JSON object with 'components' array (2..64). "
                    + "Each component requires: id (lowercase ASCII), "
                    + "description (function, material, interfaces, exact architectural detail), "
                    + "min_x,max_x,min_y,max_y,min_z,max_z relative to ONE global origin. "
                    + "All bounds within +/-2000. Allocate all requested structures and details "
                    + "with shared levels, axes and consistent touching interfaces. "
                    + "Every part will use this unchanged manifest; do not redesign sections. "
                    + (attempt == 0 ? "" : " Previous JSON invalid; return complete manifest only."));
                try {
                    String candidate = clean(response);
                    if (candidate.length() > 100_000)
                        throw new IllegalArgumentException("Manifest troppo lungo");
                    manifest = ComponentManifest.parse(candidate);
                    manifestJson = candidate;
                    break;
                } catch (RuntimeException ex) {
                    failure = ex;
                    progress.accept("Manifest non valido: nuovo tentativo");
                }
            }
            if (manifest == null) throw new IOException("Manifest non valido", failure);
            journal.write("component-manifest.json", manifestJson);
        }
        BuildPlan result = assemble(token, model, design, origin, progress,
            journal, manifest, manifestJson, true);
        journal.write("composite-complete.txt",
            "COMPLETE\ncomponents=" + manifest.components().size()
            + "\nblocks=" + result.placements().size() + "\n");
        return new GeometryGenerator.Result(null, result);
    }

    /** Load and verify a complete saved project without requiring network access. */
    public static BuildPlan loadSaved(Path directory, BlockPos origin) throws Exception {
        String json = Files.readString(directory.resolve("component-manifest.json"));
        ComponentManifest manifest = ComponentManifest.parse(json);
        return assemble(null, null, null, origin, ignored -> {},
            GenerationJournal.reopen(directory), manifest, json, false);
    }

    private static BuildPlan assemble(String token, String model, DesignContract design,
            BlockPos origin, Consumer<String> progress, GenerationJournal journal,
            ComponentManifest manifest, String manifestJson, boolean allowGeneration) throws Exception {
        Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
        int count = manifest.components().size();
        for (int i = 0; i < count; i++) {
            ComponentManifest.Component component = manifest.components().get(i);
            String fileName = String.format(java.util.Locale.ROOT, "component-%02d.json", i);
            Path file = journal.directory().resolve(fileName);
            String json;
            BuildPlan part;
            if (Files.isRegularFile(file)) {
                json = Files.readString(file);
                part = validatePart(json, component, origin);
                checkAgainstPrevious(cells, part, component);
                progress.accept("Componente salvato recuperato " + (i + 1) + "/" + count);
            } else {
                if (!allowGeneration || token == null || model == null || design == null)
                    throw new IOException("Componente mancante: " + component.id());
                progress.accept("GPT: componente " + (i + 1) + "/" + count + " — " + component.id());
                part = null;
                json = null;
                Exception failure = null;
                for (int attempt = 0; attempt < 2; attempt++) {
                    String answer = ChatGptInference.ask(token, model,
                        BuildPlan.GEOMETRY_SCHEMA + "Generate ONLY the complete geometry for ONE component of an approved "
                        + "Minecraft 1.20.1 architectural project. No redesign. "
                        + "Global master plan: " + design.masterPlan()
                        + "\nIMMUTABLE component manifest: " + manifestJson
                        + "\nTarget component ID: " + component.id()
                        + "\nTarget description: " + component.description()
                        + "\nExact relative bounds X=" + component.minX() + ".." + component.maxX()
                        + " Y=" + component.minY() + ".." + component.maxY()
                        + " Z=" + component.minZ() + ".." + component.maxZ()
                        + "\nReturn one JSON object with component_id='" + component.id()
                        + "', complete=true, arrays cuboids, shapes, blocks. "
                        + "Use compact primitives; explicit air carves necessary openings. "
                        + "Respect shared axes, elevations, interface surfaces, materials, "
                        + "stairs, facades and requested details. No other component geometry. "
                        + "Use Minecraft 1.20.1 IDs and valid state properties; JSON only. "
                        + (attempt == 0 ? "" : " Prior output invalid ("
                            + failure.getMessage() + "); fix bounds and geometry."));
                    journal.write("component-" + i + "-attempt-" + (attempt + 1) + ".json", answer);
                    try {
                        String candidate = clean(answer);
                        BuildPlan parsed = validatePart(candidate, component, origin);
                        // Reject cross-component overlaps and excess blocks BEFORE journaling.
                        checkAgainstPrevious(cells, parsed, component);
                        json = candidate;
                        part = parsed;
                        break;
                    } catch (RuntimeException ex) {
                        // A project that really exceeds the total block limit must stop.
                        // Asking GPT to shrink architecture would silently lose fidelity.
                        String detail = String.valueOf(ex.getMessage());
                        if (detail.contains("oltre 2 milioni") || detail.contains("2000000"))
                            throw new IOException("Progetto troppo grande per il motore attuale: "
                                + "servono componenti su disco senza limite aggregato. "
                                + "Dettagli e diari conservati; nessun blocco costruito.", ex);
                        failure = ex;
                        progress.accept("Componente " + component.id() + ": nuovo tentativo");
                    }
                }
                if (part == null) throw new IOException(
                    "Geometria componente non valida: " + component.id(), failure);
                // Never replace a valid saved component with a fresh GPT response.
                journal.write(fileName, json);
            }
            for (var placement : part.placements()) {
                BlockState previous = cells.putIfAbsent(placement.pos(), placement.state());
                if (previous != null && !previous.equals(placement.state()))
                    throw new IOException("Conflitto componenti in " + placement.pos().toShortString()
                        + " (" + component.id() + ")");
            }
            if (cells.size() > TOTAL_LIMIT)
                throw new IOException("Limite sicurezza 2 milioni di blocchi superato; nulla costruito");
            progress.accept("Componenti verificati: " + (i + 1) + "/" + count
                + " | blocchi " + cells.size());
        }
        BuildPlan complete = BuildPlan.fromPlacements(cells.entrySet().stream()
            .map(e -> new BuildPlan.Placement(e.getKey(), e.getValue())).toList());
        BlueprintContract.validateAndPartition(complete);
        return complete;
    }

    private static void checkAgainstPrevious(Map<BlockPos, BlockState> cells,
            BuildPlan part, ComponentManifest.Component component) {
        long added = 0;
        for (var entry : part.placements()) {
            BlockState earlier = cells.get(entry.pos());
            if (earlier != null && !earlier.equals(entry.state()))
                throw new IllegalArgumentException("Conflitto tra componenti in "
                    + entry.pos().toShortString() + " (" + component.id() + ")");
            if (earlier == null) added++;
        }
        if (cells.size() + added > TOTAL_LIMIT)
            throw new IllegalArgumentException("Progetto oltre 2 milioni di blocchi");
    }

    private static BuildPlan validatePart(String json, ComponentManifest.Component component,
            BlockPos origin) {
        if (json.length() > JSON_LIMIT)
            throw new IllegalArgumentException("Specifica componente troppo lunga");
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("component_id") || !component.id().equals(root.get("component_id").getAsString())
            || !root.has("complete") || !root.get("complete").getAsJsonPrimitive().isBoolean()
            || !root.get("complete").getAsBoolean())
            throw new IllegalArgumentException("Componente non completo o identificativo errato");
        BuildPlan part = BuildPlan.parseComplete(json, origin);
        for (var placement : part.placements()) {
            BlockPos p = placement.pos();
            long x = (long)p.getX() - origin.getX();
            long y = (long)p.getY() - origin.getY();
            long z = (long)p.getZ() - origin.getZ();
            if (!component.contains(x, y, z))
                throw new IllegalArgumentException("Fuori componente " + component.id()
                    + ": " + x + "," + y + "," + z);
        }
        return part;
    }

    private static String clean(String response) {
        String result = response.trim();
        String fence = String.valueOf((char)96).repeat(3);
        if (result.startsWith(fence)) {
            int first = result.indexOf('\n'), last = result.lastIndexOf(fence);
            if (first >= 0 && last > first) result = result.substring(first + 1, last).trim();
        }
        return result;
    }
}
