package it.minebuilder;

import net.minecraft.util.math.BlockPos;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/** Crash-conscious world-local generation journal. Never deletes previous jobs. */
public final class GenerationJournal {
    private final Path directory;

    private GenerationJournal(Path directory) { this.directory = directory; }

    public Path directory() { return directory; }

    public static GenerationJournal create(Path worldRoot, DesignContract design, BlockPos origin)
            throws IOException {
        Path journal = worldRoot.resolve("minebuilder").resolve("generation-journal");
        Files.createDirectories(journal);
        Path job = Files.createDirectory(journal.resolve(UUID.randomUUID().toString()));
        GenerationJournal result = new GenerationJournal(job);
        result.write("master-plan.txt", design.masterPlan());
        result.write("request.txt", design.request());
        result.write("origin.txt", origin.getX() + "," + origin.getY() + "," + origin.getZ());
        result.write("research-mode.txt", design.researchMode().name());
        result.write("design.txt", design.geometryInstruction().substring(
            0, Math.min(12000, design.geometryInstruction().length())));
        return result;
    }

    public void write(String name, String contents) throws IOException {
        if (!name.matches("[a-z][a-z0-9-]*\\.(txt|json)"))
            throw new IOException("Nome file journal non valido");
        Path destination = directory.resolve(name);
        Path temp = Files.createTempFile(directory, "write-", ".tmp");
        try {
            Files.writeString(temp, contents);
            try {
                Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
