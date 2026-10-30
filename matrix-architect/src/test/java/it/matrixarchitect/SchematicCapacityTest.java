package it.matrixarchitect;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.math.BlockVector3;
import java.nio.file.Files;
public final class SchematicCapacityTest {
 public static void main(String[] args){
  try{run();System.exit(0);}catch(Throwable failure){failure.printStackTrace();System.exit(1);}
 }
 static void run()throws Exception{
  long gib=1024L*1024*1024;
  for(long volume:new long[]{1,2_000_001,8_120_601,100_000_000})
   SchematicCapacity.validate(volume,4*gib,gib);
  try{SchematicCapacity.validate(0,4*gib,gib);throw new AssertionError("Invalid volume accepted");}catch(IllegalArgumentException expected){}
  try{SchematicCapacity.validate(8_120_601,gib,gib-64*SchematicCapacity.MIB);throw new AssertionError("Low memory accepted");}catch(IllegalStateException expected){}
  System.out.println("Large volume and memory reserve checks passed");
 }
}
