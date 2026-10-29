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
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(BuildExecutor::tick);
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!player.getStackInHand(hand).isOf(MineBuilder.BUILDER_TOOL))
                return TypedActionResult.pass(player.getStackInHand(hand));
            if (world.isClient) MinecraftClient.getInstance().setScreen(new BancoIaScreen());
            return TypedActionResult.success(player.getStackInHand(hand), world.isClient);
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient && player.getStackInHand(hand).isOf(MineBuilder.BUILDER_TOOL)
                    && BancoIaScreen.hasPlacementPlan()) {
                BuildPlan plan = BancoIaScreen.takePlacementPlan();
                BlockPos anchor = hit.getBlockPos().offset(hit.getSide());
                BlockPos first = plan.placements().stream()
                    .map(BuildPlan.Placement::pos)
                    .min(java.util.Comparator.comparingInt(BlockPos::getY)
                        .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ))
                    .orElseThrow();
                int dx = anchor.getX() - first.getX();
                int dy = anchor.getY() - first.getY();
                int dz = anchor.getZ() - first.getZ();
                var moved = new ArrayList<BuildPlan.Placement>();
                for (var entry : plan.placements())
                    moved.add(new BuildPlan.Placement(entry.pos().add(dx, dy, dz), entry.state()));
                try {
                    BuildExecutor.start(MinecraftClient.getInstance(), BuildPlan.fromPlacements(moved));
                    player.sendMessage(net.minecraft.text.Text.literal("Costruzione avviata nel punto scelto."), false);
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
