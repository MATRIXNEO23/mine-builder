package it.minebuilder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

/** Registers GUI behavior only on the Minecraft client. */
public final class MineBuilderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(BuildExecutor::tick);
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient || !world.getBlockState(hit.getBlockPos()).isOf(MineBuilder.BANCO_IA)) {
                return ActionResult.PASS;
            }
            MinecraftClient.getInstance().setScreen(new BancoIaScreen());
            return ActionResult.SUCCESS;
        });
    }
}
