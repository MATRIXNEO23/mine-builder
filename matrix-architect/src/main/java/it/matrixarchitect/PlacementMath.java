package it.matrixarchitect;
final class PlacementMath {
 static int[] rotate(int x,int z,int turns){for(int i=0;i<Math.floorMod(turns,4);i++){int t=x;x=-z;z=t;}return new int[]{x,z};}
}
