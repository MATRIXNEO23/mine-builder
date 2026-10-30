package it.matrixarchitect;
public final class KeyChoiceTest {
 public static void main(String[] args){
  if(KeyChoice.choose(10,new int[]{6,7},key->false)!=10)throw new AssertionError();
  if(KeyChoice.choose(10,new int[]{6,7},key->key==10||key==6)!=7)throw new AssertionError("Occupied key chosen");
  if(KeyChoice.choose(10,new int[]{6,7},key->true)!=-1)throw new AssertionError("Conflict must leave unbound");
  System.out.println("Conflict-free key selection checks passed");
 }
}
