package it.matrixarchitect;
import java.util.function.IntPredicate;
final class KeyChoice {
 static int choose(int preferred,int[] alternatives,IntPredicate occupied){
  if(!occupied.test(preferred))return preferred;
  for(int key:alternatives)if(!occupied.test(key))return key;
  return -1;
 }
}
