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
    private static LoopbackRedirect pendingLogin;
    private static volatile String accessToken;
    private static volatile String selectedModel;
    private Text status = Text.literal("ChatGPT non collegato.");
    private String lastReply = "";
    private static volatile String generatorStage = "Inattivo";
    private static volatile int generatorPages = 0;
    private static volatile int generatorBlocks = 0;
    private static volatile boolean generatorRunning = false;
    private static final java.util.concurrent.ExecutorService consoleWriter = java.util.concurrent.Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "minebuilder-console-log");
        thread.setDaemon(true);
        return thread;
    });
    private static final java.util.List<String> consoleEvents = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static volatile String consoleLastStage = "";
    private static volatile long consoleStageStarted = System.currentTimeMillis();
    private static void logConsole(String message) {
        String line = java.time.LocalTime.now().withNano(0) + " | " + message;
        consoleEvents.add(line);
        while (consoleEvents.size() > 200) consoleEvents.remove(0);
        consoleWriter.execute(() -> {
            try {
                Path folder = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("minebuilder");
                java.nio.file.Files.createDirectories(folder);
                java.nio.file.Files.writeString(folder.resolve("generation-console.log"), line + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception ignored) { /* Console remains usable if disk logging fails. */ }
        });
    }
    private static void pollConsoleStage() {
        String stage = generatorStage + (generatorRunning ? " [in corso]" : " [terminato]");
        if (!stage.equals(consoleLastStage)) {
            consoleLastStage = stage;
            consoleStageStarted = System.currentTimeMillis();
            logConsole(stage);
        }
    }
    private static BuildPlan placementPlan;
    private BuildPlan pendingPlan;
    private BuildPlan lastPlan;
    private String selectedBlueprint;
    private int blueprintIndex;
    private static String autoRecoveredWorld;
    private boolean journalRecoveryRunning;

    private record GenerationOutcome(String reply, BuildPlan plan) {}

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
        addDrawableChild(ButtonWidget.builder(Text.literal(savedTab ? "Salvati *" : "Salvati / Salva progetto"),
            button -> { savedTab = true; clearAndInit(); })
            .dimensions(left + half + 8, top, half, 20).build());
        if (!savedTab) {
            prompt = new TextFieldWidget(textRenderer, left, top + 30, panel, 20, Text.literal("Richiesta"));
            prompt.setMaxLength(512);
            prompt.setPlaceholder(Text.literal("Es. Colosseo 1:1, castello originale..."));
            addDrawableChild(prompt);
            addDrawableChild(ButtonWidget.builder(Text.literal("Ricerca: automatica (GPT)"),
                button -> status = Text.literal("GPT sceglie quando cercare riferimenti o ispirazione."))
                .dimensions(left, top + 56, panel, 20).build());
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
            addDrawableChild(ButtonWidget.builder(Text.literal("Salva progetto (dopo progettazione)"), button -> saveBlueprint())
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
        addDrawableChild(ButtonWidget.builder(Text.literal("Console LIVE / Registro"), button -> {
            if (client != null) client.setScreen(new ConsoleScreen(this));
        }).dimensions(left, height - 52, panel, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Chiudi"), button -> close())
            .dimensions(left, height - 26, panel, 20).build());
        refreshBlueprints();
        if (prompt != null && !savedTab) setInitialFocus(prompt);
        recoverProgressive();
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

    private void recoverProgressive() {
        if (client == null || client.getServer() == null || client.world == null) return;
        try {
            Path root = client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                .resolve("minebuilder/generation-journal");
            if (!java.nio.file.Files.isDirectory(root)) return;
            try (var jobs = java.nio.file.Files.list(root)) {
                for (Path directory : jobs.filter(java.nio.file.Files::isDirectory).toList()) {
                    if (java.nio.file.Files.isRegularFile(directory.resolve("approved.txt"))) {
                        ProgressiveBuild.recover(client, directory);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            status = Text.literal("Recupero progressivo: " + e.getMessage());
        }
    }

    private void autoRecoverJournal() {
        if (client == null || client.getServer() == null || accessToken == null
            || selectedModel == null || journalRecoveryRunning || ProgressiveBuild.active()) return;
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
                // New immutable composite journal takes precedence over legacy paged recovery.
                if (java.nio.file.Files.isRegularFile(job.resolve("component-manifest.json"))) {
                    resumeCompositeJournal(job, origin);
                    return;
                }
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
    private void resumeCompositeJournal(Path job, BlockPos origin) {
        if (journalRecoveryRunning || generatorRunning) {
            status = Text.literal("Recupero o generazione già in corso.");
            return;
        }
        journalRecoveryRunning = true;
        generatorRunning = true;
        generatorStage = "Recupero progetto a componenti";
        logConsole("Recupero componenti da " + job.getFileName());
        final String token = accessToken, model = selectedModel;
        CompletableFuture.supplyAsync(() -> {
            try {
                if (token == null || model == null)
                    return ComponentGeometryGenerator.loadSaved(job, origin);
                String request = java.nio.file.Files.readString(job.resolve("request.txt"));
                String masterPlan = java.nio.file.Files.readString(job.resolve("master-plan.txt"));
                DesignContract.ResearchMode mode = DesignContract.ResearchMode.valueOf(
                    java.nio.file.Files.readString(job.resolve("research-mode.txt")).trim());
                DesignContract design = new DesignContract(request, masterPlan, mode);
                return ComponentGeometryGenerator.generate(token, model, design, origin,
                    stage -> { generatorStage = stage; logConsole(stage); },
                    GenerationJournal.reopen(job)).blueprint();
            } catch (Exception ex) {
                throw new java.util.concurrent.CompletionException(ex);
            }
        }).whenComplete((plan, error) -> {
            generatorRunning = false;
            journalRecoveryRunning = false;
            if (client != null) client.execute(() -> {
                if (error != null) {
                    Throwable failure = error.getCause() == null ? error : error.getCause();
                    status = Text.literal("Recupero componenti: " + failure.getMessage());
                    logConsole("ERRORE recupero componenti: " + failure.getMessage());
                } else {
                    pendingPlan = plan;
                    lastPlan = plan;
                    status = Text.literal("Progetto recuperato: verifica e premi Costruisci.");
                    logConsole("Progetto a componenti recuperato: " + plan.placements().size() + " blocchi");
                }
            });
        });
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
                Path masterFile = job.resolve("master-plan.txt");
                if (java.nio.file.Files.isRegularFile(masterFile))
                    design += " GLOBAL MASTER PLAN: " + java.nio.file.Files.readString(masterFile);
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
                    String answer = "";
                    BuildPlan parsedPage = null;
                    String conflictFeedback = "";
                    for (int attempt = 0; attempt < 4; attempt++) {
                        String adjusted = instruction + conflictFeedback + (attempt == 0 ? "" :
                            " RETRY: previous page was rejected as too large or invalid. "
                            + "Produce at most 5000 DISTINCT block positions, including expanded "
                            + "cuboids and shapes. Reduce dimensions or split the work across "
                            + "additional pages. Return valid JSON only.");
                        answer = ChatGptInference.ask(token, model, adjusted).trim();
                        if (answer.startsWith("```")) {
                            int first = answer.indexOf((char)10), last = answer.lastIndexOf("```");
                            if (first >= 0 && last > first) answer = answer.substring(first + 1, last).trim();
                        }
                        try {
                            parsedPage = BuildPlan.parse(answer, origin);
                            java.util.List<String> conflicts = new java.util.ArrayList<>();
                            for (var placement : parsedPage.placements()) {
                                var prior = occupied.get(placement.pos());
                                if (prior != null && !prior.equals(placement.state())
                                    && conflicts.size() < 16)
                                    conflicts.add(placement.pos().toShortString());
                            }
                            if (!conflicts.isEmpty()) {
                                conflictFeedback = " CONFLICT WITH SAVED PAGES at world coordinates "
                                    + String.join("; ", conflicts)
                                    + ". Preserve previous pages. Generate ONLY unoccupied geometry.";
                                if (attempt == 3) throw new IllegalArgumentException(
                                    "Conflitti tra pagine dopo 4 tentativi: " + String.join("; ", conflicts));
                                generatorStage = "Correzione conflitti pagina " + (page + 1);
                                continue;
                            }
                            break;
                        } catch (IllegalArgumentException ex) {
                            if (attempt == 3) throw ex;
                            generatorStage = "Pagina " + (page + 1) + ": riduzione geometria, tentativo " + (attempt + 2);
                        }
                    }
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
        if (generatorRunning) {
            status = Text.literal("Attendi: progetto completo e sezioni in verifica.");
            return;
        }
        if (pendingPlan == null || client == null) { status = Text.literal("Prima genera un progetto."); return; }
        try {
            BuildExecutor.validateHeight(client, pendingPlan);
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
        if (generatorRunning || journalRecoveryRunning) {
            status = Text.literal("Una generazione o un recupero è già in corso.");
            return;
        }
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
        // The blueprint parser allows relative Y down to -32. On superflat worlds
        // the player's feet can be at Y=-60: anchoring there would place blocks
        // below the Minecraft build limit (-64). Reserve the full negative range.
        BlockPos selectedOrigin = null;
        if (build) {
            BlockPos playerPos = client.player.getBlockPos().add(4, 0, 4);
            int minOriginY = client.world.getBottomY() + 32;
            int maxOriginY = client.world.getTopY() - 129;
            if (minOriginY > maxOriginY) {
                status = Text.literal("Altezza mondo insufficiente per il progetto.");
                return;
            }
            selectedOrigin = new BlockPos(playerPos.getX(),
                Math.max(minOriginY, Math.min(playerPos.getY(), maxOriginY)),
                playerPos.getZ());
        }
        final BlockPos origin = selectedOrigin;
        pendingPlan = null;
        // The original user request is canonical; geometry instructions live in GeometryGenerator.
        String request = message;
        status = Text.literal(build ? "GPT interpreta e progetta..." : "GPT sta rispondendo...");
        generatorRunning = build;
        generatorPages = 0;
        generatorBlocks = 0;
        generatorStage = build ? "Progettazione autonoma" : "Chat";
        logConsole("Nuova richiesta: " + message);
        CompletableFuture.supplyAsync(() -> {
            try {
                if (!build) return new GenerationOutcome(ChatGptInference.ask(token, model, request), null);
                DesignContract design = ArchitecturalPlanner.plan(token, model, message, stage -> {
                    generatorStage = stage;
                    logConsole(stage);
                });
                String masterPlan = design.masterPlan();
                String enriched = design.geometryInstruction();
                GenerationJournal journal = GenerationJournal.create(
                    client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT),
                    design, origin);
                generatorStage = "Geometria completa: un unico progetto";
                logConsole("Fase avviata: Geometria completa: un unico progetto");
                GeometryGenerator.Result generated = GeometryGenerator.generate(
                    token, model, design, origin, stage -> {
                        generatorStage = stage;
                        logConsole(stage);
                    }, journal);
                String geometry = generated.json();
                BuildPlan completeGeometry = generated.blueprint();
                if (geometry != null) journal.write("complete-geometry.json", geometry);
                generatorPages = geometry == null
                    ? ComponentManifest.parse(java.nio.file.Files.readString(
                        journal.directory().resolve("component-manifest.json"))).components().size()
                    : 1;
                generatorBlocks = completeGeometry.placements().size();
                generatorStage = "Progetto completo verificato: "
                    + completeGeometry.sections().size() + " sezioni";
                return new GenerationOutcome("Progetto completo", completeGeometry);
            } catch (Exception e) {
                return new GenerationOutcome("Errore GPT: " + e.getMessage(), null);
            }
        }).thenAccept(outcome -> {
            String reply = outcome.reply();
            generatorRunning = false;
            logConsole(reply.startsWith("Errore GPT:") ? "ERRORE: " + reply.substring(0, Math.min(350, reply.length())) : "Risposta GPT ricevuta; conversione blueprint");
            generatorStage = reply.startsWith("Errore GPT:") ? "Interrotto: verifica dettagli" : "Generazione conclusa";
            if (client != null) client.execute(() -> {
                if (build && !reply.startsWith("Errore GPT:")) {
                    try {
                        // The already validated blueprint is passed directly; avoid
                        // serializing/parsing up to 2 million blocks on the render thread.
                        pendingPlan = outcome.plan();
                        if (pendingPlan == null) throw new IllegalStateException("Blueprint assente");
                        BlueprintContract.validateAndPartition(pendingPlan);
                        BuildExecutor.validateHeight(client, pendingPlan);
                        logConsole("Blueprint validato: " + pendingPlan.placements().size() + " blocchi, " + pendingPlan.sections().size() + " sezioni");
                        lastPlan = pendingPlan;
                        lastReply = "Progetto pronto: " + pendingPlan.placements().size() + " blocchi. "
                            + " Sezioni 16x16 validate: " + pendingPlan.sections().size() + ". "
                            + pendingPlan.geometryReport() + " " + pendingPlan.architectureWarnings()
                            + " Premi Costruisci.";
                        status = Text.literal("Progetto pronto, conferma.");
                        logConsole("Progetto pronto: conferma Costruisci");
                    } catch (Exception e) {
                        lastReply = "Progetto non valido (" + e.getClass().getSimpleName() + "): "
                            + e.getMessage() + ". Controlla la geometria e il JSON; usa Dettagli e controlli per copiare il messaggio.";
                        logConsole("ERRORE blueprint: " + e.getClass().getSimpleName() + ": " + e.getMessage());
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
        pollConsoleStage();
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        String buildError = BuildExecutor.lastErrorFor(client);
        if (generatorRunning || generatorPages > 0) {
            String activity = "Generatore: " + generatorStage + " | componenti/parti " + generatorPages
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

    private static final class ConsoleScreen extends Screen {
        private final Screen previous;
        private int scroll = 0;
        private ConsoleScreen(Screen previous) {
            super(Text.literal("Mine Builder - Console generazione"));
            this.previous = previous;
        }
        @Override protected void init() {
            addDrawableChild(ButtonWidget.builder(Text.literal("Indietro"), b -> close())
                .dimensions(width / 2 - 154, height - 27, 100, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Precedenti"), b -> scroll += 8)
                .dimensions(width / 2 - 50, height - 27, 100, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Recenti"), b -> scroll = Math.max(0, scroll - 8))
                .dimensions(width / 2 + 54, height - 27, 100, 20).build());
        }
        @Override public void close() {
            if (client != null) client.setScreen(previous);
        }
        @Override public boolean shouldPause() { return false; }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            pollConsoleStage();
            renderBackground(context);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
            long elapsed = Math.max(0, (System.currentTimeMillis() - consoleStageStarted) / 1000);
            String current = "Fase: " + generatorStage + (generatorRunning ? " | attiva " + elapsed + "s" : " | inattiva");
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(current, width - 24), 12, 32, 0xAADDFF);
            context.drawTextWithShadow(textRenderer,
                "Blocchi: " + generatorBlocks + " | Componenti/parti: " + generatorPages
                    + " | Sezioni: " + BuildExecutor.sectionProgress(), 12, 47, 0xCCCCCC);
            context.drawTextWithShadow(textRenderer, "Registro: config/minebuilder/generation-console.log", 12, 62, 0xAAAAAA);
            int count = consoleEvents.size();
            int visible = Math.max(1, (height - 116) / 12);
            scroll = Math.min(scroll, Math.max(0, count - visible));
            int end = count - scroll;
            int start = Math.max(0, end - visible);
            for (int i = start; i < end; i++) {
                String line = consoleEvents.get(i);
                context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(line, width - 24),
                    12, 84 + (i - start) * 12, 0xDDEEDD);
            }
            super.render(context, mouseX, mouseY, delta);
        }
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
