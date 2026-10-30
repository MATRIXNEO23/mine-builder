package it.minebuilder;

/** Shared UI and handler guard: a pending object alone is not an approved blueprint. */
final class ActionGate {
    private ActionGate() {}
    static boolean canBuild(boolean world, boolean busy, boolean validated, boolean hasPlan) {
        return world && !busy && validated && hasPlan;
    }
}
