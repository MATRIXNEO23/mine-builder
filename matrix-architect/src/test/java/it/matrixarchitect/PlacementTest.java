package it.matrixarchitect;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
public final class PlacementTest{
 public static void main(String[] args){
  for(int turns=0;turns<4;turns++)for(int x:new int[]{-30,0,7})for(int z:new int[]{-4,0,21}){
   int[] preview=PlacementMath.rotate(x,z,turns);
   var actual=new AffineTransform().rotateY(-90*turns).apply(Vector3.at(x,0,z));
   if(Math.round(actual.getX())!=preview[0]||Math.round(actual.getZ())!=preview[1])throw new AssertionError("Preview/paste rotation mismatch");
  }
  System.out.println("Preview rotation matches WorldEdit transform for all quarter turns");
 }
}
