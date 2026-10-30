package it.minebuilder;

/** Minimal planning decisions that can be regression-tested without Minecraft. */
public final class DesignContractTest {
    public static void main(String[] args) {
        for (DesignContract.ResearchMode mode : DesignContract.ResearchMode.values()) {
            DesignContract contract = DesignContract.fromPlanningResponse(
                "Villa moderna", "RESEARCH=" + mode + "\nGEOMETRY=SINGLE\nVilla moderna a due piani con scala interna.");
            if (contract.researchMode() != mode) throw new AssertionError("Mode " + mode);
            if (!contract.geometryInstruction().contains("Villa moderna a due piani"))
                throw new AssertionError("Master plan lost");
        }
        assertInvalid("No directive\nHouse");
        assertInvalid("RESEARCH=INVALID\nGEOMETRY=SINGLE\nHouse");
        assertInvalid("RESEARCH=NONE\nHouse");
        assertInvalid("RESEARCH=NONE\nGEOMETRY=UNKNOWN\nHouse");
        assertInvalid("RESEARCH=NONE\nGEOMETRY=COMPONENTS\n ");
        var stadium = DesignContract.fromPlanningResponse("Stadium",
            "RESEARCH=RECONSTRUCTION\nGEOMETRY=COMPONENTS\nLarge stadium on fixed axes");
        if (!stadium.requiresComponents()) throw new AssertionError("Composite routing lost");
        assertInvalid("RESEARCH=NONE\n ");
        System.out.println("Design contract checks passed");
    }
    private static void assertInvalid(String response) {
        try {
            DesignContract.fromPlanningResponse("house", response);
            throw new AssertionError("Invalid response accepted: " + response);
        } catch (IllegalArgumentException expected) {
            // Invalid planning output must never become a buildable blueprint.
        }
    }
}
