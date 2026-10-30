package it.blueprintbuilder;
/** Runs registry tests only when explicitly enabled in the Fabric test launcher. */
public final class RegistryTestEntrypoint implements net.fabricmc.api.ModInitializer {
 public void onInitialize() {
  if (!Boolean.getBoolean("blueprintbuilder.registry-test")) return;
  try { GeometrySmokeTest.main(new String[0]); System.exit(0); }
  catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
 }
}
