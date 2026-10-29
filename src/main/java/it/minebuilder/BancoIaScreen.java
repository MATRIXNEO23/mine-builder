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
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Lightweight client-side interface. Never modifies the Minecraft world. */
public final class BancoIaScreen extends Screen {
    private TextFieldWidget prompt;
    private static LoopbackRedirect pendingLogin;
    private Text status = Text.literal("ChatGPT non collegato.");

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
        addDrawableChild(ButtonWidget.builder(Text.literal("Chiudi"), button -> close())
            .dimensions(left + 155, height / 2 + 18, 145, 20).build());
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
            receiver.awaitCode(Duration.ofMinutes(5)).whenComplete((code, error) -> {
                if (client != null) client.execute(() -> {
                    if (pendingLogin != receiver) return;
                    status = Text.literal(error == null
                        ? "Codice ricevuto. Scambio token non ancora disponibile."
                        : "Accesso scaduto o non riuscito.");
                    receiver.close();
                    pendingLogin = null;
                });
            });
        } catch (Exception e) {
            status = Text.literal("Impossibile avviare l'accesso: controlla il log.");
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 45, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, status, width / 2, height / 2 + 52, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
