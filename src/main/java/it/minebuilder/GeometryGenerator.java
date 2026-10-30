package it.minebuilder;

import it.minebuilder.auth.ChatGptInference;
import net.minecraft.util.math.BlockPos;
import java.util.function.Consumer;

/** Generates and validates a canonical compact geometry, without UI concerns. */
public final class GeometryGenerator {
    private GeometryGenerator() {}

    public record Result(String json, BuildPlan blueprint) {}

    public static Result generate(String token, String model, DesignContract design,
                                  BlockPos origin, Consumer<String> progress) throws Exception {
        progress.accept("GPT: geometria completa");
        String geometry = ChatGptInference.ask(token, model,
            design.geometryInstruction()
            + " Produce ONE COMPLETE JSON geometric blueprint for the ENTIRE building. "
            + "Use compact cuboids and shapes for repeated structure, explicit blocks only "
            + "for fine details and air openings. Keep shared axes and materials. "
            + "Do not split into pages or redesign components. Output ONLY JSON arrays "
            + "cuboids, shapes, blocks. Local game partitions into sections. "
            + "Stay within x,z -128..128 and y -32..128. Avoid thousands of individual records.");
        geometry = geometry.trim();
        if (geometry.startsWith("```")) {
            int first = geometry.indexOf('\n');
            int last = geometry.lastIndexOf("```");
            if (first >= 0 && last > first) geometry = geometry.substring(first + 1, last).trim();
        }
        if (geometry.length() > 2_000_000)
            throw new IllegalStateException("Specifica geometrica troppo lunga");
        BuildPlan blueprint = BuildPlan.parseComplete(geometry, origin);
        BlueprintContract.validateAndPartition(blueprint);
        progress.accept("Blueprint tecnico verificato");
        return new Result(geometry, blueprint);
    }
}
