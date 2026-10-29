package it.minebuilder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MineBuilder implements ModInitializer {
    public static final String MOD_ID = "minebuilder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Block BANCO_IA = new Block(FabricBlockSettings.create().strength(2.5f)) {
        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (world.isClient) {\n                net.minecraft.client.MinecraftClient.getInstance().setScreen(new BancoIaScreen());\n            } else {
                player.sendMessage(Text.literal("Banco IA: connessione GPT non ancora disponibile. Nessun blocco del mondo verra modificato."), false);
            }
            return ActionResult.SUCCESS;
        }
    };

    @Override
    public void onInitialize() {
        Identifier id = new Identifier(MOD_ID, "banco_ia");
        Registry.register(Registries.BLOCK, id, BANCO_IA);
        Registry.register(Registries.ITEM, id, new BlockItem(BANCO_IA, new Item.Settings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(BANCO_IA));
        LOGGER.info("Mine Builder: Banco IA registered; GPT connection not yet configured.");
    }
}
