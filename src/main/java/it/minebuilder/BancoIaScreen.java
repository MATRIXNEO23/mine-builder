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
    private static BuildPlan placementPlan;
    private BuildPlan pendingPlan;
    private BuildPlan lastPlan;
    private String selectedBlueprint;
    private int blueprintIndex;

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
        addDrawableChild(ButtonWidget.builder(Text.literal("Salva progetto"), button -> saveBlueprint())
            .dimensions(left, height / 2 + 69, 145, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Elenco progetti"), button -> cycleBlueprint())
            .dimensions(left + 155, height / 2 + 69, 145, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Replica selezionato"), button -> replicateBlueprint())
            .dimensions(left + 155, height / 2 + 94, 145, 20).build());
        setInitialFocus(prompt);
    }

    private void saveBlueprint() {
        if (client == null || lastPlan == null) {
            status = Text.literal("Prima genera un progetto."); return;
        }
        String name = prompt.getText().trim().toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        if (name.isEmpty() || name.length() > 64) {
            status = Text.literal("Inserisci un nome breve nel campo."); return;
        }
        try {
            BlueprintLibrary.save(client, name, lastPlan);
            selectedBlueprint = name;
            status = Text.literal("Progetto salvato: " + name);
        } catch (Exception e) { status = Text.literal("Salvataggio: " + e.getMessage()); }
    }

    private void cycleBlueprint() {
        try {
            var names = BlueprintLibrary.names(client);
            if (names.isEmpty()) { status = Text.literal("Nessun progetto salvato."); return; }
            blueprintIndex = Math.floorMod(blueprintIndex, names.size());
            selectedBlueprint = names.get(blueprintIndex++);
            status = Text.literal("Selezionato: " + selectedBlueprint);
        } catch (Exception e) { status = Text.literal("Elenco: " + e.getMessage()); }
    }

    private void replicateBlueprint() {
        if (client == null || client.player == null || selectedBlueprint == null) {
            status = Text.literal("Seleziona prima un progetto."); return;
        }
        try {
            pendingPlan = BlueprintLibrary.load(client, selectedBlueprint,
                client.player.getBlockPos().add(4, 0, 4));
            status = Text.literal("Replica pronta: premi Costruisci.");
        } catch (Exception e) { status = Text.literal("Replica: " + e.getMessage()); }
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
            placementPlan = pendingPlan;
            pendingPlan = null;
            status = Text.literal("Posizionamento attivo: usa il martello sul blocco desiderato.");
            close();
        } catch (Exception e) { status = Text.literal("Errore: " + e.getMessage()); }
    }

    public static BuildPlan takePlacementPlan() {
        BuildPlan result = placementPlan;
        placementPlan = null;
        return result;
    }

    public static boolean hasPlacementPlan() { return placementPlan != null; }

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
        // The builder is the default mode; explicit chat prefix requests conversation.
        boolean build = !message.toLowerCase(java.util.Locale.ROOT).startsWith("chat:");
        if (build && (client == null || client.player == null || !client.isInSingleplayer())) {
            status = Text.literal("Solo mondi singleplayer."); return;
        }
        BlockPos origin = build ? client.player.getBlockPos().add(4, 0, 4) : null;
        pendingPlan = null;
        String request = build
            ? "Design the requested Minecraft object faithfully: " + message
              + ". You are a universal block architect, NOT a house generator. "
              + "Return ONLY JSON {\"blocks\":[{\"x\":0,\"y\":0,\"z\":0,\"block\":\"minecraft:stone\"}]}. "
              + "You may return arrays blocks, cuboids, shapes. A block is {x,y,z,block}; a cuboid is {x,y,z,w,h,d,block}; a shape is a line {type:line,x,y,z,x2,y2,z2,block} or sphere/ellipsoid/cylinder {type:sphere,x,y,z,rx,ry,rz,block,hollow:false}. "
              + "Use compact primitives for volumes, branches, canopies, curves and details; do not overlap primitives. All coordinates relative to origin and valid Minecraft 1.20.1 block IDs. "
              + "Include every explicitly requested feature and choose geometry appropriate to the subject. "
              + "For trees include trunk branches and leaves; for pools include water and basin; for buildings include openings and interiors. "
              + "Do not substitute a house for another requested object. "
              + "Maximum 20000 blocks; x,z within -128..128, y within -32..128; no duplicate positions. "
              + "If too complex for a single response, return a coherent small section rather than claiming completion. "
              + "No markdown, no explanation."
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
                        pendingPlan = BuildPlan.parse(json, origin);
                        lastPlan = pendingPlan;
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
        String buildError = BuildExecutor.lastError();
        Text visibleStatus = buildError.isEmpty() ? status
            : Text.literal("Costruzione: " + buildError);
        context.drawCenteredTextWithShadow(textRenderer, visibleStatus, width / 2, height / 2 + 121,
            buildError.isEmpty() ? 0xAAAAAA : 0xFF7777);
        if (!lastReply.isEmpty()) {
            String display = lastReply.length() > 240 ? lastReply.substring(0, 240) + "..." : lastReply;
            int y = height / 2 + 145;
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
