package it.blueprintbuilder;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import net.minecraft.util.math.BlockPos;
final class BlueprintImport {
 static BuildPlan load(Path file) throws IOException {
  if (!Files.isRegularFile(file) || Files.size(file)>64_000_000L) throw new IOException("File assente o oltre 64 MB");
  try (InputStream raw=Files.newInputStream(file);
       InputStream input=file.toString().endsWith(".gz")?new java.util.zip.GZIPInputStream(raw):raw) {
   byte[] bytes=input.readNBytes(64_000_001);
   if (bytes.length>64_000_000) throw new IOException("Blueprint decompresso oltre 64 MB");
   String json=new String(bytes,StandardCharsets.UTF_8);
   var root=JsonParser.parseString(json).getAsJsonObject();
   ImportContract.validate(root);
   BuildPlan plan=BuildPlan.parseComplete(json,BlockPos.ORIGIN);
   plan.validateSections();
   return plan;
  } catch (RuntimeException ex) { throw new IOException("Blueprint non valido: "+ex.getMessage(),ex); }
 }
}
