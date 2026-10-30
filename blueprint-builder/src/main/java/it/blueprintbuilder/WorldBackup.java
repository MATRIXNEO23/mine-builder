package it.blueprintbuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import java.nio.file.*;
import java.util.*;
/** Immutable backup of every affected position, including block entity NBT, before placement. */
final class WorldBackup {
 static void save(Path root,String dimension,Map<BlockPos,BlockState> blocks,Map<BlockPos,String> nbt)throws Exception{
  Path dir=root.resolve("backups");Files.createDirectories(dir);
  String id=java.time.Instant.now().toEpochMilli()+"-"+UUID.randomUUID();
  Path tmp=Files.createTempFile(dir,"backup-",".tmp");
  try{
   try(var out=new java.util.zip.GZIPOutputStream(Files.newOutputStream(tmp));
       var text=new java.io.OutputStreamWriter(out,java.nio.charset.StandardCharsets.UTF_8);
       var writer=new com.google.gson.stream.JsonWriter(text)){
    writer.beginObject();writer.name("dimension").value(dimension);writer.name("blocks").beginArray();
    for(var entry:blocks.entrySet()){
     var p=entry.getKey();writer.beginObject();writer.name("x").value(p.getX());writer.name("y").value(p.getY());writer.name("z").value(p.getZ());writer.name("block").value(BlueprintLibrary.blockStateName(entry.getValue()));
     if(nbt.containsKey(p))writer.name("block_entity_nbt").value(nbt.get(p));writer.endObject();
    }
    writer.endArray();writer.endObject();
   }
   Path dest=dir.resolve(id+".json.gz");
   try{Files.move(tmp,dest,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException ex){Files.move(tmp,dest);}
   Files.writeString(root.resolve("last-backup.txt"),dest.getFileName().toString());
   LocalBuilder.event("Backup blocchi e contenitori: "+dest.getFileName());
  }finally{Files.deleteIfExists(tmp);}
 }

 record Snapshot(java.util.List<BuildPlan.Placement> blocks,Map<BlockPos,String> nbt){}
 static Snapshot readLast(Path root,String dimension)throws Exception{
  return read(root,dimension,Files.readString(root.resolve("last-backup.txt")).trim());
 }
 static Snapshot read(Path root,String dimension,String name)throws Exception{
  if(!name.matches("[a-zA-Z0-9-]+\\.json\\.gz"))throw new java.io.IOException("Nome backup non valido");
  var blocks=new ArrayList<BuildPlan.Placement>();Map<BlockPos,String> nbt=new HashMap<>();String savedDimension=null;
  try(var in=new java.util.zip.GZIPInputStream(Files.newInputStream(root.resolve("backups").resolve(name)));
      var text=new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8);
      var reader=new com.google.gson.stream.JsonReader(text)){
   reader.beginObject();while(reader.hasNext()){
    String field=reader.nextName();if(field.equals("dimension")){savedDimension=reader.nextString();continue;}
    if(!field.equals("blocks")){reader.skipValue();continue;}
    reader.beginArray();while(reader.hasNext()){
     Integer x=null,y=null,z=null;String block=null,data=null;
     reader.beginObject();while(reader.hasNext())switch(reader.nextName()){
      case "x" -> x=reader.nextInt();case "y" -> y=reader.nextInt();case "z" -> z=reader.nextInt();
      case "block" -> block=reader.nextString();case "block_entity_nbt" -> data=reader.nextString();default -> reader.skipValue();
     }reader.endObject();
     if(x==null||y==null||z==null||block==null)throw new java.io.IOException("Backup incompleto");
     BlockPos p=new BlockPos(x,y,z);blocks.add(new BuildPlan.Placement(p,BlueprintLibrary.parseSavedState(block)));
     if(data!=null)nbt.put(p,data);if(blocks.size()>2_000_000)throw new java.io.IOException("Backup troppo grande");
    }reader.endArray();
   }reader.endObject();
  }
  if(!dimension.equals(savedDimension))throw new java.io.IOException("Backup in una dimensione diversa");
  return new Snapshot(blocks,nbt);
 }
}
