package it.minebuilder;

import java.util.Locale;

/**
 * Immutable handoff between GPT planning, optional reference research,
 * geometry generation and the Minecraft executor.
 * Research decisions are advisory; technical safety is always enforced locally.
 */
public record DesignContract(String request, String masterPlan, ResearchMode researchMode) {
    public enum ResearchMode { NONE, INSPIRATION, RECONSTRUCTION }

    public DesignContract {
        if (request == null || request.isBlank()) throw new IllegalArgumentException("Richiesta vuota");
        if (masterPlan == null || masterPlan.isBlank()) throw new IllegalArgumentException("Progetto architettonico vuoto");
        if (masterPlan.length() > 30000) throw new IllegalArgumentException("Progetto architettonico troppo lungo");
        if (researchMode == null) throw new IllegalArgumentException("Modalita ricerca assente");
    }

    /** A planning response must start with one explicit machine-readable mode line. */
    public static DesignContract fromPlanningResponse(String request, String response) {
        if (response == null) throw new IllegalArgumentException("Risposta progettazione assente");
        String trimmed = response.trim();
        int newline = trimmed.indexOf('\n');
        if (newline < 0) throw new IllegalArgumentException("Risposta progettazione senza contratto ricerca");
        String directive = trimmed.substring(0, newline).trim().toUpperCase(Locale.ROOT);
        if (!directive.startsWith("RESEARCH="))
            throw new IllegalArgumentException("Risposta progettazione senza RESEARCH=");
        ResearchMode mode;
        try { mode = ResearchMode.valueOf(directive.substring("RESEARCH=".length()).trim()); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Modalita ricerca GPT non valida", ex); }
        return new DesignContract(request, trimmed.substring(newline + 1).trim(), mode);
    }

    public String geometryInstruction() {
        return "IMMUTABLE ARCHITECTURAL CONTRACT: " + masterPlan
            + "\nOriginal request: " + request
            + "\nGenerate ONE complete JSON object with arrays cuboids, shapes, blocks. "
            + "Use compact shapes for large volumes and explicit blocks for details and air openings. "
            + "Never redesign the structure. Use shared axes, levels, materials and dimensions from the contract. "
            + "Do not output partial geometry and do not claim completion if required parts are missing. "
            + "Return JSON only.";
    }
}
