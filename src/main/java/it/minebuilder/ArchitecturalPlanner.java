package it.minebuilder;

import it.minebuilder.auth.ChatGptInference;
import java.util.function.Consumer;

/** GPT architectural planning, independent from Minecraft GUI and world state. */
public final class ArchitecturalPlanner {
    private ArchitecturalPlanner() {}

    public static DesignContract plan(String token, String model, String request,
                                      Consumer<String> progress) throws Exception {
        progress.accept("GPT: interpretazione e progetto");
        String firstPlan = ChatGptInference.ask(token, model,
            "You are the autonomous architect for a Minecraft 1.20.1 builder. "
            + "Interpret this user request: " + request + ". "
            + "FIRST LINE must be exactly RESEARCH=NONE, RESEARCH=INSPIRATION or RESEARCH=RECONSTRUCTION. "
            + "SECOND LINE must be exactly GEOMETRY=SINGLE or GEOMETRY=COMPONENTS. "
            + "Choose COMPONENTS for a stadium, large multi-part building, neighborhood or town, "
            + "or when one compact response cannot contain all requested detail. Otherwise SINGLE. "
            + "Component geometry must share the immutable global axes, floors and dimensions. "
            + "Choose NONE if references add no useful value; INSPIRATION if photographs or plans "
            + "would help create an ORIGINAL design; RECONSTRUCTION for a named real structure "
            + "requiring evidence and 1:1 fidelity. User does not have to provide sources. "
            + "Then provide ONE coherent technical architectural master plan, including type, "
            + "function, footprint, dimensions, shared coordinate axes, floors, roof, entrances, "
            + "interior circulation, materials and structural connections. "
            + "Do not output block coordinates or JSON. Never confuse estimates with verified measurements.");
        DesignContract design;
        try {
            design = DesignContract.fromPlanningResponse(request, firstPlan);
        } catch (IllegalArgumentException invalid) {
            progress.accept("Contratto GPT non valido: correzione intestazioni");
            String repaired = ChatGptInference.ask(token, model,
                "Rewrite this architectural plan as the REQUIRED machine-readable contract. "
                + "Line 1: RESEARCH=NONE, RESEARCH=INSPIRATION or RESEARCH=RECONSTRUCTION. "
                + "Line 2: GEOMETRY=SINGLE or GEOMETRY=COMPONENTS. "
                + "Then include the full architectural plan. Choose COMPONENTS for large "
                + "multi-part buildings and towns. Preserve the original requested structure "
                + "and all details, axes, dimensions and research intent; do NOT output JSON. "
                + "Original request: " + request + "\nRejected response:\n" + firstPlan);
            design = DesignContract.fromPlanningResponse(request, repaired);
        }
        if (design.researchMode() == DesignContract.ResearchMode.NONE) return design;

        progress.accept("Ricerca autonoma: " + design.researchMode());
        String references;
        try { references = ArchitectureResearch.planningReferences(request); }
        catch (Exception ex) {
            references = "Reference search unavailable: " + ex.getClass().getSimpleName();
            progress.accept(references);
        }
        try {
            String encyclopedia = ArchitectureResearch.encyclopedicReferences(request);
            references += "\n" + encyclopedia.substring(0, Math.min(3200, encyclopedia.length()));
        } catch (Exception ex) {
            progress.accept("Contesto enciclopedico non disponibile: " + ex.getClass().getSimpleName());
        }
        try {
            ArchitectureResearch.VisualReference visual = ArchitectureResearch.visualReference(request);
            if (visual != null) {
                progress.accept("GPT: analisi immagine " + visual.title());
                String analysis = ChatGptInference.askWithImage(token, model,
                    "Analyze only what is VISIBLE in this architectural reference: "
                    + visual.title() + ". Describe geometry, proportions, roof, openings and "
                    + "materials. Distinguish observations from estimates. "
                    + "For inspiration do not copy the building; for reconstruction do not "
                    + "invent dimensions. Return concise architectural observations.",
                    visual.image(), visual.mime());
                references += "\nVisual analysis (" + visual.title() + "): "
                    + analysis.substring(0, Math.min(4500, analysis.length()));
            }
        } catch (Exception ex) {
            progress.accept("Analisi immagine non disponibile: " + ex.getClass().getSimpleName());
        }
        progress.accept("GPT: consolidamento progetto e fonti");
        String revised = ChatGptInference.ask(token, model,
            "Refine this architectural master plan using ONLY relevant research. "
            + "Preserve requested building type and coherent shared geometry. "
            + "For INSPIRATION create an ORIGINAL building; for RECONSTRUCTION prioritize "
            + "documented measurements and clearly mark estimates. "
            + "Do not claim sources were verified if only search titles were found. "
            + "Do not output block coordinates or JSON. Mode: " + design.researchMode()
            + "\nInitial plan: " + design.masterPlan()
            + "\nResearch: " + references.substring(0, Math.min(9500, references.length())));
        // Research consolidation cannot silently change the geometry route or global plan mode.
        String mode = design.requiresComponents() ? "GEOMETRY=COMPONENTS" : "GEOMETRY=SINGLE";
        return new DesignContract(request, mode + "\n" + revised, design.researchMode());
    }
}
