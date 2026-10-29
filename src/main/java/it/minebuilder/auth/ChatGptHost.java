package it.minebuilder.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Stable per-installation identifier. Not a credential. */
public final class ChatGptHost {
    private ChatGptHost() {}

    public static synchronized String getOrCreate(Path configDirectory) throws IOException {
        Files.createDirectories(configDirectory);
        Path file = configDirectory.resolve("chatgpt-host-id.txt");
        if (Files.isRegularFile(file)) {
            String value = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (!value.matches("urn:uuid:[0-9a-fA-F-]{36}"))
                throw new IOException("Invalid stored ChatGPT host identifier");
            return value;
        }
        String id = "urn:uuid:" + UUID.randomUUID();
        Path temp = Files.createTempFile(configDirectory, "host-", ".tmp");
        try {
            Files.writeString(temp, id, StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
        return id;
    }
}
