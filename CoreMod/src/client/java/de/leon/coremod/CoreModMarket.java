package de.leon.coremod;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class CoreModMarket {
 private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
 private CoreModMarket(){}
 public static void refresh(String key){
  if(key==null||key.isBlank()){CoreModClient.message("ADD YOUR API KEY FIRST");return;}
  CoreModClient.message("REFRESHING MARKET DATA...");
  Thread.startVirtualThread(()->{try{
   HttpRequest req=HttpRequest.newBuilder().uri(URI.create("https://api.donutsmp.net/v1/auction/list/1")).timeout(Duration.ofSeconds(15))
    .header("Authorization","Bearer "+key.trim()).header("Accept","application/json").GET().build();
   HttpResponse<String> res=HTTP.send(req,HttpResponse.BodyHandlers.ofString());
   if(res.statusCode()!=200)throw new IllegalStateException("HTTP "+res.statusCode());
   JsonObject root=JsonParser.parseString(res.body()).getAsJsonObject();
   JsonArray arr=root.has("result")&&root.get("result").isJsonArray()?root.getAsJsonArray("result"):
     root.has("data")&&root.get("data").isJsonArray()?root.getAsJsonArray("data"):new JsonArray();
   StringBuilder out=new StringBuilder();int count=0;
   for(JsonElement e:arr){if(e==null||!e.isJsonObject())continue;JsonObject o=e.getAsJsonObject();
    String name=first(o,"display_name","name","item"),price=first(o,"price","buy_price","amount");
    if(name.isBlank())continue;out.append(name).append(" | ").append(price.isBlank()?"PRICE N/A":price).append("\n");if(++count==12)break;}
   if(count==0)out.append("NO LISTINGS COULD BE PARSED");
   String text=out.toString().trim(),time=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
   Minecraft.getInstance().execute(()->{CoreModConfig.marketSummary=text;CoreModConfig.marketUpdated=time;CoreModConfig.save();CoreModClient.message("MARKET CACHE UPDATED");});
  }catch(Exception ex){Minecraft.getInstance().execute(()->CoreModClient.message("REFRESH FAILED - OLD CACHE KEPT"));}});
 }
 private static String first(JsonObject o,String...keys){for(String k:keys)if(o.has(k)&&!o.get(k).isJsonNull())try{return o.get(k).getAsString();}catch(Exception ignored){}return "";}
}