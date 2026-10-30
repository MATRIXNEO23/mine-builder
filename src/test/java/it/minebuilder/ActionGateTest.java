package it.minebuilder;
public final class ActionGateTest {
    public static void main(String[] args) {
        // Before generation, during generation (even with an old plan), failed validation,
        // recovery, and completed validation must have distinct outcomes.
        reject(true, false, false, false);
        reject(true, true, false, false);
        reject(true, true, true, true);
        reject(true, false, false, true);
        reject(true, false, true, false);
        reject(false, false, true, true);
        if (!ActionGate.canBuild(true, false, true, true)) throw new AssertionError("Completed plan disabled");
        System.out.println("Action readiness checks passed");
    }
    static void reject(boolean world, boolean busy, boolean validated, boolean plan) {
        if (ActionGate.canBuild(world, busy, validated, plan)) throw new AssertionError("Premature build allowed");
    }
}
