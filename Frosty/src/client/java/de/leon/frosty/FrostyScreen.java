package de.leon.frosty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

public final class FrostyScreen extends Screen {
    private enum Page { MODULES, SETTINGS, STATUS }
    private Page page = Page.MODULES;
    private final List<EditBox> fields = new ArrayList<>();
    private String status = "READY";
    private int logScroll;
    public FrostyScreen() { super(Component.literal(FrostyText.sc("Frosty"))); }
    private Component c(String en,String de){return Component.literal(FrostyText.tr(en,de));}

    @Override protected void init() {
        clearWidgets(); fields.clear();
        int left=width/2-175, top=height/2-105;
        addRenderableWidget(Button.builder(c("MODULES","MODULE"),b->open(Page.MODULES)).bounds(left-105,top,96,22).build());
        addRenderableWidget(Button.builder(c("SETTINGS","EINSTELLUNGEN"),b->open(Page.SETTINGS)).bounds(left-105,top+27,96,22).build());
        addRenderableWidget(Button.builder(c("STATUS","STATUS"),b->open(Page.STATUS)).bounds(left-105,top+54,96,22).build());
        addRenderableWidget(Button.builder(c("CLOSE","SCHLIESSEN"),b->onClose()).bounds(left-105,top+81,96,22).build());
        if(page==Page.MODULES){
            moduleButton("SMALL TOTEM POPS","KLEINE TOTEM-POPS",FrostyConfig.smallTotemPops,left,top+4,()->{FrostyConfig.smallTotemPops=!FrostyConfig.smallTotemPops;FrostyConfig.save();rebuildWidgets();});
            moduleButton("FIXED SHIELDS","FIXIERTE SCHILDE",FrostyConfig.fixedShields,left,top+43,()->{FrostyConfig.fixedShields=!FrostyConfig.fixedShields;FrostyConfig.save();rebuildWidgets();});
            moduleButton("SHIELD STATUS","SCHILD-STATUS",FrostyConfig.shieldStatus,left,top+82,()->{FrostyConfig.shieldStatus=!FrostyConfig.shieldStatus;FrostyConfig.save();rebuildWidgets();});
            addRenderableWidget(Button.builder(c("CONFIGURE SHIELD STATUS","SCHILD-STATUS EINSTELLEN"),b->open(Page.SETTINGS)).bounds(left,top+122,350,22).build());
        } else if(page==Page.SETTINGS){
            addRenderableWidget(Button.builder(c("LANGUAGE: "+(FrostyConfig.language.equals("de")?"GERMAN":"ENGLISH"),"SPRACHE: "+(FrostyConfig.language.equals("de")?"DEUTSCH":"ENGLISCH")),b->{FrostyConfig.language=FrostyConfig.language.equals("de")?"en":"de";FrostyConfig.save();rebuildWidgets();}).bounds(left,top+5,350,24).build());
            addRenderableWidget(new FrostySlider(left,top+36,350,20,c("TOTEM SIZE","TOTEM-GROESSE").getString(),FrostyConfig.totemScale,.25,1.5,v->{FrostyConfig.totemScale=v;FrostyConfig.save();}));
            addRenderableWidget(new FrostySlider(left,top+63,350,20,c("HUD SIZE","HUD-GROESSE").getString(),FrostyConfig.hudScale,.5,1.5,v->{FrostyConfig.hudScale=v;FrostyConfig.save();}));
            String[] labels={"BLOCKED COLOR","NOT BLOCKING COLOR","BROKEN COLOR","BLOCKING COLOR"};
            String[] german={"BLOCKIERT-FARBE","BLOCKIERT-NICHT-FARBE","DEFEKT-FARBE","BLOCKEND-FARBE"};
            String[] values={FrostyConfig.blockedColor,FrostyConfig.notBlockingColor,FrostyConfig.brokenColor,FrostyConfig.blockingColor};
            for(int i=0;i<labels.length;i++){
                final int index=i;
                EditBox field=new EditBox(font,left,top+91+i*24,160,20,c(labels[i],german[i]));
                field.setMaxLength(7);field.setValue(values[i]);addRenderableWidget(field);fields.add(field);
                addRenderableWidget(Button.builder(c("APPLY","ANWENDEN"),b->saveColor(index)).bounds(left+168,top+91+i*24,80,20).build());
            }
            addRenderableWidget(Button.builder(c("RESET COLORS","FARBEN ZURUECKSETZEN"),b->{FrostyConfig.blockedColor="#58D7FF";FrostyConfig.notBlockingColor="#A8C7D8";FrostyConfig.brokenColor="#FF667A";FrostyConfig.blockingColor="#FFFFFF";FrostyConfig.save();rebuildWidgets();}).bounds(left+255,top+91,95,20).build());
        } else {
            int errors=FrostyLog.count(FrostyLog.LevelType.ERROR), warnings=FrostyLog.count(FrostyLog.LevelType.WARNING);
            addRenderableWidget(Button.builder(c("CLEAR","LEEREN"),b->{FrostyLog.clear();logScroll=0;rebuildWidgets();}).bounds(left,top+4,82,20).build());
            addRenderableWidget(Button.builder(c("OPEN LOG FOLDER","LOG-ORDNER OEFFNEN"),b->FrostyLog.openLogFolder()).bounds(left+88,top+4,150,20).build());
            addRenderableWidget(Button.builder(c("REFRESH","AKTUALISIEREN"),b->rebuildWidgets()).bounds(left+244,top+4,106,20).build());
            addRenderableWidget(Button.builder(c("OLDER","AELTER"),b->{logScroll=Math.min(Math.max(0,FrostyLog.recent(150).size()-1),logScroll+7);rebuildWidgets();}).bounds(left,top+31,80,20).build());
            addRenderableWidget(Button.builder(c("NEWER","NEUER"),b->{logScroll=Math.max(0,logScroll-7);rebuildWidgets();}).bounds(left+88,top+31,80,20).build());
            addRenderableWidget(Button.builder(c("COPY ALL","ALLE KOPIEREN"),b->{String all=FrostyLog.recent(150).stream().map(e->e.time()+" ["+e.level()+"] "+e.message()).reduce("",(a,v)->a+v+"\n");Minecraft.getInstance().keyboardHandler.setClipboard(all);status="LOG COPIED";}).bounds(left+176,top+31,120,20).build());
            status="ERRORS: "+errors+"  |  WARNINGS: "+warnings+"  |  ENTRIES: "+FrostyLog.recent(150).size();
        }
    }
    private void moduleButton(String en,String de,boolean enabled,int x,int y,Runnable action){
        addRenderableWidget(Button.builder(c(en+"   "+(enabled?"ON":"OFF"),de+"   "+(enabled?"AN":"AUS")),b->action.run()).bounds(x,y,350,30).build());
    }
    private void saveColor(int i){
        String value=fields.get(i).getValue().trim().toUpperCase();
        if(!value.matches("#[0-9A-F]{6}")){status="USE HEX FORMAT: #58D7FF";return;}
        switch(i){case 0->FrostyConfig.blockedColor=value;case 1->FrostyConfig.notBlockingColor=value;case 2->FrostyConfig.brokenColor=value;case 3->FrostyConfig.blockingColor=value;}
        FrostyConfig.save();status="COLOR SAVED";
    }
    private void open(Page next){page=next;logScroll=0;status="READY";rebuildWidgets();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        g.fill(0,0,width,height,0xD905101C);
        int x=width/2-175,y=height/2-112;
        g.fill(x-112,y-9,x+365,y+205,0xEE0A2032);g.fill(x-112,y-9,x+365,y-7,0xFF58D7FF);g.fill(x-101,y+3,x-99,y+79,0xFF58D7FF);
        super.extractRenderState(g,mouseX,mouseY,delta);
        g.text(font,Component.literal(FrostyText.sc("FROSTY")),x-100,y-1,0xFFFFFFFF);
        String heading=switch(page){case MODULES->FrostyText.tr("CLIENT MODULES","CLIENT-MODULE");case SETTINGS->FrostyText.tr("SETTINGS","EINSTELLUNGEN");case STATUS->FrostyText.tr("SYSTEM STATUS","SYSTEM-STATUS");};
        g.text(font,Component.literal(heading),x,y-1,0xFF9DEBFF);
        if(page==Page.STATUS){
            List<FrostyLog.Entry> entries=FrostyLog.recent(150);
            int first=Math.min(logScroll,Math.max(0,entries.size()-1));
            for(int i=first;i<Math.min(entries.size(),first+7);i++){
                FrostyLog.Entry e=entries.get(i);
                int color=e.level()==FrostyLog.LevelType.ERROR?0xFFFF7185:e.level()==FrostyLog.LevelType.WARNING?0xFFFFD27A:0xFF9DEBFF;
                String line=e.time()+" ["+e.level()+"] "+e.message().replace('\n',' ');
                if(line.length()>67)line=line.substring(0,64)+"...";
                g.text(font,Component.literal(FrostyText.sc(line)),x,y+63+(i-first)*15,color);
            }
            if(entries.isEmpty())g.text(font,c("NO LOG ENTRIES YET","NOCH KEINE LOG-EINTRAEGE"),x,y+72,0xFF9DEBFF);
        }
        g.text(font,Component.literal(FrostyText.sc(status)),x,y+191,0xFF78BBD2);
    }
    private static final class FrostySlider extends AbstractSliderButton {
        private final String label;private final double min,max;private final java.util.function.DoubleConsumer changed;
        FrostySlider(int x,int y,int w,int h,String label,double value,double min,double max,java.util.function.DoubleConsumer changed){
            super(x,y,w,h,Component.empty(),(value-min)/(max-min));this.label=label;this.min=min;this.max=max;this.changed=changed;updateMessage();
        }
        @Override protected void updateMessage(){setMessage(Component.literal(FrostyText.sc(label+"  "+Math.round((min+value*(max-min))*100)+"%")));}
        @Override protected void applyValue(){changed.accept(min+value*(max-min));}
    }
}
