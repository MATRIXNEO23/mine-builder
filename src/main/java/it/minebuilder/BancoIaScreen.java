package it.minebuilder;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import it.minebuilder.auth.OAuthPkce;
import it.minebuilder.auth.LoopbackRedirect;
import it.minebuilder.auth.ChatGptAuthorization;
import it.minebuilder.auth.ChatGptHost;
import it.minebuilder.auth.ChatGptTokenExchange;
import it.minebuilder.auth.ChatGptInference;
import java.nio.file.Path;
import net.minecraft.util.math.BlockPos;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Lightweight client-side interface. Never modifies the Minecraft world. */
public final class BancoIaScreen extends Screen {
    private TextFieldWidget prompt;
    private static LoopbackRedirect pendingLogin;
    private static volatile String accessToken;
    private static volatile String selectedModel;
    private Text status = Text.literal("ChatGPT non collegato.");
    private String lastReply = "";
    private BuildPlan pendingPlan;

    public BancoIaScreen() {
        super(Text.literal("Banco IA - Mine Builder"));
    }

    @Override
    protected void init() {
        int left = width / 2 - 150;
        prompt = new TextFieldWidget(textRenderer, left, height / 2 - 10, 300, 20, Text.literal("Richiesta"));
        prompt.setMaxLength(512);
        prompt.setPlaceholder(Text.literal("Descrivi cosa vuoi costruire..."));
        addDrawableChild(prompt);
        addDrawableChild(ButtonWidget.builder(Text.literal("Accedi con ChatGPT"), button -> startLogin()).dimensions(left, height / 2 + 18, 145, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Invia a GPT"), button -> sendMessage())
            .dimensions(left, height / 2 + 43, 145, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Chiudi"), button -> close())
            .dimensions(left + 155, height / 2 + 18, 145, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Costruisci"), button -> confirmBuild()).dimensions(left + 155, height / 2 + 43, 145, 20).build());
        setInitialFocus(prompt);
    }

    private void startLogin() {
        try {
            if (pendingLogin != null) pendingLogin.close();
            OAuthPkce.Attempt attempt = OAuthPkce.newAttempt();
            pendingLogin = LoopbackRedirect.start(attempt.state());
            Path config = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("minebuilder");
            String host = ChatGptHost.getOrCreate(config);
            String url = ChatGptAuthorization.initialUrl(host, pendingLogin.redirectUri(), attempt);
            status = Text.literal("Browser aperto: autorizza ChatGPT.");
            Util.getOperatingSystem().open(url);
            LoopbackRedirect receiver = pendingLogin;
            receiver.awaitCallback(Duration.ofMinutes(5)).whenComplete((callback, error) -> {
                if (error != null) {
                    if (client != null) client.execute(() -> {
                        if (pendingLogin == receiver) {
                            status = Text.literal("Accesso scaduto o non riuscito.");
                            pendingLogin = null;
                        }
                        receiver.close();
                    });
                    return;
                }
                // Keep the original redirect URI; close the local listener while
                // the network exchange runs off the Minecraft render thread.
                String redirectUri = receiver.redirectUri();
                CompletableFuture.supplyAsync(() -> {
                    try {
                        var tokens = ChatGptTokenExchange.exchange(callback, attempt, redirectUri);
                        String token = tokens.get("access_token").getAsString();
                        String model = ChatGptInference.firstModel(token);
                        accessToken = token;
                        selectedModel = model;
                        return "Modello disponibile: " + model;
                    } catch (Exception e) {
                        return "Scambio OAuth non riuscito o permessi insufficienti.";
                    }
                }).thenAccept(message -> {
                    if (client != null) client.execute(() -> {
                        if (pendingLogin == receiver) {
                            status = Text.literal(message);
                            pendingLogin = null;
                        }
                        receiver.close();
                    });
                });
            });
        } catch (Exception e) {
            status = Text.literal("Impossibile avviare l'accesso: controlla il log.");
        }
    }

    private void confirmBuild() {
        if (pendingPlan == null || client == null) { status = Text.literal("Prima genera un progetto."); return; }
        try {
            BuildExecutor.start(client, pendingPlan);
            status = Text.literal("Costruzione avviata.");
            pendingPlan = null;
        } catch (Exception e) { status = Text.literal("Errore: " + e.getMessage()); }
    }

    private void sendMessage() {
        String token = accessToken;
        String model = selectedModel;
        if (token == null || model == null) {
            status = Text.literal("Accedi prima con ChatGPT.");
            return;
        }
        String message = prompt.getText().trim();
        if (message.isEmpty()) return;
        if (message.startsWith("cerca edificio ")) {
            String name = message.substring("cerca edificio ".length()).trim();
            status = Text.literal("Ricerca in corso...");
            CompletableFuture.supplyAsync(() -> {
                try { return ArchitectureResearch.search(name); }
                catch (Exception e) { return "Ricerca non riuscita: " + e.getMessage(); }
            }).thenAccept(result -> {
                if (client != null) client.execute(() -> {
                    lastReply = result;
                    status = Text.literal("Ricerca completata.");
                });
            });
            return;
        }
        if (message.startsWith("riferimento ")) {
            String id = message.substring("riferimento ".length()).trim();
            try {
                var ref = ReferenceCatalog.load(
                    net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir(), id);
                lastReply = "Riferimento: " + ref.name() + ", "
                    + ref.width() + " x " + ref.depth() + " x " + ref.height()
                    + " metri. Fonte: " + ref.source();
                status = Text.literal("Misure locali caricate; ricostruzione non ancora disponibile.");
            } catch (Exception e) {
                status = Text.literal("Riferimento non disponibile o non valido.");
                lastReply = e.getMessage();
            }
            return;
        }
        if (message.startsWith("analizza immagine ")) {
            String filename = message.substring("analizza immagine ".length()).trim();
            if (!filename.matches("[a-zA-Z0-9_-]{1,64}\\.(png|jpg|jpeg)")) {
                status = Text.literal("Nome immagine non valido."); return;
            }
            Path file = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                .resolve("minebuilder").resolve("references").resolve("images").resolve(filename);
            status = Text.literal("Analisi visiva in corso...");
            CompletableFuture.supplyAsync(() -> {
                try {
                    if (!java.nio.file.Files.isRegularFile(file)
                        || java.nio.file.Files.size(file) > 4 * 1024 * 1024)
                        throw new java.io.IOException("Immagine assente o troppo grande");
                    byte[] bytes = java.nio.file.Files.readAllBytes(file);
                    String mime = filename.endsWith(".png") ? "image/png" : "image/jpeg";
                    return ChatGptInference.askWithImage(token, model,
                        "Analyze this architectural photo or floor plan in Italian. "
                        + "Describe visible shapes, materials, openings and dimensions. "
                        + "Separate observed facts from estimates and unknowns. "
                        + "Never invent measurements or claim exact 1:1 fidelity.",
                        bytes, mime);
                } catch (Exception e) { return "Analisi non riuscita: " + e.getMessage(); }
            }).thenAccept(result -> {
                if (client != null) client.execute(() -> {
                    lastReply = result;
                    status = Text.literal("Analisi visiva terminata.");
                });
            });
            return;
        }
        boolean build = message.toLowerCase(java.util.Locale.ROOT).matches("(?s).*(costruisc|edific|crea una cas|build |realizza una struttur|ricostruisc|riproduci).*");
        if (build && (client == null || client.player == null || !client.isInSingleplayer())) {
            status = Text.literal("Solo mondi singleplayer."); return;
        }
        BlockPos origin = build ? client.player.getBlockPos().add(4, 0, 4) : null;
        pendingPlan = null;
        String request = build
            ? "Generate a small Minecraft structure for: " + message
              + ". Return ONLY compact JSON with width (5-15), depth (5-15), height (3-8), wall, roof, floor, trim, roof_style (gable, hip, flat). "
              + "Materials must be valid vanilla full-block IDs like minecraft:oak_planks and minecraft:stone_bricks. Choose coherent materials for the requested architectural style. Never claim a named real landmark is accurate without measured source data; for a named real landmark return width 0 rather than invent dimensions. No block lists or markdown."
            : message;
        status = Text.literal("GPT sta rispondendo...");
        CompletableFuture.supplyAsync(() -> {
            try {
                return ChatGptInference.ask(token, model, request);
            } catch (Exception e) {
                return "Errore GPT: " + e.getMessage();
            }
        }).thenAccept(reply -> {
            if (client != null) client.execute(() -> {
                if (build && !reply.startsWith("Errore GPT:")) {
                    try {
                        String json = reply.trim();
                        if (json.startsWith("```")) {
                            int first = json.indexOf('\n');
                            int last = json.lastIndexOf("```");
                            if (first >= 0 && last > first) json = json.substring(first + 1, last).trim();
                        }
                        pendingPlan = BuildPlan.house(json, origin);
                        lastReply = "Progetto pronto: " + pendingPlan.placements().size() + " blocchi. Premi Costruisci.";
                        status = Text.literal("Progetto pronto, conferma.");
                    } catch (Exception e) {
                        lastReply = "Progetto non valido: " + e.getMessage();
                        status = Text.literal("Progetto non valido.");
                    }
                } else {
                    lastReply = reply;
                    status = Text.literal(reply.startsWith("Errore GPT:") ? "Richiesta non riuscita." : "Risposta GPT ricevuta.");
                }
            });
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 45, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height / 2 + 52, 0xAAAAAA);
        if (!lastReply.isEmpty()) {
            String display = lastReply.length() > 240 ? lastReply.substring(0, 240) + "..." : lastReply;
            int y = height / 2 + 80;
            for (int i = 0; i < display.length(); i += 46) {
                context.drawCenteredTextWithShadow(textRenderer, display.substring(i, Math.min(i + 46, display.length())), width / 2, y, 0xFFFFFF);
                y += 12;
            }
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
