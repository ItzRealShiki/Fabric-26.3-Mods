package de.leon.frosty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FrostyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("frosty.json");
    public static boolean smallTotemPops = true, fixedShields = true, shieldStatus = true;
    public static double totemScale = 0.50, hudScale = 1.00;
    public static int shieldX = 12, shieldY = 12;
    public static String blockedColor = "#58D7FF", notBlockingColor = "#A8C7D8", brokenColor = "#FF667A", blockingColor = "#FFFFFF";
    public static String language = "en";
    private FrostyConfig() {}
    public static void load() {
        if (!Files.exists(FILE)) { save(); return; }
        try {
            Data d = GSON.fromJson(Files.readString(FILE), Data.class);
            if (d == null) return;
            smallTotemPops=d.smallTotemPops; fixedShields=d.fixedShields; shieldStatus=d.shieldStatus;
            totemScale=clamp(d.totemScale,.25,1.5); hudScale=clamp(d.hudScale,.5,2.0);
            shieldX=d.shieldX; shieldY=d.shieldY;
            blockedColor=validColor(d.blockedColor,"#58D7FF"); notBlockingColor=validColor(d.notBlockingColor,"#A8C7D8");
            brokenColor=validColor(d.brokenColor,"#FF667A"); blockingColor=validColor(d.blockingColor,"#FFFFFF");
            language="de".equalsIgnoreCase(d.language) ? "de" : "en";
        } catch (Exception ex) { FrostyLog.add(FrostyLog.LevelType.WARNING,"Could not read config: "+ex.getMessage()); }
    }
    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data d=new Data(); d.smallTotemPops=smallTotemPops; d.fixedShields=fixedShields; d.shieldStatus=shieldStatus;
            d.totemScale=totemScale; d.hudScale=hudScale; d.shieldX=shieldX; d.shieldY=shieldY;
            d.blockedColor=blockedColor; d.notBlockingColor=notBlockingColor; d.brokenColor=brokenColor; d.blockingColor=blockingColor; d.language=language;
            Files.writeString(FILE,GSON.toJson(d));
        } catch(IOException ex) { FrostyLog.add(FrostyLog.LevelType.ERROR,"Could not save config: "+ex.getMessage()); }
    }
    static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private static String validColor(String v,String fallback){return v!=null&&v.matches("#[0-9a-fA-F]{6}")?v.toUpperCase():fallback;}
    static int color(String hex,int fallback){try{return 0xFF000000|Integer.parseInt(hex.substring(1),16);}catch(Exception ex){return fallback;}}
    private static final class Data {
        boolean smallTotemPops=true,fixedShields=true,shieldStatus=true;
        double totemScale=.5,hudScale=1.0; int shieldX=12,shieldY=12;
        String blockedColor="#58D7FF",notBlockingColor="#A8C7D8",brokenColor="#FF667A",blockingColor="#FFFFFF",language="en";
    }
}
