package it.blueprintbuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.WorldSavePath;
import java.nio.file.*;
import java.util.*;
/** Air-only placement. No terrain demolition. Chunk waits and global preflight precede mutations. */
final class LocalBuilder {
 static final List<String> log=new java.util.concurrent.CopyOnWriteArrayList<>();
 static volatile String phase="Nessun blueprint caricato";
 static volatile int done,total,checked;
 static volatile boolean running,paused;
 private static net.minecraft.server.MinecraftServer owner;
 private static net.minecraft.registry.RegistryKey<net.minecraft.world.World> dimension;
 private static List<BuildPlan.Placement> placements=List.of();
 private static Map<BlockPos,BlockState> expected=Map.of();
 private static boolean scanning,verifying;
 private static volatile boolean backingUp;
 private static boolean restoring;
 private static Map<BlockPos,String> restoreNbt=Map.of();
 private static Map<BlockPos,BlockState> original=new HashMap<>();
 private static Map<BlockPos,String> originalNbt=new HashMap<>();
 private static volatile boolean inFlight;
 static void event(String text) { log.add(java.time.LocalTime.now().withNano(0)+" | "+text); while(log.size()>300)log.remove(0); }
 static void start(MinecraftClient c,BuildPlan plan) throws Exception {
  if(running || inFlight || c.getServer()==null || !c.isInSingleplayer())throw new IllegalStateException("Mondo locale richiesto e nessun lavoro attivo");
  for(var e:plan.placements())if(!c.world.isInBuildLimit(e.pos()))throw new IllegalArgumentException("Fuori limite mondo: "+e.pos());
  // Preserve an interrupted project before accepting a replacement.
  Path folder=c.getServer().getSavePath(WorldSavePath.ROOT).resolve("blueprintbuilder");Files.createDirectories(folder);
  Path marker=folder.resolve("active.txt");
  if(Files.exists(marker) && !Files.readString(marker).startsWith("COMPLETE"))
   throw new IllegalStateException("Progetto sospeso presente: usa Riprendi salvato");
  plan=clearEnvelope(plan);
  BlueprintLibrary.save(c,"active",plan);
  saveOrigin(c,plan);
  Files.writeString(marker,c.world.getRegistryKey().getValue().toString());
  activate(c,plan);
 }
 private static void activate(MinecraftClient c,BuildPlan plan) {
  owner=c.getServer();dimension=c.world.getRegistryKey();
  var ordered=new ArrayList<>(plan.placements());
  ordered.sort(Comparator.comparingInt((BuildPlan.Placement e)->e.state().getFluidState().isEmpty()?0:1)
   .thenComparingInt(e->e.pos().getY()));
  placements=List.copyOf(ordered);Map<BlockPos,BlockState> map=new HashMap<>();
  for(var e:placements)map.put(e.pos(),e.state());expected=map;
  original=new HashMap<>();originalNbt=new HashMap<>();backingUp=false;restoring=false;restoreNbt=Map.of();
  done=0;checked=0;total=placements.size();scanning=true;verifying=false;paused=false;running=true;
  phase="Controllo completo area";event(phase+": "+total+" posizioni. Sovrascrittura terreno con backup preventivo.");
 }
 static BuildPlan clearEnvelope(BuildPlan plan) {
  int minX=Integer.MAX_VALUE,minY=minX,minZ=minX,maxX=Integer.MIN_VALUE,maxY=maxX,maxZ=maxX;
  for(var e:plan.placements()){var p=e.pos();minX=Math.min(minX,p.getX());minY=Math.min(minY,p.getY());minZ=Math.min(minZ,p.getZ());maxX=Math.max(maxX,p.getX());maxY=Math.max(maxY,p.getY());maxZ=Math.max(maxZ,p.getZ());}
  long volume=((long)maxX-minX+1)*((long)maxY-minY+1)*((long)maxZ-minZ+1);
  if(volume>2_000_000)throw new IllegalArgumentException("Ingombro oltre 2 milioni di posizioni: suddividere il blueprint senza ridimensionarlo");
  Map<BlockPos,BlockState> cells=new LinkedHashMap<>();
  for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++)cells.put(new BlockPos(x,y,z),net.minecraft.block.Blocks.AIR.getDefaultState());
  for(var e:plan.placements())cells.put(e.pos(),e.state());
  event("Ingombro da liberare e costruire: "+volume+" posizioni; backup di tutto il volume");
  return BuildPlan.fromPlacements(cells.entrySet().stream().map(e->new BuildPlan.Placement(e.getKey(),e.getValue())).toList());
 }
 static void restore(MinecraftClient c)throws Exception{
  if(running || inFlight)throw new IllegalStateException("Lavoro in corso");
  Path root=c.getServer().getSavePath(WorldSavePath.ROOT).resolve("blueprintbuilder");
  var snapshot=WorldBackup.readLast(root,c.world.getRegistryKey().getValue().toString());
  BuildPlan plan=BuildPlan.fromPlacements(snapshot.blocks());
  BlueprintLibrary.save(c,"active",plan);saveOrigin(c,plan);
  Files.writeString(root.resolve("active.txt"),c.world.getRegistryKey().getValue().toString());
  activate(c,plan);restoring=true;restoreNbt=snapshot.nbt();
  event("Ripristino ultimo backup richiesto; prima viene salvato lo stato attuale dell'area");
 }
 static void recover(MinecraftClient c) throws Exception {
  if(running || inFlight)throw new IllegalStateException("Lavoro gia attivo");
  Path marker=c.getServer().getSavePath(WorldSavePath.ROOT).resolve("blueprintbuilder/active.txt");
  String saved=Files.readString(marker);
  if(saved.startsWith("COMPLETE"))throw new IllegalStateException("Costruzione gia completata");
  if(!saved.equals(c.world.getRegistryKey().getValue().toString()))throw new IllegalStateException("Dimensione errata");
  // Saved library is relative to its first block. The absolute origin is stored separately.
  String[] xyz=Files.readString(marker.resolveSibling("origin.txt")).split(",");
  BuildPlan plan=BlueprintLibrary.load(c,"active",new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2])));
  activate(c,plan);event("Ripresa da disco: controllo dei blocchi gia presenti, nessuna rigenerazione");
 }
 static void saveOrigin(MinecraftClient c,BuildPlan plan)throws Exception {
  Path f=c.getServer().getSavePath(WorldSavePath.ROOT).resolve("blueprintbuilder/origin.txt");
  BlockPos p=plan.placements().get(0).pos();Files.writeString(f,p.getX()+","+p.getY()+","+p.getZ());
 }
 static void fail(String message){phase="ERRORE: "+message;event(phase);running=false;paused=false;}
 static void tick(MinecraftClient c) {
  if(!running||paused||inFlight||backingUp)return;
  if(c.getServer()!=owner||c.world==null||!c.world.getRegistryKey().equals(dimension)){fail("Mondo cambiato; progetto conservato");return;}
  inFlight=true;
  owner.execute(()->{
   try {
    var w=owner.getWorld(dimension);if(w==null){fail("Dimensione assente");return;}
    long until=System.nanoTime()+2_000_000L;int batch=0;
    while(batch++<128 && System.nanoTime()<until && running && !paused){
     int index=scanning||verifying?checked:done;
     if(index>=total){
      if(scanning){
       scanning=false;checked=0;backingUp=true;phase="Salvataggio backup prima della posa";event(phase);
       var before=Map.copyOf(original);var nbt=Map.copyOf(originalNbt);var server=owner;var dim=dimension;
       java.util.concurrent.CompletableFuture.runAsync(()->{
        try{WorldBackup.save(server.getSavePath(WorldSavePath.ROOT).resolve("blueprintbuilder"),dim.getValue().toString(),before,nbt);
         phase="Costruzione";event("Backup salvato: avvio sovrascrittura");
        }catch(Exception ex){fail("Backup non salvato; nessun blocco modificato: "+ex.getMessage());}
        finally{backingUp=false;}
       });return;
      }
      if(!verifying){verifying=true;checked=0;phase="Verifica finale";continue;}
      w.getChunkManager().save(true);
      Path marker=owner.getSavePath(WorldSavePath.ROOT).resolve("blueprintbuilder/active.txt");
      Files.writeString(marker,"COMPLETE");running=false;phase="Completato e verificato";event(phase);return;
     }
     var e=placements.get(index);BlockPos p=e.pos();
     if(!w.isChunkLoaded(p)){phase="Attesa terreno: avvicinati a "+p.toShortString();return;}
     var current=w.getBlockState(p);
     if(verifying){if(!current.equals(e.state())){fail("Differenza dopo aggiornamenti Minecraft: "+p.toShortString());return;}checked++;continue;}

     if(scanning){
      original.put(p,current);
      var blockEntity=w.getBlockEntity(p);
      if(blockEntity!=null)originalNbt.put(p,blockEntity.createNbtWithId().toString());
      if(!restoring && !e.state().getFluidState().isEmpty())for(var side:new net.minecraft.util.math.Direction[]{net.minecraft.util.math.Direction.DOWN,net.minecraft.util.math.Direction.NORTH,net.minecraft.util.math.Direction.SOUTH,net.minecraft.util.math.Direction.EAST,net.minecraft.util.math.Direction.WEST}){
       BlockPos n=p.offset(side);BlockState boundary=expected.get(n);
       if(boundary==null){if(!w.isChunkLoaded(n))return;boundary=w.getBlockState(n);}
       if(boundary.isAir()||(!boundary.isSolidBlock(w,n)&&boundary.getFluidState().isEmpty())){fail("Liquido non contenuto presso "+n.toShortString());return;}
      }
      checked++;continue;
     }
     // Reject edits made after the snapshot instead of overwriting unbacked changes.
     if(!current.equals(e.state()) && !current.equals(original.get(p))){fail("Area cambiata dopo il backup in "+p.toShortString());return;}
     var liveEntity=w.getBlockEntity(p);
     if(liveEntity!=null && !java.util.Objects.equals(originalNbt.get(p),liveEntity.createNbtWithId().toString())){fail("Contenitore cambiato dopo backup in "+p.toShortString());return;}
     if(!current.equals(e.state()) && liveEntity!=null)w.removeBlockEntity(p);
     if(!current.equals(e.state()) && !w.setBlockState(p,e.state(),18)){fail("Posa fallita: "+p.toShortString());return;}
     if(restoring && restoreNbt.containsKey(p)){
      var entity=w.getBlockEntity(p);if(entity==null){fail("Contenitore backup non ripristinabile in "+p.toShortString());return;}
      entity.readNbt(net.minecraft.nbt.StringNbtReader.parse(restoreNbt.get(p)));entity.markDirty();
     }
     done++;
    }
   }catch(Exception ex){fail(ex.getClass().getSimpleName()+": "+ex.getMessage());}
   finally{inFlight=false;}
  });
 }
}
