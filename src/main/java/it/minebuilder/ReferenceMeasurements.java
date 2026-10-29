package it.minebuilder;

/** Verified metric dimensions for a user-supplied real building reference. */
public record ReferenceMeasurements(String name, int width, int depth, int height, String source) {
    public ReferenceMeasurements {
        if (name == null || name.isBlank() || source == null || source.isBlank())
            throw new IllegalArgumentException("Reference name and source required");
        if (width < 1 || depth < 1 || height < 1 || width > 2000 || depth > 2000 || height > 2000)
            throw new IllegalArgumentException("Invalid dimensions in metres");
    }
}
