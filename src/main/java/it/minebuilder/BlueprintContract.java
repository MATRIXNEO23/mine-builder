package it.minebuilder;

import java.util.List;

/**
 * Technical handoff: the GPT architect owns design choices; Minecraft owns
 * block validity, bounded coordinates, deterministic partitioning and safety.
 * This class never imposes house-specific dimensions or aesthetic rules.
 */
public final class BlueprintContract {
    private BlueprintContract() {}

    public static List<BuildPlan> validateAndPartition(BuildPlan plan) {
        if (plan == null || plan.placements().isEmpty())
            throw new IllegalArgumentException("Blueprint vuoto");
        if (plan.placements().size() > 2_000_000)
            throw new IllegalArgumentException("Blueprint oltre il limite tecnico di sicurezza");
        plan.validateSections();
        List<BuildPlan> sections = plan.sections();
        if (sections.isEmpty())
            throw new IllegalArgumentException("Blueprint senza sezioni");
        return sections;
    }
}
