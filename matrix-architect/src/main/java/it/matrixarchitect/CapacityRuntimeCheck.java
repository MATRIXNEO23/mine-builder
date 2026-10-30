package it.matrixarchitect;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.math.BlockVector3;
import java.nio.file.Files;
final class CapacityRuntimeCheck {
 static boolean started;
 static void run(){
 try{
 long gib=1024L*1024*1024;
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

 System.out.println("FABRIC_WORLDEDIT_LARGE_SCHEMATIC_PASS");
 Runtime.getRuntime().halt(0);
 }catch(Throwable failure){failure.printStackTrace();Runtime.getRuntime().halt(1);}
 }
}
