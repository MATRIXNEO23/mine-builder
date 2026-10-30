package it.matrixarchitect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import java.nio.file.*;
public final class ArchitectScreen extends Screen {
 private ButtonWidget load,position,undo;private int scroll;
 public ArchitectScreen(){super(Text.literal("MATRIX Architect — WorldEdit"));}
 protected void init(){
  int panel=Math.min(230,width/2-12),left=width-panel-10;
  load=addDrawableChild(ButtonWidget.builder(Text.literal("Carica schematic"),b->choose()).dimensions(left,74,panel,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Prendi bacchetta (Creativa)"),b->{if(client.getServer()==null)return;var id=client.player.getUuid();client.getServer().execute(()->{var p=client.getServer().getPlayerManager().getPlayer(id);if(p!=null&&p.isCreative())p.giveItemStack(new net.minecraft.item.ItemStack(MatrixArchitect.WAND));else WorldEditBridge.event("Disponibile in modalita Creativa; bacchetta anche nell'inventario creativo");});}).dimensions(left,100,panel,20).build());
  position=addDrawableChild(ButtonWidget.builder(Text.literal("Posiziona nel mondo"),b->close()).dimensions(left,126,panel,20).build());
  undo=addDrawableChild(ButtonWidget.builder(Text.literal("Annulla ultima modifica WE"),b->WorldEditBridge.undo(client)).dimensions(left,152,panel,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Copia registro"),b->client.keyboard.setClipboard(String.join("\n",WorldEditBridge.log))).dimensions(left,178,panel,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Precedenti"),b->scroll+=8).dimensions(8,height-26,100,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Segui LIVE"),b->scroll=0).dimensions(112,height-26,100,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("Chiudi"),b->close()).dimensions(left,height-26,panel,20).build());update();
 }
 private void choose(){
  if(WorldEditBridge.busy)return;
  client.setScreen(new SchematicBrowserScreen(this,file->WorldEditBridge.load(client,file)));
 }
 @Override public void filesDragged(java.util.List<Path> files){if(files.size()==1&&!WorldEditBridge.busy)WorldEditBridge.load(client,files.get(0));}
 private void update(){load.active=!WorldEditBridge.busy&&client.isInSingleplayer();position.active=WorldEditBridge.ready(client);undo.active=!WorldEditBridge.busy&&client.isInSingleplayer();}
 public void tick(){update();}
 public boolean shouldPause(){return false;}
 public void render(DrawContext d,int mx,int my,float delta){
  update();renderBackground(d);d.drawCenteredTextWithShadow(textRenderer,title,width/2,10,0xFFFFFF);
  d.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(WorldEditBridge.status,width-16),width/2,28,0xAADDFF);
  int right=width-Math.min(230,width/2-12)-22;
  d.drawTextWithShadow(textRenderer,textRenderer.trimToWidth(WorldEditBridge.filename,Math.min(230,width/2-12)),right+12,48,0xCCCCCC);d.fill(6,46,right,height-32,0xCC101820);
  var lines=new java.util.ArrayList<net.minecraft.text.OrderedText>();for(String event:WorldEditBridge.log)lines.addAll(textRenderer.wrapLines(Text.literal(event),Math.max(40,right-18)));
  int visible=Math.max(1,(height-90)/12);scroll=Math.min(scroll,Math.max(0,lines.size()-visible));int end=lines.size()-scroll,start=Math.max(0,end-visible);
  for(int i=start;i<end;i++)d.drawTextWithShadow(textRenderer,lines.get(i),10,52+(i-start)*12,0xDDEEDD);
  super.render(d,mx,my,delta);
 }
}
