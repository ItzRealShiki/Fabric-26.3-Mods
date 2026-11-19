package de.leon.packmanager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads optional creator metadata from resource packs. Compatibility metadata
 * (pack.mcmeta / pack format) is deliberately never used for update decisions.
 */
public final class PackManagerService {
    private static final AtomicInteger INFO_INDEX = new AtomicInteger();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private PackManagerService() {
    }

    public record PackInfo(String displayName, String projectId, String version, String manifestUrl) {
    }

    public record UpdateResult(String translationKey, int count) {
    }

    public static int nextInfoIndex() {
        return INFO_INDEX.getAndIncrement();
    }

    public static List<PackInfo> scanInstalledPacks(Minecraft client) {
        Path directory = client.gameDirectory.toPath().resolve("resourcepacks");
        List<PackInfo> result = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return result;
        }

        try (var paths = Files.list(directory)) {
            paths.sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(path -> readMetadata(path).ifPresent(result::add));
        } catch (Exception ignored) {
            // A missing or unreadable pack must not prevent the screen from opening.
        }
        return result;
    }

    private static java.util.Optional<PackInfo> readMetadata(Path packPath) {
        try {
            JsonObject json;
            if (Files.isDirectory(packPath)) {
                Path metadata = packPath.resolve("packmanager.json");
                if (!Files.isRegularFile(metadata)) return java.util.Optional.empty();
                try (InputStream input = Files.newInputStream(metadata);
                     InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                }
            } else if (packPath.getFileName().toString().toLowerCase().endsWith(".zip")) {
                try (ZipFile zip = new ZipFile(packPath.toFile())) {
                    ZipEntry entry = zip.getEntry("packmanager.json");
                    if (entry == null) return java.util.Optional.empty();
                    try (InputStream input = zip.getInputStream(entry);
                         InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                        json = JsonParser.parseReader(reader).getAsJsonObject();
                    }
                }
            } else {
                return java.util.Optional.empty();
            }

            if (json.get("schemaVersion") == null || json.get("schemaVersion").getAsInt() != 1) {
                return java.util.Optional.empty();
            }
            String projectId = requiredString(json, "projectId");
            String version = requiredString(json, "version");
            String manifest = requiredString(json, "updateManifest");
            if (projectId == null || version == null || manifest == null) return java.util.Optional.empty();
            URI uri = URI.create(manifest);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new PackInfo(
                    packPath.getFileName().toString(), projectId, version, uri.toString()));
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private static String requiredString(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonPrimitive()
                ? json.get(key).getAsString().trim() : null;
    }

    public static void checkUpdates(Minecraft client, Consumer<UpdateResult> callback) {
        Thread.ofVirtual().name("PackManager-update-check").start(() -> {
            List<PackInfo> packs = scanInstalledPacks(client);
            if (packs.isEmpty()) {
                client.execute(() -> callback.accept(new UpdateResult("screen.packmanager.no_metadata", 0)));
                return;
            }

            int updates = 0;
            int unknown = 0;
            for (PackInfo pack : packs) {
                try {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(pack.manifestUrl()))
                            .timeout(Duration.ofSeconds(8))
                            .header("Accept", "application/json")
                            .GET()
                            .build();
                    HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    try (InputStream input = response.body();
                         InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            unknown++;
                            continue;
                        }
                        JsonObject manifest = JsonParser.parseReader(reader).getAsJsonObject();
                        if (manifest.get("schemaVersion") == null || manifest.get("schemaVersion").getAsInt() != 1
                                || !manifest.has("projectId")
                                || !pack.projectId().equals(manifest.get("projectId").getAsString())
                                || !manifest.has("latestVersion")) {
                            unknown++;
                            continue;
                        }
                        String latest = manifest.get("latestVersion").getAsString();
                        if (isNewer(latest, pack.version())) updates++;
                    }
                } catch (Exception ignored) {
                    unknown++;
                }
            }

            final int available = updates;
            final int unavailable = unknown;
            client.execute(() -> {
                if (available > 0) {
                    callback.accept(new UpdateResult("screen.packmanager.updates_found", available));
                } else if (unavailable > 0) {
                    callback.accept(new UpdateResult("screen.packmanager.update_unknown", unavailable));
                } else {
                    callback.accept(new UpdateResult("screen.packmanager.up_to_date", 0));
                }
            });
        });
    }

    private static boolean isNewer(String latest, String installed) {
        try {
            String[] a = latest.split("[.+-]");
            String[] b = installed.split("[.+-]");
            int length = Math.max(a.length, b.length);
            for (int i = 0; i < length; i++) {
                int left = i < a.length ? Integer.parseInt(a[i]) : 0;
                int right = i < b.length ? Integer.parseInt(b[i]) : 0;
                if (left != right) return left > right;
            }
        } catch (NumberFormatException ignored) {
            return false;
        }
        return false;
    }
}
