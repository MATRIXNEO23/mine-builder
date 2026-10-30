package it.minebuilder;

/** Self-contained regression tests: no OpenAI credentials or live Minecraft needed. */
public final class ComponentManifestTest {
    public static void main(String[] args) {
        String valid = """
            {"components":[
              {"id":"stand-north","description":"North stand","min_x":-150,"max_x":150,"min_y":0,"max_y":60,"min_z":-150,"max_z":-1},
              {"id":"stand-south","description":"South stand","min_x":-150,"max_x":150,"min_y":0,"max_y":60,"min_z":1,"max_z":150}
            ]}""";
        ComponentManifest manifest = ComponentManifest.parse(valid);
        if (manifest.components().size() != 2) throw new AssertionError("Wrong component count");
        if (!manifest.components().get(0).contains(120, 35, -14))
            throw new AssertionError("Relative component bounds incorrect");
        if (manifest.components().get(0).contains(120, 35, 14))
            throw new AssertionError("Out-of-bounds position allowed");
        reject(valid.replace("\"stand-south\"", "\"stand-north\""));
        reject(valid.replace("\"max_z\":150", "\"max_z\":2001"));
        reject(valid.replace("\"max_z\":150", "\"max_z\":-999"));
        reject("{\"components\":[]}");
        var large = new DesignContract("Large stadium",
            "GEOMETRY=COMPONENTS\nStadium axes and floors",
            DesignContract.ResearchMode.RECONSTRUCTION);
        if (!large.requiresComponents()) throw new AssertionError("Composite mode not selected");
        var small = new DesignContract("House", "GEOMETRY=SINGLE\nSmall house",
            DesignContract.ResearchMode.NONE);
        if (small.requiresComponents()) throw new AssertionError("Small project requires components");
        System.out.println("Component manifest contract checks passed");
    }

    private static void reject(String value) {
        try { ComponentManifest.parse(value); }
        catch (IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("Invalid manifest accepted: " + value);
    }
}
