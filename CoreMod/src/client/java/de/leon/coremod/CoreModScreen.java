package de.leon.coremod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class CoreModScreen extends Screen {
    private enum Page { HOME, SETTINGS, META, FARM, SCHEMATICS, SCOREBOARD }
    private Page page = Page.HOME;
    private final List<EditBox> fields = new ArrayList<>();
    private String status = "READY";
    public CoreModScreen() { super(Component.literal(CoreModText.smallCaps("CoreMod"))); }

    @Override protected void init() {
        clearWidgets(); fields.clear();
        int x = width / 2 - 150, y = height / 2 - 112;
        if (page == Page.HOME) {
            add("DONUT SMP META",x,y,()->open(Page.META)); add("FARM FINDER",x,y+25,()->open(Page.FARM));
            add("SCHEMATIC CATALOG",x,y+50,()->open(Page.SCHEMATICS)); add("CUSTOM SCOREBOARD",x,y+75,()->open(Page.SCOREBOARD));
            add("SETTINGS",x,y+100,()->open(Page.SETTINGS)); add("FULLBRIGHT: "+(CoreModConfig.fullbrightEnabled?"ON":"OFF"),x,y+125,()->{CoreModConfig.fullbrightEnabled=!CoreModConfig.fullbrightEnabled;CoreModConfig.save();rebuildWidgets();}); add("CLOSE",x,y+150,this::onClose);
        } else if (page == Page.SETTINGS) {
            for(int i=0;i<4;i++){ final int slot=i; EditBox f=field(x,y+i*27,300,CoreModConfig.quickCommands[i],"COMMAND");
                add("SAVE SLOT "+(i+1),x+306,y+i*27,()->{CoreModConfig.quickCommands[slot]=f.getValue().trim();CoreModConfig.save();status="COMMAND SAVED";});}
            add("BUTTON SIDE: "+CoreModConfig.quickButtonSide,x,y+116,()->{CoreModConfig.quickButtonSide=CoreModConfig.quickButtonSide.equals("LEFT")?"RIGHT":"LEFT";CoreModConfig.save();rebuildWidgets();});
            add("BACK",x,y+143,()->open(Page.HOME));
        } else if(page==Page.META){
            field(x,y,300,CoreModConfig.apiKey,"DONUTSMP API KEY");
            add("SAVE KEY + REFRESH",x,y+25,()->{CoreModConfig.apiKey=fields.get(0).getValue().trim();CoreModConfig.save();CoreModMarket.refresh(CoreModConfig.apiKey);status="REQUEST SENT";});
            add("REFRESH",x+160,y+25,()->CoreModMarket.refresh(CoreModConfig.apiKey)); add("BACK",x,y+150,()->open(Page.HOME));
        } else if(page==Page.FARM){
            field(x,y,135,String.valueOf(CoreModConfig.balance),"BALANCE");
            field(x+145,y,135,String.valueOf(CoreModConfig.farmBudget),"BUDGET");
            field(x,y+28,135,String.valueOf(CoreModConfig.farmCost),"BUILD COST");
            field(x+145,y+28,135,String.valueOf(CoreModConfig.farmRevenue),"EST. REVENUE");
            add("CALCULATE",x,y+58,()->calculateFarm()); add("BACK",x,y+150,()->open(Page.HOME));
        } else if(page==Page.SCHEMATICS){
            field(x,y,300,"","SCHEMATIC NAME"); field(x,y+27,300,"","HTTPS DOWNLOAD / INFO LINK"); field(x,y+54,300,"","MATERIAL LIST");
            add("SAVE ENTRY",x,y+82,()->saveSchematic());
            add("COPY SAVED ENTRIES",x+160,y+82,()->{MinecraftClipboard.copy(String.join("\n",CoreModConfig.schematicEntries));status="CATALOG COPIED";});
            add("BACK",x,y+150,()->open(Page.HOME));
        } else if(page==Page.SCOREBOARD){
            field(x,y,300,CoreModConfig.scoreboardTitle,"SCOREBOARD TITLE");
            for(int i=0;i<Math.min(4,CoreModConfig.scoreboardLines.length);i++)field(x,y+27+i*25,300,CoreModConfig.scoreboardLines[i],"SCOREBOARD LINE");
            add("SAVE + "+(CoreModConfig.customScoreboardEnabled?"HIDE":"SHOW"),x,y+130,()->saveScoreboard());
            add("BACK",x+160,y+130,()->open(Page.HOME));
        }
    }
    private EditBox field(int x,int y,int w,String value,String hint){
        EditBox b=new EditBox(font,x,y,w,20,Component.literal(CoreModText.smallCaps(hint)));
        b.setMaxLength(256);b.setValue(value);addRenderableWidget(b);fields.add(b);return b;
    }
    private void add(String label,int x,int y,Runnable action){addRenderableWidget(Button.builder(Component.literal(CoreModText.smallCaps(label)),b->action.run()).bounds(x,y,150,20).build());}
    private void open(Page p){page=p;status="READY";rebuildWidgets();}
    private void calculateFarm(){
        try{CoreModConfig.balance=Double.parseDouble(fields.get(0).getValue());CoreModConfig.farmBudget=Double.parseDouble(fields.get(1).getValue());
            CoreModConfig.farmCost=Double.parseDouble(fields.get(2).getValue());CoreModConfig.farmRevenue=Double.parseDouble(fields.get(3).getValue());CoreModConfig.save();
            status="PROFIT: "+String.format("%.2f",CoreModConfig.farmRevenue-CoreModConfig.farmCost)+" | "+(CoreModConfig.balance>=CoreModConfig.farmCost?"BALANCE OK":"LOW BALANCE");
        }catch(NumberFormatException e){status="ENTER VALID NUMBERS";}}
    private void saveSchematic(){
        String n=fields.get(0).getValue().trim(),u=fields.get(1).getValue().trim(),m=fields.get(2).getValue().trim();
        if(n.isEmpty()||!u.startsWith("https://")){status="NAME AND HTTPS LINK REQUIRED";return;}
        List<String> all=new ArrayList<>(List.of(CoreModConfig.schematicEntries));all.add(n+" | "+u+" | "+m);
        CoreModConfig.schematicEntries=all.toArray(String[]::new);CoreModConfig.save();status="SCHEMATIC ENTRY SAVED";
    }
    private void saveScoreboard(){
        CoreModConfig.scoreboardTitle=fields.get(0).getValue();String[] lines=new String[fields.size()-1];
        for(int i=1;i<fields.size();i++)lines[i-1]=fields.get(i).getValue();
        CoreModConfig.scoreboardLines=lines;CoreModConfig.customScoreboardEnabled=!CoreModConfig.customScoreboardEnabled;
        CoreModConfig.save();status="SCOREBOARD SAVED";rebuildWidgets();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        g.fill(width/2-210,height/2-155,width/2+210,height/2+155,0xF02B293B);\n        g.fill(width/2-205,height/2-150,width/2+205,height/2+150,0xE51B1A29);\n        super.extractRenderState(g,mx,my,d);
        g.centeredText(font,title,width/2,height/2-145,0xFFFFFFFF);
        String sub=switch(page){case HOME->"CLIENT TOOLS";case SETTINGS->"QUICK COMMAND SETTINGS";case META->"AUCTION HOUSE DATA - API KEY REQUIRED";case FARM->"FARM COST / PROFIT CALCULATOR";case SCHEMATICS->"PERSONAL SCHEMATIC CATALOG";case SCOREBOARD->"CUSTOM HUD OVERLAY";};
        g.centeredText(font,Component.literal(CoreModText.smallCaps(sub)),width/2,height/2-130,0xFFD2B8FF);
        if(page==Page.META){int yy=height/2-35;g.text(font,Component.literal(CoreModText.smallCaps("LAST UPDATE: "+CoreModConfig.marketUpdated)),width/2-145,yy,0xFFAAAAAA);
            for(String line:CoreModConfig.marketSummary.split("\n")){yy+=12;if(yy>height/2+95)break;g.text(font,Component.literal(CoreModText.smallCaps(line)),width/2-145,yy,0xFFFFFFFF);}}
        if(page==Page.SCHEMATICS){int yy=height/2+15;for(String entry:CoreModConfig.schematicEntries){if(yy>height/2+110)break;g.text(font,Component.literal(CoreModText.smallCaps(entry)),width/2-145,yy,0xFFDDDDDD);yy+=11;}}
        g.centeredText(font,Component.literal(CoreModText.smallCaps(status)),width/2,height/2+140,0xFFE0D9F2);
    }
    private static final class MinecraftClipboard { static void copy(String s){net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(s);} }
}