package de.leon.coremod;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
public final class CoreModHud {
 private CoreModHud(){}
 public static void register(){
  HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("coremod","custom_scoreboard"),(g,delta)->{
   if(!CoreModConfig.customScoreboardEnabled)return;
   var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.options.hideGui)return;
   int x=g.guiWidth()-160,y=12,h=22+CoreModConfig.scoreboardLines.length*11;
   g.fill(x-7,y-5,g.guiWidth()-7,y+h,0x990D0B18);
   g.centeredText(mc.font,Component.literal(CoreModText.smallCaps(CoreModConfig.scoreboardTitle)),x+73,y,0xFFBDA0FF);
   y+=17;
   for(String line:CoreModConfig.scoreboardLines){String v=line.replace("%player%",mc.player.getName().getString()).replace("%balance%",String.format("%.2f",CoreModConfig.balance)).replace("%ping%","N/A");
    g.text(mc.font,Component.literal(CoreModText.smallCaps(v)),x,y,0xFFFFFFFF);y+=11;}
  });
 }
}