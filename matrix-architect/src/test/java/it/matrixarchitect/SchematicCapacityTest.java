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
  // Real WorldEdit writer/reader roundtrip above the former limit, including origin.
  var region=new CuboidRegion(BlockVector3.ZERO,BlockVector3.at(200,50,200));
  var clip=new BlockArrayClipboard(region);clip.setOrigin(BlockVector3.at(9,3,7));
  var file=Files.createTempFile("matrix-large-schematic-", ".schem");
  try{
   try(var output=Files.newOutputStream(file);var writer=BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(output)){writer.write(clip);}
   try(var input=Files.newInputStream(file);var reader=BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(input)){
    var read=reader.read();var d=read.getDimensions();long volume=(long)d.getBlockX()*d.getBlockY()*d.getBlockZ();
    if(volume!=2_060_451L||!read.getOrigin().equals(clip.getOrigin()))throw new AssertionError("Dimensions/origin changed: "+volume);
    SchematicCapacity.validate(volume,4*gib,gib);
   }
  }finally{Files.deleteIfExists(file);}
  System.out.println("Large schematic WorldEdit roundtrip and memory reserve checks passed");
 }
}
