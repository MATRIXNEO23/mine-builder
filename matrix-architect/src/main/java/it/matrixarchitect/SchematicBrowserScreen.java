package it.matrixarchitect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
/** In-game file picker: works with headless Minecraft runtimes and fullscreen windows. */
final class SchematicBrowserScreen extends Screen {
 private static Path lastDirectory=Path.of(System.getProperty("user.home"));
 private final Screen previous;private final Consumer<Path> onSelect;
 private Path directory=lastDirectory,selected;
 private List<FileBrowserModel.Entry> entries=List.of();
 private boolean needsScan=true,loading;private String error="";private int page;
 SchematicBrowserScreen(Screen previous,Consumer<Path> onSelect){super(Text.literal("Seleziona schematic — MATRIX Architect"));this.previous=previous;this.onSelect=onSelect;}
 protected void init(){
  int left=12,panel=width-24,third=(panel-8)/3;
  button("Cartella personale",left,48,third,()->navigate(Path.of(System.getProperty("user.home"))));
  button("Download",left+third+4,48,third,()->navigate(Path.of(System.getProperty("user.home"),"Downloads")));
  button("Dischi / Unita",left+2*(third+4),48,third,()->navigate(null));
  var up=button("Cartella superiore",left,72,panel,()->navigate(directory==null?null:directory.getParent()));up.active=!loading&&directory!=null;
  int perPage=Math.max(1,(height-160)/22),pages=Math.max(1,(entries.size()+perPage-1)/perPage);page=Math.min(page,pages-1);
  for(int i=0;i<perPage && page*perPage+i<entries.size();i++){
   var e=entries.get(page*perPage+i);String name=e.path().getFileName()==null?e.path().toString():e.path().getFileName().toString();
   String label=(e.directory()?"[Cartella] ":e.path().equals(selected)?"> ":"  ")+name;
   var item=button(textRenderer.trimToWidth(label,panel-10),left,98+i*22,panel,()->{
    if(e.directory())navigate(e.path());else{selected=e.path();clearAndInit();}
   });item.active=!loading;
  }
  var prev=button("Precedenti",left,height-54,(panel-4)/2,()->{page--;clearAndInit();});prev.active=!loading&&page>0;
  var next=button("Successivi",left+(panel+4)/2,height-54,(panel-4)/2,()->{page++;clearAndInit();});next.active=!loading&&page+1<pages;
  button("Annulla",left,height-28,(panel-4)/2,this::close);
  var open=button("Apri schematic selezionato",left+(panel+4)/2,height-28,(panel-4)/2,()->{
   Path chosen=selected;if(chosen==null||loading)return;
   if(directory!=null)lastDirectory=directory;
   client.setScreen(previous);onSelect.accept(chosen);
  });open.active=!loading&&selected!=null;
  if(needsScan){needsScan=false;scan();}
 }
 private ButtonWidget button(String name,int x,int y,int w,Runnable action){return addDrawableChild(ButtonWidget.builder(Text.literal(name),b->action.run()).dimensions(x,y,w,20).build());}
 private void navigate(Path path){if(loading)return;directory=path;selected=null;entries=List.of();page=0;error="";needsScan=true;clearAndInit();}
 private void scan(){
  loading=true;Path requested=directory;
  for(var child:children())if(child instanceof ButtonWidget b)b.active=b.getMessage().getString().equals("Annulla");
  java.util.concurrent.CompletableFuture.supplyAsync(()->{
   try{return FileBrowserModel.list(requested);}catch(Exception ex){throw new java.util.concurrent.CompletionException(ex);}
  }).whenComplete((result,failure)->client.execute(()->{
   if(client.currentScreen!=this)return;
   loading=false;entries=failure==null?result:List.of();
   error=failure==null?"": "Cartella non accessibile: "+String.valueOf(failure.getCause().getMessage());
   clearAndInit();
  }));
 }
 public void close(){client.setScreen(previous);}
 public boolean shouldPause(){return false;}
 public void render(DrawContext d,int mx,int my,float delta){
  renderBackground(d);d.drawCenteredTextWithShadow(textRenderer,title,width/2,8,0xFFFFFF);
  String info=loading?"Lettura cartella...":!error.isEmpty()?error:directory==null?"Seleziona un disco":directory.toString();
  d.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(info,width-24),12,28,error.isEmpty()?0xAACCEE:0xFF8888);
  if(!loading&&entries.isEmpty()&&error.isEmpty())d.drawTextWithShadow(textRenderer,"Nessuna cartella o file .schem/.schematic",12,100,0xAAAAAA);
  super.render(d,mx,my,delta);
 }
}
