package it.blueprintbuilder;
import com.google.gson.JsonParser;
public final class ImportContractTest{
 public static void main(String[] args){
  check("{\"format_version\":1,\"minecraft_version\":\"1.20.1\",\"blocks\":[]}");
  reject("{\"format_version\":2}");reject("{\"minecraft_version\":\"1.21\"}");reject("{\"entities\":[]}");reject("{\"blocks\":[{\"nbt\":{}}]}");reject("{\"blocks\":null}");
  try{BlueprintFields.requiredInt(JsonParser.parseString("{}").getAsJsonObject(),"x");throw new AssertionError();}catch(IllegalArgumentException expected){}
  System.out.println("Import contract checks passed");
 }
 static void check(String s){ImportContract.validate(JsonParser.parseString(s).getAsJsonObject());}
 static void reject(String s){try{check(s);}catch(RuntimeException expected){return;}throw new AssertionError(s);}
}
