package it.minebuilder;

import it.minebuilder.auth.ChatGptInference;
import net.minecraft.util.math.BlockPos;
import java.util.function.Consumer;

/** Generates and validates a canonical compact geometry, without UI concerns. */
public final class GeometryGenerator {
    private GeometryGenerator() {}

    public record Result(String json, BuildPlan blueprint) {}

    public static Result generate(String token, String model, DesignContract design,
                                  BlockPos origin, Consumer<String> progress,
                                  GenerationJournal journal) throws Exception {
        if (design.requiresComponents())
            return ComponentGeometryGenerator.generate(token, model, design, origin, progress, journal);
        progress.accept("GPT: geometria completa");
        String instruction = BuildPlan.GEOMETRY_SCHEMA + design.geometryInstruction()
            + " Produce ONE COMPLETE JSON geometric blueprint for the ENTIRE building. "
            + "Use compact cuboids and shapes for repeated structure, explicit blocks only "
            + "for fine details and air openings. Keep shared axes and materials. "
            + "Do not split into pages or redesign components. Output ONLY JSON arrays "
            + "cuboids, shapes, blocks. Local game partitions into sections. "
            + "Coordinates are relative to origin and may extend to +/-2000 on each axis; "
            + "the Minecraft world height and loaded chunks are checked before building. "
            + "Use compact primitives to avoid thousands of individual records. "
            + "Use valid Minecraft 1.20.1 block IDs and block state properties. "
            + "For multistorey buildings include accessible internal stairs and walkable openings; "
            + "for pools include basin and water; for trees include trunk and leaves. "
            + "Use air blocks for entrances and passageways; explicit detail blocks override primitives. "
            + "Include all requested architectural features and avoid generic box-shaped substitutes. "
            + "Use no more than 2,000,000 expanded block positions; never silently omit requested sections.";
        String geometry = "";
        BuildPlan blueprint = null;
        Exception lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            String response = ChatGptInference.ask(token, model,
                instruction + (attempt == 0 ? "" :
                    " Prior output could not be parsed or exceeded the safe geometry limit. "
                    + "Repair the previous blueprint below without redesigning or omitting features. "
                    + "Use the exact schemas above. Failure: " + lastFailure.getMessage()
                    + "\nPrevious blueprint:\n" + geometry.substring(0, Math.min(200000, geometry.length()))));
            geometry = response.trim();
            if (geometry.startsWith("```")) {
                int first = geometry.indexOf('\n');
                int last = geometry.lastIndexOf("```");
                if (first >= 0 && last > first) geometry = geometry.substring(first + 1, last).trim();
            }
            journal.write("geometry-attempt-" + (attempt + 1) + ".json", geometry);
            progress.accept("Risposta geometria ricevuta: " + geometry.length() + " caratteri; verifica formato");
            try {
                if (geometry.length() > 2_000_000)
                    throw new IllegalArgumentException("Specifica geometrica troppo lunga");
                blueprint = BuildPlan.parseComplete(geometry, origin);
                BlueprintContract.validateAndPartition(blueprint);
                break;
            } catch (RuntimeException ex) {
                // Invalid/truncated GPT JSON is a recoverable parse error. Do not
                // claim completion or place blocks when retries are exhausted.
                lastFailure = ex;
                journal.write("geometry-error-" + (attempt + 1) + ".txt", String.valueOf(ex.getMessage()));
                progress.accept("Formato non valido: " + ex.getMessage());
                if (attempt == 0) progress.accept("GPT: correzione formato, tentativo 2/2");
            }
        }
        if (blueprint == null) throw new IllegalStateException("Blueprint non valido dopo due tentativi: " + lastFailure.getMessage(), lastFailure);
        progress.accept("Blueprint tecnico verificato");
        return new Result(geometry, blueprint);
    }
}
