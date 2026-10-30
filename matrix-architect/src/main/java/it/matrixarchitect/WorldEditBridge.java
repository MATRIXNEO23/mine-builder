package it.matrixarchitect;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
/** Reads and pastes through WorldEdit API. Never executes player-supplied commands. */
final class WorldEditBridge {
 static volatile Clipboard clipboard;
 static volatile boolean busy;
 static volatile String filename="Nessun schematic",status="Carica un .schem dal pannello F10";
 static volatile int turns;
 static final java.util.List<String> log=new java.util.concurrent.CopyOnWriteArrayList<>();
 private static net.minecraft.server.MinecraftServer loadedServer;
 private static net.minecraft.registry.RegistryKey<net.minecraft.world.World> loadedDimension;
 static void event(String message){status=message;log.add(java.time.LocalTime.now().withNano(0)+" | "+message);while(log.size()>250)log.remove(0);}
 static boolean ready(MinecraftClient c){return !busy&&clipboard!=null&&c.getServer()==loadedServer&&c.world!=null&&c.world.getRegistryKey().equals(loadedDimension);}
 static void load(MinecraftClient c,Path file){
  if(busy)return;
  if(c.getServer()==null||!c.isInSingleplayer()){event("Disponibile in singleplayer con WorldEdit");return;}
  var server=c.getServer();var dimension=c.world.getRegistryKey();clipboard=null;busy=true;event("Lettura schematic con WorldEdit: "+file.getFileName());
  CompletableFuture.supplyAsync(()->{
   try{
    if(!Files.isRegularFile(file)||Files.size(file)>64_000_000)throw new java.io.IOException("File assente o oltre 64 MB");
    var format=ClipboardFormats.findByFile(file.toFile());if(format==null)throw new java.io.IOException("Formato non riconosciuto da WorldEdit");
    try(var input=Files.newInputStream(file);var reader=format.getReader(input)){
     Clipboard clip=reader.read();var d=clip.getDimensions();long volume=(long)d.getBlockX()*d.getBlockY()*d.getBlockZ();
     if(volume<1||volume>2_000_000)throw new java.io.IOException("Ingombro oltre 2 milioni di posizioni; nessun ridimensionamento");
     if(!clip.getEntities().isEmpty())throw new java.io.IOException("Schematic con entita: questa versione non le importa");
     return clip;
    }
   }catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}
  }).whenComplete((clip,error)->c.execute(()->{
   busy=false;
   if(c.getServer()!=server||c.world==null||!c.world.getRegistryKey().equals(dimension)){event("Mondo cambiato durante caricamento: file non attivato");return;}
   if(error!=null){event("ERRORE caricamento: "+error.getCause().getMessage());return;}
   clipboard=clip;filename=file.getFileName().toString();loadedServer=server;loadedDimension=dimension;turns=0;
   ArchitectClient.resetTarget();event("Pronto: "+filename+" | ingombro "+clip.getDimensions()+". Esci dal pannello e impugna la bacchetta.");
  }));
 }
 static void paste(MinecraftClient c,BlockPos target){
  if(!ready(c)||target==null)return;
  final var clip=clipboard;final int rotation=turns;final var server=c.getServer();final var id=c.player.getUuid();final var dim=loadedDimension;
  // Freeze the exact target/rotation displayed when P was pressed.
  final BlockPos anchor=target.toImmutable();busy=true;event("Incolla WorldEdit a "+anchor.toShortString()+" | "+rotation*90+" gradi. Include aria e sovrascrive il terreno.");
  server.execute(()->{
   EditSession edit=null;LocalSession session=null;
   try{
    var player=server.getPlayerManager().getPlayer(id);
    if(player==null||!player.getWorld().getRegistryKey().equals(dim))throw new IllegalStateException("Giocatore o dimensione cambiati");
    var actor=FabricAdapter.adaptPlayer(player);
    if(!actor.hasPermission("worldedit.clipboard.paste"))throw new IllegalStateException("WorldEdit richiede permesso di incolla: abilita i comandi nel mondo");
    var world=server.getWorld(dim);var bounds=ArchitectClient.bounds(clip,anchor,rotation);
    if(bounds.minY<world.getBottomY()||bounds.maxY>world.getTopY())throw new IllegalArgumentException("Anteprima fuori altezza mondo");
    // Avoid a half-paste caused by missing chunks: no world mutations until all are loaded.
    for(int x=(int)Math.floor(bounds.minX/16);x<=Math.floor((bounds.maxX-1)/16);x++)
     for(int z=(int)Math.floor(bounds.minZ/16);z<=Math.floor((bounds.maxZ-1)/16);z++){
      BlockPos check=new BlockPos(x*16,anchor.getY(),z*16);
      if(!world.isChunkLoaded(check))throw new IllegalStateException("Terreno non caricato: avvicinati all'intero ingombro e riprova");
      if(!world.getWorldBorder().contains(check))throw new IllegalStateException("Ingombro oltre il confine del mondo");
     }
    session=WorldEdit.getInstance().getSessionManager().get(actor);
    var holder=new ClipboardHolder(clip);holder.setTransform(new AffineTransform().rotateY(-rotation*90));
    edit=session.createEditSession(actor);
    try(EditSession active=edit){Operations.complete(holder.createPaste(active).to(BlockVector3.at(anchor.getX(),anchor.getY(),anchor.getZ())).ignoreAirBlocks(false).copyEntities(false).copyBiomes(false).build());}
    event("WorldEdit ha terminato la posa: "+edit.size()+" modifiche. Il tasto di annullamento annulla l'ultima operazione WorldEdit.");
   }catch(Throwable error){event("ERRORE posa: "+error.getMessage()+". Eventuali modifiche parziali restano nella cronologia WorldEdit: usa il comando Annulla.");}
   finally{
    try{if(session!=null&&edit!=null)session.remember(edit);}finally{busy=false;}
   }
  });
 }
 static void undo(MinecraftClient c){
  if(busy||c.getServer()==null||c.player==null||!c.isInSingleplayer())return;
  busy=true;event("Annullamento ultima operazione WorldEdit (cronologia della sessione)");var server=c.getServer();var id=c.player.getUuid();
  server.execute(()->{try{
   var player=server.getPlayerManager().getPlayer(id);if(player==null)throw new IllegalStateException("Giocatore assente");
   var actor=FabricAdapter.adaptPlayer(player);if(!actor.hasPermission("worldedit.history.undo"))throw new IllegalStateException("Permesso WorldEdit undo assente");
   var result=WorldEdit.getInstance().getSessionManager().get(actor).undo(null,actor);
   event(result==null?"Nessuna operazione WorldEdit da annullare":"Ultima operazione WorldEdit annullata");
  }catch(Throwable ex){event("ERRORE annullamento: "+ex.getMessage());}finally{busy=false;}});
 }
}
