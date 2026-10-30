package it.matrixarchitect;
/** No arbitrary volume cap: capacity depends on the Java heap, not scale/detail reduction.
 * This reserve check is not a prediction of WorldEdit's peak allocation or undo size. */
final class SchematicCapacity {
 static final long MIB=1024L*1024;
 static long availableMiB(long maximum,long used){return Math.max(0,maximum-used)/MIB;}
 static void validate(long volume,long maximum,long used){
  if(volume<1)throw new IllegalArgumentException("Schematic con dimensioni non valide");
  long reserve=Math.max(128*MIB,maximum/10);
  if(maximum-used<reserve)throw new IllegalStateException("RAM Java disponibile insufficiente: "+availableMiB(maximum,used)+" MiB. Aumenta la memoria dell'istanza Prism e riavvia. Nessun ridimensionamento applicato.");
 }
}
