package it.blueprintbuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
/** Persistent console at left, explicit import and placement controls at right. */
public final class BuilderScreen extends Screen {
 private static BuildPlan imported;
 private static boolean loading;
 private static String filename="Nessun file";
 private TextFieldWidget path,x,y,z;
 private ButtonWidget load,build,recover,pause,rotate,restore;
 private int turns,scroll;
 public BuilderScreen(){super(Text.literal("Blueprint Builder 0.1.0"));}
 protected void init(){
  int panel=Math.min(230,width/2-12),left=width-panel-10,top=40;
  path=new TextFieldWidget(textRenderer,left,top,panel,20,Text.literal("Percorso blueprint"));path.setMaxLength(2048);path.setPlaceholder(Text.literal("Percorso .json o .json.gz"));addDrawableChild(path);
  load=addDrawableChild(ButtonWidget.builder(Text.literal("Carica blueprint"),b->{try{if(!path.getText().isBlank())importFile(Path.of(path.getText()));else chooseFile();}catch(Exception ex){LocalBuilder.event("ERRORE percorso: "+ex.getMessage());}}).dimensions(left,top+18,panel,18).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Apri cartella blueprint"),b->{try{Path dir=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("blueprintbuilder/imports");Files.createDirectories(dir);net.minecraft.util.Util.getOperatingSystem().open(dir.toFile());}catch(Exception ex){LocalBuilder.event("ERRORE cartella: "+ex.getMessage());}}).dimensions(left,top+36,panel,18).build());
  int col=(panel-8)/3;
  x=new TextFieldWidget(textRenderer,left,top+64,col,20,Text.literal("X"));
  y=new TextFieldWidget(textRenderer,left+col+4,top+64,col,20,Text.literal("Y"));
  z=new TextFieldWidget(textRenderer,left+2*(col+4),top+64,col,20,Text.literal("Z"));
  BlockPos p=client.player.getBlockPos().add(4,0,4);x.setText(""+p.getX());y.setText(""+p.getY());z.setText(""+p.getZ());addDrawableChild(x);addDrawableChild(y);addDrawableChild(z);
  rotate=addDrawableChild(ButtonWidget.builder(Text.literal("Rotazione: 0 gradi"),b->{turns=(turns+1)%4;b.setMessage(Text.literal("Rotazione: "+turns*90+" gradi"));}).dimensions(left,top+82,panel,18).build());
  build=addDrawableChild(ButtonWidget.builder(Text.literal("Costruisci qui"),b->start()).dimensions(left,top+100,panel,18).build());
  pause=addDrawableChild(ButtonWidget.builder(Text.literal("Pausa / Continua"),b->{LocalBuilder.paused=!LocalBuilder.paused;LocalBuilder.event(LocalBuilder.paused?"Pausa richiesta":"Costruzione ripresa");}).dimensions(left,top+118,panel,18).build());
  recover=addDrawableChild(ButtonWidget.builder(Text.literal("Riprendi salvato"),b->{try{LocalBuilder.recover(client);}catch(Exception ex){LocalBuilder.event("ERRORE ripresa: "+ex.getMessage());}}).dimensions(left,top+136,panel,18).build());
  restore=addDrawableChild(ButtonWidget.builder(Text.literal("Ripristina ultimo backup"),b->{try{LocalBuilder.restore(client);}catch(Exception ex){LocalBuilder.event("ERRORE backup: "+ex.getMessage());}}).dimensions(left,top+154,panel,18).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Registro precedente"),b->scroll+=8).dimensions(10,height-26,115,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Segui LIVE"),b->scroll=0).dimensions(129,height-26,90,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Chiudi"),b->close()).dimensions(left,height-26,panel,18).build());
  updateButtons();
 }
 private void chooseFile(){
  if(loading||LocalBuilder.running)return;
  loading=true;LocalBuilder.event("Seleziona un file blueprint; in alternativa incolla il percorso nel campo");
  CompletableFuture.runAsync(()->{
   try {
    java.awt.EventQueue.invokeAndWait(()->{
     java.awt.FileDialog dialog=new java.awt.FileDialog((java.awt.Frame)null,"Carica blueprint JSON / JSON.GZ",java.awt.FileDialog.LOAD);
     try{dialog.setVisible(true);String name=dialog.getFile();String dir=dialog.getDirectory();
      client.execute(()->{loading=false;if(name!=null)importFile(Path.of(dir,name));});
     }finally{dialog.dispose();}
    });
   }catch(Exception ex){client.execute(()->{loading=false;LocalBuilder.event("Selettore non disponibile: incolla il percorso nel campo e premi Carica");});}
  });
 }
 private void importFile(Path file){
  if(loading||LocalBuilder.running)return;loading=true;imported=null;filename=file.getFileName().toString();LocalBuilder.phase="Caricamento blueprint";LocalBuilder.event("Lettura e validazione: "+filename);
  CompletableFuture.supplyAsync(()->{try{return BlueprintImport.load(file);}catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}}).whenComplete((plan,error)->client.execute(()->{
   loading=false;if(error!=null){LocalBuilder.phase="Importazione fallita";LocalBuilder.event("ERRORE: "+error.getCause().getMessage());return;}
   imported=plan;LocalBuilder.phase="Blueprint pronto";LocalBuilder.event("Validato: "+plan.placements().size()+" blocchi. "+plan.geometryReport());LocalBuilder.event(plan.architectureWarnings());
  }));
 }
 @Override public void filesDragged(java.util.List<Path> files){if(files.size()==1)importFile(files.get(0));else LocalBuilder.event("Trascina un solo file blueprint");}
 private void start(){
  if(imported==null||loading||LocalBuilder.running)return;
  try{
   int ax=Integer.parseInt(x.getText()),ay=Integer.parseInt(y.getText()),az=Integer.parseInt(z.getText());
   if(Math.abs((long)ax)>29_990_000 || Math.abs((long)az)>29_990_000)throw new IllegalArgumentException("Coordinate fuori limite");
   var moved=new java.util.ArrayList<BuildPlan.Placement>();
   for(var e:imported.placements()){
    int px=e.pos().getX(),pz=e.pos().getZ();for(int i=0;i<turns;i++){int tmp=px;px=-pz;pz=tmp;}
    moved.add(new BuildPlan.Placement(new BlockPos(ax+px,ay+e.pos().getY(),az+pz),e.state().rotate(net.minecraft.util.BlockRotation.values()[turns])));
   }
   LocalBuilder.start(client,BuildPlan.fromPlacements(moved));
  }catch(Exception ex){LocalBuilder.event("ERRORE avvio: "+ex.getMessage());}
 }
 private void updateButtons(){
  boolean idle=!loading&&!LocalBuilder.running;
  load.active=idle;rotate.active=idle&&imported!=null;build.active=idle&&imported!=null&&validCoordinate(x)&&validCoordinate(y)&&validCoordinate(z);
  recover.active=idle&&client.getServer()!=null&&Files.isRegularFile(client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("blueprintbuilder/active.txt"));
  restore.active=idle&&client.getServer()!=null&&Files.isRegularFile(client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("blueprintbuilder/last-backup.txt"));
  pause.active=LocalBuilder.running;
 }
 private boolean validCoordinate(TextFieldWidget field){try{Integer.parseInt(field.getText());return true;}catch(Exception e){return false;}}
 @Override public void tick(){updateButtons();}
 @Override public boolean shouldPause(){return false;}
 public void render(DrawContext c,int mx,int my,float delta){
  updateButtons();renderBackground(c);c.drawCenteredTextWithShadow(textRenderer,title,width/2,8,0xFFFFFF);
  c.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(LocalBuilder.phase+" | posa "+LocalBuilder.done+"/"+LocalBuilder.total,width-20),width/2,24,0xAADDFF);
  int panel=Math.min(230,width/2-12),right=width-panel-22;
  c.fill(6,42,right,height-32,0xDD101820);
  var lines=new java.util.ArrayList<net.minecraft.text.OrderedText>();for(String line:LocalBuilder.log)lines.addAll(textRenderer.wrapLines(Text.literal(line),Math.max(40,right-20)));
  int visible=Math.max(1,(height-86)/12);scroll=Math.min(scroll,Math.max(0,lines.size()-visible));int end=lines.size()-scroll,start=Math.max(0,end-visible);
  for(int i=start;i<end;i++)c.drawTextWithShadow(textRenderer,lines.get(i),10,48+(i-start)*12,0xDDEEDD);
  c.drawTextWithShadow(textRenderer,"Origine X / Y / Z",width-panel-10,94,0xAAAAAA);
  super.render(c,mx,my,delta);
 }
}
