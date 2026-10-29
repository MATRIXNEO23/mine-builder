package it.minebuilder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
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
    public static final Block BANCO_IA = new Block(FabricBlockSettings.create().strength(2.5f));

    @Override
    public void onInitialize() {
        Identifier id = new Identifier(MOD_ID, "banco_ia");
        Registry.register(Registries.BLOCK, id, BANCO_IA);
        Registry.register(Registries.ITEM, id, new BlockItem(BANCO_IA, new Item.Settings()));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(BANCO_IA));
        LOGGER.info("Mine Builder: Banco IA registered; GPT connection not yet configured.");
    }
}
