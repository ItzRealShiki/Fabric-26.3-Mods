package de.leon.frosty;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class FrostyPackManager {
    private FrostyPackManager() {}

    public static List<Pack> packs() {
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        return repository.getAvailablePacks().stream()
            .filter(pack -> !pack.isHidden())
            .sorted(Comparator.comparing(pack -> pack.getTitle().getString(), String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public static boolean enabled(Pack pack) {
        return Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains(pack.getId());
    }

    public static boolean canDelete(Pack pack) {
        if (pack.isRequired() || !pack.getId().startsWith("file/")) return false;
        Path root = Minecraft.getInstance().getResourcePackDirectory().toAbsolutePath().normalize();
        Path target = root.resolve(pack.getId().substring("file/".length())).normalize();
        return target.startsWith(root) && !target.equals(root) && Files.exists(target);
    }

    public static void toggle(Pack pack) {
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        List<String> selected = new ArrayList<>(repository.getSelectedIds());
        if (selected.contains(pack.getId())) selected.remove(pack.getId());
        else selected.add(pack.getId());
        repository.setSelected(selected);
        Minecraft.getInstance().options.save();
        Minecraft.getInstance().reloadResourcePacks();
    }

    public static void move(Pack pack, int direction) {
        PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
        List<String> selected = new ArrayList<>(repository.getSelectedIds());
        int index = selected.indexOf(pack.getId());
        int next = index + direction;
        if (index < 0 || next < 0 || next >= selected.size()) return;
        String value = selected.remove(index);
        selected.add(next, value);
        repository.setSelected(selected);
        Minecraft.getInstance().options.save();
        Minecraft.getInstance().reloadResourcePacks();
    }

    public static boolean delete(Pack pack) {
        if (!canDelete(pack) || enabled(pack)) return false;
        Path root = Minecraft.getInstance().getResourcePackDirectory().toAbsolutePath().normalize();
        Path target = root.resolve(pack.getId().substring("file/".length())).normalize();
        if (!target.startsWith(root) || target.equals(root)) return false;
        try {
            if (Files.isDirectory(target)) {
                try (var paths = Files.walk(target)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                }
            } else {
                Files.deleteIfExists(target);
            }
            Minecraft.getInstance().getResourcePackRepository().reload();
            return true;
        } catch (IOException exception) {
            FrostyLog.add(FrostyLog.LevelType.ERROR, "Could not delete resource pack " + pack.getId() + ": " + exception.getMessage());
            return false;
        }
    }
}
