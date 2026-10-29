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
    private TextFieldWidget projectName;
    private TextFieldWidget blueprintSearch;
    private java.util.List<String> visibleBlueprints = java.util.List.of();
    private int blueprintPage = 0;
    private boolean savedTab = false;
    private boolean originalDesign = false;
    private static LoopbackRedirect pendingLogin;
    private static volatile String accessToken;
    private static volatile String selectedModel;
    private Text status = Text.literal("ChatGPT non collegato.");
    private String lastReply = "";
    private static volatile String generatorStage = "Inattivo";
    private static volatile int generatorPages = 0;
    private static volatile int generatorBlocks = 0;
    private static volatile boolean generatorRunning = false;
    private static BuildPlan placementPlan;
    private BuildPlan pendingPlan;
    private BuildPlan lastPlan;
    private String selectedBlueprint;
    private int blueprintIndex;
    private static String autoRecoveredWorld;
    private boolean journalRecoveryRunning;

    public BancoIaScreen() {
        super(Text.literal("Banco IA - Mine Builder"));
    }

    @Override
    protected void init() {
        int panel = Math.min(300, Math.max(180, width - 24));
        int left = (width - panel) / 2;
        int half = (panel - 8) / 2;
        int top = 62;
        addDrawableChild(ButtonWidget.builder(Text.literal(savedTab ? "Nuovo progetto" : "Nuovo progetto *"),
            button -> { savedTab = false; clearAndInit(); })
            .dimensions(left, top, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(savedTab ? "Salvati *" : "Salvati"),
            button -> { savedTab = true; clearAndInit(); })
            .dimensions(left + half + 8, top, half, 20).build());
        if (!savedTab) {
            prompt = new TextFieldWidget(textRenderer, left, top + 30, panel, 20, Text.literal("Richiesta"));
            prompt.setMaxLength(512);
            prompt.setPlaceholder(Text.literal("Es. Colosseo 1:1, castello originale..."));
            addDrawableChild(prompt);
            addDrawableChild(ButtonWidget.builder(Text.literal(originalDesign ? "Modalita: Originale" : "Modalita: Riferimenti"),
                button -> {
                    originalDesign = !originalDesign;
                    button.setMessage(Text.literal(originalDesign ? "Modalita: Originale" : "Modalita: Riferimenti"));
                }).dimensions(left, top + 56, panel, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("1. Collega ChatGPT"), button -> startLogin())
                .dimensions(left, top + 84, panel, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("2. Progetta"), button -> sendMessage())
                .dimensions(left, top + 110, half, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("3. Costruisci"), button -> confirmBuild())
                .dimensions(left + half + 8, top + 110, half, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Recupera diario GPT"), button -> recoverJournal())
                .dimensions(left, top + 188, panel, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Riprendi costruzione"), button -> {
                try {
                    BuildExecutor.resume(client);
                    status = Text.literal("Costruzione ripresa: sezioni " + BuildExecutor.sectionProgress());
                    close();
                } catch (Exception e) {
                    status = Text.literal("Ripresa non riuscita: " + e.getMessage());
                }
            }).dimensions(left, top + 162, panel, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Dettagli e controlli"), button -> openDetails())
                .dimensions(left, top + 136, panel, 20).build());
        } else {
            projectName = new TextFieldWidget(textRenderer, left, top + 30, panel, 20, Text.literal("Nome progetto"));
            projectName.setMaxLength(64);
            projectName.setPlaceholder(Text.literal("Nome progetto da salvare..."));
            addDrawableChild(projectName);
            addDrawableChild(ButtonWidget.builder(Text.literal("Salva progetto"), button -> saveBlueprint())
                .dimensions(left, top + 56, half, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Seleziona salvato"), button -> cycleBlueprint())
                .dimensions(left + half + 8, top + 56, half, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Replica selezionato"), button -> replicateBlueprint())
                .dimensions(left, top + 82, panel, 20).build());
            blueprintSearch = new TextFieldWidget(textRenderer, left, top + 110, panel, 20, Text.literal("Cerca progetto"));
            blueprintSearch.setMaxLength(64);
            blueprintSearch.setPlaceholder(Text.literal("Filtra progetti..."));
            addDrawableChild(blueprintSearch);
            addDrawableChild(ButtonWidget.builder(Text.literal("Precedenti"), button -> showBlueprintPage(-1))
                .dimensions(left, top + 136, half, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Successivi"), button -> showBlueprintPage(1))
                .dimensions(left + half + 8, top + 136, half, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Chiudi"), button -> close())
            .dimensions(left, height - 26, panel, 20).build());
        refreshBlueprints();
        if (prompt != null && !savedTab) setInitialFocus(prompt);
        autoRecoverJournal();
    }

    private void saveBlueprint() {
        if (client == null || lastPlan == null) {
            status = Text.literal("Prima genera un progetto."); return;
        }
        String name = projectName.getText().trim().toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        if (name.isEmpty() || name.length() > 64) {
            status = Text.literal("Inserisci un nome nel campo Nome progetto."); return;
        }
        try {
            if (BlueprintLibrary.names(client).contains(name)) {
                status = Text.literal("Nome gia presente: scegline un altro per non sovrascrivere."); return;
            }
            BlueprintLibrary.save(client, name, lastPlan);
            selectedBlueprint = name;
            refreshBlueprints();
            status = Text.literal("Progetto salvato: " + name);
        } catch (Exception e) { status = Text.literal("Salvataggio: " + e.getMessage()); }
    }

    private void refreshBlueprints() {
        try { visibleBlueprints = BlueprintLibrary.names(client); }
        catch (Exception e) { visibleBlueprints = java.util.List.of(); }
    }

    private void showBlueprintPage(int direction) {
        refreshBlueprints();
        String filter = blueprintSearch.getText().trim().toLowerCase(java.util.Locale.ROOT);
        long count = visibleBlueprints.stream().filter(n -> n.contains(filter)).count();
        int pages = Math.max(1, ((int)count + 5) / 6);
        blueprintPage = Math.floorMod(blueprintPage + direction, pages);
        status = Text.literal("Pagina progetti " + (blueprintPage + 1) + "/" + pages);
    }

    private void cycleBlueprint() {
        try {
            var names = BlueprintLibrary.names(client);
            if (names.isEmpty()) { status = Text.literal("Nessun progetto salvato."); return; }
            blueprintIndex = Math.floorMod(blueprintIndex, names.size());
            selectedBlueprint = names.get(blueprintIndex++);
            projectName.setText(selectedBlueprint);
            status = Text.literal("Selezionato: " + selectedBlueprint + " (" + names.size() + " progetti)");
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
                            if (accessToken != null && selectedModel != null) autoRecoverJournal();
                        }
                        receiver.close();
                    });
                });
            });
        } catch (Exception e) {
            status = Text.literal("Impossibile avviare l'accesso: controlla il log.");
        }
    }

    private void autoRecoverJournal() {
        if (client == null || client.getServer() == null || accessToken == null
            || selectedModel == null || journalRecoveryRunning) return;
        String world = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
            .toAbsolutePath().normalize().toString();
        if (world.equals(autoRecoveredWorld)) return;
        autoRecoveredWorld = world;
        recoverJournal();
    }
    private void recoverJournal() {
        if (client == null || client.player == null || !client.isInSingleplayer()) {
            status = Text.literal("Recupero disponibile solo in singleplayer."); return;
        }
        try {
            Path root = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                .resolve("minebuilder").resolve("generation-journal");
            if (!java.nio.file.Files.isDirectory(root)) throw new java.io.IOException("Nessun diario");
            java.util.List<Path> jobs;
            try (var stream = java.nio.file.Files.list(root)) {
                jobs = stream.filter(java.nio.file.Files::isDirectory)
                    .sorted(java.util.Comparator.reverseOrder()).toList();
            }
            for (Path job : jobs) {
                Path originFile = job.resolve("origin.txt");
                if (!java.nio.file.Files.isRegularFile(originFile)) continue;
                String[] xyz = java.nio.file.Files.readString(originFile).trim().split(",");
                if (xyz.length != 3) continue;
                BlockPos origin = new BlockPos(Integer.parseInt(xyz[0]),
                    Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                java.util.List<Path> files;
                try (var stream = java.nio.file.Files.list(job)) {
                    files = stream.filter(p -> p.getFileName().toString().matches("[0-9]{4}\\.json"))
                        .sorted().toList();
                }
                if (files.isEmpty()) continue;
                java.util.Map<BlockPos, net.minecraft.block.BlockState> cells = new java.util.LinkedHashMap<>();
                boolean complete = false;
                for (int i = 0; i < files.size(); i++) {
                    if (!files.get(i).getFileName().toString().equals(
                            String.format(java.util.Locale.ROOT, "%04d.json", i)))
                        throw new java.io.IOException("Pagine mancanti nel diario");
                    String json = java.nio.file.Files.readString(files.get(i));
                    var object = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                    if (!object.has("complete")) throw new java.io.IOException("Pagina senza stato");
                    if (complete) throw new java.io.IOException("Pagine successive al completamento");
                    BuildPlan page = BuildPlan.parse(json, origin);
                    for (var placement : page.placements()) {
                        var old = cells.putIfAbsent(placement.pos(), placement.state());
                        if (old != null && !old.equals(placement.state()))
                            throw new java.io.IOException("Conflitto geometrico nel diario");
                    }
                    complete = object.get("complete").getAsBoolean();
                }
                if (!complete) {
                    continueJournal(job, origin, files.size(), cells);
                    return;
                }
                pendingPlan = BuildPlan.fromPlacements(cells.entrySet().stream()
                    .map(e -> new BuildPlan.Placement(e.getKey(), e.getValue())).toList());
                pendingPlan.validateSections();
                lastPlan = pendingPlan;
                status = Text.literal("Diario recuperato: premi Costruisci.");
                return;
            }
            status = Text.literal("Nessun diario recuperabile.");
        } catch (Exception e) {
            status = Text.literal("Recupero diario: " + e.getMessage());
        }
    }
    private void continueJournal(Path job, BlockPos origin, int startPage,
                                 java.util.Map<BlockPos, net.minecraft.block.BlockState> existing) {
        String token = accessToken, model = selectedModel;
        if (token == null || model == null) {
            status = Text.literal("Ricollega ChatGPT per continuare il diario."); return;
        }
        status = Text.literal("GPT riprende dalla pagina " + (startPage + 1));
        CompletableFuture.supplyAsync(() -> {
            try {
                String design = java.nio.file.Files.readString(job.resolve("design.txt"));
                String request = java.nio.file.Files.readString(job.resolve("request.txt"));
                var occupied = new java.util.LinkedHashMap<>(existing);
                String previous = "";
                boolean complete = false;
                for (int page = startPage; page < 100; page++) {
                    String instruction = design + " Original request: " + request
                        + ". Continue page " + (page + 1)
                        + ". Preserve the original coordinates and design. Do not repeat any"
                        + " previous blocks. Existing blocks count: " + occupied.size()
                        + ". Previous page summary: " + previous
                        + ". Return JSON geometry and mandatory boolean complete.";
                    String answer = ChatGptInference.ask(token, model, instruction).trim();
                    if (answer.startsWith("```")) {
                        int first = answer.indexOf((char)10), last = answer.lastIndexOf("```");
                        if (first >= 0 && last > first) answer = answer.substring(first + 1, last).trim();
                    }
                    var object = com.google.gson.JsonParser.parseString(answer).getAsJsonObject();
                    if (!object.has("complete") || !object.get("complete").getAsJsonPrimitive().isBoolean())
                        throw new java.io.IOException("GPT non ha dichiarato lo stato della pagina");
                    BuildPlan parsed = BuildPlan.parse(answer, origin);
                    for (var placement : parsed.placements()) {
                        var previousState = occupied.get(placement.pos());
                        if (previousState != null && !previousState.equals(placement.state()))
                            throw new java.io.IOException("Conflitto con pagina salvata: " + placement.pos());
                    }
                    if (occupied.size() + parsed.placements().size() > 2_000_000)
                        throw new java.io.IOException("Limite blocchi raggiunto");
                    Path tmp = java.nio.file.Files.createTempFile(job, "page-", ".tmp");
                    try {
                        java.nio.file.Files.writeString(tmp, answer);
                        java.nio.file.Files.move(tmp, job.resolve(
                            String.format(java.util.Locale.ROOT, "%04d.json", page)),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } finally { java.nio.file.Files.deleteIfExists(tmp); }
                    for (var placement : parsed.placements())
                        occupied.putIfAbsent(placement.pos(), placement.state());
                    previous = answer.substring(0, Math.min(350, answer.length()));
                    complete = object.get("complete").getAsBoolean();
                    if (complete) break;
                }
                if (!complete) throw new java.io.IOException("Generazione sospesa: limite di 100 pagine");
                BuildPlan result = BuildPlan.fromPlacements(occupied.entrySet().stream()
                    .map(e -> new BuildPlan.Placement(e.getKey(), e.getValue())).toList());
                result.validateSections();
                return result;
            } catch (Exception e) { throw new java.util.concurrent.CompletionException(e); }
        }).whenComplete((plan, error) -> {
            if (client != null) client.execute(() -> {
                if (error != null) status = Text.literal("Ripresa GPT: " + error.getCause().getMessage());
                else { pendingPlan = plan; lastPlan = plan;
                    status = Text.literal("Generazione ripresa e completata. Premi Costruisci."); }
            });
        });
    }
    private void confirmBuild() {
        if (pendingPlan == null || client == null) { status = Text.literal("Prima genera un progetto."); return; }
        try {
            placementPlan = pendingPlan;
            pendingPlan = null;
            status = Text.literal("Martello: accovacciati e clicca per ruotare; clic normale per costruire.");
            close();
        } catch (Exception e) { status = Text.literal("Errore: " + e.getMessage()); }
    }

    public static BuildPlan peekPlacementPlan() { return placementPlan; }

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
        String message = prompt == null ? "" : prompt.getText().trim();
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
        final boolean useReferences = !originalDesign;
        String request = build
            ? "Design the requested Minecraft object faithfully: " + message
              + ". You are a universal block architect, NOT a house generator. "
              + "Return ONLY JSON {\"blocks\":[{\"x\":0,\"y\":0,\"z\":0,\"block\":\"minecraft:stone\"}]}. "
              + "You may return arrays blocks, cuboids, shapes. A block is {x,y,z,block}; a cuboid is {x,y,z,w,h,d,block}; a shape is a line {type:line,x,y,z,x2,y2,z2,block}, sphere/ellipsoid/cylinder {type:sphere,x,y,z,rx,ry,rz,block,hollow:false}, or elliptic_ring {type:elliptic_ring,x,y,z,rx,rz,thickness,height,block}. Elliptic rings are centred at x/z and start at y; use them for amphitheatres, stadiums, rotundas and original oval architecture. "
              + "Use compact primitives for volumes and shapes; avoid overlapping primitives with different materials. Explicit blocks are a final detail layer and override primitive geometry for doors, windows, openings, stairs, roof trim and decorations. Do not give two different explicit blocks at the same coordinate. All coordinates relative to origin and valid Minecraft 1.20.1 block IDs. "
              + "Use the full Minecraft 1.20.1 palette: stairs, slabs, walls, fences, panes, trapdoors, doors, logs, terracotta, concrete and decorative blocks to reproduce silhouettes, texture, shading, fine trim, arches, roofs and realistic visual effects. "
              + "For directional or partial blocks encode exact block states as block strings, e.g. minecraft:stone_brick_stairs[facing=north,half=bottom], minecraft:stone_brick_slab[type=top], minecraft:oak_log[axis=x]. "
              + "Choose valid state values for each block; connect adjoining elements visually, and ensure stair and door orientations make sense. Prefer nuanced architectural details over featureless full-block walls. "
              + "Include every explicitly requested feature and choose geometry appropriate to the subject. "
              + "For trees include trunk branches and leaves; for pools include water and basin; for buildings include openings and interiors. "
              + "For multi-storey buildings provide a walkable INTERNAL staircase or ladder linking every floor, with clear floor openings and access from the entrance; never require exterior access to upper floors. "
              + "Use minecraft:air blocks explicitly to clear doorways, stairs and passageways. "
              + "Descriptive adjectives such as scary, gothic, enormous or haunted describe style and must not change the requested object category. "
              + "Do not substitute a house for another requested object. "
              + "Maximum 20000 blocks; x,z within -128..128, y within -32..128; avoid conflicting materials at the same position. "
              + "If too complex for a single response, return a coherent small section rather than claiming completion. "
              + "No markdown, no explanation."
            : message;
        status = Text.literal(build ? (useReferences ? "Ricerca architettura e planimetrie..." : "Progettazione originale senza ricerca esterna...") : "GPT sta rispondendo...");
        generatorRunning = build;
        generatorPages = 0;
        generatorBlocks = 0;
        generatorStage = build ? (useReferences ? "Modalita riferimenti: ricerca fonti" : "Modalita originale: progetto creativo") : "Chat";
        CompletableFuture.supplyAsync(() -> {
            try {
                String enriched = request;
                if (build && useReferences) {
                    String references;
                    try {
                        references = ArchitectureResearch.planningReferences(message);
                    } catch (Exception e) {
                        references = "Reference discovery unavailable. No verified measurements.";
                    }
                    enriched += " Planning references (metadata, not visually inspected): "
                        + references + " Use only supported facts; do not invent measured dimensions. "
                        + "Reason about structural layout before emitting the final JSON. For amphitheatres such as the Colosseum, distinguish the elliptical footprint, concentric rings, arena, tiered seating, repeated arches, circulation and entrances. Use rings for massing and explicit detail blocks or air for openings; do not claim 1:1 accuracy without verified dimensions. This example is not a mandatory template for other requests.";
                }
                if (build && !originalDesign) {
                    generatorStage = "Analisi immagini e planimetrie";
                    try {
                        ArchitectureResearch.VisualReference visual = ArchitectureResearch.visualReference(message);
                        if (visual != null) {
                            String analysis = ChatGptInference.askWithImage(token, model,
                                "Analyze this Wikimedia architectural reference image: " + visual.title()
                                + ". In Italian, identify only VISIBLE geometry, proportions, openings, "
                                + "architectural details and possible floor-plan layout. "
                                + "Separate observations from estimates. Do not invent measurements. "
                                + "Return a concise technical description, not building JSON.",
                                visual.image(), visual.mime());
                            enriched += " Actual visual analysis of reference (" + visual.title()
                                + "): " + analysis.substring(0, Math.min(4500, analysis.length()))
                                + ". This is an example, not proof of the requested building's dimensions.";
                        }
                    } catch (Exception ignored) {
                        enriched += " Visual analysis unavailable; do not claim reference images were inspected.";
                    }
                }
                if (build) {
                    generatorStage = "Progettazione architettonica";
                    String architecturalBrief = ChatGptInference.ask(token, model,
                        "Act as an architectural designer creating an ORIGINAL, coherent Minecraft "
                        + "structure. User request: " + message + ". "
                        + "First establish the building category, function, approximate dimensions "
                        + "in metres (1 Minecraft block = 1 metre), footprint and floor plan, "
                        + "structural system, symmetry or asymmetry, circulation, stairs, "
                        + "entrances, windows, roof, facade rhythm, materials and distinctive details. "
                        + "Ensure every level is accessible and volumes are plausible. "
                        + "Use historical examples only as inspiration, never copy an unverified plan. "
                        + "If the user explicitly requests a real 1:1 building, distinguish "
                        + "verified dimensions from unknowns and do not claim exact fidelity. "
                        + "Do not produce block coordinates or JSON. "
                        + "Return a concise technical architectural design brief in Italian. "
                        + "Available research context: "
                        + enriched.substring(0, Math.min(6500, enriched.length())));
                    enriched += " MANDATORY PRELIMINARY ARCHITECTURAL DESIGN (follow its spatial "
                        + "organization, dimensions, circulation and details when generating JSON): "
                        + architecturalBrief.substring(0, Math.min(6000, architecturalBrief.length()));
                }
                if (!build) return ChatGptInference.ask(token, model, enriched);
                // Persist each GPT page before requesting the next one.
                Path journal = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve("minebuilder").resolve("generation-journal");
                java.nio.file.Files.createDirectories(journal);
                Path jobDir = journal.resolve(java.util.UUID.randomUUID().toString());
                java.nio.file.Files.createDirectory(jobDir);
                java.nio.file.Files.writeString(jobDir.resolve("request.txt"), message);
                java.nio.file.Files.writeString(jobDir.resolve("origin.txt"),
                    origin.getX() + "," + origin.getY() + "," + origin.getZ());
                java.nio.file.Files.writeString(jobDir.resolve("design.txt"),
                    enriched.substring(0, Math.min(enriched.length(), 12000)));
                java.util.List<String> pages = new java.util.ArrayList<>();
                java.util.List<BuildPlan.Placement> validated = new java.util.ArrayList<>();
                java.util.Map<BlockPos, net.minecraft.block.BlockState> occupied = new java.util.HashMap<>();
                String previous = "";
                for (int page = 0; page < 100; page++) {
                    generatorStage = "Generazione geometria, pagina " + (page + 1);
                    String instruction = enriched + " This is generation page " + (page + 1)
                        + " of a progressive architectural project. Keep the SAME architectural blueprint and coordinate origin."
                        + " Never repeat blocks from previous pages. Previously generated summary: "
                        + previous.substring(0, Math.min(2500, previous.length()))
                        + " Return only JSON geometry plus mandatory boolean field complete."
                        + " Set complete=true ONLY if the entire requested building is fully represented.";
                    String answer = ChatGptInference.ask(token, model, instruction).trim();
                    if (answer.startsWith("```")) {
                        int first = answer.indexOf((char)10), last = answer.lastIndexOf("```");
                        if (first >= 0 && last > first) answer = answer.substring(first + 1, last).trim();
                    }
                    com.google.gson.JsonObject object = com.google.gson.JsonParser.parseString(answer).getAsJsonObject();
                    if (!object.has("complete") || !object.get("complete").getAsJsonPrimitive().isBoolean())
                        throw new IllegalStateException("Risposta GPT senza indicatore di completamento");
                    Path tmpPage = java.nio.file.Files.createTempFile(jobDir, "page-", ".tmp");
                    try {
                        java.nio.file.Files.writeString(tmpPage, answer);
                        java.nio.file.Files.move(tmpPage,
                            jobDir.resolve(String.format(java.util.Locale.ROOT, "%04d.json", page)),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    } finally {
                        java.nio.file.Files.deleteIfExists(tmpPage);
                    }
                    BuildPlan parsedPage = BuildPlan.parse(answer, origin);
                    for (var placement : parsedPage.placements()) {
                        var existing = occupied.putIfAbsent(placement.pos(), placement.state());
                        if (existing != null && !existing.equals(placement.state()))
                            throw new IllegalStateException("Conflitto geometrico tra pagine: " + placement.pos());
                        if (existing == null) validated.add(placement);
                    }
                    if (validated.size() > 2_000_000)
                        throw new IllegalStateException("Limite di sicurezza del progetto superato");
                    pages.add(answer);
                    generatorPages = pages.size();
                    generatorBlocks = validated.size();
                    generatorStage = "Pagina " + pages.size() + " validata e salvata";
                    if (object.get("complete").getAsBoolean()) return "PAGED:" + String.join("##PAGE##", pages);
                    previous += " Page " + (page + 1) + ": " + answer.substring(0, Math.min(350, answer.length()));
                }
                throw new IllegalStateException("Progetto oltre 100 pagine: generazione sospesa in sicurezza");
            } catch (Exception e) {
                return "Errore GPT: " + e.getMessage();
            }
        }).thenAccept(reply -> {
            generatorRunning = false;
            generatorStage = reply.startsWith("Errore GPT:") ? "Interrotto: verifica dettagli" : "Generazione conclusa";
            if (client != null) client.execute(() -> {
                if (build && !reply.startsWith("Errore GPT:")) {
                    try {
                        String json = reply.trim();
                        if (json.startsWith("```")) {
                            int first = json.indexOf('\n');
                            int last = json.lastIndexOf("```");
                            if (first >= 0 && last > first) json = json.substring(first + 1, last).trim();
                        }
                        // Accept a JSON object wrapped in explanatory text, without executing commands.
                        if (!json.startsWith("{") && !json.startsWith("PAGED:")) {
                            int start = json.indexOf('{');
                            int end = json.lastIndexOf('}');
                            if (start >= 0 && end > start) json = json.substring(start, end + 1);
                        }
                        if (json.startsWith("PAGED:")) {
                            java.util.List<BuildPlan.Placement> combined = new java.util.ArrayList<>();
                            java.util.Map<BlockPos, net.minecraft.block.BlockState> occupied = new java.util.HashMap<>();
                            for (String page : json.substring(6).split("##PAGE##")) {
                                BuildPlan part = BuildPlan.parse(page, origin);
                                for (var placement : part.placements()) {
                                    var existing = occupied.putIfAbsent(placement.pos(), placement.state());
                                    if (existing != null && !existing.equals(placement.state()))
                                        throw new IllegalArgumentException("Sezioni sovrapposte: " + placement.pos());
                                    if (existing == null) combined.add(placement);
                                }
                                if (combined.size() > 2_000_000)
                                    throw new IllegalArgumentException("Progetto oltre 2 milioni di blocchi");
                            }
                            pendingPlan = BuildPlan.fromPlacements(combined);
                        } else pendingPlan = BuildPlan.parse(json, origin);
                        pendingPlan.validateSections();
                        lastPlan = pendingPlan;
                        lastReply = "Progetto pronto: " + pendingPlan.placements().size() + " blocchi. "
                            + " Sezioni 16x16 validate: " + pendingPlan.sections().size() + ". "
                            + pendingPlan.geometryReport() + " " + pendingPlan.architectureWarnings()
                            + " Premi Costruisci.";
                        status = Text.literal("Progetto pronto, conferma.");
                    } catch (Exception e) {
                        lastReply = "Progetto non valido (" + e.getClass().getSimpleName() + "): "
                            + e.getMessage() + ". Controlla la geometria e il JSON; usa Dettagli e controlli per copiare il messaggio.";
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
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        String buildError = BuildExecutor.lastError();
        if (generatorRunning || generatorPages > 0) {
            String activity = "Generatore: " + generatorStage + " | pagine " + generatorPages
                + " | blocchi " + generatorBlocks;
            context.drawCenteredTextWithShadow(textRenderer,
                textRenderer.trimToWidth(activity, Math.max(100, width - 24)),
                width / 2, 46, 0xBBDDFF);
        }
        String summary = !buildError.isEmpty() ? "Costruzione: " + buildError : status.getString();
        String shortSummary = textRenderer.trimToWidth(summary, Math.max(100, width - 24));
        context.drawCenteredTextWithShadow(textRenderer, shortSummary, width / 2, 32,
            buildError.isEmpty() ? 0xAAAAAA : 0xFF7777);
        if (blueprintSearch != null) {
            String filter = blueprintSearch.getText().trim().toLowerCase(java.util.Locale.ROOT);
            var filtered = visibleBlueprints.stream().filter(n -> n.contains(filter)).toList();
            int pages = Math.max(1, (filtered.size() + 5) / 6);
            int page = Math.floorMod(blueprintPage, pages);
            for (int i = 0; i < 6 && page * 6 + i < filtered.size(); i++) {
                String name = filtered.get(page * 6 + i);
                context.drawTextWithShadow(textRenderer, (name.equals(selectedBlueprint) ? "> " : "  ") + name,
                    width / 2 - 145, 232 + i * 12, 0xFFFFFF);
            }
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void openDetails() {
        if (client == null) return;
        String error = BuildExecutor.lastError();
        String details = "Stato: " + status.getString() + "\\n\\n"
            + (error.isEmpty() ? "" : "Errore costruzione: " + error + "\\n\\n")
            + (lastReply.isEmpty() ? "Nessuna risposta ancora." : lastReply);
        client.setScreen(new DetailsScreen(this, details));
    }

    private static final class DetailsScreen extends Screen {
        private final Screen previous;
        private final String details;
        private int page;
        private DetailsScreen(Screen previous, String details) {
            super(Text.literal("Dettagli progetto / errori"));
            this.previous = previous;
            this.details = details;
        }
        @Override protected void init() {
            int y = height - 28;
            addDrawableChild(ButtonWidget.builder(Text.literal("Indietro"), b -> close())
                .dimensions(width / 2 - 155, y, 100, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Copia testo"), b -> {
                if (client != null) client.keyboard.setClipboard(details);
            }).dimensions(width / 2 - 50, y, 100, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Pagina +"), b -> page++)
                .dimensions(width / 2 + 55, y, 100, 20).build());
        }
        @Override public void close() {
            if (client != null) client.setScreen(previous);
        }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
            java.util.List<net.minecraft.text.OrderedText> lines = new java.util.ArrayList<>();
            for (String paragraph : details.split("\\n", -1)) {
                if (paragraph.isEmpty()) lines.add(net.minecraft.text.Text.literal(" ").asOrderedText());
                else lines.addAll(textRenderer.wrapLines(Text.literal(paragraph), Math.max(100, width - 32)));
            }
            int perPage = Math.max(1, (height - 72) / 12);
            int pages = Math.max(1, (lines.size() + perPage - 1) / perPage);
            page = Math.floorMod(page, pages);
            for (int i = 0; i < perPage && page * perPage + i < lines.size(); i++) {
                context.drawTextWithShadow(textRenderer, lines.get(page * perPage + i),
                    16, 32 + i * 12, 0xFFFFFF);
            }
            context.drawCenteredTextWithShadow(textRenderer,
                "Pagina " + (page + 1) + "/" + pages, width / 2, height - 42, 0xAAAAAA);
            super.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
