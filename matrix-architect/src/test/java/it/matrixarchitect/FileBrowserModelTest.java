package it.matrixarchitect;
import java.nio.file.*;
public final class FileBrowserModelTest {
 public static void main(String[] args)throws Exception{
  Path dir=Files.createTempDirectory("matrix-picker-test");
  try{
   Files.createDirectory(dir.resolve("Cartella con spazi"));Files.writeString(dir.resolve("colosseo.schem"),"");Files.writeString(dir.resolve("A.SCHEMATIC"),"");Files.writeString(dir.resolve("note.txt"),"");
   var entries=FileBrowserModel.list(dir);
   if(entries.size()!=3||!entries.get(0).directory())throw new AssertionError("Folders/filter/order wrong");
   if(!entries.get(1).path().getFileName().toString().equals("A.SCHEMATIC"))throw new AssertionError("Case-insensitive sort wrong");
   try{FileBrowserModel.list(dir.resolve("absent"));throw new AssertionError("Missing directory accepted");}catch(java.io.IOException expected){}
   if(FileBrowserModel.list(null).isEmpty())throw new AssertionError("No drives");
   System.out.println("In-game file browser model checks passed");
  }finally{try(var stream=Files.walk(dir)){for(Path p:stream.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(p);}}
 }
}
