package it.minebuilder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.TypedActionResult;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;

/** Registers GUI behavior only on the Minecraft client. */
public final class MineBuilderClient implements ClientModInitializer {
    private static int quarterTurns = 0;
    private static boolean awaitingPreflight = false;
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(BuildExecutor::tick);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!awaitingPreflight) return;
            if (BuildExecutor.preflightPassed()) {
                BancoIaScreen.takePlacementPlan();
                quarterTurns = 0;
                awaitingPreflight = false;
                if (client.player != null) client.player.sendMessage(net.minecraft.text.Text.literal("Area verificata: costruzione in corso."), false);
            } else if (!BuildExecutor.lastError().isEmpty()
                    && !BuildExecutor.lastError().startsWith("In attesa")) {
                awaitingPreflight = false;
                if (client.player != null) client.player.sendMessage(net.minecraft.text.Text.literal(
                    BuildExecutor.lastError() + " Usa il martello su un altro punto per riprovare."), false);
            }
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!player.getStackInHand(hand).isOf(MineBuilder.BUILDER_TOOL))
                return TypedActionResult.pass(player.getStackInHand(hand));
            if (world.isClient) MinecraftClient.getInstance().setScreen(new BancoIaScreen());
            return TypedActionResult.success(player.getStackInHand(hand), world.isClient);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient && player.getStackInHand(hand).isOf(MineBuilder.BUILDER_TOOL)
                    && BancoIaScreen.hasPlacementPlan()) {
                if (player.isSneaking()) {
                    quarterTurns = (quarterTurns + 1) % 4;
                    player.sendMessage(net.minecraft.text.Text.literal("Rotazione progetto: " + (quarterTurns * 90) + " gradi. Clic normale per costruire."), true);
                    return ActionResult.SUCCESS;
                }
                BuildPlan plan = BancoIaScreen.peekPlacementPlan();
                BlockPos anchor = hit.getBlockPos().offset(hit.getSide());
                BlockPos first = plan.placements().stream()
                    .map(BuildPlan.Placement::pos)
                    .min(java.util.Comparator.comparingInt(BlockPos::getY)
                        .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ))
                    .orElseThrow();
                int dy = anchor.getY() - first.getY();
                var moved = new ArrayList<BuildPlan.Placement>();
                for (var entry : plan.placements()) {
                    int x = entry.pos().getX() - first.getX();
                    int z = entry.pos().getZ() - first.getZ();
                    int rx = x, rz = z;
                    for (int i = 0; i < quarterTurns; i++) { int temp = rx; rx = -rz; rz = temp; }
                    moved.add(new BuildPlan.Placement(new BlockPos(anchor.getX() + rx,
                        entry.pos().getY() + dy, anchor.getZ() + rz),
                        entry.state().rotate(net.minecraft.util.BlockRotation.values()[quarterTurns])));
                }
                try {
                    BuildExecutor.start(MinecraftClient.getInstance(), BuildPlan.fromPlacements(moved));
                    awaitingPreflight = true;
                    player.sendMessage(net.minecraft.text.Text.literal("Verifica area in corso; se occupata potrai riprovare senza rigenerare."), false);
                } catch (Exception ex) {
                    player.sendMessage(net.minecraft.text.Text.literal("Posizionamento: " + ex.getMessage()), false);
                }
                return ActionResult.SUCCESS;
            }
            if (!world.isClient || !world.getBlockState(hit.getBlockPos()).isOf(MineBuilder.BANCO_IA)) {
                return ActionResult.PASS;
            }
            MinecraftClient.getInstance().setScreen(new BancoIaScreen());
            return ActionResult.SUCCESS;
        });
    }
}
