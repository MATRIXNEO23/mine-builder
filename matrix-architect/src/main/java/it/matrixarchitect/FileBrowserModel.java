package it.matrixarchitect;
import java.nio.file.*;
import java.util.*;
/** Filesystem-only model, independent of AWT and Minecraft. */
final class FileBrowserModel {
 record Entry(Path path,boolean directory){}
 static boolean schematic(Path p){String n=p.getFileName().toString().toLowerCase(Locale.ROOT);return n.endsWith(".schem")||n.endsWith(".schematic");}
 static List<Entry> list(Path directory)throws java.io.IOException{
  if(directory==null)return Arrays.stream(java.io.File.listRoots()).map(f->new Entry(f.toPath(),true)).toList();
  try(var files=Files.list(directory)){
   return files.map(p->new Entry(p,Files.isDirectory(p)))
    .filter(e->e.directory()||schematic(e.path()))
    .sorted(Comparator.comparing((Entry e)->!e.directory()).thenComparing(e->e.path().getFileName().toString().toLowerCase(Locale.ROOT)))
    .toList();
  }
 }
}
