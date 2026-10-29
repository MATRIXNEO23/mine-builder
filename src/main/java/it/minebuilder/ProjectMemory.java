package it.minebuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Local conversation and project checkpoint. No access tokens or passwords.
 * Caller must supply a world-specific directory to prevent cross-world mixing.
 */
public final class ProjectMemory {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_MESSAGES = 200;
    private static final int MAX_TEXT = 2048;

    private ProjectMemory() {}

    public static final class State {
        public String projectName = "";
        public String status = "idle";
        public int completedBlocks;
        public int plannedBlocks;
        public List<Message> messages = new ArrayList<>();
    }

    public static final class Message {
        public String role;
        public String text;
        public long timestamp;

        public Message(String role, String text) {
            this.role = role;
            this.text = text;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static State load(Path worldDirectory) throws IOException {
        Path file = worldDirectory.resolve("minebuilder").resolve("project.json");
        if (!Files.isRegularFile(file)) return new State();
        if (Files.size(file) > 1024 * 1024) throw new IOException("Project checkpoint exceeds size limit");
        State state = JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), State.class);
        if (state == null || state.messages == null || state.messages.size() > MAX_MESSAGES)
            throw new IOException("Invalid project checkpoint");
        return state;
    }

    public static void append(State state, String role, String message) {
        if (!"user".equals(role) && !"assistant".equals(role))
            throw new IllegalArgumentException("Unsupported message role");
        if (message == null || message.length() > MAX_TEXT)
            throw new IllegalArgumentException("Message too long");
        state.messages.add(new Message(role, message));
        while (state.messages.size() > MAX_MESSAGES) state.messages.remove(0);
    }

    public static void save(Path worldDirectory, State state) throws IOException {
        Path directory = worldDirectory.resolve("minebuilder");
        Files.createDirectories(directory);
        Path temp = Files.createTempFile(directory, "project-", ".tmp");
        Path destination = directory.resolve("project.json");
        try {
            Files.writeString(temp, JSON.toJson(state), StandardCharsets.UTF_8);
            try {
                Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
