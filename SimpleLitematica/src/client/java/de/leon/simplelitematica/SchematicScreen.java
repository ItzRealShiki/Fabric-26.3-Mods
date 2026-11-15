package de.leon.simplelitematica;

import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

public final class SchematicScreen extends Screen {
    private enum View { MAIN, LOAD, DELETE }
    private static final int PAGE_SIZE = 7;
    private View view = View.MAIN;
    private int page;
    private Path deleteCandidate;
    private List<Path> files = List.of();

    public SchematicScreen() {
        super(Component.literal(SchematicManager.smallCaps("SimpleLitematica")));
    }

    @Override
    protected void init() {
        refresh();
        int left = width / 2 - 150;
        int top = height / 2 - 135;

        if (view == View.LOAD || view == View.DELETE) {
            addRenderableWidget(Button.builder(label("BACK"), b -> {
                view = View.MAIN; deleteCandidate = null; page = 0; rebuildWidgets();
            }).bounds(left, top + 226, 300, 20).build());

            int start = page * PAGE_SIZE;
            int end = Math.min(files.size(), start + PAGE_SIZE);
            for (int i = start; i < end; i++) {
                Path file = files.get(i);
                String name = file.getFileName().toString();
                boolean chosen = file.equals(deleteCandidate);
                String prefix = chosen ? "> " : "";
                addRenderableWidget(Button.builder(label(prefix + name), b -> {
                    if (view == View.LOAD) {
                        if (SchematicManager.load(file)) {
                            showMessage("Schematic loaded: " + name);
                            view = View.MAIN; page = 0;
                        } else showMessage("Import failed. Check the game log for the format error.");
                    } else deleteCandidate = file;
                    rebuildWidgets();
                }).bounds(left, top + 18 + (i-start)*22, 300, 20).build());
            }

            if (files.size() > PAGE_SIZE) {
                addRenderableWidget(Button.builder(label("PREVIOUS"), b -> { page=Math.max(0,page-1); rebuildWidgets(); })
                    .bounds(left, top+174, 145, 20).build());
                addRenderableWidget(Button.builder(label("NEXT"), b -> { page=Math.min((files.size()-1)/PAGE_SIZE,page+1); rebuildWidgets(); })
                    .bounds(left+155, top+174, 145, 20).build());
            }
            if (view == View.DELETE) {
                addRenderableWidget(Button.builder(label("CONFIRM DELETE"), b -> {
                    if (deleteCandidate == null) { showMessage("Select a file first."); return; }
                    String name=deleteCandidate.getFileName().toString();
                    boolean deleted=SchematicManager.delete(deleteCandidate);
                    deleteCandidate=null; refresh();
                    page=Math.min(page,Math.max(0,(files.size()-1)/PAGE_SIZE));
                    rebuildWidgets();
                    showMessage(deleted ? "Deleted: "+name : "Could not delete: "+name);
                }).bounds(left, top+199, 300, 20).build());
            }
            return;
        }

        add("LOAD", left, top+18, 145, 20, b -> { view=View.LOAD; page=0; refresh(); rebuildWidgets(); });
        add("DELETE", left+155, top+18, 145, 20, b -> { view=View.DELETE; page=0; deleteCandidate=null; refresh(); rebuildWidgets(); });
        add("UNLOAD", left, top+45, 145, 20, b -> showMessage(SchematicManager.unload() ? "Schematic unloaded. File kept." : "Nothing is loaded."));
        add("PASTE", left+155, top+45, 145, 20, b -> SchematicManager.print(false));
        add(SchematicManager.layerMode() ? "MODE: LAYER" : "MODE: ALL", left, top+72, 145, 20, b -> { SchematicManager.toggleLayerMode(); rebuildWidgets(); });
        add("NEXT LAYER", left+155, top+72, 145, 20, b -> { SchematicManager.cycleLayer(); rebuildWidgets(); });
        add(SchematicManager.missingBlocksEnabled() ? "MISSING: ON" : "MISSING: OFF", left, top+99, 145, 20,
            b -> { SchematicManager.toggleMissingBlocks(); rebuildWidgets(); });
        add("OPEN FOLDER", left+155, top+99, 145, 20, b -> {
            try { Blaze3D.openPath(SchematicManager.directory()); }
            catch (Exception e) { showMessage("Could not open the schematic folder."); }
        });
        add("X -1", left, top+126, 95, 20, b -> SchematicManager.move(-1,0,0));
        add("X +1", left+105, top+126, 95, 20, b -> SchematicManager.move(1,0,0));
        add("Y -1", left, top+153, 95, 20, b -> SchematicManager.move(0,-1,0));
        add("Y +1", left+105, top+153, 95, 20, b -> SchematicManager.move(0,1,0));
        add("Z -1", left, top+180, 95, 20, b -> SchematicManager.move(0,0,-1));
        add("Z +1", left+105, top+180, 95, 20, b -> SchematicManager.move(0,0,1));
        add("ROTATE 90", left+205, top+126, 95, 20, b -> { SchematicManager.rotate(); rebuildWidgets(); });
        add("CLOSE", left+205, top+153, 95, 20, b -> onClose());
    }

    private void add(String text, int x, int y, int w, int h, java.util.function.Consumer<Button> action) {
        addRenderableWidget(Button.builder(label(text), action::accept).bounds(x,y,w,h).build());
    }

    private static Component label(String value) {
        return Component.literal(SchematicManager.smallCaps(value));
    }

    private void showMessage(String message) {
        Minecraft mc=Minecraft.getInstance();
        if (mc.player!=null) mc.player.sendSystemMessage(label(message));
    }

    private void refresh() { files=SchematicManager.files(); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics,mouseX,mouseY,delta);
        int top=height/2-135;
        graphics.centeredText(font,title,width/2,top-16,0xFFFFFFFF);
        if (view==View.LOAD || view==View.DELETE) {
            graphics.centeredText(font,label(view==View.LOAD ? "SELECT A SCHEMATIC" : "SELECT A FILE TO DELETE"),
                width/2,top-2,0xFFAAAAAA);
            if(files.isEmpty()) graphics.centeredText(font,label("NO SUPPORTED FILES FOUND"),
                width/2,top+75,0xFFAAAAAA);
            graphics.centeredText(font,label("PAGE "+(page+1)+" / "+Math.max(1,(files.size()+PAGE_SIZE-1)/PAGE_SIZE)),
                width/2,top+178,0xFFAAAAAA);
        } else {
            SchematicData data=SchematicManager.active();
            String active=data==null ? "NO SCHEMATIC LOADED" : "LOADED: "+data.name+"  ("+data.blocks.size()+" BLOCKS)";
            graphics.centeredText(font,label(active),width/2,top-2,0xFFFFFFFF);
            graphics.centeredText(font,label("X "+SchematicManager.offsetX()+"  Y "+SchematicManager.offsetY()+
                "  Z "+SchematicManager.offsetZ()+"  ROT "+(SchematicManager.rotation()*90)+" DEG"),
                width/2,top+207,0xFFAAAAAA);
            graphics.centeredText(font,label("SHIFT + M: MENU | PASTE ONLY WHEN PRESSED"),
                width/2,top+220,0xFFAAAAAA);
        }
    }
}
