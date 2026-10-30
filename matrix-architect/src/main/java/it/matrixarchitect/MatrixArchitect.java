package it.matrixarchitect;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
public final class MatrixArchitect implements ModInitializer {
 public static final Item WAND=new Item(new Item.Settings().maxCount(1));
 public void onInitialize(){
  if(Boolean.getBoolean("matrixarchitect.capacity-test"))net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->CapacityRuntimeCheck.run());
  Registry.register(Registries.ITEM,new Identifier("matrixarchitect","placement_wand"),WAND);
  ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries->entries.add(WAND));
 }
}
