package it.matrixarchitect;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.render.*;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.lwjgl.glfw.GLFW;
public final class ArchitectClient implements ClientModInitializer {
 static BlockPos target;
 static boolean locked;
 static int height;
 static KeyBinding menu,rotate,place,undo,up,down,left,right,forward,back,unlock;
 static boolean holding(MinecraftClient c){return c.player!=null&&c.player.getMainHandStack().isOf(MatrixArchitect.WAND);}
 static void resetTarget(){target=null;locked=false;height=0;}
 static KeyBinding key(String id,int code){return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.matrixarchitect."+id,InputUtil.Type.KEYSYM,code,"category.matrixarchitect"));}
 public void onInitializeClient(){
  menu=key("menu",GLFW.GLFW_KEY_K);rotate=key("rotate",GLFW.GLFW_KEY_R);place=key("place",GLFW.GLFW_KEY_P);undo=key("undo",GLFW.GLFW_KEY_U);
  up=key("up",GLFW.GLFW_KEY_PAGE_UP);down=key("down",GLFW.GLFW_KEY_PAGE_DOWN);left=key("left",GLFW.GLFW_KEY_LEFT);right=key("right",GLFW.GLFW_KEY_RIGHT);forward=key("forward",GLFW.GLFW_KEY_UP);back=key("back",GLFW.GLFW_KEY_DOWN);unlock=key("unlock",GLFW.GLFW_KEY_L);
  ClientTickEvents.END_CLIENT_TICK.register(c->{
   if(c.player==null||c.world==null)return;
   while(menu.wasPressed())if(c.currentScreen==null)c.setScreen(new ArchitectScreen());
   if(c.currentScreen!=null||!holding(c)){
    for(KeyBinding action:new KeyBinding[]{rotate,place,undo,up,down,left,right,forward,back,unlock})while(action.wasPressed()){}
    return;
   }
   if(WorldEditBridge.ready(c)&&!locked){
    HitResult hit=c.player.raycast(64,0,false);
    if(hit instanceof BlockHitResult b && hit.getType()==HitResult.Type.BLOCK)target=b.getBlockPos().offset(b.getSide());
   }
   while(rotate.wasPressed())if(WorldEditBridge.ready(c))WorldEditBridge.turns=(WorldEditBridge.turns+1)%4;
   while(up.wasPressed())height=Math.min(512,height+1);
   while(down.wasPressed())height=Math.max(-512,height-1);
   while(left.wasPressed())move(-1,0);while(right.wasPressed())move(1,0);while(forward.wasPressed())move(0,-1);while(back.wasPressed())move(0,1);
   while(unlock.wasPressed()){locked=false;height=0;}
   while(place.wasPressed())if(WorldEditBridge.ready(c)&&target!=null)WorldEditBridge.paste(c,target.up(height));
   while(undo.wasPressed())WorldEditBridge.undo(c);
  });
  UseBlockCallback.EVENT.register((player,world,hand,hit)->{
   if(hand!=Hand.MAIN_HAND||!player.getStackInHand(hand).isOf(MatrixArchitect.WAND))return ActionResult.PASS;
   if(world.isClient){var c=MinecraftClient.getInstance();if(player.isSneaking()||WorldEditBridge.clipboard==null)c.setScreen(new ArchitectScreen());else{target=hit.getBlockPos().offset(hit.getSide());locked=true;}}
   return ActionResult.SUCCESS;
  });
  UseItemCallback.EVENT.register((player,world,hand)->{
   if(hand!=Hand.MAIN_HAND||!player.getStackInHand(hand).isOf(MatrixArchitect.WAND))return TypedActionResult.pass(player.getStackInHand(hand));
   if(world.isClient){var c=MinecraftClient.getInstance();if(player.isSneaking()||WorldEditBridge.clipboard==null)c.setScreen(new ArchitectScreen());else locked=!locked;}
   return TypedActionResult.success(player.getStackInHand(hand));
  });
  AttackBlockCallback.EVENT.register((player,world,hand,pos,side)->player.getStackInHand(hand).isOf(MatrixArchitect.WAND)?ActionResult.FAIL:ActionResult.PASS);
  HudRenderCallback.EVENT.register((draw,delta)->{
   var c=MinecraftClient.getInstance();if(!holding(c)||c.currentScreen!=null)return;
   draw.drawTextWithShadow(c.textRenderer,"MATRIX Architect | "+WorldEditBridge.filename,8,8,0xFFFFFF);
   String position=target==null?"Punta un blocco entro 64 m":target.up(height).toShortString();
   draw.drawTextWithShadow(c.textRenderer,position+" | "+WorldEditBridge.turns*90+" gradi | "+(locked?"Punto bloccato":"Segue lo sguardo"),8,21,0x88FFBB);
   draw.drawTextWithShadow(c.textRenderer,"K pannello | clic blocca | R ruota | frecce sposta | PgUp/PgDn altezza | L sblocca | P POSIZIONA | U undo",8,34,0xDDDDDD);
   draw.drawTextWithShadow(c.textRenderer,c.textRenderer.trimToWidth(WorldEditBridge.status,draw.getScaledWindowWidth()-16),8,47,0xFFFF88);
  });
  WorldRenderEvents.AFTER_ENTITIES.register(context->{
   var c=MinecraftClient.getInstance();if(!holding(c)||!WorldEditBridge.ready(c)||target==null)return;
   var matrix=context.matrixStack();if(matrix==null||context.consumers()==null)return;
   Box box=bounds(WorldEditBridge.clipboard,target.up(height),WorldEditBridge.turns);var camera=context.camera().getPos();
   matrix.push();matrix.translate(-camera.x,-camera.y,-camera.z);
   WorldRenderer.drawBox(matrix,context.consumers().getBuffer(RenderLayer.getLines()),box,0.2f,1f,0.5f,1f);matrix.pop();
  });
 }
 static void move(int x,int z){if(target!=null&&!WorldEditBridge.busy){target=target.add(x,0,z);locked=true;}}
 static Box bounds(Clipboard clip,BlockPos anchor,int turns){
  var min=clip.getMinimumPoint();var max=clip.getMaximumPoint();var origin=clip.getOrigin();
  int minX=Integer.MAX_VALUE,minZ=minX,maxX=Integer.MIN_VALUE,maxZ=maxX;
  for(int x:new int[]{min.getBlockX(),max.getBlockX()})for(int z:new int[]{min.getBlockZ(),max.getBlockZ()}){
   int[] r=PlacementMath.rotate(x-origin.getBlockX(),z-origin.getBlockZ(),turns);minX=Math.min(minX,r[0]);maxX=Math.max(maxX,r[0]);minZ=Math.min(minZ,r[1]);maxZ=Math.max(maxZ,r[1]);
  }
  return new Box(anchor.getX()+minX,anchor.getY()+min.getBlockY()-origin.getBlockY(),anchor.getZ()+minZ,
   anchor.getX()+maxX+1,anchor.getY()+max.getBlockY()-origin.getBlockY()+1,anchor.getZ()+maxZ+1);
 }
}
