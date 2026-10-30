package it.blueprintbuilder;
import java.nio.file.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.BlockRotation;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
public final class GeometrySmokeTest {
 public static void main(String[] args)throws Exception {
  net.minecraft.SharedConstants.createGameVersion();
  net.minecraft.Bootstrap.initialize();
  BuildPlan p=BlueprintImport.load(Path.of("examples/test-pedestal.json"));
  p.validateSections();
  if(p.placements().size()!=29)throw new AssertionError("Geometry count mismatch");
  var states=new java.util.HashMap<BlockPos,net.minecraft.block.BlockState>();
  for(var e:p.placements()){
   states.put(e.pos(),e.state());
   var s=e.state();for(int i=0;i<4;i++)s=s.rotate(BlockRotation.CLOCKWISE_90);
   if(!s.equals(e.state()))throw new AssertionError("Rotation loses state");
  }
  if(!states.get(new BlockPos(2,1,3)).isOf(Blocks.HOPPER))throw new AssertionError("Hopper lost");
  if(states.get(new BlockPos(3,1,1)).get(Properties.HORIZONTAL_FACING)!=net.minecraft.util.math.Direction.EAST)throw new AssertionError("Stair orientation lost");
  if(states.get(new BlockPos(2,1,1)).get(Properties.SLAB_TYPE)!=net.minecraft.block.enums.SlabType.TOP)throw new AssertionError("Slab height lost");
  try{BuildPlan.parseComplete("{\"blocks\":[{\"x\":0,\"y\":0,\"z\":0,\"block\":\"minecraft:quartz_stairs[facing=wrong]\"}]}",BlockPos.ORIGIN);throw new AssertionError("Invalid property accepted");}catch(IllegalArgumentException expected){}
  System.out.println("Minecraft registry geometry and rotation checks passed");
 }
}
